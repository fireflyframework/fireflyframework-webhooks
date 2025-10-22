

# WebhookResponseDTO

Response after webhook event is received

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**eventId** | **UUID** | Unique identifier for the processed webhook event |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) | Status of the webhook processing |  [optional] |
|**message** | **String** | Message describing the result |  [optional] |
|**receivedAt** | **LocalDateTime** | Timestamp when the webhook was received by the platform |  [optional] |
|**processedAt** | **LocalDateTime** | Timestamp when the webhook was processed and acknowledged |  [optional] |
|**providerName** | **String** | Provider name for reference |  [optional] |
|**receivedPayload** | **Object** | Echo of the received payload for verification purposes |  [optional] |
|**metadata** | **Map&lt;String, Object&gt;** | Metadata about the webhook processing |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| ACCEPTED | &quot;ACCEPTED&quot; |
| ERROR | &quot;ERROR&quot; |
| REJECTED | &quot;REJECTED&quot; |



