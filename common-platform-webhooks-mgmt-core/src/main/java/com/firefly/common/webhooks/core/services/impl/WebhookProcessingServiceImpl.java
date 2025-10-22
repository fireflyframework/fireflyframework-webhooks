/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.webhooks.core.services.impl;

import com.firefly.common.eda.publisher.EventPublisher;
import com.firefly.common.eda.publisher.EventPublisherFactory;
import com.firefly.common.webhooks.core.domain.events.WebhookReceivedEvent;
import com.firefly.common.webhooks.core.mappers.WebhookEventMapper;
import com.firefly.common.webhooks.core.services.WebhookProcessingService;
import com.firefly.common.webhooks.interfaces.dto.WebhookEventDTO;
import com.firefly.common.webhooks.interfaces.dto.WebhookResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of webhook processing service.
 * <p>
 * This service publishes webhook events directly to the message queue using lib-common-eda.
 * Workers consuming from the queue will handle signature validation and idempotency.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookProcessingServiceImpl implements WebhookProcessingService {

    private final EventPublisherFactory eventPublisherFactory;
    private final WebhookEventMapper webhookEventMapper;

    @Value("${firefly.webhooks.destination.prefix:}")
    private String destinationPrefix;

    @Value("${firefly.webhooks.destination.suffix:}")
    private String destinationSuffix;

    @Value("${firefly.webhooks.destination.use-provider-as-topic:true}")
    private boolean useProviderAsTopic;

    @Value("${firefly.webhooks.destination.custom:}")
    private String customDestination;

    @Override
    public Mono<WebhookResponseDTO> processWebhook(WebhookEventDTO eventDto) {
        log.info("Processing webhook from provider: {}", eventDto.getProviderName());

        // Convert DTO to domain event
        WebhookReceivedEvent domainEvent = webhookEventMapper.toDomainEvent(eventDto);

        // Publish directly to queue
        return publishToQueue(domainEvent)
                .then(Mono.just(WebhookResponseDTO.success(
                        eventDto.getEventId(),
                        eventDto.getProviderName()
                )))
                .doOnSuccess(response ->
                        log.info("Successfully processed webhook {} from provider: {}",
                                eventDto.getEventId(), eventDto.getProviderName()))
                .onErrorResume(error -> {
                    log.error("Error processing webhook from provider: {}",
                            eventDto.getProviderName(), error);
                    return Mono.just(WebhookResponseDTO.error(
                            eventDto.getEventId(),
                            eventDto.getProviderName(),
                            "Error processing webhook: " + error.getMessage()
                    ));
                });
    }

    /**
     * Publishes the webhook event to the message queue.
     * <p>
     * The event is published AS-IS to preserve the original webhook payload
     * and metadata for downstream consumers.
     * <p>
     * Destination resolution strategy:
     * 1. If custom destination is set, use it
     * 2. If use-provider-as-topic is true (default), use provider name as topic
     * 3. Apply prefix and suffix if configured
     * <p>
     * Examples:
     * - Default: "stripe" (provider name only)
     * - With prefix: "webhooks.stripe" (prefix="webhooks.")
     * - With suffix: "stripe.received" (suffix=".received")
     * - Both: "webhooks.stripe.received" (prefix="webhooks.", suffix=".received")
     * - Custom: "custom.topic" (custom="custom.topic")
     *
     * @param event the webhook event
     * @return a Mono that completes when published
     */
    private Mono<Void> publishToQueue(WebhookReceivedEvent event) {
        // Resolve destination based on configuration
        String destination = resolveDestination(event.getProviderName());

        // Build headers with metadata
        Map<String, Object> headers = new HashMap<>();
        headers.put("provider", event.getProviderName());
        headers.put("eventId", event.getEventId().toString());
        headers.put("receivedAt", event.getReceivedAt().toString());
        
        if (event.getCorrelationId() != null) {
            headers.put("correlationId", event.getCorrelationId());
        }

        log.debug("Publishing webhook event to destination: {}", destination);

        // Get default publisher from factory
        EventPublisher publisher = eventPublisherFactory.getDefaultPublisher();
        
        if (publisher == null) {
            log.error("No event publisher available");
            return Mono.error(new IllegalStateException("No event publisher available"));
        }

        // Publish the event AS-IS to the message queue
        return publisher.publish(event, destination, headers)
                .doOnSuccess(v -> log.debug("Successfully published webhook event: {}", event.getEventId()))
                .doOnError(error -> log.error("Error publishing webhook event: {}", event.getEventId(), error));
    }

    /**
     * Resolves the destination/topic name based on configuration.
     *
     * @param providerName the provider name
     * @return the resolved destination
     */
    private String resolveDestination(String providerName) {
        // If custom destination is set, use it directly
        if (customDestination != null && !customDestination.isEmpty()) {
            log.debug("Using custom destination: {}", customDestination);
            return customDestination;
        }

        // Build destination with provider name (default behavior)
        StringBuilder destination = new StringBuilder();

        // Add prefix if configured
        if (destinationPrefix != null && !destinationPrefix.isEmpty()) {
            destination.append(destinationPrefix);
        }

        // Add provider name if use-provider-as-topic is true (default)
        if (useProviderAsTopic) {
            destination.append(providerName);
        }

        // Add suffix if configured
        if (destinationSuffix != null && !destinationSuffix.isEmpty()) {
            destination.append(destinationSuffix);
        }

        String resolvedDestination = destination.toString();
        log.debug("Resolved destination for provider {}: {}", providerName, resolvedDestination);
        
        return resolvedDestination;
    }
}
