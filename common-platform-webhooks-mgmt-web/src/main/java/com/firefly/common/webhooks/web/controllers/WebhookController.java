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

package com.firefly.common.webhooks.web.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.firefly.common.webhooks.core.services.WebhookProcessingService;
import com.firefly.common.webhooks.interfaces.dto.WebhookEventDTO;
import com.firefly.common.webhooks.interfaces.dto.WebhookResponseDTO;
import com.firefly.common.webhooks.core.idempotency.HttpIdempotencyService;
import com.firefly.common.webhooks.core.metrics.WebhookMetricsService;
import com.firefly.common.webhooks.core.validation.WebhookValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for receiving webhooks from multiple providers.
 * <p>
 * This controller provides a dynamic endpoint that accepts webhooks from any
 * provider without requiring code changes. The provider name is captured from
 * the URL path and the webhook payload is accepted AS-IS.
 * <p>
 * <b>Important:</b> This controller does NOT validate webhook signatures.
 * Signature validation is performed by the worker/processor that consumes
 * events from the message queue. This allows:
 * <ul>
 *   <li>Headers to be stored in the event store for audit trail</li>
 *   <li>Workers to validate signatures when processing</li>
 *   <li>Replay capability with signature re-validation</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Webhook ingestion endpoints for multiple providers")
public class WebhookController {

    private final WebhookProcessingService webhookProcessingService;
    private final WebhookValidator webhookValidator;
    private final HttpIdempotencyService httpIdempotencyService;
    private final WebhookMetricsService metricsService;
    private final com.firefly.common.webhooks.core.ratelimit.WebhookRateLimitService rateLimitService;

