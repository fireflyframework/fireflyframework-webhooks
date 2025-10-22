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
 * DTO representing a webhook event query result from the event store.
 * <p>
 * This DTO contains the essential information about a webhook event
 * for query and display purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Webhook event query result")
public class WebhookEventQueryDTO {

    @Schema(description = "Unique identifier for the webhook event")
    private UUID aggregateId;

    @Schema(description = "Provider name (e.g., stripe, github)")
    private String provider;

    @Schema(description = "Event type")
    private String eventType;

    @Schema(description = "Aggregate type (always 'Webhook')")
    private String aggregateType;

    @Schema(description = "Aggregate version number")
    private Long aggregateVersion;

    @Schema(description = "Global sequence number in the event store")
    private Long globalSequence;

    @Schema(description = "Event data as JSON string")
    private String eventData;

    @Schema(description = "Event metadata as JSON string")
    private String metadata;

    @Schema(description = "Timestamp when the event was created")
    private Instant createdAt;
}

