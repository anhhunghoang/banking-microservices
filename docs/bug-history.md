# 🛡️ Bug History & Critical Fixes

This document records major bugs encountered during the development of the Banking Microservices system and their permanent solutions. Use this as a reference to prevent these issues from reoccurring.

---

## 1. Kafka Double-Serialization Issue
**Date:** Jan 13, 2026
**Symptoms:** 
- `transaction-service` publishes messages successfully.
- `account-service` receives messages but they look like `""{\"accountId\":...}""` (wrapped in extra quotes).
- Deserialization fails with `JsonParseException`.

**Root Cause:**
The `transaction-service` was using `JsonSerializer` in `application.yml`, but the code in `OutboxProcessor` was already converting the payload to a JSON string using `ObjectMapper`. This caused the `JsonSerializer` to treat the JSON string as a regular string and escape it again.

**Fix:**
- Changed `value-serializer` in all services (`transaction-service`, `account-service`, `customer-service`) to `org.apache.kafka.common.serialization.StringSerializer`.
- This ensures the raw JSON string from the Outbox is sent exactly as-is, preventing consumers from failing with `MismatchedInputException`.

---

## 2. Kafka Listener Exception Swallowing
**Date:** Jan 14, 2026
**Symptoms:** 
- Failed messages are silently ignored.
- No retries occur.
- Messages never reach the Dead Letter Queue (DLQ).

**Root Cause:**
Listeners were wrapped in `try-catch` blocks that logged the error but didn't rethrow it. The Spring Kafka container saw a "successful" execution (even though it failed) and committed the offset.

**Fix:**
- Removed catch blocks in `EventListener` classes.
- Listeners now `throws Exception`.
- Configured a global `CommonErrorHandler` with `DeadLetterPublishingRecoverer` in `common-lib` to handle retries and move failed messages to `{topic}.DLT`.

---

## 2. Saga Correlation Failure (Missing Transaction ID)
**Date:** Jan 13, 2026
**Symptoms:**
- Deposits/Withdrawals update the account balance correctly.
- The corresponding Transaction record stays in `PENDING` status forever in the `transaction-service`.

**Root Cause:**
The `account-service` was emitting response events (`MoneyCredited`, `MoneyDebited`) but was not setting the `transactionId` field in the `BaseEvent` envelope. The `transaction-service` received the event but couldn't match it to any local record.

**Fix:**
- Updated `AccountEventProducer` interface to accept `transactionId`.
- Modified `AccountServiceImpl` to flow the `transactionId` from the incoming command to the outgoing event.
- Updated `BaseEvent` builder in `AccountEventProducerImpl` to explicitly set the `transactionId`.

---

## 3. Deserialization Brittleness (Missing Type Headers)
**Date:** Jan 13, 2026
**Symptoms:**
- Services fail to consume messages with errors like "No type information found" or "Class not found".
- Kafka Consumer Groups appear and disappear randomly.

**Root Cause:**
Default Spring `JsonDeserializer` relies on `__TypeId__` headers, which can be lost or mismatched when different services use different package structures for their event classes.

**Fix:**
- Switched all consumers to use `StringDeserializer`.
- Implemented manual deserialization in `EventListener` classes using `ObjectMapper` and `TypeReference`.
- This decouples the serialization format from the internal class names of the services.

---

## 4. Kafka Internal/External Port Confusion
**Date:** Jan 13, 2026
**Symptoms:**
- `kafka-consumer-groups.sh` returns empty lists when run inside the container.
- Services cannot connect to Kafka when deployed in Docker.

**Root Cause:**
Kafka is configured with two listeners:
1. `EXTERNAL` (localhost:9092) for apps running on the host machine.
2. `INTERNAL` (kafka:29092) for scripts and apps running inside the Docker network.
Running a command inside the container using `localhost:9092` fails because Kafka isn't listening on localhost *inside* the container for that protocol.

**Fix:**
- Always use `kafka:29092` for any command executed via `docker exec`.
- Example: `kafka-topics.sh --bootstrap-server kafka:29092 --list`

---

## 5. Asynchronous Trace Discontinuity (Outbox Pattern)
**Date:** Jan 13, 2026
**Symptoms:**
- Jaeger traces were "broken" at the Outbox boundary.
- The HTTP request showed up as one trace in `transaction-service`.
- The background `OutboxProcessor` and downstream `account-service` work showed up as separate, unlinked traces.

**Root Cause:**
- Distributed tracing headers (`traceparent`) are typically propagated in-memory.
- The Outbox pattern persists the event to a database, which effectively "kills" the active trace context.
- Standard Kafka tracing instrumentation (`observation-enabled`) only looks for headers; it does not peek into the JSON payload for a `trace_id`.

**Fix:**
- **Trace Context Restoration**: Enhanced `TracingService` in `common-lib` to manually "resurrect" a trace context from a String ID using the Micrometer `Propagator` API.
- **Header Promotion Interceptor**: Implemented a `TracingProducerInterceptor` in `common-lib` that automatically extracts the `trace_id` from the JSON payload of every outgoing Kafka message and injects it into the standard `traceparent` header.
- **Zero-Code Integration**: Registered this interceptor globally via `SharedTracingAutoConfiguration`. This allows business services to remain 100% clean and "trace-unaware" while still maintaining full visibility in Jaeger.

---

## 🛡️ Prevention Checklist for New Services
- [ ] Use `StringSerializer`/`StringDeserializer` for Kafka to avoid double-wrapping and type-header issues.
- [ ] Always pass `transactionId` and `correlationId` through the entire flow (Saga requirement).
- [ ] Ensure Outbox events are created in the same DB transaction as the business logic.
- [ ] Use `kafka:29092` for internal docker communication and `localhost:9092` for development machine access.
- [ ] Include `common-lib` and set `management.tracing.sampling.probability: 1.0` for full visibility.
- [ ] Ensure `spring.kafka.template.observation-enabled: true` and `spring.kafka.listener.observation-enabled: true` are set in `application.yml`.
- [ ] Use centralized constants (`EventTypes`, `Topics`, `ErrorCodes`) in `common-lib` to avoid typos across services.
- [ ] Do NOT swallow exceptions in `@KafkaListener` methods; let them propagate to the `CommonErrorHandler` for DLQ support.
- [ ] Ensure DLT topics are created in `infra/kafka/init-topics.sh`.
