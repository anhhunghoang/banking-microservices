# Eventing and Idempotency Specification

## 1. Kafka Topic Registry
Managed via `infra/kafka/init-topics.sh`.

| Topic | Primary Usage | DLT Topic |
| :--- | :--- | :--- |
| `transactions.commands` | Requesting state changes | `transactions.commands.DLT` |
| `accounts.events` | Notifying status of account changes | `accounts.events.DLT` |
| `transactions.events` | Final transaction outcomes | `transactions.events.DLT` |
| `notifications.events` | External alerts | `notifications.events.DLT` |

## 2. Event Envelope (BaseEvent)
Every message uses the `BaseEvent<T>` structure:
- `event_id`: Unique key for consumer idempotency.
- `transaction_id`: Correlation ID for the entire Saga.
- `aggregate_type`: E.g., `ACCOUNT`, `TRANSACTION`.
- `trace_id`: Propagated for distributed tracing.

## 3. Serialization Standard
- **Producers**: Use `StringSerializer`. Payloads are pre-serialized JSON from the Outbox.
- **Consumers**: Use `StringDeserializer`. Manual deserialization via `ObjectMapper` + `TypeReference` in the `EventListener`.

## 4. Idempotency Strategy
- **At-Least-Once Delivery**: Kafka guarantees delivery but may duplicate messages.
- **Deduplication**: 
    1. Check `processed_events` table for `event_id`.
    2. If missing, process and save `event_id`.
    3. Operation is transactional.

## 5. Error Handling & DLQ
- **Retry**: 3 retries (1s delay) configured via `SharedKafkaAutoConfiguration`.
- **Promotion**: After exhaustion, message moves to `{originalTopic}.DLT`.
- **Rethrowing**: Listeners must NOT catch-and-swallow; they must throw exceptions to trigger the DLQ mechanism.
