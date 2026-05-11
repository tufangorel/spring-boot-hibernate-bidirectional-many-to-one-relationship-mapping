# Spring Boot Hibernate Bidirectional Many-to-One Relationship Mapping

[![Java](https://img.shields.io/badge/Java-25-orange)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9.11-blue)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A comprehensive Spring Boot application demonstrating Hibernate bidirectional many-to-one relationship mapping with a complete customer order management system.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [Caching](#caching)
- [Rate Limiting](#rate-limiting)
- [Rate limiting design](doc/rate-limiting.md)
- [Distributed tracing](#distributed-tracing)
- [Tracing design document](doc/tracing.md)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## ✨ Features

- **Bidirectional Relationship Mapping**: Demonstrates Hibernate many-to-one relationships between Customer, CustomerOrder, OrderItem, and ShippingAddress entities
- **RESTful API**: Complete CRUD operations for all entities
- **Swagger UI**: Interactive API documentation
- **H2 Database**: In-memory database with console access
- **Resilience4j**: Circuit breaker, retry, time limiter, and bulkhead patterns for service reliability
- **Global Error Handling**: Centralized exception handling with standardized HTTP error responses
- **Validation**: Jakarta Bean Validation for request payloads and domain constraints
- **Spring Boot Actuator**: Health checks, metrics, info, and Prometheus-compatible metrics export
- **Comprehensive testing**: Unit tests, `*IntegrationTest` Spring Boot tests, optional **`TracingSmokeTest`** (HTTP tracing sanity check without the `IntegrationTest` name so it still runs when Surefire excludes `**/*IntegrationTest.java`), and Layer 2 container smoke tests
- **Containerized Testing**: Docker-based smoke tests for production-like validation
- **Idempotent order creation**: Optional `Idempotency-Key` header on `POST /customerorder/save` stores a mapping in `idempotency_record` so retries return the same saved order instead of creating duplicates
- **Response caching**: Spring Cache with Caffeine on read paths in the service layer (`@Cacheable` / `@CacheEvict`), with cache-friendly JPA fetch queries for customer orders and shipping-address lookups
- **Distributed tracing (Micrometer + Brave)**: W3C `traceparent` propagation, `traceId` / `spanId` in logs, `X-Trace-Id` on JSON responses, and `traceId` on standardized error payloads
- **Graceful shutdown**: Spring Boot graceful server shutdown with lifecycle timeout and application shutdown hooks for predictable stop behavior
- **Stabilized integration tests**: Isolated Spring test contexts, per-test circuit-breaker reset, and dedicated in-memory H2 URLs for flaky integration classes

## 🏗️ Architecture

### Entity Relationships

```
Customer (1) ────→ (Many) CustomerOrder
Customer (1) ────→ (1) ShippingAddress
CustomerOrder (1) ────→ (Many) OrderItem
```

**Key Points:**
- Many OrderItems are related to one parent CustomerOrder entity
- Bidirectional navigation between entities
- Cascade operations for data persistence
- Lazy/Eager loading configurations

### Idempotency (customer orders)

Create-order requests may send an optional HTTP header `Idempotency-Key` (non-blank string, up to 128 characters in storage). The first successful save persists the new order id keyed by `(idempotency_key, entity_type)` in the `idempotency_record` table. Later requests with the same key for customer orders load that order from the database and return it with no second insert. If the header is omitted or blank, behavior is unchanged from a normal create.

### ER Diagram

![Entity Relationship Diagram](doc/many_to_one_er_diagram.png)

## 🛠️ Tech Stack

- **Java**: 25
- **Spring Boot**: 4.0.6
- **Spring Data JPA**: Hibernate implementation
- **Resilience4j**: Circuit breaker, retry, time limiter, and bulkhead support
- **Jakarta Bean Validation**: Request payload validation via Spring Boot starter validation
- **Database**: H2 (In-memory)
- **Build Tool**: Maven 3.9.11
- **Documentation**: SpringDoc OpenAPI (Swagger)
- **Testing**: JUnit 5, Spring Boot Test
- **Monitoring**: Spring Boot Actuator (health, metrics, info) and Micrometer Prometheus registry
- **Logging**: Logback
- **SQL observability**: datasource-proxy (JDBC proxy for SQL logging and diagnostics)
- **Caching**: Spring Cache abstraction backed by **Caffeine** (`spring-boot-starter-cache` + `caffeine`)
- **Rate limiting**: [Bucket4j](https://github.com/bucket4j/bucket4j) token buckets (`bucket4j-core`) cached by Caffeine and applied through Spring AOP
- **Tracing**: Spring Boot Micrometer Tracing with Brave (`spring-boot-micrometer-tracing-brave`, `micrometer-tracing-bridge-brave`); W3C propagation; no remote exporter in the default POM (add Zipkin/OTLP when you need a trace backend)
- **Containerization**: Docker & Docker Compose

## 📋 Prerequisites

- **Java**: JDK 25 or higher
- **Maven**: 3.9.11 or higher
- **Docker**: For running smoke tests (optional)
- **Git**: For version control

## 🚀 Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/tufangorel/spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping.git
   cd spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping
   ```

2. **Build the application**

   **Linux / macOS / Git Bash:**
   ```bash
   ./mvnw clean compile
   ```

   **Windows (Command Prompt or PowerShell, from the project root):**
   ```powershell
   .\mvnw.cmd clean compile
   ```

3. **Run the application**

   **Linux / macOS / Git Bash:**
   ```bash
   ./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
   ```

   **Windows:**
   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
   ```

The application will start on `http://localhost:8080/customer-info`

## 📖 Usage

### Application Profiles

- **dev**: Development profile with detailed logging; **Spring Cache is disabled** (`spring.cache.type=none`) so integration tests stay deterministic
- **test**: Testing profile with test-specific configurations
- **default** (no profile, or non-dev): Caffeine cache settings from `application.yml` apply

### Accessing the Application

- **Swagger UI**: http://localhost:8080/customer-info/swagger-ui/index.html
- **H2 Console**: http://localhost:8080/customer-info/h2/
- **Health Check**: http://localhost:8080/customer-info/actuator/health
- **Readiness Health Check**: http://localhost:8080/customer-info/actuator/health/readiness
- **Prometheus scrape**: http://localhost:8080/customer-info/actuator/prometheus

## ⚙️ Configuration

The application uses `src/main/resources/application.yml` to configure the servlet context path, H2 datasource, JPA settings, logging, actuator endpoints, **Micrometer tracing** (Brave, W3C propagation, sampling), Resilience4j policies, graceful shutdown behavior, and Caffeine-backed Spring Cache. The default context path is `/customer-info`.

The `resilience4j` configuration includes:
- `circuitbreaker` for failure isolation
- `retry` for transient error retries
- `timelimiter` for request timeouts
- `bulkhead` for concurrent call limits

Actuator web endpoints exposed by default in `application.yml` include `health`, `metrics`, `prometheus`, and `info`.

### Detailed DB Readiness Health

The readiness group includes a custom `readinessDb` indicator with deep database checks:

- Executes `SELECT 1` and reports `queryLatencyMs`
- Adds Hikari pool details when available (`active`, `idle`, `total`, `max`, `threadsAwaitingConnection`)
- Applies configurable degradation thresholds under `app.health.db.*`

Threshold configuration in `application.yml`:

- `app.health.db.max-latency-ms` (default `200`)
- `app.health.db.max-active-ratio` (default `0.9`)
- `app.health.db.max-waiting-threads` (default `0`)

Health states:

- `UP`: query succeeds and thresholds are within limits
- `OUT_OF_SERVICE`: query succeeds but one or more thresholds are exceeded (`reasons` field explains why)
- `DOWN`: query or connection fails

Example readiness endpoints:

- `GET /customer-info/actuator/health/readiness`
- `GET /customer-info/actuator/health`

### Graceful Shutdown

Graceful shutdown is enabled for both `.yml` and `.properties` configuration paths:

- `server.shutdown=graceful`
- `spring.lifecycle.timeout-per-shutdown-phase=30s`

On shutdown, `GracefulShutdownListener` logs both shutdown start (`ContextClosedEvent`) and resource release completion (`@PreDestroy`).

### H2 Database Configuration

- **URL**: `jdbc:h2:mem:cust`
- **Username**: `sa`
- **Password**: `123456`
- **Driver**: `org.h2.Driver`

## Caching

Caching is applied in **services** (not controllers): `@Cacheable` on read methods and `@CacheEvict` (including `@Caching`) after writes so lists and detail views stay consistent.

| Cache name | Backed data | Typical invalidation |
|------------|-------------|----------------------|
| `customers` | `CustomerService.findAll`, `findCustomerById` | Customer save/delete (and idempotent customer save) |
| `customerOrders` | `CustomerOrderService.findAll`, `findById` | Customer order save/delete; order item save/delete |
| `orderItems` | `OrderItemService.findAll`, `findById` | Order item save/delete; customer order save/delete |
| `customerByShippingAddress` | `ShippingAddressService.findCustomerByShippingAddressID` | Customer save/delete |

**Defaults** (`application.yml`): `spring.cache.type=caffeine`, up to **1000 entries** per cache, **10 minutes** time-to-live after write (`expireAfterWrite`). Cache names are declared explicitly under `spring.cache.cache-names`.

**JPA and JSON**: Customer orders are loaded with **join-fetch** repository methods (`findAllWithAssociations`, `findByIdWithAssociations`) so cached `CustomerOrder` graphs include `orderItems`, `customer`, and `shippingAddress` where needed. The shipping-address lookup uses a `Customer`-root fetch query so Hibernate 6 join rules are satisfied.

**Profiles**: The **`dev`** profile sets `spring.cache.type=none` in `application-dev.properties` (integration tests use `@ActiveProfiles("dev")`). **Test** `application*.properties` also set `spring.cache.type=none` so Surefire runs do not depend on cache state. Run **without** the `dev` profile (default `application.yml`) to exercise in-memory caching locally.

## Rate Limiting

Per-user rate limiting is enforced at the **service layer** with [Bucket4j](https://github.com/bucket4j/bucket4j) token buckets. The `RateLimitAspect` is ordered at `Ordered.HIGHEST_PRECEDENCE`, so a rejected call short-circuits **before** any other advice runs (transactions, caching, circuit breakers, retries, validation).

> A standalone design document with rationale, full Mermaid diagrams (architecture, sequence, bucket-resolution flow, class diagram), the "Why Bucket4j" comparison, and operational notes lives in [`doc/rate-limiting.md`](doc/rate-limiting.md).

### How a request is identified

A `UserKeyFilter` registered with `Ordered.HIGHEST_PRECEDENCE + 10` (after Micrometer’s HTTP observation filter opens the trace span) populates a `ThreadLocal` `UserContext` at the start of every request via `DefaultUserKeyResolver`:

1. `X-User-Id` header (trimmed) → `user:<id>`
2. else first hop of `X-Forwarded-For` → `ip:<address>`
3. else `HttpServletRequest.getRemoteAddr()` → `ip:<address>`
4. else no key resolved (the aspect treats the caller as `anonymous`)

The filter clears `UserContext` after the request completes.

### Annotating a service method

Methods are annotated with `@RateLimited`. The annotation supports a logical bucket key plus optional per-method overrides:

```java
@RateLimited(key = "customer.write")
public Customer save(Customer customer, String idempotencyKey) { ... }

@RateLimited(key = "customer.read")
public List<Customer> findAll() { ... }

@RateLimited(key = "report.export", capacity = 5, refillTokens = 5, refillPeriodSeconds = 300)
public byte[] export() { ... }
```

If `key` is empty the bucket id falls back to `<SimpleClassName>#<methodName>`. The annotation is applied across the four service classes:

| Service | Methods | Bucket key |
|---|---|---|
| `CustomerService` | `save`, `deleteCustomerById` | `customer.write` |
| `CustomerService` | `findAll`, `findCustomerById` | `customer.read` |
| `CustomerOrderService` | `save`, `deleteCustomerOrderById` | `customerOrder.write` |
| `CustomerOrderService` | `findAll`, `findById` | `customerOrder.read` |
| `OrderItemService` | `save`, `deleteOrderItemById` | `orderItem.write` |
| `OrderItemService` | `findAll`, `findById` | `orderItem.read` |
| `ShippingAddressService` | `findCustomerByShippingAddressID` | `shippingAddress.read` |

### Bucket sizing

When the aspect resolves the limit it checks, in order:

1. Per-annotation values (`capacity`, `refillTokens`, `refillPeriodSeconds`) when greater than 0
2. A named profile under `app.rate-limit.profiles.<suffix>`, where `<suffix>` is the substring after the last `.` of the key (e.g. `customer.write` → `write`)
3. The global `app.rate-limit.default` profile

Buckets are stored in a Caffeine cache keyed by `<userKey>|<bucketId>` so each user gets an isolated bucket per logical operation; the cache is bounded by `app.rate-limit.cache.max-size`.

### Configuration

```yaml
app:
  rate-limit:
    enabled: true
    cache:
      max-size: 100000        # max cached buckets across all (user, bucket) pairs
    default:
      capacity: 60
      refill-tokens: 60
      refill-period-seconds: 60
    profiles:
      write:
        capacity: 20
        refill-tokens: 20
        refill-period-seconds: 60
      read:
        capacity: 100
        refill-tokens: 100
        refill-period-seconds: 60
```

Set `app.rate-limit.enabled=false` to disable enforcement entirely; the aspect short-circuits to `proceed()` without consulting any bucket.

### Response when the limit is exceeded

`RateLimitExceededException` is mapped by `GlobalExceptionHandler` to `HTTP 429 Too Many Requests` with a `Retry-After` header (seconds until the next token is available):

Example **429** response shape (values vary at runtime: `timestamp` from `LocalDateTime`, `retryAfterSeconds` / `Retry-After` from the bucket probe, `traceId` / `X-Trace-Id` only when Micrometer has a current span):

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 12
X-Trace-Id: 6a01a08d44bad222069ae3b5d782f228
Content-Type: application/json
```

```json
{
  "timestamp": "2026-05-08T12:00:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded for bucket: customer.write",
  "retryAfterSeconds": 12,
  "traceId": "6a01a08d44bad222069ae3b5d782f228"
}
```

### Tests

- `RateLimitAspectTest` (8 unit tests) — token consumption, per-user isolation, `enabled=false` short-circuit, anonymous-user fallback, per-annotation overrides, named-profile resolution, blank-key signature fallback.
- `UserKeyResolverTest` (7 unit tests) — `null` request, `X-User-Id` precedence and trimming, `X-Forwarded-For` first-hop, blank/missing forwarded header, missing `RemoteAddr`, all-missing.
- `RateLimitExceededExceptionTest` (1 unit test) — exposes `userKey`, `bucketId`, and `retryAfterNanos` accessors.
- `RateLimitIntegrationTest` (3 end-to-end tests) — boots the full Spring context and verifies HTTP 429 with `Retry-After` after `capacity + 1` requests, isolation between two `X-User-Id` values, IP-based bucketing when no header is present, and **`traceId` / `X-Trace-Id`** on 429 when tracing is active.

## Distributed tracing

> **Design reference:** rationale, lifecycle diagrams, configuration tables, and operational notes are in [`doc/tracing.md`](doc/tracing.md).

The app uses **Spring Boot Micrometer Tracing** with the **Brave** bridge (`spring-boot-micrometer-tracing-brave` + `micrometer-tracing-bridge-brave`). There is **no Zipkin/OTLP exporter** in the default `pom.xml`; add one when you want a remote trace backend.

### Propagation and sampling

- **W3C** `traceparent` / `tracestate` are the configured consume/produce propagation type (`management.tracing.propagation` in `application.yml`).
- **Sampling**: probability `1.0` by default; the **`prod`** profile sets `management.tracing.sampling.probability` to `0.1`.

### Logs

- Logback patterns include **`[%X{traceId:-},%X{spanId:-}]`** for request-scoped correlation.
- `logging.pattern.correlation` is set for Spring Boot’s default correlation formatting alongside the custom layout.

### HTTP API

- **`X-Trace-Id`**: added on successful `@RestController` JSON responses via `TraceIdResponseAdvice` (runs while the Micrometer span is still current during body serialization).
- **Error JSON**: `GlobalExceptionHandler` adds a **`traceId`** field and repeats **`X-Trace-Id`** on validation and other standardized error responses (including HTTP 429 from rate limiting).

### Tests

- **`TracingIntegrationTest`** — random-port HTTP checks: `X-Trace-Id` on **GET**/**POST** success (including **201**), `traceId` aligned with **`X-Trace-Id`** on validation **400** and **`IllegalArgumentException`** **400**, inbound W3C **`traceparent`** honored.
- **`TracingSmokeTest`** — three minimal HTTP checks (same themes as above, no `IntegrationTest` suffix so it still runs when Surefire excludes `**/*IntegrationTest.java`).
- **`TraceIdResponseAdviceTest`** / **`GlobalExceptionHandlerTracingTest`** — Mockito unit tests for response advice and exception handler trace correlation (see [`doc/tracing.md`](doc/tracing.md#82-unit-tests-mockito)).

## 📚 API Documentation

### Customer Management

#### Create Customer
```bash
POST /customer-info/customer/save
Content-Type: application/json
```

```json
{
  "name": "John Doe",
  "age": 30,
  "shippingAddress": {
    "streetName": "123 Main St",
    "city": "Istanbul",
    "country": "TR"
  }
}
```

`name` is **2–100** characters, `age` **18–120**, `shippingAddress` is optional; when present, `streetName` **2–100**, `city` and `country` each **2–50** (all `@NotBlank`).

#### List Customers
```bash
GET /customer-info/customer/list
```

#### Update Customer
```bash
PUT /customer-info/customer/update/{id}
```

#### Delete Customer
```bash
DELETE /customer-info/customer/delete/{id}
```

### Order Management

#### Create Customer Order with Items

Optional header for safe retries (same body + same key returns the first persisted order):

```http
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

```bash
POST /customer-info/customerorder/save
Content-Type: application/json
```

```json
{
  "customer": {
    "id": 1,
    "name": "John Doe",
    "age": 30,
    "shippingAddress": {
      "id": 1,
      "streetName": "123 Main St",
      "city": "Istanbul",
      "country": "TR"
    }
  },
  "orderDate": "2026-04-28T16:00:00",
  "title": "Spring Order",
  "orderItems": [
    { "quantity": 2 },
    { "quantity": 1 }
  ]
}
```

`orderDate` is ISO-8601 `LocalDateTime` (no timezone). `title` is **2–100** characters. Each `orderItems[].quantity` is **1–10000**. The nested **`customer.id`** and **`shippingAddress.id`** must match rows already in the database (for example the ids returned by **Create Customer**). Wrong or missing ids lead to persistence errors or wrong associations.

#### Create customer order (customer reference only)

Same endpoint. When the customer already exists, you can send **only** `customer.id` (no name, address, or shipping-address ids). Replace **`42`** in the JSON below with the integer `id` from **Create Customer** or **List Customers**; this matches the payload shape used in `CustomerOrderIdempotencyIntegrationTest`. A **new** customer cannot be created from this shape alone (`CustomerOrder.customer` has no `CascadeType.PERSIST`); use the full nested customer above or create the customer first.

```bash
POST /customer-info/customerorder/save
Content-Type: application/json
```

```json
{
  "customer": { "id": 42 },
  "orderDate": "2026-06-01T10:30:00",
  "title": "Minimal payload order",
  "orderItems": [
    { "quantity": 1 },
    { "quantity": 2 }
  ]
}
```

#### List Customer Orders
```bash
GET /customer-info/customerorder/list
```

#### Update Customer Order
```bash
PUT /customer-info/customerorder/update/{id}
```

#### Delete Customer Order
```bash
DELETE /customer-info/customerorder/delete/{id}
```

### Order Item Management

#### List Order Items
```bash
GET /customer-info/orderitem/list
```

#### Create Order Item
```bash
POST /customer-info/orderitem/save
Content-Type: application/json
```

```json
{
  "quantity": 5
}
```

`quantity` must be **1–10000** (`int`, required).

#### Update Order Item
```bash
PUT /customer-info/orderitem/update/{id}
```

#### Delete Order Item
```bash
DELETE /customer-info/orderitem/delete/{id}
```

## 🧪 Testing

### Unit Tests & Integration Tests

Use the Maven Wrapper from the repo root (`./mvnw` on Unix/macOS, `mvnw.cmd` on Windows).

Run all tests (unit + integration + `TracingSmokeTest`):

```bash
./mvnw clean test
```

Run only classes named `*IntegrationTest` (full Spring contexts; excludes `TracingSmokeTest`):

```bash
./mvnw test "-Dsurefire.includes=**/*IntegrationTest.java"
```

Run **unit-style tests only** (skip `*IntegrationTest` and `TracingSmokeTest`; faster CI slice):

```bash
./mvnw test "-Dsurefire.excludes=**/*IntegrationTest.java,**/TracingSmokeTest.java"
```

Integration tests include, among others, `CustomerOrderIdempotencyIntegrationTest` (duplicate `POST` with the same `Idempotency-Key`), `CustomerOrderServiceIntegrationTest`, `CustomerServiceIntegrationTest`, `DatasourceProxyListenerIntegrationTest`, `GracefulShutdownIntegrationTest`, `RateLimitIntegrationTest`, and `TracingIntegrationTest`.

Caching is turned off under the `dev` profile and in shared test `application*.properties` (`spring.cache.type=none`), so these tests always hit the database unless you change that configuration.

Integration test stability improvements:

- Spring context isolation with `@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)` on core integration suites
- Explicit `CircuitBreakerRegistry` reset in `@BeforeEach` for `customerService`
- Dedicated H2 in-memory URLs per integration class to avoid shared schema/state side effects

Graceful shutdown coverage:

- `GracefulShutdownIntegrationTest` validates graceful shutdown properties and shutdown listener lifecycle logs during context close.

JaCoCo runs in the **`test`** phase (`pom.xml`); after `./mvnw clean test`, open **`target/site/jacoco/index.html`** for the HTML report.

### Layer 2 Smoke Tests

Layer 2 smoke tests run the **packaged** Spring Boot app in **Docker Compose**, then call a small set of **public REST endpoints** over HTTP. They are **not** the same as Maven `*IntegrationTest` classes under `src/test` (those run via Surefire).

#### Prerequisites for Smoke Tests

- **Docker** installed and running (`docker info` must succeed)
- **`docker compose`** (Compose V2) on your `PATH`
- **Repository layout** — each runner resolves the **repository root** from the script path and `cd`s there, so you may invoke the script from any working directory if you use an absolute or correct relative path to the script file
- **Host port `8080` free** — `docker-compose.yml` publishes `8080:8080`

#### Running Smoke Tests

**Unix / Linux / macOS:**

```bash
bash .github/integration-tests/run-layer2-tests.sh
```

**Windows (PowerShell or cmd, from repo root):**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .github/integration-tests/run-layer2-tests.ps1
```

#### Execution flow (both runners)

1. **Build JAR** — `mvnw` / `mvnw.cmd -DskipTests package` (tests skipped; a fresh `target/...jar` is required for the Docker image `COPY`).
2. **Start stack** — `docker compose -f .github/integration-tests/docker-compose.yml up -d --build`  
   - **Image:** `customer-info-app:layer2-smoke`  
   - **Dockerfile:** `.github/integration-tests/Dockerfile` — copies `target/spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping.jar` into `eclipse-temurin:25-jdk-jammy`, `java -jar /app/app.jar`  
   - **Container env:** `SPRING_PROFILES_ACTIVE=dev`, `SERVER_SERVLET_CONTEXT_PATH=/customer-info`
3. **Wait for readiness** — poll **`GET http://localhost:8080/customer-info/actuator/health`** until success or **~120 seconds** elapses (**3 second** sleep between attempts).
4. **HTTP checks** (all against `http://localhost:8080/customer-info`):
   - **`POST /customer/save`** — JSON customer + shipping address; response must contain `"id"`.
   - **`POST /customerorder/save`** — JSON order with nested customer and two order items; response must contain `"id"` and `"orderItems"`.
   - **`GET /orderitem/list`** — response must contain `"quantity"` (order items persisted and listed).
5. **Teardown** — `docker compose ... down --remove-orphans` (Unix: `trap` on `EXIT`; Windows: registered on `PowerShell.Exiting`; run cleanup when the shell exits).

#### What Smoke Tests Validate

- Application **startup** and **Actuator health** (`/actuator/health`) from the host
- **Customer** create via REST and persistence (H2 in the container)
- **Customer order** create with **multiple order items** and nested JSON
- **Order item list** retrieval after writes (relationship / persistence path)
- End-to-end behavior **inside the published container port** (same as a local `8080` smoke)

#### Console output

On success the scripts print a banner similar to:

```
Layer 2 smoke tests PASSED
========================================
```

## 📁 Project Structure

```
spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping/
├── .github/
│   └── integration-tests/          # Layer 2 smoke tests
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/company/customerinfo/
│   │   │       ├── config/         # Configuration classes
│   │   │       ├── controller/     # REST controllers
│   │   │       ├── model/          # JPA entities (including IdempotencyRecord)
│   │   │       ├── repository/     # Data repositories (including IdempotencyRecordRepository)
│   │   │       ├── service/        # Business logic, Resilience4j, @Cacheable / @CacheEvict
│   │   │       └── CustomerInfoApplication.java
│   │   └── resources/              # Application properties
│   └── test/                       # Unit and integration tests
├── doc/                            # Documentation and diagrams
├── pom.xml                         # Maven configuration
├── mvnw & mvnw.cmd                 # Maven wrapper
└── README.md                       # This file
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Spring Boot best practices
- Write comprehensive unit tests
- Update documentation for API changes
- Ensure all tests pass before submitting PR
- Use meaningful commit messages

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙋 Support

If you have any questions or issues:

1. Check the [Issues](https://github.com/tufangorel/spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping/issues) page
2. Review the API documentation in Swagger UI
3. Check the Layer 2 smoke test results for common issues

---

**Happy Coding! 🚀**