    /**
     * Universal webhook endpoint that accepts webhooks from any provider.
     * <p>
     * The endpoint accepts:
     * - Dynamic provider name in the URL path (e.g., /api/v1/webhook/stripe)
     * - Any JSON payload in the request body
     * - All HTTP headers for signature validation
     * - Query parameters if needed
     * <p>
     * Examples:
     * - POST /api/v1/webhook/stripe
     * - POST /api/v1/webhook/paypal
     * - POST /api/v1/webhook/twilio
     * - POST /api/v1/webhook/custom-provider
     *
     * @param providerName the name of the webhook provider
     * @param payload the raw webhook payload as JSON
     * @param request the HTTP request for extracting headers and metadata
     * @param queryParams query parameters from the request
     * @return a Mono containing the webhook response
     */
    @PostMapping(
            value = "/{providerName}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary = "Receive webhook from any provider",
            description = "Universal endpoint that accepts webhooks from any provider. " +
                    "The webhook payload is stored AS-IS and published to a message queue for processing."
    )
    @ApiResponse(
            responseCode = "202",
            description = "Webhook accepted and queued for processing",
            content = @Content(schema = @Schema(implementation = WebhookResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid webhook payload"
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
    )
    public Mono<WebhookResponseDTO> receiveWebhook(
            @Parameter(description = "Name of the webhook provider", example = "stripe", required = true)
            @PathVariable String providerName,
            @Parameter(description = "Raw webhook payload", required = true)
            @RequestBody JsonNode payload,
            ServerHttpRequest request,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        Instant startTime = Instant.now();
        UUID eventId = UUID.randomUUID();

        // Set MDC for structured logging
        MDC.put("eventId", eventId.toString());
        MDC.put("provider", providerName);

        log.info("Received webhook from provider: {}", providerName);

        // Record metrics
        metricsService.recordWebhookReceived(providerName);

        // Apply rate limiting
        String sourceIp = extractSourceIp(request);

        return rateLimitService.executeWithRateLimit(providerName,
                rateLimitService.executeWithIpRateLimit(sourceIp,
                        Mono.defer(() -> {
                            try {
                                // 1. Validate request
                                webhookValidator.validateRequest(providerName, payload, request);

                                // 2. Check HTTP-level idempotency
                                String extractedIdempotencyKey = httpIdempotencyService.extractIdempotencyKey(idempotencyKey);
                                if (extractedIdempotencyKey != null) {
                                    return httpIdempotencyService.getCachedResponse(extractedIdempotencyKey)
                                            .flatMap(cachedResponse -> {
                                                if (cachedResponse.isPresent()) {
                                                    log.info("Returning cached response for idempotency key: {}", extractedIdempotencyKey);
                                                    metricsService.recordDuplicateWebhook(providerName);
                                                    return Mono.just(cachedResponse.get());
                                                }
                                                return processNewWebhook(providerName, payload, request, queryParams,
                                                        eventId, startTime, extractedIdempotencyKey);
                                            });
                                }

                                return processNewWebhook(providerName, payload, request, queryParams,
                                        eventId, startTime, null);

                            } catch (ResponseStatusException e) {
                                // Validation failed
                                metricsService.recordWebhookRejected(providerName, e.getReason());
                                return Mono.error(e);
                            }
                        })
                )
        )
        .doFinally(signal -> {
            MDC.remove("eventId");
            MDC.remove("provider");
        });
    }

    /**
     * Processes a new webhook (not cached).
     */
    private Mono<WebhookResponseDTO> processNewWebhook(
            String providerName,
            JsonNode payload,
            ServerHttpRequest request,
            MultiValueMap<String, String> queryParams,
            UUID eventId,
            Instant startTime,
            String idempotencyKey) {

        // Extract headers - these will be stored in event store and passed to workers
        Map<String, String> headers = extractHeaders(request);

        // Record payload size
        long payloadSize = payload.toString().getBytes().length;
        metricsService.recordPayloadSize(providerName, payloadSize);

        // Build webhook event DTO
        WebhookEventDTO eventDto = WebhookEventDTO.builder()
                .eventId(eventId)
                .providerName(providerName.toLowerCase())
                .payload(payload)
                .headers(headers)
                .queryParams(extractQueryParams(queryParams))
                .receivedAt(startTime)
                .sourceIp(extractSourceIp(request))
                .httpMethod(request.getMethod().name())
                .build();

        // Process the webhook
        return webhookProcessingService.processWebhook(eventDto)
                .flatMap(response -> {
                    // Cache response if idempotency key provided
                    if (idempotencyKey != null) {
                        return httpIdempotencyService.cacheResponse(idempotencyKey, response)
                                .thenReturn(response);
                    }
                    return Mono.just(response);
                })
                .doOnSuccess(response -> {
                    log.info("Webhook processed successfully: {} from provider: {}", eventId, providerName);
                    metricsService.recordWebhookPublished(providerName);
                    metricsService.recordProcessingTime(providerName, startTime);
                })
                .doOnError(error -> {
                    log.error("Error processing webhook from provider: {}", providerName, error);
                    String errorType = error.getClass().getSimpleName();
                    metricsService.recordWebhookFailed(providerName, errorType);
                });
    }

    /**
     * Extracts HTTP headers from the request.
     * <p>
     * Headers are stored in the event store and passed to workers for signature validation.
     * This allows workers to validate signatures when processing events from the queue.
     *
     * @param request the HTTP request
     * @return map of header names to values
     */
    private Map<String, String> extractHeaders(ServerHttpRequest request) {
        Map<String, String> headers = new HashMap<>();
        request.getHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0)); // Take first value if multiple
            }
        });
        return headers;
    }

    /**
     * Extracts query parameters from the request.
     *
     * @param queryParams the query parameters
     * @return map of parameter names to values
     */
    private Map<String, String> extractQueryParams(MultiValueMap<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return Map.of();
        }

        Map<String, String> params = new HashMap<>();
        queryParams.forEach((name, values) -> {
            if (!values.isEmpty()) {
                params.put(name, values.get(0)); // Take first value if multiple
            }
        });
        return params;
    }

    /**
     * Extracts the source IP address from the request.
     * <p>
     * Handles X-Forwarded-For header for proxied requests.
     *
     * @param request the HTTP request
     * @return the source IP address
     */
    private String extractSourceIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }
}
