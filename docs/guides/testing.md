# Testing and Verification Guide

## 1. Automated Testing Strategy
### 1.1 Unit Tests
- Business logic in `AccountServiceImpl` and `TransactionServiceImpl`.
- Focus on state transitions and error conditions.

### 1.2 Integration Tests
- **Database**: Uses Spring `@DataJpaTest`.
- **Concurrency**: `AccountConcurrencyIntegrationTest` verifies optimistic locking under load.
- **Messaging**: `AccountEventListenerTest` mocks Kafka messages to verify consumer logic.

## 2. Manual Verification
### 2.1 API Testing via Swagger
Each service exposes OpenAPI docs:
- `account-service`: http://localhost:8082/swagger-ui.html
- `transaction-service`: http://localhost:8083/swagger-ui.html

### 2.2 Kafka Inspection
Use `docker exec` to list and consume topics during development:
```bash
docker exec -it kafka kafka-topics.sh --bootstrap-server kafka:29092 --list
```

## 3. Idempotency Verification
To test idempotency, send a request with the same `X-Request-Id` twice. The system should return the exact same response without duplicating the state change (e.g., balance won't increase twice).
