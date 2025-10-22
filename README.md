# Firefly Webhook Management Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A production-ready, scalable webhook ingestion platform that receives webhooks from any provider and publishes them to Kafka/RabbitMQ for asynchronous processing. Built with reactive Spring WebFlux and designed for high throughput and horizontal scalability.

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Module Structure](#module-structure)
- [Technology Stack](#technology-stack)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)

## 🎯 Overview

The Firefly Webhook Management Platform is a **universal webhook ingestion service** that:

1. **Receives** webhooks from any provider via a single dynamic HTTP endpoint
2. **Publishes** webhook events to Kafka/RabbitMQ for asynchronous processing
3. **Enables** downstream workers to process webhooks with idempotency, signature validation, and retry logic

This platform follows a **producer-consumer pattern** where:
- The **webhook platform** (this project) acts as the **producer** - it receives HTTP webhooks and publishes them to message queues
- **Worker applications** act as **consumers** - they consume events from queues, validate signatures, ensure idempotency, and execute business logic

### Why This Architecture?

- **Decoupling**: Webhook ingestion is separated from business logic processing
- **Scalability**: Both ingestion and processing can scale independently
- **Reliability**: Message queues provide durability and retry mechanisms
- **Flexibility**: Add new webhook providers without changing the platform
- **Audit Trail**: All webhooks are preserved in their original form

## ✨ Key Features

### Webhook Ingestion
- **Dynamic Provider Support**: Accept webhooks from any provider without code changes
- **Universal Endpoint**: Single endpoint pattern `/api/v1/webhook/{providerName}`
- **AS-IS Preservation**: Stores complete webhook payload, headers, and metadata
- **Reactive Processing**: Built on Spring WebFlux for high concurrency
- **Idempotency Support**: HTTP-level idempotency using `X-Idempotency-Key` header
- **Enhanced Response**: Rich acknowledgment response with payload echo, timestamps, and processing metadata for webhook sender verification

### Message Queue Integration
- **Multi-Protocol Support**: Kafka (primary) and RabbitMQ via `lib-common-eda`
- **Flexible Topic Routing**: Configurable destination strategies (provider-based, custom, etc.)
- **JSON Serialization**: Events published as JSON for easy consumption
- **Guaranteed Delivery**: At-least-once delivery semantics

### Worker Processing Framework
- **Abstract Base Classes**: `AbstractWebhookEventListener` for simplified event consumption
- **Idempotency Service**: Redis-based distributed idempotency using `lib-common-cache`
- **Signature Validation**: Provider-specific signature validation (Stripe, GitHub, etc.)
- **Retry Logic**: Configurable retry strategies with exponential backoff
- **Processing Lifecycle**: Hooks for before/after processing and error handling

### Production Features
- **Health Checks**: Actuator endpoints for Kafka, Redis, and application health
- **Metrics**: Prometheus-compatible metrics via Micrometer
- **OpenAPI Documentation**: Interactive Swagger UI at `/swagger-ui.html`
- **Distributed Caching**: Redis for idempotency and caching (with Caffeine fallback)
- **Structured Logging**: JSON-formatted logs with correlation IDs

## 🏗️ Architecture

### High-Level Flow

```
┌─────────────┐         ┌──────────────────────┐         ┌─────────────┐         ┌─────────────────┐
│   Webhook   │  HTTP   │  Webhook Platform    │  Kafka  │   Kafka     │  Poll   │  Worker App     │
│  Provider   ├────────>│  (This Project)      ├────────>│   Topic     ├────────>│  (Consumer)     │
│  (Stripe)   │  POST   │  - WebhookController │ Publish │  "stripe"   │ Consume │  - Idempotency  │
└─────────────┘         │  - ProcessingService │         └─────────────┘         │  - Validation   │
                        └──────────────────────┘                                 │  - Processing   │
                                                                                 └─────────────────┘
```

### Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                         Webhook Management Platform                            │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│  ┌─────────────────┐      ┌──────────────────┐      ┌──────────────────────┐   │
│  │WebhookController│─────>│ ProcessingService│─────>│ EventPublisherFactory│   │
│  │  (REST API)     │      │  (Business Logic)│      │   (lib-common-eda)   │   │
│  └─────────────────┘      └──────────────────┘      └──────────────────────┘   │
│          │                         │                           │               │
│          │                         │                           ▼               │
│          │                         │                  ┌─────────────────┐      │
│          │                         │                  │  Kafka/RabbitMQ │      │
│          │                         │                  │    Publisher    │      │
│          │                         │                  └─────────────────┘      │
│          ▼                         ▼                                           │
│  ┌──────────────┐         ┌──────────────┐                                     │
│  │  WebhookDTO  │         │ DomainEvent  │                                     │
│  │  (Interface) │         │   (Core)     │                                     │
│  └──────────────┘         └──────────────┘                                     │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────────┐
│                         Worker Application (Separate)                          │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│  ┌──────────────────────┐      ┌─────────────────────┐                         │
│  │ WebhookEventListener │─────>│  WebhookProcessor   │                         │
│  │  (@EventListener)    │      │  (Business Logic)   │                         │
│  └──────────────────────┘      └─────────────────────┘                         │
│          │                               │                                     │
│          ▼                               ▼                                     │
│  ┌──────────────────┐          ┌──────────────────┐                            │
│  │IdempotencyService│          │SignatureValidator│                            │
│  │    Redis Cache   │          │Provider-specific │                            │
│  └──────────────────┘          └──────────────────┘                            │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- **Java 21+** - [Download OpenJDK](https://openjdk.org/projects/jdk/21/)
- **Maven 3.8+** - [Download Maven](https://maven.apache.org/download.cgi)
- **Kafka** - Running on `localhost:29092` (or configure via environment variables)
- **Redis** - Running on `localhost:26379` (optional, for idempotency and caching)

### 1. Build the Project

```bash
git clone https://github.com/firefly-oss/common-platform-webhooks-mgmt.git
cd common-platform-webhooks-mgmt
mvn clean install
```

### 2. Configure Environment Variables

```bash
export FIREFLY_KAFKA_BOOTSTRAP_SERVERS=localhost:29092
export FIREFLY_REDIS_ENABLED=true
export REDIS_HOST=localhost
export REDIS_PORT=26379
export REDIS_SSL=false
```

### 3. Run the Application

```bash
cd common-platform-webhooks-mgmt-web
mvn spring-boot:run
```

The application will start on **port 8080**.

### 4. Verify the Application

```bash
# Check health
curl http://localhost:8080/actuator/health

# View API documentation
open http://localhost:8080/swagger-ui.html

# Test webhook endpoint
curl -X POST http://localhost:8080/api/v1/webhook/stripe \
  -H "Content-Type: application/json" \
  -d '{"type":"payment_intent.succeeded","amount":1000}'
```

## 📦 Module Structure

The project follows a **multi-module Maven structure** with clear separation of concerns:

```
common-platform-webhooks-mgmt/
├── common-platform-webhooks-mgmt-interfaces/    # DTOs and API contracts
├── common-platform-webhooks-mgmt-core/          # Business logic and domain events
├── common-platform-webhooks-mgmt-processor/     # Worker framework (ports & adapters)
└── common-platform-webhooks-mgmt-web/           # REST API and Spring Boot application
```

### Module Details

#### 1. `common-platform-webhooks-mgmt-interfaces`
**Purpose**: Shared DTOs and contracts used across all modules

**Key Components**:
- `WebhookEventDTO` - Webhook event data transfer object
- `WebhookResponseDTO` - HTTP response DTO
- `WebhookEventQueryDTO` - Query result DTO
- `WebhookEventFilterDTO` - Filter criteria DTO

**Dependencies**: None (pure POJOs with Jackson annotations)

#### 2. `common-platform-webhooks-mgmt-core`
**Purpose**: Core business logic and domain events

**Key Components**:
- `WebhookProcessingService` - Service interface for webhook processing
- `WebhookProcessingServiceImpl` - Implementation that publishes to Kafka
- `WebhookReceivedEvent` - Domain event representing a received webhook
- `WebhookEventMapper` - MapStruct mapper for DTO ↔ Domain Event conversion

**Dependencies**: 
- `lib-common-eda` - Event publishing
- `lib-common-core` - Core utilities
- MapStruct for mapping

#### 3. `common-platform-webhooks-mgmt-processor`
**Purpose**: Hexagonal architecture framework for building webhook workers

**Key Components**:
- **Ports** (Interfaces):
  - `WebhookProcessor` - Business logic processor interface
  - `WebhookIdempotencyService` - Idempotency service interface
  - `WebhookSignatureValidator` - Signature validation interface
  
- **Adapters** (Implementations):
  - `AbstractWebhookEventListener` - Base class for Kafka consumers
  - `CacheBasedWebhookIdempotencyService` - Redis-based idempotency
  
- **Models**:
  - `WebhookProcessingContext` - Context object with all webhook data
  - `WebhookProcessingResult` - Processing result with status

**Dependencies**:
- `lib-common-eda` - Event consumption
- `lib-common-cache` - Redis/Caffeine caching
- Spring Kafka

#### 4. `common-platform-webhooks-mgmt-web`
**Purpose**: REST API and Spring Boot application

**Key Components**:
- `WebhookManagementApplication` - Main Spring Boot application
- `WebhookController` - REST controller for webhook ingestion
- `HealthCheckController` - Health check endpoints for testing
- `application.yml` - Configuration file

**Dependencies**:
- Spring Boot WebFlux
- Spring Boot Actuator
- SpringDoc OpenAPI
- `lib-common-web` - Web utilities
- `lib-common-eda` - Event publishing
- `lib-common-cache` - Caching

## 🛠️ Technology Stack

### Core Technologies
- **Java 21** - Latest LTS with virtual threads and pattern matching
- **Spring Boot 3.2.2** - Application framework
- **Spring WebFlux** - Reactive web framework (Netty-based)
- **Project Reactor** - Reactive programming library

### Firefly Libraries
- **lib-common-eda** - Event-driven architecture (Kafka/RabbitMQ abstraction)
- **lib-common-cache** - Distributed caching (Redis/Caffeine abstraction)
- **lib-common-web** - Web utilities (logging, idempotency, error handling)
- **lib-common-core** - Core utilities
- **lib-common-cqrs** - CQRS framework (optional)

### Message Queues
- **Apache Kafka 3.6.1** - Primary message broker
- **RabbitMQ** - Alternative message broker (via lib-common-eda)

### Caching & Storage
- **Redis 7+** - Distributed cache for idempotency
- **Caffeine** - In-memory cache (fallback)

### Observability
- **Micrometer** - Metrics collection
- **Prometheus** - Metrics export
- **Spring Boot Actuator** - Health checks and management endpoints

### Development Tools
- **MapStruct** - DTO mapping
- **Lombok** - Boilerplate reduction
- **SpringDoc OpenAPI** - API documentation
- **JUnit 5** - Testing framework
- **Testcontainers** - Integration testing with Docker

## ⚙️ Configuration

### Environment Variables

#### Required Configuration

```bash
# Kafka Configuration (Required)
FIREFLY_KAFKA_BOOTSTRAP_SERVERS=localhost:29092

# Redis Configuration (Required for distributed deployments)
FIREFLY_REDIS_ENABLED=true
REDIS_HOST=localhost
REDIS_PORT=26379
REDIS_SSL=false
```

#### Optional Configuration

```bash
# Server Configuration
SERVER_PORT=8080

# Webhook Destination Strategy
FIREFLY_WEBHOOKS_DESTINATION_PREFIX=""           # Prefix for topic names (e.g., "webhooks.")
FIREFLY_WEBHOOKS_DESTINATION_SUFFIX=""           # Suffix for topic names (e.g., ".events")
FIREFLY_WEBHOOKS_DESTINATION_USE_PROVIDER=true   # Use provider name as topic
FIREFLY_WEBHOOKS_DESTINATION_CUSTOM=""           # Custom topic (overrides all)

# Cache Configuration
FIREFLY_CACHE_ENABLED=true
FIREFLY_CACHE_TYPE=REDIS                         # REDIS or CAFFEINE
REDIS_DATABASE=0
REDIS_PASSWORD=
REDIS_USERNAME=

# Consumer Configuration (for workers)
FIREFLY_CONSUMER_GROUP_ID=webhook-worker
FIREFLY_EDA_CONSUMER_TYPE=KAFKA
```

### Topic Routing Strategies

#### Strategy 1: Provider-Based Routing (Default)
Each provider gets its own Kafka topic:

```yaml
firefly:
  webhooks:
    destination:
      use-provider-as-topic: true
      prefix: ""
      suffix: ""
```

**Result**:
- `POST /api/v1/webhook/stripe` → Kafka topic: `stripe`
- `POST /api/v1/webhook/paypal` → Kafka topic: `paypal`
- `POST /api/v1/webhook/github` → Kafka topic: `github`

#### Strategy 2: Prefixed Topics
Add a namespace prefix to all topics:

```yaml
firefly:
  webhooks:
    destination:
      use-provider-as-topic: true
      prefix: "webhooks."
      suffix: ""
```

**Result**:
- `POST /api/v1/webhook/stripe` → Kafka topic: `webhooks.stripe`
- `POST /api/v1/webhook/paypal` → Kafka topic: `webhooks.paypal`

#### Strategy 3: Single Topic for All Providers
Route all webhooks to one topic:

```yaml
firefly:
  webhooks:
    destination:
      custom: "webhooks.all"
```

**Result**:
- All providers → Kafka topic: `webhooks.all`

## 📚 API Documentation

### Webhook Ingestion Endpoint

**Endpoint**: `POST /api/v1/webhook/{providerName}`

**Description**: Universal webhook endpoint that accepts webhooks from any provider

**Path Parameters**:
- `providerName` (string, required) - Name of the webhook provider (e.g., "stripe", "paypal", "github")

**Request Headers**:
- `Content-Type: application/json` (required)
- `X-Idempotency-Key` (optional) - For HTTP-level idempotency
- Provider-specific headers (e.g., `Stripe-Signature`, `X-Hub-Signature-256`)

**Request Body**: Any valid JSON payload

**Response**: `202 ACCEPTED`
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "ACCEPTED",
  "message": "Webhook received and queued for processing",
  "receivedAt": "2025-10-22T10:00:00.123Z",
  "processedAt": "2025-10-22T10:00:00.456Z",
  "providerName": "stripe",
  "receivedPayload": {
    "type": "payment_intent.succeeded",
    "data": {
      "object": {
        "id": "pi_1234567890",
        "amount": 2000,
        "currency": "usd"
      }
    }
  },
  "metadata": {
    "destination": "stripe",
    "sourceIp": "192.168.1.100",
    "httpMethod": "POST",
    "payloadSize": 1024,
    "headerCount": 15
  }
}
```

**Response Fields**:
- `eventId` - Unique identifier assigned to this webhook event
- `status` - Processing status (`ACCEPTED`, `ERROR`, or `REJECTED`)
- `message` - Human-readable description of the result
- `receivedAt` - Timestamp when the webhook was received by the platform
- `processedAt` - Timestamp when the webhook was acknowledged
- `providerName` - Echo of the provider name from the URL
- `receivedPayload` - Echo of the received payload for verification
- `metadata` - Additional processing metadata:
  - `destination` - Kafka topic/queue where the event was published
  - `sourceIp` - IP address of the webhook sender
  - `httpMethod` - HTTP method used (typically POST)
  - `payloadSize` - Size of the payload in bytes
  - `headerCount` - Number of HTTP headers received
  - `correlationId` - Correlation ID if provided in headers

### cURL Examples

#### Example 1: Stripe Payment Success Webhook
```bash
curl -X POST http://localhost:8080/api/v1/webhook/stripe \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: t=1234567890,v1=5257a869e7ecebeda32affa62cdca3fa51cad7e77a0e56ff536d0ce8e108d8bd" \
  -d '{
    "id": "evt_1234567890",
    "type": "payment_intent.succeeded",
    "data": {
      "object": {
        "id": "pi_1234567890",
        "amount": 2000,
        "currency": "usd",
        "status": "succeeded",
        "customer": "cus_123456",
        "description": "Payment for order #12345"
      }
    },
    "created": 1234567890
  }'
```

#### Example 2: Stripe Subscription Created
```bash
curl -X POST http://localhost:8080/api/v1/webhook/stripe \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: t=1234567890,v1=abc123..." \
  -d '{
    "id": "evt_9876543210",
    "type": "customer.subscription.created",
    "data": {
      "object": {
        "id": "sub_1234567890",
        "customer": "cus_123456",
        "status": "active",
        "plan": {
          "id": "plan_premium",
          "amount": 2999,
          "currency": "usd",
          "interval": "month"
        }
      }
    }
  }'
```

#### Example 3: GitHub Pull Request Webhook
```bash
curl -X POST http://localhost:8080/api/v1/webhook/github \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=abc123def456..." \
  -H "X-GitHub-Event: pull_request" \
  -H "X-GitHub-Delivery: 12345678-1234-1234-1234-123456789012" \
  -d '{
    "action": "opened",
    "number": 42,
    "pull_request": {
      "id": 1,
      "number": 42,
      "title": "Add new feature",
      "state": "open",
      "user": {
        "login": "octocat",
        "id": 1
      },
      "head": {
        "ref": "feature-branch",
        "sha": "abc123"
      },
      "base": {
        "ref": "main",
        "sha": "def456"
      }
    },
    "repository": {
      "id": 123456,
      "name": "my-repo",
      "full_name": "octocat/my-repo"
    }
  }'
