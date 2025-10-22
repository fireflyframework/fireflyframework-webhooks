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
 * DTO for filtering webhook events from the event store.
 * <p>
 * This DTO is used with FilterRequest to query webhook events
 * with various criteria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter criteria for querying webhook events")
public class WebhookEventFilterDTO {

    @Schema(description = "Filter by specific webhook event ID")
    private UUID aggregateId;

    @Schema(description = "Filter by provider name (e.g., stripe, github)")
    private String provider;

    @Schema(description = "Filter by event type")
    private String eventType;

    @Schema(description = "Filter events from this timestamp (inclusive)")
    private Instant fromTimestamp;

    @Schema(description = "Filter events until this timestamp (inclusive)")
    private Instant toTimestamp;
}

