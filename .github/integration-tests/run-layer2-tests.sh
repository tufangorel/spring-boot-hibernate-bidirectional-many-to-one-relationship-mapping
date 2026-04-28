#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/.github/integration-tests/docker-compose.yml"

cd "$REPO_ROOT"

echo "Checking Docker availability..."
if ! docker info > /dev/null 2>&1; then
  echo "ERROR: Docker is not running. Please start Docker and try again."
  exit 1
fi

echo "Building application jar..."
./mvnw -DskipTests package

echo "Starting application with Docker Compose..."
docker compose -f "$COMPOSE_FILE" up -d --build

cleanup() {
  echo "Stopping Docker Compose environment..."
  docker compose -f "$COMPOSE_FILE" down --remove-orphans
}
trap cleanup EXIT

BASE_URL="http://localhost:8080/customer-info"

echo "Waiting for application readiness..."
MAX_WAIT=120
WAIT_SECONDS=0
until curl --silent --fail "$BASE_URL/actuator/health" > /dev/null 2>&1; do
  if [ "$WAIT_SECONDS" -ge "$MAX_WAIT" ]; then
    echo "ERROR: Application did not become healthy within $MAX_WAIT seconds."
    exit 1
  fi
  sleep 3
  WAIT_SECONDS=$((WAIT_SECONDS + 3))
  echo "Waiting for health endpoint... ($WAIT_SECONDS/$MAX_WAIT)"
done

echo "Application is healthy. Running Layer 2 smoke tests..."

run_request() {
  local method="$1"
  local url="$2"
  local body="$3"
  local output

  if [ "$method" = "GET" ]; then
    output="$(curl --silent --fail -H 'Accept: application/json' "$url")"
  else
    output="$(curl --silent --fail -H 'Content-Type: application/json' -X "$method" -d "$body" "$url")"
  fi

  echo "$output"
}

customer_payload='{"name":"Smoke Customer","age":30,"shippingAddress":{"streetName":"Smoke Street","city":"Ankara","country":"TR"}}'
customer_response="$(run_request POST "$BASE_URL/customer/save" "$customer_payload")"
if ! echo "$customer_response" | grep -q '"id"'; then
  echo "ERROR: Customer save response did not contain id."
  echo "$customer_response"
  exit 1
fi

echo "Customer save passed."

order_payload='{"customer":{"id":1,"name":"Smoke Customer","age":30,"shippingAddress":{"id":1,"streetName":"Smoke Street","city":"Ankara","country":"TR"}},"orderDate":"2026-04-28T16:00:00","title":"Smoke Test Order","orderItems":[{"quantity":1},{"quantity":2}]}'
order_response="$(run_request POST "$BASE_URL/customerorder/save" "$order_payload")"
if ! echo "$order_response" | grep -q '"id"'; then
  echo "ERROR: Customer order save response did not contain id."
  echo "$order_response"
  exit 1
fi
if ! echo "$order_response" | grep -q '"orderItems"'; then
  echo "ERROR: Customer order save response did not contain orderItems."
  echo "$order_response"
  exit 1
fi

echo "Customer order save passed."

order_items_response="$(run_request GET "$BASE_URL/orderitem/list" "")"
if ! echo "$order_items_response" | grep -q '"quantity"'; then
  echo "ERROR: Order item list response did not contain order items."
  echo "$order_items_response"
  exit 1
fi

echo "Order item list passed."

echo ""
echo "========================================"
echo "✅ Layer 2 smoke tests PASSED"
echo "========================================"
