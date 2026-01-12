# Banking Microservices Demo System — Specification

## 1. System Goals

Build a simple but realistic banking system composed of multiple microservices communicating via REST and Kafka, simulating core financial operations:

- Bank account management (account creation, balance management).
- Money operations: deposit, withdraw, transfer.
- Asynchronous processing with eventual consistency using choreography-based saga for business-level atomicity.
- Ensure no money is lost, duplicated, or processed twice (idempotent requests and events).

The system must:
- Handle duplicate requests and duplicate events (Redis idempotency keys).
- Recover from service crashes (retry mechanisms, DLQ, saga resumption).
- Provide traceability and auditability (distributed tracing with correlation IDs, audit logs in MongoDB).
- Include production-ready basics: security, monitoring, and deployment.

These specifications are the **single source of truth** for system behavior.  
All implementation must strictly follow these documents.

---

## 2. Actors

| Actor  | Description |
|--------|-------------|
| Client | User or external system calling APIs (e.g., mobile app or partner system). |
| Admin  | Administrator managing customers/accounts via admin endpoints. |
| System | Internal microservices communicating via Kafka events or REST. |

---

## 3. Services

### 3.1 Account Service
- Manage accounts: create, update, delete.
- Store and manage balances (optimistic locking to avoid race conditions).
- Reserve, deduct, refund, credit money (idempotent operations).
- Cache balance reads in Redis (TTL 30s for performance).

### 3.2 Transaction Service
- Receive transaction requests and orchestrate sagas.
- Store transaction states (PENDING / COMPLETED / FAILED) in PostgreSQL.
- Publish events to Kafka to trigger saga steps.
- Handle compensating transactions on failures.

### 3.3 Customer Service
- Manage customer profiles: create, update (name, email, etc.).
- One-to-many relationship between customer and accounts.

### 3.4 Notification Service
- Consume Kafka events (e.g., transaction-completed, transaction-failed).
- Send notifications (simulated via logs or integrated with external providers like Twilio).

### 3.5 Additional Services
- API Gateway: routing, load balancing, rate limiting (Bucket4j + Redis), authentication.
- Config Service: centralized configuration using Spring Cloud Config (optional, backed by Git).

---

## 4. Core Business Flows

### 4.1 Create Account
1. Client calls `POST /customers` to create a customer.
2. Client calls `POST /accounts` with `customer_id` to create an account (initial balance = 0).
3. Publish `account-created` event to Kafka for audit/notification.

### 4.2 Deposit
- Client calls `POST /transactions/deposit` with `request_id` (UUID for idempotency).
- Transaction Service checks idempotency, creates PENDING transaction, stores outbox event.
- Publish `deposit-requested`.
- Account Service increases balance atomically, emits `deposit-completed`.
- Transaction Service marks COMPLETED and emits notification event.

### 4.3 Withdraw
- Client calls `POST /transactions/withdraw`.
- Transaction Service creates PENDING and emits `withdraw-requested`.
- Account Service checks sufficient balance and deducts or fails.
- On failure → mark FAILED and emit notification.

### 4.4 Transfer (Saga Choreography)

1. Client calls `POST /transactions/transfers` with `request_id`, `from_account`, `to_account`, `amount`.
2. Transaction Service checks idempotency and creates PENDING transaction.
3. Emit `transfer-requested` with `correlation_id`.
4. Sender Account Service reserves money and emits `money-reserved` or `reservation-failed`.
5. If failed → mark FAILED and emit `transfer-failed`.
6. Receiver Account Service credits money and emits `money-credited`.
7. Transaction Service marks COMPLETED and emits `transfer-completed`.

If credit fails → compensating event `refund-sender`.

---

## 5. Business Rules

| Rule | Description |
|------|-------------|
| R1 | Balance must never be negative. |
| R2 | Each transaction is processed exactly once (idempotency via request_id). |
| R3 | Duplicate requests must not produce duplicate effects. |
| R4 | Failures must be compensated (saga rollback). |
| R5 | All transactions must have audit logs with correlation_id. |
| R6 | Optimistic concurrency control for balance updates (version column). |

---

## 6. Non-Functional Requirements

### 6.1 Reliability
- Kafka at-least-once delivery (`acks=all`, retries).
- Idempotent consumers.
- Retry and DLQ for failures.
- Outbox pattern for reliable publishing.
- Circuit breakers (Resilience4j).

### 6.2 Performance
- Redis caching for balance reads.
- Strong consistency for writes (Postgres transactions).
- Rate limiting at gateway (e.g., 100 req/min/user).
- Horizontal scaling with stateless services and Kafka partitions.

### 6.3 Observability
- Correlation ID propagation.
- Distributed tracing (Zipkin/Jaeger + OpenTelemetry).
- Metrics via Micrometer + Prometheus.
- Centralized logging (ELK optional).
- Audit logs stored in MongoDB.

### 6.4 Security
- Authentication: JWT/OAuth2 with Keycloak.
- Authorization: RBAC (ROLE_USER, ROLE_ADMIN).
- Input validation and OWASP protections.
- Secret management via Vault or environment variables.
- Rate limiting and DDoS basics.
- Compliance note: no real card data, encrypt sensitive fields.

### 6.5 Deployment & Operations
- Docker + Docker Compose for local dev.
- Kubernetes-ready (Helm charts, blue-green deployments).
- CI/CD with GitHub Actions.
- Service discovery with Eureka or Kubernetes services.

---

## 7. Failure Scenarios

| Scenario | Handling |
|----------|----------|
| Duplicate request | Redis idempotency check, return existing result. |
| Kafka duplicate event | Consumer idempotency check. |
| Account Service crash | Retry + saga resume. |
| Transaction Service crash | Resume saga from stored state. |
| Partial commit | Compensating transaction. |
| Network partition | Circuit breaker and fallback. |
| DB downtime | Retry with exponential backoff. |

---

## 8. Scope

### Included
- REST APIs, Kafka events, saga choreography.
- Outbox pattern.
- Redis (idempotency, caching).
- MongoDB audit logs.
- Testing: unit, integration (Testcontainers), contract, chaos.
- Documentation: C4 diagrams, README, demo flows.

### Excluded
- Real payment gateways.
- KYC/AML.
- Full PCI compliance.
- ML fraud detection.

---
