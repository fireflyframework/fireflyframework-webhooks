/*
 * Copyright 2024-2026 Firefly Software Solutions Inc
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

package org.fireflyframework.webhooks.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for recording webhook metrics using Micrometer.
 */
@Service
@Slf4j
public class WebhookMetricsService {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> receivedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> publishedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> failedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> rejectedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> duplicateCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> processingTimers = new ConcurrentHashMap<>();

    public WebhookMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Records a webhook received event.
     *
     * @param providerName the provider name
     */
    public void recordWebhookReceived(String providerName) {
        getReceivedCounter(providerName).increment();
        log.debug("Recorded webhook received for provider: {}", providerName);
    }

    /**
     * Records a webhook published to Kafka event.
     *
     * @param providerName the provider name
     */
    public void recordWebhookPublished(String providerName) {
        getPublishedCounter(providerName).increment();
        log.debug("Recorded webhook published for provider: {}", providerName);
    }

    /**
     * Records a webhook processing failure.
     *
     * @param providerName the provider name
     * @param errorType the type of error (e.g., "kafka_error", "validation_error")
     */
    public void recordWebhookFailed(String providerName, String errorType) {
        getFailedCounter(providerName, errorType).increment();
        log.debug("Recorded webhook failure for provider: {}, error: {}", providerName, errorType);
    }

    /**
     * Records a webhook rejection (validation failed, rate limited, etc.).
     *
     * @param providerName the provider name
     * @param reason the rejection reason
     */
    public void recordWebhookRejected(String providerName, String reason) {
        getRejectedCounter(providerName, reason).increment();
        log.debug("Recorded webhook rejection for provider: {}, reason: {}", providerName, reason);
    }

    /**
     * Records a duplicate webhook (HTTP idempotency).
     *
     * @param providerName the provider name
     */
    public void recordDuplicateWebhook(String providerName) {
        getDuplicateCounter(providerName).increment();
        log.debug("Recorded duplicate webhook for provider: {}", providerName);
    }

    /**
     * Records webhook processing time.
     *
     * @param providerName the provider name
     * @param startTime the start time
     */
    public void recordProcessingTime(String providerName, Instant startTime) {
        Duration duration = Duration.between(startTime, Instant.now());
        getProcessingTimer(providerName).record(duration);
        log.debug("Recorded processing time for provider: {} - {}ms", providerName, duration.toMillis());
    }

    /**
     * Records payload size.
     *
     * @param providerName the provider name
     * @param sizeBytes the payload size in bytes
     */
    public void recordPayloadSize(String providerName, long sizeBytes) {
        meterRegistry.summary("webhooks.payload.size",
                        "provider", providerName)
                .record(sizeBytes);
    }

    // Counter getters with lazy initialization

    private Counter getReceivedCounter(String providerName) {
        return receivedCounters.computeIfAbsent(providerName, provider ->
                Counter.builder("webhooks.received")
                        .description("Total number of webhooks received")
                        .tag("provider", provider)
                        .register(meterRegistry)
        );
    }

    private Counter getPublishedCounter(String providerName) {
        return publishedCounters.computeIfAbsent(providerName, provider ->
                Counter.builder("webhooks.published")
                        .description("Total number of webhooks published to Kafka")
                        .tag("provider", provider)
                        .register(meterRegistry)
        );
    }

    private Counter getFailedCounter(String providerName, String errorType) {
        String key = providerName + ":" + errorType;
        return failedCounters.computeIfAbsent(key, k ->
                Counter.builder("webhooks.failed")
                        .description("Total number of webhook processing failures")
                        .tag("provider", providerName)
                        .tag("error_type", errorType)
                        .register(meterRegistry)
        );
    }

    private Counter getRejectedCounter(String providerName, String reason) {
        String key = providerName + ":" + reason;
        return rejectedCounters.computeIfAbsent(key, k ->
                Counter.builder("webhooks.rejected")
                        .description("Total number of webhooks rejected")
                        .tag("provider", providerName)
                        .tag("reason", reason)
                        .register(meterRegistry)
        );
    }

    private Counter getDuplicateCounter(String providerName) {
        return duplicateCounters.computeIfAbsent(providerName, provider ->
                Counter.builder("webhooks.duplicates")
                        .description("Total number of duplicate webhooks (HTTP idempotency)")
                        .tag("provider", provider)
                        .register(meterRegistry)
        );
    }

    private Timer getProcessingTimer(String providerName) {
        return processingTimers.computeIfAbsent(providerName, provider ->
                Timer.builder("webhooks.processing.time")
                        .description("Webhook processing time from receipt to Kafka publish")
                        .tag("provider", provider)
                        .register(meterRegistry)
        );
    }
}