```

#### Example 4: GitHub Push Event
```bash
curl -X POST http://localhost:8080/api/v1/webhook/github \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=xyz789..." \
  -H "X-GitHub-Event: push" \
  -d '{
    "ref": "refs/heads/main",
    "before": "abc123",
    "after": "def456",
    "commits": [
      {
        "id": "def456",
        "message": "Fix bug in payment processing",
        "author": {
          "name": "John Doe",
          "email": "john@example.com"
        }
      }
    ],
    "repository": {
      "name": "my-repo",
      "full_name": "octocat/my-repo"
    }
  }'
```

#### Example 5: Shopify Order Created Webhook
```bash
curl -X POST http://localhost:8080/api/v1/webhook/shopify \
  -H "Content-Type: application/json" \
  -H "X-Shopify-Hmac-SHA256: base64encodedhmac..." \
  -H "X-Shopify-Topic: orders/create" \
  -H "X-Shopify-Shop-Domain: my-shop.myshopify.com" \
  -d '{
    "id": 820982911946154508,
    "email": "customer@example.com",
    "total_price": "199.00",
    "currency": "USD",
    "financial_status": "paid",
    "fulfillment_status": null,
    "line_items": [
      {
        "id": 466157049,
        "title": "Product Name",
        "quantity": 1,
        "price": "199.00",
        "sku": "PROD-001"
      }
    ],
    "customer": {
      "id": 207119551,
      "email": "customer@example.com",
      "first_name": "John",
      "last_name": "Doe"
    }
  }'
