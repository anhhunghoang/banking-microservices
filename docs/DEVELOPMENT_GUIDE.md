# Development, Setup, and Troubleshooting Guide

This guide covers everything needed to set up, run, test, and debug the banking microservices system.

## 1. Local Infrastructure Setup
The infrastructure is containerized via Docker Compose.

### Prerequisites
- Java 21
- Docker & Docker Compose

### Quick Start
Use the provided script to initialize the environment (Postgres schemas, Kafka topics, Jaeger, etc.):
```bash
./infra/start.sh
```

### Kafka Topics (Managed by Infra)
Topics are provisioned with 3 partitions (main) and 1 partition (DLT):
- `transactions.commands` / `transactions.commands.DLT`
- `accounts.events` / `accounts.events.DLT`
- `transactions.events` / `transactions.events.DLT`

## 2. Running Services
Start services using Gradle in separate terminals:
1. `customer-service` (Port 8081)
2. `account-service` (Port 8082)
3. `transaction-service` (Port 8083)

```bash
./gradlew :service-name:bootRun
```

## 3. Observability and Tracing
We use **Jaeger** for distributed tracing and logs for auditing.
- **Jaeger UI**: http://localhost:16686
- **Tracing Restoration**: Our `common-lib` includes logic to propagate trace IDs across the Outbox boundary and Kafka topics automatically.

## 4. Testing Guide
### API Testing
- **Swagger/OpenAPI**: Available at `http://localhost:PORT/swagger-ui.html` for each service.
- **Postman/cURL**: Ensure to include `X-Request-Id` for idempotency.

### Automated Tests
```bash
./gradlew test # Run all tests
./gradlew :account-service:test # Run specific service tests
```

## 5. Bug History and Prevention
Crucial lessons learned from development:

### Kafka Double-Serialization
- **Issue**: Extra quotes in JSON payloads due to `JsonSerializer` wrapping already-serialized strings.
- **Prevention**: Use `StringSerializer` for all outbox-based producers.

### Listener Exception Swallowing
- **Issue**: Wrapping listeners in `try-catch` without rethrowing prevented Spring Kafka from triggering retries or DLQ.
- **Prevention**: Listeners must throw exceptions to allow the `CommonErrorHandler` to take effect.

### Sage Correlation
- **Issue**: Transactions hanging in PENDING because response events lacked `transactionId`.
- **Prevention**: Always propagate `transactionId` in every event envelope.

## 6. Cleanup
To wipe all data and start fresh:
```bash
docker-compose -f infra/docker-compose.yml down -v
```
