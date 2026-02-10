# Firefly Framework Webhooks Library

[![CI](https://github.com/fireflyframework/fireflyframework-webhooks/actions/workflows/ci.yml/badge.svg)](https://github.com/fireflyframework/fireflyframework-webhooks/actions/workflows/ci.yml)

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/21/)
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

The Firefly Framework Webhooks Library is a **universal webhook ingestion service** that:

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
- **HTTP-level Idempotency**: Using `X-Idempotency-Key` header with Redis caching (24h TTL)
- **Enhanced Response**: Rich acknowledgment response with payload echo, timestamps, and processing metadata

### Security & Validation
- **Rate Limiting**: Per-provider and per-IP rate limiting using Resilience4j (100 req/s default)
- **Payload Size Validation**: Configurable max payload size (1MB default) to prevent DoS attacks
- **Provider Name Validation**: Regex-based validation of provider names (alphanumeric and hyphens)
- **IP Whitelisting**: Optional IP whitelist per provider with CIDR notation support (e.g., `192.168.1.0/24`)
- **Content-Type Validation**: Ensures proper content type headers are present

### Message Queue Integration
- **Multi-Protocol Support**: Kafka (primary) and RabbitMQ via `lib-common-eda`
- **Flexible Topic Routing**: Configurable destination strategies (provider-based, custom, etc.)
- **JSON Serialization**: Events published as JSON for easy consumption
- **Guaranteed Delivery**: At-least-once delivery semantics

### Resilience Patterns
- **Circuit Breaker**: Prevents cascading failures when Kafka is down (no fallback, relies on lib-common-eda)
- **Timeout Protection**: Configurable timeouts for Kafka publishing operations (10s default)
- **Retry Logic**: Configurable exponential backoff with jitter per provider (3 retries default, 1s-30s delay)
- **Dead Letter Queue (DLQ)**: Rejected webhooks published to `webhooks.dlq` topic with rejection metadata
- **Bulkhead Pattern**: Resource isolation to prevent thread pool exhaustion (configured)

### Worker Processing Framework
- **Abstract Base Classes**: `AbstractWebhookEventListener` for simplified event consumption
- **Idempotency Service**: Redis-based distributed idempotency using `lib-common-cache` (7 days TTL)
- **Signature Validation**: Provider-specific signature validation (Stripe, GitHub, etc.)
- **Processing Lifecycle**: Hooks for before/after processing and error handling

### Advanced Features (NEW)
- **Webhook Batching**: Automatic batching with configurable size and time windows
- **Payload Compression**: GZIP compression for large payloads (>1KB)
- **Metadata Enrichment**: User-Agent parsing, timestamps, request IDs

### Observability & Monitoring
- **Custom Metrics**: Detailed webhook metrics (received, published, processing time) via Micrometer
- **Distributed Tracing**: Complete B3 propagation (HTTP → Kafka → Worker) with Zipkin backend
- **Enhanced Health Checks**:
  - Kafka connectivity with real cluster health check
  - Redis read/write test for cache validation
  - Separate liveness (`/actuator/health/liveness`) and readiness (`/actuator/health/readiness`) probes
  - Circuit breaker state monitoring
- **Structured Logging**: JSON-formatted logs with MDC correlation fields (traceId, spanId, transactionId)
- **Prometheus Metrics**: Prometheus-compatible metrics export at `/actuator/prometheus`
- **OpenAPI Documentation**: Interactive Swagger UI at `/swagger-ui.html`

### Performance & Optimization
- **Webhook Batching**: Configurable batching with `bufferTimeout` for improved throughput (disabled by default)
  - Per-provider batch size and wait time configuration
  - Automatic flushing on size or time threshold
- **Payload Compression**: GZIP compression for large payloads (>1KB) to reduce network bandwidth
  - Configurable compression algorithm (GZIP, ZSTD, LZ4)
  - Automatic compression/decompression in workers
- **Metadata Enrichment**: Automatic enrichment of webhook events with:
  - User-Agent parsing (browser, OS, device type, bot detection)
  - High-precision timestamps (nanosecond precision)
  - Unique request ID per webhook



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
git clone https://github.com/firefly-oss/fireflyframework-webhooks.git
cd fireflyframework-webhooks
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
cd fireflyframework-webhooks-web
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
fireflyframework-webhooks/
├── fireflyframework-webhooks-interfaces/    # DTOs and API contracts
├── fireflyframework-webhooks-core/          # Business logic, services, and configuration
├── fireflyframework-webhooks-processor/     # Worker framework (ports & adapters)
├── fireflyframework-webhooks-web/           # Main application and REST controllers only
└── fireflyframework-webhooks-sdk/           # Java SDK (auto-generated from OpenAPI)
```

### Module Details

#### 1. `fireflyframework-webhooks-interfaces`
**Purpose**: Shared DTOs and contracts used across all modules

**Key Components**:
- `WebhookEventDTO` - Webhook event data transfer object
- `WebhookResponseDTO` - HTTP response DTO

**Dependencies**: None (pure POJOs with Jackson annotations)

#### 2. `fireflyframework-webhooks-core`
**Purpose**: Core business logic, services, configuration, and infrastructure

**Package Structure**:
```
org.fireflyframework.webhooks.core/
├── config/                    # Configuration classes
│   ├── ResilienceConfig.java           # Resilience4j configuration (circuit breaker, rate limiter, timeout)
│   └── WebhookSecurityProperties.java  # Security configuration properties (env var support)
├── domain/                    # Domain events
│   └── events/
│       └── WebhookReceivedEvent.java   # Domain event for received webhooks
├── filter/                    # Web filters
│   └── TracingWebFilter.java           # Distributed tracing filter (B3 propagation, MDC)
├── health/                    # Health indicators
│   └── WebhookCircuitBreakerHealthIndicator.java  # Circuit breaker health check
├── idempotency/              # (Removed - now using lib-common-web)
│   # HTTP-level idempotency moved to lib-common-web IdempotencyWebFilter
├── mappers/                   # MapStruct mappers
│   └── WebhookEventMapper.java         # DTO ↔ Domain Event conversion
├── metrics/                   # Metrics services
│   └── WebhookMetricsService.java      # Custom webhook metrics (Micrometer)
├── ratelimit/                # Rate limiting
│   └── WebhookRateLimitService.java    # Per-provider and per-IP rate limiting
├── resilience/               # Resilience patterns
│   └── ResilientWebhookProcessingService.java  # Circuit breaker + timeout decorator
├── services/                  # Business services
│   ├── WebhookProcessingService.java   # Service interface
│   └── impl/
│       └── WebhookProcessingServiceImpl.java  # Kafka publishing implementation
└── validation/               # Validation services
    └── WebhookValidator.java           # Payload size, provider name, IP whitelist validation
```

**Key Features**:
- **Resilience Patterns**: Circuit breaker, rate limiter, timeout, bulkhead (Resilience4j)
- **Security**: Payload validation, rate limiting, IP whitelisting
- **Observability**: Custom metrics, distributed tracing, health indicators
- **Idempotency**: Event-level idempotency (worker-level, see processor module)

**Dependencies**:
- `lib-common-eda` - Event publishing
- `lib-common-cache` - Redis/Caffeine caching
- `lib-common-core` - Core utilities
- Resilience4j - Resilience patterns
- Micrometer - Metrics
- MapStruct - Mapping

#### 3. `fireflyframework-webhooks-processor`
**Purpose**: Hexagonal architecture framework for building webhook workers

**Key Components**:
- **Ports** (Interfaces):
  - `WebhookProcessor` - Business logic processor interface
  - `WebhookIdempotencyService` - Idempotency service interface
  - `WebhookSignatureValidator` - Signature validation interface

- **Adapters** (Implementations):
  - `AbstractWebhookEventListener` - Base class for Kafka consumers
  - `CacheBasedWebhookIdempotencyService` - Redis-based idempotency (7 days TTL)

- **Models**:
  - `WebhookProcessingContext` - Context object with all webhook data
  - `WebhookProcessingResult` - Processing result with status

**Dependencies**:
- `lib-common-eda` - Event consumption
- `lib-common-cache` - Redis/Caffeine caching
- Spring Kafka

#### 4. `fireflyframework-webhooks-web`
**Purpose**: Main Spring Boot application and REST controllers **ONLY**

**Key Components**:
- `WebhookManagementApplication` - Main Spring Boot application class
- `controllers/WebhookController` - REST controller for webhook ingestion
- `application.yml` - Application configuration
- `logback-spring.xml` - Logging configuration

**Note**: All business logic, services, configuration, and infrastructure code has been moved to the `-core` module. This module contains only the application entry point and REST controllers.

**Dependencies**:
- `fireflyframework-webhooks-core` - Core business logic
- Spring Boot WebFlux
- Spring Boot Actuator
- SpringDoc OpenAPI

#### 5. `fireflyframework-webhooks-sdk`
**Purpose**: Auto-generated Java SDK for webhook platform API

**Key Components**:
- `WebhooksApi` - API client for webhook endpoints
- `WebhookResponseDTO` - Response model
- `ApiClient` - HTTP client configuration

**Generation**: Auto-generated from OpenAPI specification using `openapi-generator-maven-plugin`

**Dependencies**:
- Jackson - JSON serialization
- OkHttp - HTTP client

## 🛠️ Technology Stack

### Core Technologies
- **Java 25** - Latest LTS with virtual threads and pattern matching (Java 21+ compatible)
- **Spring Boot 3.5.10** - Application framework
- **Spring WebFlux** - Reactive web framework (Netty-based)
- **Project Reactor** - Reactive programming library

### Firefly Libraries
- **lib-common-eda** - Event-driven architecture (Kafka/RabbitMQ abstraction)
- **lib-common-cache** - Distributed caching (Redis/Caffeine abstraction)
- **lib-common-web** - Web utilities (**HTTP idempotency**, logging, error handling)
- **lib-common-core** - Core utilities
- **lib-common-cqrs** - CQRS framework (optional)

### Message Queues
- **Apache Kafka 3.6.1** - Primary message broker
- **RabbitMQ** - Alternative message broker (via lib-common-eda)

### Caching & Storage
- **Redis 7+** - Distributed cache for event-level idempotency
- **Caffeine** - In-memory cache (fallback)

### Resilience & Fault Tolerance
- **Resilience4j** - Resilience patterns library
  - Circuit Breaker - Prevents cascading failures
  - Rate Limiter - Per-provider and per-IP rate limiting
  - Time Limiter - Timeout protection
  - Bulkhead - Resource isolation (configured)

### Observability
- **Micrometer** - Metrics collection and custom metrics
- **Prometheus** - Metrics export
- **Zipkin** - Distributed tracing backend
- **Spring Boot Actuator** - Health checks and management endpoints
- **Logback** - Structured JSON logging with MDC

### Development Tools
- **MapStruct** - DTO mapping
- **Lombok** - Boilerplate reduction
- **SpringDoc OpenAPI** - API documentation and SDK generation
- **JUnit 5** - Testing framework
- **Testcontainers** - Integration testing with Docker (Kafka + Redis)

## ⚙️ Configuration

### Environment Variables

All configuration properties can be set via environment variables using Spring Boot's standard naming convention:
- Replace dots (`.`) with underscores (`_`)
- Convert to uppercase
- Example: `firefly.webhooks.security.max-payload-size` → `FIREFLY_WEBHOOKS_SECURITY_MAX_PAYLOAD_SIZE`

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

#### Security Configuration

All security properties support environment variable configuration:

```bash
# Payload Size Validation
FIREFLY_WEBHOOKS_SECURITY_MAX_PAYLOAD_SIZE=1048576              # Max payload size in bytes (default: 1MB)
FIREFLY_WEBHOOKS_SECURITY_VALIDATE_PAYLOAD_SIZE=true            # Enable payload size validation

# Provider Name Validation
FIREFLY_WEBHOOKS_SECURITY_VALIDATE_PROVIDER_NAME=true           # Enable provider name validation
FIREFLY_WEBHOOKS_SECURITY_PROVIDER_NAME_PATTERN="^[a-z0-9-]+$" # Regex pattern for provider names

# IP Whitelisting (Optional)
FIREFLY_WEBHOOKS_SECURITY_ENABLE_IP_WHITELIST=false             # Enable IP whitelist
# IP whitelist per provider (JSON format) - supports exact IPs and CIDR notation:
# FIREFLY_WEBHOOKS_SECURITY_IP_WHITELIST='{"stripe":["54.187.174.169","54.187.205.235"],"github":["192.30.252.0/22","185.199.108.0/22"]}'

# HTTP Idempotency (managed by lib-common-web)
# Configure via idempotency.* properties instead:
# IDEMPOTENCY_HEADER_NAME=X-Idempotency-Key
# IDEMPOTENCY_CACHE_TTL_HOURS=24

# Request Validation
FIREFLY_WEBHOOKS_SECURITY_ENABLE_REQUEST_VALIDATION=true        # Enable request validation
FIREFLY_WEBHOOKS_SECURITY_REQUIRE_CONTENT_TYPE=true             # Require Content-Type header
```

#### Resilience Configuration

```bash
# Circuit Breaker (Kafka Publisher)
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_WEBHOOKKAFKAPUBLISHER_FAILURE_RATE_THRESHOLD=50
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_WEBHOOKKAFKAPUBLISHER_SLOW_CALL_RATE_THRESHOLD=50
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_WEBHOOKKAFKAPUBLISHER_SLOW_CALL_DURATION_THRESHOLD=5s
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_WEBHOOKKAFKAPUBLISHER_WAIT_DURATION_IN_OPEN_STATE=30s

# Time Limiter (Timeout)
RESILIENCE4J_TIMELIMITER_INSTANCES_WEBHOOKKAFKAPUBLISHER_TIMEOUT_DURATION=10s

# Rate Limiter (Per Provider)
RESILIENCE4J_RATELIMITER_INSTANCES_WEBHOOK_PROVIDER_DEFAULT_LIMIT_FOR_PERIOD=100
RESILIENCE4J_RATELIMITER_INSTANCES_WEBHOOK_PROVIDER_DEFAULT_LIMIT_REFRESH_PERIOD=1s

# Rate Limiter (Per IP)
RESILIENCE4J_RATELIMITER_INSTANCES_WEBHOOK_IP_DEFAULT_LIMIT_FOR_PERIOD=100
RESILIENCE4J_RATELIMITER_INSTANCES_WEBHOOK_IP_DEFAULT_LIMIT_REFRESH_PERIOD=1s
```

#### Dead Letter Queue (DLQ) Configuration

```bash
# Enable/disable DLQ for rejected webhooks
FIREFLY_WEBHOOKS_DLQ_ENABLED=true

# Kafka topic for rejected webhooks
FIREFLY_WEBHOOKS_DLQ_TOPIC=webhooks.dlq
```

**DLQ Features**:
- Captures webhooks that fail validation (signature, IP whitelist, payload size, etc.)
- Stores webhooks that fail processing after max retries
- Includes rejection metadata (reason, category, error details, retry count)
- Enables manual inspection, debugging, and replay of failed webhooks

**Rejection Categories**:
- `VALIDATION_FAILURE` - Signature, IP whitelist, payload size violations
- `PROCESSING_FAILURE` - Processing failures after max retries
- `TIMEOUT_FAILURE` - Timeout or circuit breaker open
- `UNRECOVERABLE_ERROR` - Malformed payload, missing required fields
- `RATE_LIMIT_EXCEEDED` - Rate limit violations
- `OTHER` - Unknown failures

#### Retry Configuration

```bash
# Global retry defaults
FIREFLY_WEBHOOKS_RETRY_MAX_ATTEMPTS=3
FIREFLY_WEBHOOKS_RETRY_INITIAL_DELAY=PT1S          # 1 second
FIREFLY_WEBHOOKS_RETRY_MAX_DELAY=PT30S             # 30 seconds
FIREFLY_WEBHOOKS_RETRY_MULTIPLIER=2.0              # Exponential backoff multiplier
FIREFLY_WEBHOOKS_RETRY_ENABLE_JITTER=true          # Enable jitter to prevent thundering herd
FIREFLY_WEBHOOKS_RETRY_JITTER_FACTOR=0.5           # Jitter factor (0.0 to 1.0)

# Retry conditions
FIREFLY_WEBHOOKS_RETRY_ON_TIMEOUT=true
FIREFLY_WEBHOOKS_RETRY_ON_CONNECTION_ERROR=true
FIREFLY_WEBHOOKS_RETRY_ON_SERVER_ERROR=true
FIREFLY_WEBHOOKS_RETRY_ON_CLIENT_ERROR=false       # Usually false for 4xx errors
```

**Retry Behavior**:
- **Exponential Backoff**: Delay = `initialDelay * (multiplier ^ attemptNumber)`
- **Jitter**: Randomizes retry delays to prevent thundering herd: `actualDelay = baseDelay * (1 + random(0, jitterFactor))`
- **Max Delay Cap**: Ensures delays don't exceed `maxDelay`
- **Per-Provider Overrides**: Configure different retry strategies per provider in YAML

**Example Retry Delays** (with default config):
- Attempt 1: 1s (+ 0-500ms jitter)
- Attempt 2: 2s (+ 0-1s jitter)
- Attempt 3: 4s (+ 0-2s jitter)

#### Batching Configuration

```bash
# Enable/disable webhook batching
FIREFLY_WEBHOOKS_BATCHING_ENABLED=false

# Maximum number of events in a batch
FIREFLY_WEBHOOKS_BATCHING_MAX_BATCH_SIZE=100

# Maximum time to wait before flushing a batch
FIREFLY_WEBHOOKS_BATCHING_MAX_WAIT_TIME=PT1S  # 1 second

# Size of the internal buffer for pending events
FIREFLY_WEBHOOKS_BATCHING_BUFFER_SIZE=1000
```

**Batching Behavior**:
- **Buffer Timeout**: Events are batched using Project Reactor's `bufferTimeout(maxSize, maxTime)`
- **Per-Destination Sinks**: Separate batching sinks for each destination (provider)
- **Automatic Flushing**: Batches are flushed when either size or time threshold is reached
- **Backpressure Handling**: Uses `onBackpressureBuffer` with configurable buffer size

**Use Cases**:
- High-throughput scenarios where batching improves Kafka publishing performance
- Reducing network overhead by publishing multiple events in a single Kafka transaction
- Optimizing for latency vs throughput trade-offs

#### Compression Configuration

```bash
# Enable/disable payload compression
FIREFLY_WEBHOOKS_COMPRESSION_ENABLED=false

# Minimum payload size (in bytes) to trigger compression
FIREFLY_WEBHOOKS_COMPRESSION_MIN_SIZE=1024  # 1KB

# Compression algorithm (GZIP, ZSTD, LZ4)
FIREFLY_WEBHOOKS_COMPRESSION_ALGORITHM=GZIP

# Compression level (1-9 for GZIP)
FIREFLY_WEBHOOKS_COMPRESSION_LEVEL=6
```

**Compression Behavior**:
- **Automatic Compression**: Payloads exceeding `minSize` are automatically compressed
- **Transparent Decompression**: Workers automatically decompress payloads
- **Compression Ratio Tracking**: Metrics track compression effectiveness
- **Algorithm Support**: GZIP (default), ZSTD (better compression), LZ4 (faster)

**Benefits**:
- Reduces network bandwidth for large webhook payloads
- Decreases Kafka storage requirements
- Improves throughput for high-volume scenarios

#### Metadata Enrichment Configuration

```bash
# Enable/disable metadata enrichment
FIREFLY_WEBHOOKS_METADATA_ENRICHMENT_ENABLED=true
```

**Enriched Metadata**:
- **Request ID**: Unique UUID per webhook request
- **Timestamp**: High-precision timestamp (nanosecond precision)
- **Source IP**: Client IP address
- **User-Agent Parsing**: Browser, OS, device type, bot detection
- **Request Size**: Payload size in bytes

**Use Cases**:
- Debugging and troubleshooting webhook issues
- Analytics and reporting on webhook sources
- Security monitoring and fraud detection
- Compliance and audit trail requirements

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

# Distributed Tracing
MANAGEMENT_TRACING_ENABLED=true
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0      # Sample 100% of requests (adjust for production)
MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://localhost:9411/api/v2/spans

# Metrics
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,prometheus,metrics
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

### Security Configuration (YAML)

Configure security features in `application.yml`:

```yaml
firefly:
  webhooks:
    security:
      # Payload size validation
      max-payload-size: 1048576  # 1MB in bytes
      validate-payload-size: true

      # Provider name validation
      validate-provider-name: true
      provider-name-pattern: "^[a-z0-9-]+$"

      # IP whitelisting (supports exact IPs and CIDR notation)
      enable-ip-whitelist: true
      ip-whitelist:
        stripe:
          - "54.187.174.169"      # Exact IP
          - "54.187.205.235"      # Exact IP
        github:
          - "192.30.252.0/22"     # CIDR notation
          - "185.199.108.0/22"    # CIDR notation
        paypal:
          - "173.0.82.0/24"       # CIDR notation
          - "64.4.240.0/21"       # CIDR notation

      # HTTP-level idempotency removed - now handled by lib-common-web
      # Configure via idempotency.* properties (see below)

      # Request validation
      enable-request-validation: true
      require-content-type: true
```

**IP Whitelisting Notes**:
- Supports both exact IP addresses (e.g., `54.187.174.169`) and CIDR notation (e.g., `192.30.252.0/22`)
- CIDR notation allows you to whitelist entire IP ranges efficiently
- Uses Apache Commons Net for CIDR matching
- Invalid IP addresses or CIDR notations are logged and rejected

## 📚 API Documentation

### Webhook Ingestion Endpoint

**Endpoint**: `POST /api/v1/webhook/{providerName}`

**Description**: Universal webhook endpoint that accepts webhooks from any provider

**Path Parameters**:
- `providerName` (string, required) - Name of the webhook provider (e.g., "stripe", "paypal", "github")

**Request Headers**:
- `Content-Type: application/json` (required)
- `X-Idempotency-Key` (optional) - For HTTP-level idempotency (handled by lib-common-web IdempotencyWebFilter)
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

**Response**: `202 ACCEPTED`
```json
{
  "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "ACCEPTED",
  "message": "Webhook received and queued for processing",
  "receivedAt": "2025-10-22T10:00:00.123Z",
  "processedAt": "2025-10-22T10:00:00.456Z",
  "providerName": "stripe",
  "receivedPayload": {
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
  },
  "metadata": {
    "destination": "stripe",
    "sourceIp": "192.168.1.100",
    "httpMethod": "POST",
    "payloadSize": 245,
    "headerCount": 3
  }
}
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

**Response**: `202 ACCEPTED`
```json
{
  "eventId": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
  "status": "ACCEPTED",
  "message": "Webhook received and queued for processing",
  "receivedAt": "2025-10-22T10:05:30.789Z",
  "processedAt": "2025-10-22T10:05:30.891Z",
  "providerName": "github",
  "receivedPayload": {
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
  },
  "metadata": {
    "destination": "github",
    "sourceIp": "140.82.115.1",
    "httpMethod": "POST",
    "payloadSize": 412,
    "headerCount": 5
  }
}
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

#### Example 7: Custom Provider with HTTP Idempotency
```bash
# The X-Idempotency-Key header is automatically handled by lib-common-web
# If the same key is sent again, the cached response is returned immediately
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

**Response**: `202 ACCEPTED`
```json
{
  "eventId": "1a2b3c4d-5e6f-7890-abcd-1234567890ab",
  "status": "ACCEPTED",
  "message": "Webhook received and queued for processing",
  "receivedAt": "2025-10-22T10:15:45.234Z",
  "processedAt": "2025-10-22T10:15:45.345Z",
  "providerName": "my-custom-provider",
  "receivedPayload": {
    "event_type": "user.created",
    "user_id": "12345",
    "email": "user@example.com",
    "timestamp": "2025-10-22T10:00:00Z",
    "metadata": {
      "source": "web",
      "ip_address": "192.168.1.1"
    }
  },
  "metadata": {
    "destination": "my-custom-provider",
    "sourceIp": "192.168.1.1",
    "httpMethod": "POST",
    "payloadSize": 178,
    "headerCount": 4
  }
}
```

#### Example 8: Twilio SMS Webhook
```bash
curl -X POST http://localhost:8080/api/v1/webhook/twilio \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'MessageSid=SM1234567890&From=%2B15551234567&To=%2B15559876543&Body=Hello+World&MessageStatus=received'
```

**Response**: `202 ACCEPTED`
```json
{
  "eventId": "9z8y7x6w-5v4u-3t2s-1r0q-ponmlkjihgfe",
  "status": "ACCEPTED",
  "message": "Webhook received and queued for processing",
  "receivedAt": "2025-10-22T10:20:12.567Z",
  "processedAt": "2025-10-22T10:20:12.678Z",
  "providerName": "twilio",
  "receivedPayload": {
    "MessageSid": "SM1234567890",
    "From": "+15551234567",
    "To": "+15559876543",
    "Body": "Hello World",
    "MessageStatus": "received"
  },
  "metadata": {
    "destination": "twilio",
    "sourceIp": "54.172.60.0",
    "httpMethod": "POST",
    "payloadSize": 112,
    "headerCount": 2
  }
}
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

Spring Boot Actuator provides comprehensive health checks with **real connectivity tests**:

#### Main Health Endpoint

```bash
GET /actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "kafkaConnectivity": {
      "status": "UP",
      "details": {
        "clusterId": "kafka-cluster-1",
        "nodes": 3,
        "status": "Connected"
      }
    },
    "redisConnectivity": {
      "status": "UP",
      "details": {
        "ping": "PONG",
        "readWrite": "OK",
        "version": "7.0.0"
      }
    },
    "readiness": {
      "status": "UP",
      "details": {
        "status": "Ready to accept traffic",
        "circuitBreakerState": "CLOSED"
      }
    },
    "liveness": {
      "status": "UP",
      "details": {
        "status": "Application is alive"
      }
    },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

#### Kubernetes Liveness Probe

```bash
GET /actuator/health/liveness
```

**Purpose**: Determines if the application should be restarted
- Only checks basic application health
- Does NOT check external dependencies (Kafka, Redis)
- Always returns `UP` if application is running

#### Kubernetes Readiness Probe

```bash
GET /actuator/health/readiness
```

**Purpose**: Determines if the application can accept traffic
- Checks circuit breaker state
- Returns `UP` if circuit breaker is CLOSED, HALF_OPEN, or DISABLED
- Returns `DOWN` if circuit breaker is OPEN or FORCED_OPEN

**Available Health Indicators**:
- **kafkaConnectivity**: Real Kafka cluster health check (describes cluster, counts nodes)
- **redisConnectivity**: Real Redis read/write test (PING, SET, GET, DEL operations)
- **readiness**: Circuit breaker state for Kubernetes readiness probe
- **liveness**: Basic application health for Kubernetes liveness probe
- **Disk Space**: Monitors available disk space
- **Ping**: Basic application liveness check

**Configuration**: Health check details are controlled in `application.yml`:
```yaml
management:
  endpoint:
    health:
      show-details: always  # Shows detailed health information
      probes:
        enabled: true       # Enable liveness and readiness probes
  health:
    redis:
      enabled: ${REDIS_HEALTH_ENABLED:true}
    rabbit:
      enabled: ${RABBITMQ_HEALTH_ENABLED:false}
    livenessState:
      enabled: true
    readinessState:
      enabled: true
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
  "sourceIp": "192.168.1.1",
  "httpMethod": "POST"
}
```

### Kafka Message Headers

The platform also sets Kafka message headers:
- `provider` - Provider name (e.g., "stripe")
- `eventId` - Unique event identifier
- `receivedAt` - ISO-8601 timestamp

## 🧪 Development

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
cd fireflyframework-webhooks-web
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
  4. Event-level idempotency check using Redis (worker-level)
  5. Signature validation
  6. Business logic processing

Tests automatically start Docker containers for:
- Kafka (Confluent Platform 7.5.0)
- Redis (Redis 7 Alpine)

### Code Structure

```
fireflyframework-webhooks-web/
├── src/main/java/
│   └── com/firefly/common/webhooks/
│       ├── web/
│       │   ├── WebhookManagementApplication.java    # Main application
│       │   └── controllers/
│       │       └── WebhookController.java           # Webhook ingestion
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

See the [Processor Framework README](fireflyframework-webhooks-processor/README.md) for detailed instructions on building webhook workers.

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

### Custom Webhook Metrics

The platform exposes custom webhook metrics via Micrometer:

```bash
curl http://localhost:8080/actuator/prometheus | grep webhook
```

**Custom Metrics**:
- `webhook_received_total{provider="stripe"}` - Total webhooks received per provider
- `webhook_published_total{provider="stripe"}` - Total webhooks published to Kafka per provider
- `webhook_processing_time_seconds{provider="stripe"}` - Processing time histogram per provider

**Resilience4j Metrics**:
- `resilience4j_circuitbreaker_state{name="webhookKafkaPublisher"}` - Circuit breaker state (0=closed, 1=open, 2=half_open)
- `resilience4j_circuitbreaker_calls_total{name="webhookKafkaPublisher",kind="successful"}` - Successful calls
- `resilience4j_circuitbreaker_calls_total{name="webhookKafkaPublisher",kind="failed"}` - Failed calls
- `resilience4j_ratelimiter_available_permissions{name="webhook-provider-stripe"}` - Available rate limit permits
- `resilience4j_timelimiter_calls_total{name="webhookKafkaPublisher",kind="successful"}` - Timeout metrics

**Standard Metrics**:
- `http_server_requests_seconds` - HTTP request duration
- `kafka_producer_record_send_total` - Kafka messages sent
- `cache_gets_total` - Cache get operations
- `cache_puts_total` - Cache put operations
- `jvm_memory_used_bytes` - JVM memory usage

### Health Checks

Comprehensive health checks available at `/actuator/health`:

```bash
curl http://localhost:8080/actuator/health
```

**Response Example**:
```json
{
  "status": "UP",
  "components": {
    "webhookCircuitBreaker": {
      "status": "UP",
      "details": {
        "circuitBreakerName": "webhookKafkaPublisher",
        "state": "CLOSED",
        "failureRate": "0.0%",
        "slowCallRate": "0.0%",
        "bufferedCalls": 10,
        "failedCalls": 0,
        "slowCalls": 0,
        "notPermittedCalls": 0
      }
    },
    "kafka": {
      "status": "UP",
      "details": {
        "clusterId": "kafka-cluster-1"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

**Health Indicators**:
- **webhookCircuitBreaker**: Circuit breaker state and metrics
- **kafka**: Kafka broker connectivity
- **redis**: Redis connectivity (if enabled)
- **diskSpace**: Available disk space
- **ping**: Basic application liveness

### Distributed Tracing

The platform supports **complete end-to-end distributed tracing** with B3 propagation across the entire webhook processing pipeline:

**Trace Flow**:
1. **HTTP Request** → Trace context created/extracted from headers
2. **Kafka Message** → Trace context propagated to Kafka message headers
3. **Worker Processing** → Trace context extracted from Kafka headers and set in MDC

**Trace Headers**:
- `X-B3-TraceId` - Unique trace identifier (propagated across HTTP → Kafka → Worker)
- `X-B3-SpanId` - Unique span identifier
- `X-Request-ID` - Request ID for correlation

**MDC Fields** (included in all logs):
- `traceId` - Trace ID for correlation
- `spanId` - Span ID for correlation
- `requestId` - Request ID for business correlation

**Kafka Message Headers** (automatically propagated):
- `X-B3-TraceId` - Trace ID from HTTP request
- `X-B3-SpanId` - Span ID from HTTP request
- `X-Request-ID` - Request ID from HTTP request

**Worker Trace Extraction**:
Workers automatically extract trace context from Kafka message headers using `TracingContextExtractor`:

```java
// In your worker listener
@Override
protected void processEvent(WebhookReceivedEvent event, Message<?> message) {
    // Trace context is automatically extracted and set in MDC
    // All logs will include traceId, spanId, requestId
    log.info("Processing webhook: {}", event.getEventId());
}
```

**Zipkin Integration**:
```bash
# Configure Zipkin endpoint
export MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://localhost:9411/api/v2/spans
export MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0  # Sample 100% (adjust for production)

# View traces in Zipkin UI
open http://localhost:9411/zipkin/
```

**Trace Visualization**:
In Zipkin, you can see the complete trace from HTTP request → Kafka publish → Worker processing, with timing information for each step.

### Structured Logging

The application uses structured JSON logging with correlation IDs:

**Log Format**:
```json
{
  "timestamp": "2025-10-22T10:00:00.123Z",
  "level": "INFO",
  "logger": "org.fireflyframework.webhooks.web.controllers.WebhookController",
  "message": "Webhook received",
  "eventId": "evt-456",
  "providerName": "stripe",
  "traceId": "d85ad13a8f294eeba88630568721bcca",
  "spanId": "b1866364ef3f43fb",
  "transactionId": "30f7fe73-349a-4336-adc8-e5bd8bf80f51"
}
```

**HTTP Request Logging**:
```json
{
  "type": "HTTP_REQUEST",
  "timestamp": "2025-10-22T14:00:38.347739Z",
  "requestId": "951399678300500",
  "method": "POST",
  "path": "/api/v1/webhook/stripe",
  "headers": {
    "Content-Type": "application/json",
    "Stripe-Signature": "t=**********,v1=3141dbd5...",
    "X-Transaction-Id": "30f7fe73-349a-4336-adc8-e5bd8bf80f51"
  }
}
```

**HTTP Response Logging**:
```json
{
  "type": "HTTP_RESPONSE",
  "timestamp": "2025-10-22T14:00:38.579737Z",
  "requestId": "951399678300500",
  "statusCode": 202,
  "durationMs": 232,
  "headers": {
    "X-B3-TraceId": "d85ad13a8f294eeba88630568721bcca",
    "X-B3-SpanId": "b1866364ef3f43fb",
    "X-Request-ID": "053291b8-0f03-4dbd-8942-4036c34857fb"
  }
}
```



## 📊 Monitoring & Observability

### Prometheus Metrics

The platform exposes comprehensive metrics:

```
# Request metrics
webhook_received_total{provider="stripe",status="success"}
webhook_received_rate{provider="stripe"}
webhook_processing_latency_seconds{provider="stripe",quantile="0.95"}

# DLQ metrics
webhook_dlq_total{category="VALIDATION_FAILED"}
webhook_dlq_rate

# Batching metrics
webhook_batch_size{provider="stripe"}
webhook_batch_efficiency_ratio

# Compression metrics
webhook_compression_ratio{provider="stripe"}
webhook_compression_bytes_saved_total
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

- [Processor Framework Guide](fireflyframework-webhooks-processor/README.md) - How to build webhook workers
- [Configuration Guide](CONFIGURATION_GUIDE.md) - Complete configuration reference for all features