```

#### Example 6: PayPal Payment Completed
```bash
curl -X POST http://localhost:8080/api/v1/webhook/paypal \
  -H "Content-Type: application/json" \
  -H "PayPal-Transmission-Id: unique-id-123" \
  -H "PayPal-Transmission-Sig: signature-here" \
  -d '{
    "event_type": "PAYMENT.CAPTURE.COMPLETED",
    "resource": {
      "id": "CAPTURE-123",
      "amount": {
        "currency_code": "USD",
        "value": "100.00"
      },
      "status": "COMPLETED",
      "create_time": "2025-10-22T10:00:00Z"
    }
  }'
```

#### Example 7: Custom Provider with Idempotency
```bash
curl -X POST http://localhost:8080/api/v1/webhook/my-custom-provider \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-request-id-12345" \
  -H "X-Custom-Signature: your-signature-here" \
  -d '{
    "event_type": "user.created",
    "user_id": "12345",
    "email": "user@example.com",
    "timestamp": "2025-10-22T10:00:00Z",
    "metadata": {
      "source": "web",
      "ip_address": "192.168.1.1"
    }
  }'
```

#### Example 8: Twilio SMS Webhook
```bash
curl -X POST http://localhost:8080/api/v1/webhook/twilio \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'MessageSid=SM1234567890&From=%2B15551234567&To=%2B15559876543&Body=Hello+World&MessageStatus=received'
```

**Note**: The platform accepts both JSON and form-encoded payloads. All data is preserved as-is and published to Kafka.

### Enhanced Response Benefits

The webhook platform returns a comprehensive response that provides valuable information to webhook senders:

#### 1. **Payload Verification**
The `receivedPayload` field echoes back the exact payload received, allowing webhook senders to:
- Verify the payload was received correctly
- Debug payload formatting issues
- Confirm data integrity
- Audit webhook deliveries

#### 2. **Event Tracking**
Each webhook receives a unique `eventId` that can be used to:
- Track the webhook through the entire processing pipeline
- Correlate logs across distributed systems
- Query event status and processing results
- Support customer service inquiries

#### 3. **Processing Metadata**
The `metadata` object provides operational insights:
- **destination**: Confirms which Kafka topic/queue received the event
- **sourceIp**: Helps identify the webhook sender for security auditing
- **payloadSize**: Useful for monitoring and capacity planning
- **headerCount**: Validates that all expected headers were received
- **correlationId**: Enables distributed tracing across microservices

#### 4. **Timestamp Precision**
Two timestamps provide detailed timing information:
- **receivedAt**: When the platform received the webhook (useful for latency analysis)
- **processedAt**: When the acknowledgment was sent (measures platform processing time)

#### 5. **Status Clarity**
The `status` field clearly indicates the outcome:
- `ACCEPTED`: Webhook successfully queued for processing
- `ERROR`: Platform encountered an error (with details in `message`)
- `REJECTED`: Webhook was rejected (e.g., invalid format, rate limit exceeded)

This enhanced response format is particularly valuable for:
- **Debugging**: Webhook senders can verify their integration is working correctly
- **Monitoring**: Operations teams can track webhook delivery success rates
- **Compliance**: Audit trails for financial and regulated industries
- **Support**: Customer service can quickly look up webhook events by ID

### Health Check Endpoints

#### Application Health
```bash
GET /actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "kafka": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

