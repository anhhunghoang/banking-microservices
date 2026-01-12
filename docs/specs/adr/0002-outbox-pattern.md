# ADR 0002: Outbox Pattern for Reliable Event Publishing

## Status
Accepted

## Context
The system uses event-driven communication with Kafka.  
However, publishing events directly after database commits can lead to inconsistencies:

- Database commit succeeds but event publish fails → lost event.
- Event publish succeeds but database commit fails → ghost event.

We need a mechanism to guarantee that state changes and emitted events remain consistent.

## Decision
We adopt the **Outbox Pattern**.

Each service that publishes events will:

1. Write domain state and an outbox event record in the same local database transaction.
2. Use a background publisher to read pending outbox events.
3. Publish them to Kafka.
4. Mark them as published once acknowledged.

## Implementation

- Outbox table is stored in the service database.
- Schema:
  - id
  - aggregate_type
  - aggregate_id
  - event_type
  - payload (JSON)
  - status (PENDING, SENT)
  - created_at

- Publisher:
  - Runs on a scheduled interval.
  - Retries on failure.
  - Uses idempotent Kafka producers.

## Consequences

### Positive
- Guarantees no lost events.
- Ensures consistency between DB and Kafka.
- Allows replay and recovery.

### Negative
- Slight increase in complexity.
- Additional storage and polling overhead.

---

## Alternatives Considered

- Two-phase commit: rejected due to complexity and poor scalability.
- Best-effort publishing: rejected due to risk of data inconsistency.

---

## Result
The Outbox Pattern ensures reliable and consistent event publishing across services.
