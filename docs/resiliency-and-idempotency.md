# Resiliency, Idempotency, and Business Rules

This document outlines the implementation details for event consumer idempotency, error handling, and business rule enforcement in the banking microservices system.

## 1. Event Consumer Idempotency

To ensure that events are processed exactly once (or effectively exactly once) despite Kafka's at-least-once delivery guarantee, we implement an idempotency check in every event listener.

### Implementation
- **Repository**: `ProcessedEventRepository`
- **Table**: `processed_events` (Schema: `event_id` UUID, `processed_at` Timestamp)
- **Logic**:
  1. Before processing an event, the listener checks if the `event_id` exists in the `processed_events` table.
  2. If it exists, the event is skipped.
  3. After successful processing, the `event_id` is saved to the table.
  4. This operation is wrapped in a `@Transactional` boundary to ensure atomicity with business state changes.

## 2. Kafka Resiliency and Error Handling

We use a combination of retries and Dead Letter Queues (DLQ) to handle processing failures.

### Error Handling Strategy
- **Centralized Configuration**: `SharedKafkaAutoConfiguration` in `common-lib`.
- **Retry Policy**: 
  - Messages that fail processing are retried **3 times**.
  - Delay: 1 second between retries (`FixedBackOff`).
- **Dead Letter Queue (DLQ)**:
  - If a message fails after all retries, it is moved to a topic named `{originalTopic}.DLT`.
  - Topics are provisioned with 1 partition in the infrastructure Layer.

### Infrastructure Management
Topic creation is managed via infrastructure scripts (`infra/kafka/init-topics.sh`) rather than application code, following DevOps best practices.

## 3. Business Rule Enforcement

### Frozen Accounts
As per `docs/specs/accounts.md`, accounts in a `FROZEN` state must reject all operations.
- **Implementation**: `AccountServiceImpl.checkAccountStatus()` is called before any state-changing operation (Deposit, Withdraw, Reserve, Refund).
- **Error Propagation**: Throws a `BusinessException` with error code `ACCOUNT_FROZEN`. If part of a Saga, it emits a `ReservationFailed` event to notify the coordinator.

## 4. Saga Compensation (Refunds)

To support the Transfer Saga compensation flow, the `account-service` implements refund logic.
- **Event**: `RefundRequested`
- **Response**: `RefundCompleted`
- **Logic**: Increases account balance and records the completion in the outbox to notify the `transaction-service`.

## 5. Architectural Standards

### Centralized Constants
To prevent typos and ensure consistency across the distributed system, all critical strings are centralized in `common-lib`:
- `EventTypes`: Event and command names.
- `Topics`: Kafka topic names.
- `ServiceGroups`: Consumer group IDs.
- `AggregateTypes`: Domain aggregate identifiers.
- `ErrorCodes`: Shared business error codes.

### Serialization Fix: Double-Serialization
To avoid `MismatchedInputException` in consumers, we use `StringSerializer` for Kafka producers. Since our Outbox Worker already serializes the `BaseEvent` to a JSON string in the database, the producer should send it as a raw string to Kafka without further transformation.