#### Service Status (Custom)
```bash
GET /api/v1/health/status
```

Response:
```json
{
  "timestamp": "2025-10-22T10:00:00Z",
  "application": "common-platform-webhooks-mgmt",
  "status": "UP",
  "cache": {
    "available": true,
    "type": "Redis Distributed Cache (redis)",
    "name": "webhook-idempotency"
  },
  "kafka": {
    "configured": true
  }
}
```

#### Test Kafka Connection
```bash
POST /api/v1/health/test-kafka
```

#### Test Cache Connection
```bash
POST /api/v1/health/test-cache
```

### OpenAPI Documentation

Interactive API documentation is available at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 📨 Message Format

Events published to Kafka/RabbitMQ have the following structure:

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "providerName": "stripe",
  "payload": {
    "id": "evt_1234567890",
    "type": "payment_intent.succeeded",
    "data": {
      "object": {
        "id": "pi_1234567890",
        "amount": 1000,
        "currency": "usd"
      }
    }
  },
  "headers": {
    "Stripe-Signature": "t=1234567890,v1=abc123...",
    "Content-Type": "application/json",
    "User-Agent": "Stripe/1.0",
    "X-Transaction-Id": "txn-123"
  },
  "queryParams": {},
  "receivedAt": "2025-10-22T10:00:00.123Z",
  "correlationId": "corr-123",
  "sourceIp": "192.168.1.1",
  "httpMethod": "POST"
}
```

### Kafka Message Headers

The platform also sets Kafka message headers:
- `provider` - Provider name (e.g., "stripe")
- `eventId` - Unique event identifier
- `receivedAt` - ISO-8601 timestamp
- `correlationId` - Correlation ID for tracing (if provided)

## 🧪 Development

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
cd common-platform-webhooks-mgmt-web
mvn test

# Run integration tests only
mvn verify -P integration-tests

# Skip tests during build
mvn clean install -DskipTests
```

