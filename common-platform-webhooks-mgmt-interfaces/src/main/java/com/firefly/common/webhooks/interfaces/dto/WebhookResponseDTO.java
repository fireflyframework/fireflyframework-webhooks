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

package com.firefly.common.webhooks.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing the response after receiving a webhook.
 * <p>
 * This is returned to the webhook provider to acknowledge receipt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after webhook event is received")
public class WebhookResponseDTO {

    @Schema(description = "Unique identifier for the processed webhook event", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID eventId;

    @Schema(description = "Status of the webhook processing", example = "ACCEPTED")
    private String status;

    @Schema(description = "Message describing the result", example = "Webhook received and queued for processing")
    private String message;

    @Schema(description = "Timestamp when the webhook was processed")
    private Instant processedAt;

    @Schema(description = "Provider name for reference", example = "stripe")
    private String providerName;

    /**
     * Creates a success response.
     *
     * @param eventId the event ID
     * @param providerName the provider name
     * @return a success response
     */
    public static WebhookResponseDTO success(UUID eventId, String providerName) {
        return WebhookResponseDTO.builder()
                .eventId(eventId)
                .status("ACCEPTED")
                .message("Webhook received and queued for processing")
                .processedAt(Instant.now())
                .providerName(providerName)
                .build();
    }

    /**
     * Creates an error response.
     *
     * @param eventId the event ID (may be null)
     * @param providerName the provider name
     * @param errorMessage the error message
     * @return an error response
     */
    public static WebhookResponseDTO error(UUID eventId, String providerName, String errorMessage) {
        return WebhookResponseDTO.builder()
                .eventId(eventId)
                .status("ERROR")
                .message(errorMessage)
                .processedAt(Instant.now())
                .providerName(providerName)
                .build();
    }
}
