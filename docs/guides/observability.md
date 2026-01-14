# Observability and Monitoring Specification

## 1. Distributed Tracing
We use **OpenTelemetry** and **Micrometer Tracing** to provide full system visibility.

### 1.1 Trace ID Propagation
Trace IDs are passed through every internal and external hop:
- **REST**: `traceparent` header.
- **Kafka**: Custom header extraction in `common-lib`.
- **Outbox**: The worker extracts `trace_id` from JSON and injects it into Kafka headers.

### 1.2 Jaeger Visualization
Navigate to `http://localhost:16686` to view full Saga traces. A single trace will show:
1. HTTP Entry in Transaction Service.
2. DB Persistence (via OutboxInterceptor).
3. Outbox Processing.
4. Kafka Consumer in Account Service.

## 2. Infrastructure Monitoring
All infrastructure components are visible via their respective ports:
- **Kafka**: Observation enabled in Spring listeners.
- **Postgres/Redis/Mongo**: Accessible via dev tools (DBeaver/RedisInsight).

## 3. Logs
Log pattern includes trace context for easy correlation:
`%d{yyyy-MM-dd HH:mm:ss} [%X{traceId}, %X{spanId}] %-5p %m%n`
