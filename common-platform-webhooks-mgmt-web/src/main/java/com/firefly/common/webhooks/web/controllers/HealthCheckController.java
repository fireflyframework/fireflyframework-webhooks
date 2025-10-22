package com.firefly.common.webhooks.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.firefly.common.cache.manager.FireflyCacheManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Health check controller for testing service connectivity.
 */
@RestController
@RequestMapping("/api/v1/health")
@Slf4j
@Tag(name = "Health Check", description = "Endpoints for testing service connectivity")
public class HealthCheckController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FireflyCacheManager cacheManager;
    private final ObjectMapper objectMapper;

    @Autowired
    public HealthCheckController(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            FireflyCacheManager cacheManager,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/status")
    @Operation(summary = "Get service status", description = "Returns the status of all connected services")
    public Mono<ObjectNode> getStatus() {
        return Mono.fromCallable(() -> {
            ObjectNode status = objectMapper.createObjectNode();
            status.put("timestamp", Instant.now().toString());
            status.put("application", "common-platform-webhooks-mgmt");
            status.put("status", "UP");

            // Cache status
            ObjectNode cacheStatus = objectMapper.createObjectNode();
            try {
                if (cacheManager != null) {
                    cacheStatus.put("available", true);
                    cacheStatus.put("type", cacheManager.getCacheType().toString());
                    cacheStatus.put("name", cacheManager.getCacheName());
                } else {
                    cacheStatus.put("available", false);
                    cacheStatus.put("error", "Cache manager not found");
                }
            } catch (Exception e) {
                cacheStatus.put("available", false);
                cacheStatus.put("error", e.getMessage());
            }
            status.set("cache", cacheStatus);

            // Kafka status (basic check - just verify template exists)
            ObjectNode kafkaStatus = objectMapper.createObjectNode();
            kafkaStatus.put("configured", kafkaTemplate != null);
            status.set("kafka", kafkaStatus);

            return status;
        });
    }

    @PostMapping("/test-kafka")
    @Operation(summary = "Test Kafka connectivity", description = "Sends a test message to Kafka to verify connectivity")
    public Mono<ObjectNode> testKafka() {
        return Mono.fromCallable(() -> {
            ObjectNode result = objectMapper.createObjectNode();
            String testTopic = "health-check-test";
            String testMessage = "Test message at " + Instant.now();
            String messageId = UUID.randomUUID().toString();

            try {
                if (kafkaTemplate == null) {
                    result.put("success", false);
                    result.put("error", "KafkaTemplate not available");
                    return result;
                }

                log.info("Sending test message to Kafka topic: {}", testTopic);

                // Send message and wait for result
                CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>> future =
                    kafkaTemplate.send(testTopic, messageId, testMessage);

                // Wait for up to 5 seconds
                var sendResult = future.get(5, java.util.concurrent.TimeUnit.SECONDS);

                result.put("success", true);
                result.put("topic", testTopic);
                result.put("messageId", messageId);
                result.put("partition", sendResult.getRecordMetadata().partition());
                result.put("offset", sendResult.getRecordMetadata().offset());
                result.put("timestamp", sendResult.getRecordMetadata().timestamp());

                log.info("Successfully sent test message to Kafka: topic={}, partition={}, offset={}",
                    testTopic,
                    sendResult.getRecordMetadata().partition(),
                    sendResult.getRecordMetadata().offset());
                
            } catch (Exception e) {
                log.error("Failed to send test message to Kafka", e);
                result.put("success", false);
                result.put("error", e.getMessage());
                result.put("errorType", e.getClass().getSimpleName());
            }

            return result;
        });
    }

    @PostMapping("/test-cache")
    @Operation(summary = "Test cache connectivity", description = "Tests cache read/write operations")
    public Mono<ObjectNode> testCache() {
        if (cacheManager == null) {
            return Mono.fromCallable(() -> {
                ObjectNode result = objectMapper.createObjectNode();
                result.put("success", false);
                result.put("error", "Cache manager not found");
                return result;
            });
        }

        String testKey = "health-check-test-" + UUID.randomUUID();
        String testValue = "Test value at " + Instant.now();

        log.info("Testing cache operations with key: {}", testKey);

        // Test write, read, and delete in a reactive chain
        return cacheManager.put(testKey, testValue, Duration.ofMinutes(1))
            .doOnSuccess(v -> log.info("Successfully wrote to cache"))
            .then(cacheManager.get(testKey, String.class))
            .doOnSuccess(v -> log.info("Successfully read from cache: {}", v))
            .flatMap(retrievedOptional -> {
                String retrievedValue = retrievedOptional.isPresent() ? retrievedOptional.get() : null;

                // Test delete
                return cacheManager.evict(testKey)
                    .doOnSuccess(v -> log.info("Successfully deleted from cache"))
                    .thenReturn(retrievedValue);
            })
            .map(retrievedValue -> {
                ObjectNode result = objectMapper.createObjectNode();
                result.put("success", true);
                result.put("cacheType", cacheManager.getCacheType().toString());
                result.put("cacheName", cacheManager.getCacheName());
                result.put("writeSuccess", true);
                result.put("readSuccess", testValue.equals(retrievedValue));
                result.put("deleteSuccess", true);
                return result;
            })
            .onErrorResume(e -> {
                log.error("Failed to test cache operations", e);
                return Mono.fromCallable(() -> {
                    ObjectNode result = objectMapper.createObjectNode();
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    result.put("errorType", e.getClass().getSimpleName());
                    return result;
                });
            });
    }
}

