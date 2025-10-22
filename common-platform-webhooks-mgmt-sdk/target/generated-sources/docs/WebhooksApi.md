# WebhooksApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**receiveWebhook**](WebhooksApi.md#receiveWebhook) | **POST** /api/v1/webhook/{providerName} | Receive webhook from any provider |



## receiveWebhook

> WebhookResponseDTO receiveWebhook(providerName, body)

Receive webhook from any provider

Universal endpoint that accepts webhooks from any provider. The webhook payload is stored AS-IS and published to a message queue for processing.

### Example

```java
// Import classes:
import com.firefly.common.webhooks.sdk.invoker.ApiClient;
import com.firefly.common.webhooks.sdk.invoker.ApiException;
import com.firefly.common.webhooks.sdk.invoker.Configuration;
import com.firefly.common.webhooks.sdk.invoker.models.*;
import com.firefly.common.webhooks.sdk.api.WebhooksApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        WebhooksApi apiInstance = new WebhooksApi(defaultClient);
        String providerName = "stripe"; // String | Name of the webhook provider
        Object body = null; // Object | 
        try {
            WebhookResponseDTO result = apiInstance.receiveWebhook(providerName, body);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling WebhooksApi#receiveWebhook");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **providerName** | **String**| Name of the webhook provider | |
| **body** | **Object**|  | |

### Return type

[**WebhookResponseDTO**](WebhookResponseDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Webhook accepted and queued for processing |  -  |
| **400** | Invalid webhook payload |  -  |
| **500** | Internal server error |  -  |

