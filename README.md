# Firefly Framework - Webhooks

[![CI](https://github.com/fireflyframework/fireflyframework-webhooks/actions/workflows/ci.yml/badge.svg)](https://github.com/fireflyframework/fireflyframework-webhooks/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

> Reactive webhook ingestion platform with provider-agnostic routing, idempotency, rate limiting, batching, and dead letter queue support.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

## Overview

Firefly Framework Webhooks provides a comprehensive inbound webhook processing platform for receiving, validating, and routing webhooks from external providers. It features provider-agnostic webhook reception, signature validation, idempotency handling, rate limiting, batching, compression, and dead letter queue support.

The project is structured as a multi-module build with five sub-modules: interfaces (DTOs), core (processing services and infrastructure), processor (consumer-side processing with idempotency), SDK (client library), and web (REST controller for webhook ingestion). The core module includes resilient processing with circuit breakers, metadata enrichment, and comprehensive metrics collection.

The processor module provides `AbstractWebhookEventListener` as a base class for implementing webhook event consumers with built-in idempotency, signature validation, and tracing context propagation.

## Features

- Provider-agnostic webhook ingestion via REST endpoint
- Webhook signature validation with pluggable validators
- Cache-based idempotency service for duplicate detection
- Rate limiting per provider/tenant
- Webhook batching for high-throughput scenarios
- Payload compression support
- Dead letter queue for failed webhook processing
- Metadata enrichment for incoming webhooks
- Resilient processing with circuit breakers
- `AbstractWebhookEventListener` base class for consumers
- Webhook event domain events (received, rejected)
- Health indicators: Redis connectivity, circuit breaker, liveness, readiness
- Micrometer metrics for webhook processing
- Tracing context extraction and propagation
- REST controller for webhook reception
- Multi-module architecture: interfaces, core, processor, SDK, web

## Requirements

- Java 21+
- Spring Boot 3.x
- Maven 3.9+
- Redis (for idempotency and rate limiting)

## Installation

The webhooks library is a multi-module project. Include the modules you need:

```xml
<!-- Core webhook processing -->
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>fireflyframework-webhooks-core</artifactId>
    <version>26.02.06</version>
</dependency>

<!-- Webhook processor for consumers -->
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>fireflyframework-webhooks-processor</artifactId>
    <version>26.02.06</version>
</dependency>

<!-- SDK for client integration -->
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>fireflyframework-webhooks-sdk</artifactId>
    <version>26.02.06</version>
</dependency>
```

## Quick Start

```java
import org.fireflyframework.webhooks.processor.listener.AbstractWebhookEventListener;
import org.fireflyframework.webhooks.processor.model.WebhookProcessingContext;

@Component
public class PaymentWebhookHandler extends AbstractWebhookEventListener {

    @Override
    protected Mono<WebhookProcessingResult> processWebhook(WebhookProcessingContext context) {
        String eventType = context.getEventType();
        Map<String, Object> payload = context.getPayload();
        // Process the webhook event
        return handlePaymentEvent(eventType, payload);
    }
}
```

## Configuration

```yaml
firefly:
  webhooks:
    security:
      signature-validation: true
    rate-limit:
      enabled: true
      requests-per-second: 100
    batching:
      enabled: false
      batch-size: 50
    compression:
      enabled: true
    retry:
      max-attempts: 3
      backoff: 1s
```

## Documentation

No additional documentation available for this project.

## Contributing

Contributions are welcome. Please read the [CONTRIBUTING.md](CONTRIBUTING.md) guide for details on our code of conduct, development process, and how to submit pull requests.

## License

Copyright 2024-2026 Firefly Software Solutions Inc.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
