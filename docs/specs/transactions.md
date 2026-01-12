# Transaction Service Specification

## Responsibility
Coordinates money-related operations using Saga choreography.

## API

### POST /transactions/deposit
### POST /transactions/withdraw
### POST /transactions/transfer

All requests require:
- request_id (UUID)
- amount
- account references

---

## Data Model

Transaction:
- id
- request_id
- type (DEPOSIT, WITHDRAW, TRANSFER)
- from_account
- to_account
- amount
- status (PENDING, COMPLETED, FAILED)
- created_at

---

## Events

Outbound:
- DepositRequested
- WithdrawRequested
- TransferRequested
- RefundRequested
- TransactionCompleted
- TransactionFailed

Inbound:
- MoneyReserved
- MoneyCredited
- MoneyDebited
- ReservationFailed
- RefundCompleted

---

## Saga Flows

### Transfer

1. Emit TransferRequested
2. Wait MoneyReserved
3. Emit Credit command
4. Wait MoneyCredited
5. Mark COMPLETED

Failure:
- If reservation fails → FAILED
- If credit fails → RefundRequested

---

## Business Rules

- request_id ensures idempotency.
- Saga state must be persisted.
- Only one saga may process a transaction.

---

## Failure Handling

| Scenario | Handling |
|----------|-----------|
Timeout | Mark FAILED |
Partial success | Compensate |

---

## Non-Functional

- Must recover from crashes via replay.
- Must be idempotent on event consumption.
