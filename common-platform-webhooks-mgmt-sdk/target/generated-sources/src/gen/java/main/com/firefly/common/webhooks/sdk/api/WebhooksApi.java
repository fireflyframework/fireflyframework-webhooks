package com.firefly.common.webhooks.sdk.api;

import com.firefly.common.webhooks.sdk.invoker.ApiClient;

import com.firefly.common.webhooks.sdk.model.WebhookResponseDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-10-22T12:59:41.350272+02:00[Europe/Madrid]")
public class WebhooksApi {
    private ApiClient apiClient;

    public WebhooksApi() {
        this(new ApiClient());
    }

    @Autowired
    public WebhooksApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Receive webhook from any provider
     * Universal endpoint that accepts webhooks from any provider. The webhook payload is stored AS-IS and published to a message queue for processing.
     * <p><b>202</b> - Webhook accepted and queued for processing
     * <p><b>400</b> - Invalid webhook payload
     * <p><b>500</b> - Internal server error
     * @param providerName Name of the webhook provider
     * @param body The body parameter
     * @return WebhookResponseDTO
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec receiveWebhookRequestCreation(String providerName, Object body) throws WebClientResponseException {
        Object postBody = body;
        // verify the required parameter 'providerName' is set
        if (providerName == null) {
            throw new WebClientResponseException("Missing the required parameter 'providerName' when calling receiveWebhook", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new WebClientResponseException("Missing the required parameter 'body' when calling receiveWebhook", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("providerName", providerName);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<WebhookResponseDTO> localVarReturnType = new ParameterizedTypeReference<WebhookResponseDTO>() {};
        return apiClient.invokeAPI("/api/v1/webhook/{providerName}", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Receive webhook from any provider
     * Universal endpoint that accepts webhooks from any provider. The webhook payload is stored AS-IS and published to a message queue for processing.
     * <p><b>202</b> - Webhook accepted and queued for processing
     * <p><b>400</b> - Invalid webhook payload
     * <p><b>500</b> - Internal server error
     * @param providerName Name of the webhook provider
     * @param body The body parameter
     * @return WebhookResponseDTO
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<WebhookResponseDTO> receiveWebhook(String providerName, Object body) throws WebClientResponseException {
        ParameterizedTypeReference<WebhookResponseDTO> localVarReturnType = new ParameterizedTypeReference<WebhookResponseDTO>() {};
        return receiveWebhookRequestCreation(providerName, body).bodyToMono(localVarReturnType);
    }

    /**
     * Receive webhook from any provider
     * Universal endpoint that accepts webhooks from any provider. The webhook payload is stored AS-IS and published to a message queue for processing.
     * <p><b>202</b> - Webhook accepted and queued for processing
     * <p><b>400</b> - Invalid webhook payload
     * <p><b>500</b> - Internal server error
     * @param providerName Name of the webhook provider
     * @param body The body parameter
     * @return ResponseEntity&lt;WebhookResponseDTO&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<WebhookResponseDTO>> receiveWebhookWithHttpInfo(String providerName, Object body) throws WebClientResponseException {
        ParameterizedTypeReference<WebhookResponseDTO> localVarReturnType = new ParameterizedTypeReference<WebhookResponseDTO>() {};
        return receiveWebhookRequestCreation(providerName, body).toEntity(localVarReturnType);
    }

    /**
     * Receive webhook from any provider
     * Universal endpoint that accepts webhooks from any provider. The webhook payload is stored AS-IS and published to a message queue for processing.
     * <p><b>202</b> - Webhook accepted and queued for processing
     * <p><b>400</b> - Invalid webhook payload
     * <p><b>500</b> - Internal server error
     * @param providerName Name of the webhook provider
     * @param body The body parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec receiveWebhookWithResponseSpec(String providerName, Object body) throws WebClientResponseException {
        return receiveWebhookRequestCreation(providerName, body);
    }
}
