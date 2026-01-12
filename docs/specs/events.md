# Eventing, Idempotency and Messaging Specification

## 1. Purpose

This document defines:

- Kafka topic naming conventions  
- Event envelope and schemas  
- Idempotency and deduplication strategy  
- Retry, timeout and error handling policies  

This file is the single source of truth for all inter-service messaging.

---

## 2. Topic Naming Convention

Topics are grouped by bounded context and message direction.

| Topic | Description |
|------|-------------|
| transactions.commands | DepositRequested, WithdrawRequested, TransferRequested, RefundRequested |
| accounts.events | MoneyReserved, MoneyCredited, MoneyDebited, ReservationFailed, RefundCompleted |
| transactions.events | TransactionCompleted, TransactionFailed |
| notifications.events | NotificationRequested |

Rules:

- Topics are partitioned by aggregate id (`account_id` or `transaction_id`).
- Delivery semantics: at-least-once.
- Ordering is guaranteed per aggregate id.

---

## 3. Event Envelope

All events share a common envelope.

| Field | Type | Description |
|------|------|-------------|
| event_id | UUID | Unique event identifier |
| event_type | String | Logical name |
| event_version | Integer | Schema version |
| aggregate_type | String | Account, Transaction, etc |
| aggregate_id | UUID | Entity id |
| transaction_id | UUID | Saga correlation id |
| request_id | UUID | Idempotency key |
| correlation_id | UUID | Tracing |
| timestamp | ISO-8601 | Event creation time |
| payload | Object | Event-specific data |

Example:

```json
{
  "event_id": "uuid",
  "event_type": "MoneyReserved",
  "event_version": 1,
  "aggregate_type": "Account",
  "aggregate_id": "uuid",
  "transaction_id": "uuid",
  "request_id": "uuid",
  "correlation_id": "uuid",
  "timestamp": "2026-01-12T10:00:00Z",
  "payload": {}
}
```

---

## 4. Event Schemas

### 4.1 DepositRequested

**Topic:** `transactions.commands`

**Payload:**

| Field | Type |
|-------|------|
| account_id | UUID |
| amount | number |
| currency | string |

### 4.2 WithdrawRequested

**Topic:** `transactions.commands`

**Payload:**

| Field | Type |
|-------|------|
| account_id | UUID |
| amount | number |
| currency | string |

### 4.3 TransferRequested

**Topic:** `transactions.commands`

**Payload:**

| Field | Type |
|-------|------|
| from_account_id | UUID |
| to_account_id | UUID |
| amount | number |
| currency | string |

### 4.4 MoneyReserved

**Topic:** `accounts.events`

**Payload:**

| Field | Type |
|-------|------|
| account_id | UUID |
| amount | number |
| currency | string |

### 4.5 ReservationFailed

**Topic:** `accounts.events`

**Payload:**

| Field | Type |
|-------|------|
| account_id | UUID |
| reason | string |

### 4.6 MoneyCredited

**Topic:** `accounts.events`

**Payload:**

| Field | Type |
|-------|------|
| account_id | UUID |
| amount | number |
| currency | string |

### 4.7 MoneyDebited

**Topic:** `accounts.events`

**Payload:**

| Field | Type |
|-------|------|
| account_id | UUID |
| amount | number |
| currency | string |

### 4.8 RefundCompleted

**Topic:** `accounts.events`

**Payload:**

| Field | Type |
|-------|------|
| account_id | UUID |
| amount | number |
| currency | string |

### 4.9 TransactionCompleted

**Topic:** `transactions.events`

**Payload:**

| Field | Type |
|-------|------|
| transaction_id | UUID |
| status | string |

### 4.10 TransactionFailed

**Topic:** `transactions.events`

**Payload:**

| Field | Type |
|-------|------|
| transaction_id | UUID |
| reason | string |

---

## 5. Idempotency Strategy

### 5.1 API Layer

Clients must send `request_id` (UUID).

- Stored in Redis: `idempotency:{request_id}`
- TTL: 24 hours
- If key exists → return stored result

### 5.2 Event Consumers

Each service maintains `processed_event` table:

- `event_id` (PK)
- `processed_at`

On consume:

- If `event_id` exists → skip
- Else process and insert

---

## 6. Retry & Error Handling

- Kafka retry: enabled with exponential backoff
- Max retries: 5
- Failed messages go to `.DLQ` topics

| Topic | DLQ |
|-------|-----|
| transactions.commands | transactions.commands.DLQ |
| accounts.events | accounts.events.DLQ |

---

## 7. Versioning Strategy

- Backward compatible changes only
- Breaking change → new topic or `event_version++`

---

## 8. Correlation and Tracing

`correlation_id` propagated via:

- HTTP headers
- Kafka headers
- Integrated with OpenTelemetry / Zipkin

---

## 9. Security Considerations

- Sensitive fields must be masked in logs
- No PII in events
- Payload encryption is optional for demo

---

## 10. Summary

This specification ensures:

- No duplicate processing
- Safe retry behavior
- Deterministic saga execution
- Observability and traceability