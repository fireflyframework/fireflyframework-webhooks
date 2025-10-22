package com.firefly.common.webhooks.core.mappers;

import com.firefly.common.webhooks.core.domain.events.WebhookReceivedEvent;
import com.firefly.common.webhooks.interfaces.dto.WebhookEventDTO;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-22T12:59:38+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Homebrew)"
)
@Component
public class WebhookEventMapperImpl implements WebhookEventMapper {

    @Override
    public WebhookReceivedEvent toDomainEvent(WebhookEventDTO dto) {
        if ( dto == null ) {
            return null;
        }

        WebhookReceivedEvent.WebhookReceivedEventBuilder webhookReceivedEvent = WebhookReceivedEvent.builder();

        webhookReceivedEvent.eventId( dto.getEventId() );
        webhookReceivedEvent.providerName( dto.getProviderName() );
        webhookReceivedEvent.payload( dto.getPayload() );
        Map<String, String> map = dto.getHeaders();
        if ( map != null ) {
            webhookReceivedEvent.headers( new LinkedHashMap<String, String>( map ) );
        }
        Map<String, String> map1 = dto.getQueryParams();
        if ( map1 != null ) {
            webhookReceivedEvent.queryParams( new LinkedHashMap<String, String>( map1 ) );
        }
        webhookReceivedEvent.receivedAt( dto.getReceivedAt() );
        webhookReceivedEvent.correlationId( dto.getCorrelationId() );
        webhookReceivedEvent.sourceIp( dto.getSourceIp() );
        webhookReceivedEvent.httpMethod( dto.getHttpMethod() );

        return webhookReceivedEvent.build();
    }

    @Override
    public WebhookEventDTO toDto(WebhookReceivedEvent event) {
        if ( event == null ) {
            return null;
        }

        WebhookEventDTO.WebhookEventDTOBuilder webhookEventDTO = WebhookEventDTO.builder();

        webhookEventDTO.eventId( event.getEventId() );
        webhookEventDTO.providerName( event.getProviderName() );
        webhookEventDTO.payload( event.getPayload() );
        Map<String, String> map = event.getHeaders();
        if ( map != null ) {
            webhookEventDTO.headers( new LinkedHashMap<String, String>( map ) );
        }
        Map<String, String> map1 = event.getQueryParams();
        if ( map1 != null ) {
            webhookEventDTO.queryParams( new LinkedHashMap<String, String>( map1 ) );
        }
        webhookEventDTO.receivedAt( event.getReceivedAt() );
        webhookEventDTO.correlationId( event.getCorrelationId() );
        webhookEventDTO.sourceIp( event.getSourceIp() );
        webhookEventDTO.httpMethod( event.getHttpMethod() );

        return webhookEventDTO.build();
    }
}
