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

package com.firefly.common.webhooks.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.common.cache.manager.FireflyCacheManager;
import com.firefly.common.webhooks.integration.support.StripeSignatureValidator;
import com.firefly.common.webhooks.integration.support.TestStripeWebhookListener;
import com.firefly.common.webhooks.integration.support.TestStripeWebhookProcessor;
import com.firefly.common.webhooks.processor.idempotency.CacheBasedWebhookIdempotencyService;
import com.firefly.common.webhooks.processor.port.WebhookIdempotencyService;
import com.firefly.common.webhooks.processor.port.WebhookSignatureValidator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for webhook integration tests.
 * <p>
 * Provides test-specific beans including:
 * - Test webhook processor that tracks processed events
 * - Test webhook listener that consumes from Kafka
 * - Stripe signature validator for testing
 * - Cache-based idempotency service
 */
@TestConfiguration
public class WebhookIntegrationTestConfiguration {

    private static final String STRIPE_SECRET = "whsec_test_secret_key_12345";

    @Bean
    public TestStripeWebhookProcessor testStripeWebhookProcessor() {
        return new TestStripeWebhookProcessor();
    }

    @Bean
    public StripeSignatureValidator stripeSignatureValidator() {
        return new StripeSignatureValidator(STRIPE_SECRET);
    }

    @Bean
    public WebhookIdempotencyService webhookIdempotencyService(FireflyCacheManager cacheManager) {
        return new CacheBasedWebhookIdempotencyService(cacheManager);
    }

    @Bean
    public TestStripeWebhookListener testStripeWebhookListener(
            TestStripeWebhookProcessor processor,
            ObjectMapper objectMapper,
            StripeSignatureValidator signatureValidator,
            WebhookIdempotencyService idempotencyService) {
        return new TestStripeWebhookListener(processor, objectMapper, signatureValidator, idempotencyService);
    }
}