### Integration Tests

The project includes comprehensive integration tests using **Testcontainers**:

- **WebhookIntegrationTest**: End-to-end test covering:
  1. HTTP POST to webhook endpoint
  2. Event published to Kafka
  3. Event consumed from Kafka
  4. Idempotency check using Redis
  5. Signature validation
  6. Business logic processing

Tests automatically start Docker containers for:
- Kafka (Confluent Platform 7.5.0)
- Redis (Redis 7 Alpine)

### Code Structure

```
common-platform-webhooks-mgmt-web/
├── src/main/java/
│   └── com/firefly/common/webhooks/
│       ├── web/
│       │   ├── WebhookManagementApplication.java    # Main application
│       │   └── controllers/
│       │       ├── WebhookController.java           # Webhook ingestion
│       │       └── HealthCheckController.java       # Health checks
│       └── ...
├── src/main/resources/
│   └── application.yml                              # Configuration
└── src/test/java/
    └── com/firefly/common/webhooks/
        └── integration/
            ├── WebhookIntegrationTest.java          # E2E tests
            ├── WebhookIntegrationTestConfiguration.java
            └── support/
                ├── TestStripeWebhookProcessor.java
                ├── TestStripeWebhookListener.java
                └── StripeSignatureValidator.java
```

### Building a Worker Application

