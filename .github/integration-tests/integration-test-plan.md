# Layer 2 Smoke Test Plan

## Purpose
This plan adds Layer 2 smoke tests for the `spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping` application. Layer 2 validates the running Spring Boot application through HTTP requests inside a Docker Compose environment without adding new JUnit test classes.

## Coverage Gap
- No Docker Compose environment currently exists for app-level smoke tests.
- Existing tests are unit and integration tests inside the application, but no shell-based Layer 2 smoke tests validate start-to-serve behavior in a containerized environment.
- The application relies on H2 in-memory database and should be exercised through its public REST endpoints.

## Target Components
- `CustomerController` `/customer/save` and `/customer/list`
- `CustomerOrderController` `/customerorder/save`
- `OrderItemController` `/orderitem/list`
- Spring Boot application startup and health endpoint under `/customer-info`
- The existing in-memory H2 database configuration

## Strategy
1. Build the application JAR with the Maven wrapper: **`./mvnw`** (Unix) or **`mvnw.cmd`** (Windows) **`-DskipTests package`** (Surefire tests are skipped; the Docker image copies `target/...jar`).
2. Use Docker Compose to build and launch the application container (`docker compose ... up -d --build`).
3. Poll **`GET http://localhost:8080/customer-info/actuator/health`** from the host until HTTP succeeds or **~120 seconds** elapse, sleeping **3 seconds** between attempts.
4. Run HTTP smoke requests against **`http://localhost:8080/customer-info`**:
   - `POST /customer/save` (customer + shipping address)
   - `POST /customerorder/save` (order with nested customer and multiple order items)
   - `GET /orderitem/list` (assert order items with quantities)
5. Tear down the Docker Compose environment (`docker compose ... down --remove-orphans`).

## Dependencies and Requirements
- Docker must be installed and running (`docker info`).
- Docker Compose V2 must be available as **`docker compose`**.
- **Host port `8080` must be free** (compose maps `8080:8080`).
- **`curl`** is used by the Unix runner for HTTP checks; the **Windows** runner uses **`Invoke-WebRequest`** (PowerShell). The **container** Compose healthcheck uses `curl` inside the image.
- On Windows, PowerShell must be available to run `run-layer2-tests.ps1`.
- The application uses the existing in-memory H2 database and does not require additional external services.

## Implementation Artifacts
- `.github/integration-tests/Dockerfile`
- `.github/integration-tests/docker-compose.yml`
- `.github/integration-tests/run-layer2-tests.sh`
- `.github/integration-tests/run-layer2-tests.ps1`
- `.github/integration-tests/integration-test-plan.md`

## Validation Criteria
- `./mvnw -DskipTests package` completes successfully.
- Docker Compose starts the application successfully.
- The health endpoint responds with HTTP 200.
- The customer save endpoint returns an object containing an `id`.
- The customer order save endpoint returns an object containing an `id` and nested order items.
- The order item list endpoint returns an array structure.
- The runner script exits with code 0 on success.
