# 🛡️ Bug History & Critical Fixes

This historical record tracks major architectural bugs and their long-term solutions.

## 1. Kafka Double-Serialization
- **Symptoms**: Extra quotes in JSON strings (e.g. `""{...}""`).
- **Cause**: Outbox worker serialized to JSON, then Kafka `JsonSerializer` serialized the string again.
- **Fix**: Switched to `StringSerializer` across all producers.

## 2. Listener Exception Swallowing
- **Symptoms**: Messages failed but were marked "Processed" and never hit the DLT.
- **Cause**: Listeners used `try-catch` blocks and logged errors without rethrowing.
- **Fix**: Removed catch blocks; listeners now propagate exceptions to trigger `CommonErrorHandler`.

## 3. Saga Correlation Failure (Missing ID)
- **Symptoms**: Accounts updated but Transactions stayed `PENDING`.
- **Cause**: Account Service response events didn't include the `transactionId`.
- **Fix**: Standardized `BaseEvent` to always propagate the `transactionId` throughout the Saga.

## 4. Trace Discontinuity in Outbox
- **Symptoms**: Traces broken between API request and Kafka publishing.
- **Fix**: Implemented `TracingProducerInterceptor` to extract `trace_id` from JSON payloads and inject it into Kafka headers.

## 5. Idempotency Gap
- **Symptoms**: Retried events caused double-balancing.
- **Fix**: Implemented `ProcessedEvent` repository check in all Kafka listeners.