See the [Processor Framework README](common-platform-webhooks-mgmt-processor/README.md) for detailed instructions on building webhook workers.

Quick example:

```java
@Component
public class StripeWebhookProcessor implements WebhookProcessor {

    @Override
    public String getProviderName() {
        return "stripe";
    }

    @Override
    public Mono<WebhookProcessingResult> process(WebhookProcessingContext context) {
        String eventType = context.getPayload().path("type").asText();

        return switch (eventType) {
            case "payment_intent.succeeded" -> handlePaymentSuccess(context);
            case "payment_intent.failed" -> handlePaymentFailure(context);
            default -> Mono.just(WebhookProcessingResult.success("Event type not handled"));
        };
    }

    private Mono<WebhookProcessingResult> handlePaymentSuccess(WebhookProcessingContext context) {
        // Your business logic here
        return Mono.just(WebhookProcessingResult.success("Payment processed"));
    }
}
```

## 🚀 Deployment

### Docker

Build and run with Docker:

```bash
# Build Docker image
docker build -t firefly/webhook-platform:latest .

# Run with Docker Compose
docker-compose up -d
```

### Kubernetes

Deploy to Kubernetes:

```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/

# Check deployment status
kubectl get pods -l app=webhook-platform

# View logs
kubectl logs -f deployment/webhook-platform
```

