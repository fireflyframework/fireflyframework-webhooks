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

package com.firefly.common.webhooks.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Web filter for distributed tracing and structured logging.
 * Adds trace ID and span ID to MDC for correlation across logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TracingWebFilter implements WebFilter {

    private static final String TRACE_ID_HEADER = "X-B3-TraceId";
    private static final String SPAN_ID_HEADER = "X-B3-SpanId";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    
    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Extract or generate trace ID
        String traceId = extractHeader(exchange, TRACE_ID_HEADER);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        // Extract or generate span ID
        String spanId = extractHeader(exchange, SPAN_ID_HEADER);
        if (spanId == null) {
            spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        // Extract or generate request ID
        String requestId = extractHeader(exchange, REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        // Add trace headers to response
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);
        exchange.getResponse().getHeaders().add(SPAN_ID_HEADER, spanId);
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        // Create context with tracing information
        final String finalTraceId = traceId;
        final String finalSpanId = spanId;
        final String finalRequestId = requestId;

        return chain.filter(exchange)
                .contextWrite(Context.of(
                        TRACE_ID_KEY, finalTraceId,
                        SPAN_ID_KEY, finalSpanId,
                        REQUEST_ID_KEY, finalRequestId
                ))
                .doFirst(() -> {
                    // Set MDC for logging
                    MDC.put(TRACE_ID_KEY, finalTraceId);
                    MDC.put(SPAN_ID_KEY, finalSpanId);
                    MDC.put(REQUEST_ID_KEY, finalRequestId);
                })
                .doFinally(signalType -> {
                    // Clear MDC
                    MDC.remove(TRACE_ID_KEY);
                    MDC.remove(SPAN_ID_KEY);
                    MDC.remove(REQUEST_ID_KEY);
                });
    }

    private String extractHeader(ServerWebExchange exchange, String headerName) {
        String value = exchange.getRequest().getHeaders().getFirst(headerName);
        return (value != null && !value.isBlank()) ? value : null;
    }
}

