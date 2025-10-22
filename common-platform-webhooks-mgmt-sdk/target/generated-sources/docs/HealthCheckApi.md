# HealthCheckApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getStatus**](HealthCheckApi.md#getStatus) | **GET** /api/v1/health/status | Get service status |
| [**testCache**](HealthCheckApi.md#testCache) | **POST** /api/v1/health/test-cache | Test cache connectivity |
| [**testKafka**](HealthCheckApi.md#testKafka) | **POST** /api/v1/health/test-kafka | Test Kafka connectivity |



## getStatus

> Object getStatus()

Get service status

Returns the status of all connected services

### Example

```java
// Import classes:
import com.firefly.common.webhooks.sdk.invoker.ApiClient;
import com.firefly.common.webhooks.sdk.invoker.ApiException;
import com.firefly.common.webhooks.sdk.invoker.Configuration;
import com.firefly.common.webhooks.sdk.invoker.models.*;
import com.firefly.common.webhooks.sdk.api.HealthCheckApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        HealthCheckApi apiInstance = new HealthCheckApi(defaultClient);
        try {
            Object result = apiInstance.getStatus();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling HealthCheckApi#getStatus");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## testCache

> Object testCache()

Test cache connectivity

Tests cache read/write operations

### Example

```java
// Import classes:
import com.firefly.common.webhooks.sdk.invoker.ApiClient;
import com.firefly.common.webhooks.sdk.invoker.ApiException;
import com.firefly.common.webhooks.sdk.invoker.Configuration;
import com.firefly.common.webhooks.sdk.invoker.models.*;
import com.firefly.common.webhooks.sdk.api.HealthCheckApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        HealthCheckApi apiInstance = new HealthCheckApi(defaultClient);
        try {
            Object result = apiInstance.testCache();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling HealthCheckApi#testCache");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## testKafka

> Object testKafka()

Test Kafka connectivity

Sends a test message to Kafka to verify connectivity

### Example

```java
// Import classes:
import com.firefly.common.webhooks.sdk.invoker.ApiClient;
import com.firefly.common.webhooks.sdk.invoker.ApiException;
import com.firefly.common.webhooks.sdk.invoker.Configuration;
import com.firefly.common.webhooks.sdk.invoker.models.*;
import com.firefly.common.webhooks.sdk.api.HealthCheckApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        HealthCheckApi apiInstance = new HealthCheckApi(defaultClient);
        try {
            Object result = apiInstance.testKafka();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling HealthCheckApi#testKafka");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

