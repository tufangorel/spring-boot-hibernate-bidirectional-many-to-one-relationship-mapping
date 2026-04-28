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
1. Build the application jar using the Maven wrapper.
2. Use Docker Compose to launch the application container.
3. Wait for the `/customer-info/actuator/health` endpoint to become available.
4. Execute shell-based smoke tests using HTTP requests:
   - health endpoint check
   - save a customer
   - save a customer order with multiple order items
   - list order items and assert the expected response structure
5. Tear down the Docker Compose environment after tests.

## Dependencies and Requirements
- Docker must be installed and running.
- Docker Compose capability must be available via `docker compose`.
- `curl` must be available for the shell-based smoke script.
- On Windows, PowerShell must be available.
- The application uses the existing H2 database and does not require additional external services.

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
