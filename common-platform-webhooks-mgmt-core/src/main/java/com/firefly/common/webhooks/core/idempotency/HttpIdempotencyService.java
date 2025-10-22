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

package com.firefly.common.webhooks.core.idempotency;

import com.firefly.common.cache.manager.FireflyCacheManager;
import com.firefly.common.webhooks.interfaces.dto.WebhookResponseDTO;
import com.firefly.common.webhooks.core.config.WebhookSecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * Service for HTTP-level idempotency using X-Idempotency-Key header.
 * Caches webhook responses by idempotency key to prevent duplicate processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HttpIdempotencyService {

    private static final String IDEMPOTENCY_KEY_PREFIX = "webhook:http:idempotency:";

    private final FireflyCacheManager fireflyCacheManager;
    private final WebhookSecurityProperties securityProperties;

    /**
     * Checks if a request with the given idempotency key has already been processed.
     *
     * @param idempotencyKey the idempotency key from X-Idempotency-Key header
     * @return Mono containing the cached response if exists, empty otherwise
     */
    public Mono<Optional<WebhookResponseDTO>> getCachedResponse(String idempotencyKey) {
        if (!securityProperties.isEnableHttpIdempotency() || idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.just(Optional.empty());
        }

        String cacheKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;

        return fireflyCacheManager.get(cacheKey, WebhookResponseDTO.class)
                .doOnNext(cached -> {
                    if (cached.isPresent()) {
                        log.info("Found cached response for idempotency key: {}", idempotencyKey);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error retrieving cached response for idempotency key {}: {}",
                            idempotencyKey, error.getMessage(), error);
                    return Mono.just(Optional.empty());
                });
    }

    /**
     * Caches a webhook response by idempotency key.
     *
     * @param idempotencyKey the idempotency key from X-Idempotency-Key header
     * @param response the webhook response to cache
     * @return Mono that completes when caching is done
     */
    public Mono<Void> cacheResponse(String idempotencyKey, WebhookResponseDTO response) {
        if (!securityProperties.isEnableHttpIdempotency() || idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.empty();
        }

        String cacheKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        Duration ttl = Duration.ofSeconds(securityProperties.getHttpIdempotencyTtlSeconds());

        log.debug("Caching response for idempotency key: {} with TTL: {}", idempotencyKey, ttl);

        return fireflyCacheManager.put(cacheKey, response, ttl)
                .doOnSuccess(v -> log.debug("Successfully cached response for idempotency key: {}", idempotencyKey))
                .doOnError(error -> log.error("Error caching response for idempotency key {}: {}",
                        idempotencyKey, error.getMessage(), error))
                .onErrorResume(error -> Mono.empty()); // Don't fail the request if caching fails
    }

    /**
     * Extracts the idempotency key from the request header.
     *
     * @param idempotencyKeyHeader the value of X-Idempotency-Key header
     * @return the idempotency key, or null if not present
     */
    public String extractIdempotencyKey(String idempotencyKeyHeader) {
        if (idempotencyKeyHeader == null || idempotencyKeyHeader.isBlank()) {
            return null;
        }
        return idempotencyKeyHeader.trim();
    }
}

