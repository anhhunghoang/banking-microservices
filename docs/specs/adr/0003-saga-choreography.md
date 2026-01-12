# ADR 0003: Saga Choreography for Distributed Transactions

## Status
Accepted

## Context
Money transfers span multiple services (Transaction Service, Account Service).  
Traditional ACID transactions cannot span multiple microservices.

We need to ensure:
- No money is lost or duplicated.
- Partial failures are compensated.
- Services remain loosely coupled.

## Decision
We use **Saga Choreography** instead of orchestration.

Each service reacts to events and performs its local transaction independently.

## Implementation

### Transfer Saga

1. Transaction Service emits TransferRequested.
2. Account Service (sender) reserves funds and emits MoneyReserved.
3. Account Service (receiver) credits funds and emits MoneyCredited.
4. Transaction Service marks transaction as COMPLETED.

### Compensation

- If reservation fails → TransactionFailed.
- If credit fails → RefundRequested.
- Sender account refunds and emits RefundCompleted.

## Consequences

### Positive
- Loose coupling between services.
- No central coordinator bottleneck.
- Scales horizontally.

### Negative
- Harder to reason about flows.
- Requires careful event design.
- Debugging is more complex.

---

## Alternatives Considered

- Saga Orchestration: rejected due to centralization and coupling.
- Two-phase commit: rejected due to scalability issues.

---

## Result
Saga choreography provides resilient, scalable business-level consistency.