### Environment-Specific Configuration

#### Development
```yaml
firefly:
  cache:
    default-cache-type: CAFFEINE  # In-memory cache for local dev
  eda:
    publishers:
      kafka:
        default:
          bootstrap-servers: localhost:29092
```

#### Production
```yaml
firefly:
  cache:
    default-cache-type: REDIS  # Distributed cache for production
  eda:
    publishers:
      kafka:
        default:
          bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
          producer:
            acks: all
            retries: 3
            compression-type: snappy
```

## 📊 Monitoring & Observability

### Metrics

Prometheus metrics are exposed at `/actuator/prometheus`:

```bash
curl http://localhost:8080/actuator/prometheus
```

Key metrics:
- `http_server_requests_seconds` - HTTP request duration
- `kafka_producer_record_send_total` - Kafka messages sent
- `cache_gets_total` - Cache get operations
- `cache_puts_total` - Cache put operations
- `jvm_memory_used_bytes` - JVM memory usage

### Health Checks

Health checks available at `/actuator/health`:

```bash
curl http://localhost:8080/actuator/health
```

Components monitored:
- Kafka connectivity
- Redis connectivity (if enabled)
- Disk space
- Application liveness

### Logging

The application uses structured JSON logging with correlation IDs:

```json
{
  "timestamp": "2025-10-22T10:00:00.123Z",
  "level": "INFO",
  "logger": "com.firefly.common.webhooks.web.controllers.WebhookController",
  "message": "Webhook received",
  "correlationId": "corr-123",
  "eventId": "evt-456",
  "providerName": "stripe"
}
```

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow Java coding conventions
- Use Lombok to reduce boilerplate
- Write unit tests for new features
- Ensure all tests pass before submitting PR

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For questions or issues:
- Open an issue on GitHub
- Contact the Firefly team

## 🔗 Related Documentation

- [Processor Framework Guide](common-platform-webhooks-mgmt-processor/README.md) - How to build webhook workers
- [Architecture Documentation](ARCHITECTURE.md) - Detailed architecture and design decisions
- [Configuration Guide](CONFIGURATION.md) - Complete configuration reference
- [Development Guide](DEVELOPMENT.md) - Development setup and guidelines
- [Deployment Guide](DEPLOYMENT.md) - Production deployment strategies

