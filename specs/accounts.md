# Account Service Specification

## Responsibility
Manages bank accounts and balances.

## API

### POST /accounts
Create an account for a customer.

### GET /accounts/{id}
Fetch account details.

### GET /accounts/{id}/balance
Fetch current balance.

---

## Data Model

Account:
- id
- customer_id
- balance
- status (ACTIVE, FROZEN)
- version (for optimistic locking)
- created_at

---

## Events

Inbound:
- DepositRequested
- WithdrawRequested
- TransferRequested
- RefundRequested

Outbound:
- AccountCreated
- MoneyReserved
- MoneyCredited
- MoneyDebited
- ReservationFailed
- RefundCompleted

---

## Business Rules

- Balance must never become negative.
- All operations must be idempotent by request_id.
- Frozen accounts reject all operations.
- Optimistic locking is mandatory.

---

## Failure Handling

| Scenario | Handling |
|----------|-----------|
Insufficient funds | Emit ReservationFailed |
Concurrent update | Retry up to 3 times |
Duplicate request | Ignore and return previous result |

---

## Non-Functional

- Strong consistency for balance updates.
- High durability.
