# System Architecture and Specifications

This document defines the core architecture, functional specifications, and technical standards for the Banking Microservices system.

## 1. System Goals and Actors
The system facilitates core financial operations (account management, deposits, withdrawals, transfers) using a microservices architecture with strong consistency for writes and eventual consistency for distributed business processes (Sagas).

### Actors
- **Client**: External systems or users calling the API.
- **System**: Internal microservices coordinating via Kafka.

## 2. Service Responsibilities
- **Account Service**: Manages balances with optimistic locking and enforces business rules (e.g., preventing negative balances, frozen accounts).
- **Transaction Service**: Orchestrates Sagas and manages transaction states (PENDING/COMPLETED/FAILED).
- **Customer Service**: Manages customer profiles.
- **Notification Service**: Sends alerts based on transaction outcomes.
- **API Gateway**: Provides routing, rate limiting, and security.

## 3. Core Business Flows
## 3. Detailed Specifications
- **[Account Service](specs/accounts.md)**: Ledger and balance rules.
- **[Transaction Service](specs/transactions.md)**: Sagas and Outbox pattern.
- **[Messaging & Idempotency](specs/messaging.md)**: Kafka, DLT, and deduplication standards.

## 4. Technical Standards
### 4.1 Centralized Constants (`common-lib`)
Shared identifiers are centralized to ensure consistency across microservices:
- `EventTypes`, `Topics`, `ServiceGroups`, `AggregateTypes`, `ErrorCodes`.

### 4.2 Kafka Messaging and Resiliency
- **Topic Ownership**: Topics are provisioned by infrastructure scripts, not application code.
- **Retry Policy**: 3 retries (1s delay) followed by DLT promotion.
- **Serialization**: `StringSerializer` for all outbox-based producers.

### 4.2 Kafka Messaging and Resiliency
- **Topic Naming**:
    - `transactions.commands` (DLT: `transactions.commands.DLT`)
    - `accounts.events` (DLT: `accounts.events.DLT`)
- **Reliability**:
    - **Outbox Pattern**: Events are saved to the database in the same transaction as business logic to ensure "at-least-once" delivery.
    - **Retry Policy**: Failed messages are retried **3 times** with a 1s delay.
    - **Dead Letter Queue (DLQ)**: messages that fail all retries are moved to the corresponding `.DLT` topic for manual intervention/inspection.
- **Serialization**: We use `StringSerializer`/`StringDeserializer` to avoid brittle type-header dependencies and double-serialization issues.

### 4.3 Idempotency and Deduplication
- **API Level**: Managed via Redis with a `request_id`.
- **Event Level**: Every service maintains a `processed_events` table. Before processing, the service checks if the `event_id` exists. This ensures "exactly-once" effect despite "at-least-once" delivery.

## 5. Business Rules
- **Negative Balance**: Prohibited at the database level and enforced in `AccountServiceImpl`.
- **Frozen Accounts**: All state-changing operations are rejected with an `ACCOUNT_FROZEN` error code if the account status is `FROZEN`.
- **Optimistic Locking**: Every balance update uses a `@Version` field to prevent race conditions during concurrent updates.
