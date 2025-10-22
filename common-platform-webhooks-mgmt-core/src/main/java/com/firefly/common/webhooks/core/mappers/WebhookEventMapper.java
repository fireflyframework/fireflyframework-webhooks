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

package com.firefly.common.webhooks.core.mappers;

import com.firefly.common.webhooks.core.domain.events.WebhookReceivedEvent;
import com.firefly.common.webhooks.interfaces.dto.WebhookEventDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for converting between webhook DTOs and domain events.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WebhookEventMapper {

    /**
     * Converts a WebhookEventDto to a WebhookReceivedEvent.
     *
     * @param dto the DTO
     * @return the domain event
     */
    WebhookReceivedEvent toDomainEvent(WebhookEventDTO dto);

    /**
     * Converts a WebhookReceivedEvent to a WebhookEventDto.
     *
     * @param event the domain event
     * @return the DTO
     */
    WebhookEventDTO toDto(WebhookReceivedEvent event);
}
