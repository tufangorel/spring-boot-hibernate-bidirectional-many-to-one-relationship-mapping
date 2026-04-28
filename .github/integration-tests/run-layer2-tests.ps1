param()

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path "$scriptDir\..\.."
$composeFile = Join-Path $repoRoot '.github\integration-tests\docker-compose.yml'

Set-Location $repoRoot

Write-Host 'Checking Docker availability...'
try {
    docker info > $null 2>&1
} catch {
    Write-Host 'ERROR: Docker is not running. Please start Docker and try again.' -ForegroundColor Red
    exit 1
}

Write-Host 'Building application jar...'
& "$repoRoot\mvnw.cmd" -DskipTests package

Write-Host 'Starting application with Docker Compose...'
docker compose -f $composeFile up -d --build

function Cleanup {
    Write-Host 'Stopping Docker Compose environment...'
    docker compose -f $composeFile down --remove-orphans | Out-Null
}

Register-EngineEvent PowerShell.Exiting -Action { Cleanup } | Out-Null

$baseUrl = 'http://localhost:8080/customer-info'
Write-Host 'Waiting for application readiness...'
$maxWait = 120
$waitSeconds = 0
while ($true) {
    try {
        Invoke-WebRequest -Uri "$baseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 | Out-Null
        break
    } catch {
        if ($waitSeconds -ge $maxWait) {
            Write-Host "ERROR: Application did not become healthy within $maxWait seconds." -ForegroundColor Red
            exit 1
        }
        Start-Sleep -Seconds 3
        $waitSeconds += 3
        Write-Host "Waiting for health endpoint... ($waitSeconds/$maxWait)"
    }
}

Write-Host 'Application is healthy. Running Layer 2 smoke tests...'

function Run-Request($method, $url, $body) {
    if ($method -eq 'GET') {
        return Invoke-WebRequest -Uri $url -UseBasicParsing -Headers @{ 'Accept' = 'application/json' } -TimeoutSec 30
    }
    return Invoke-WebRequest -Uri $url -UseBasicParsing -Method $method -Headers @{ 'Content-Type' = 'application/json' } -Body $body -TimeoutSec 30
}

$customerPayload = '{"name":"Smoke Customer","age":30,"shippingAddress":{"streetName":"Smoke Street","city":"Ankara","country":"TR"}}'
$customerResponse = Run-Request 'POST' "$baseUrl/customer/save" $customerPayload
if (-not ($customerResponse.Content -match '"id"')) {
    Write-Host 'ERROR: Customer save response did not contain id.' -ForegroundColor Red
    Write-Host $customerResponse.Content
    exit 1
}
Write-Host 'Customer save passed.'

$orderPayload = '{"customer":{"id":1,"name":"Smoke Customer","age":30,"shippingAddress":{"id":1,"streetName":"Smoke Street","city":"Ankara","country":"TR"}},"orderDate":"2026-04-28T16:00:00","title":"Smoke Test Order","orderItems":[{"quantity":1},{"quantity":2}]}'
$orderResponse = Run-Request 'POST' "$baseUrl/customerorder/save" $orderPayload
if (-not ($orderResponse.Content -match '"id"')) {
    Write-Host 'ERROR: Customer order save response did not contain id.' -ForegroundColor Red
    Write-Host $orderResponse.Content
    exit 1
}
if (-not ($orderResponse.Content -match '"orderItems"')) {
    Write-Host 'ERROR: Customer order save response did not contain orderItems.' -ForegroundColor Red
    Write-Host $orderResponse.Content
    exit 1
}
Write-Host 'Customer order save passed.'

$orderItemsResponse = Run-Request 'GET' "$baseUrl/orderitem/list" $null
if (-not ($orderItemsResponse.Content -match '"quantity"')) {
    Write-Host 'ERROR: Order item list response did not contain order items.' -ForegroundColor Red
    Write-Host $orderItemsResponse.Content
    exit 1
}
Write-Host 'Order item list passed.'

Write-Host ''
Write-Host '========================================'
Write-Host '✅ Layer 2 smoke tests PASSED'
Write-Host '========================================'
