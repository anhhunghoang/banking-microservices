# Infrastructure Requirements

## 1. Overview
This document defines the infrastructure components required for the Banking Microservices environment. All infrastructure is managed via Docker Compose.

## 2. Kafka (KRaft Mode)
- **Image**: `apache/kafka:4.1.1`
- **Mode**: KRaft (No ZooKeeper)
- **Environment**:
  - `CLUSTER_ID`: Static UUID (e.g., `MkU3OEVBNTcwNTJENDM2Qk`)
  - `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR`: 1
- **Topics to Create**:
  - `transactions.commands` (partitions: 3, replication: 1)
  - `accounts.events` (partitions: 3, replication: 1)
  - `transactions.events` (partitions: 3, replication: 1)
  - `notifications.events` (partitions: 3, replication: 1)
  - `customers.events.created` (partitions: 3, replication: 1)
- **Consumer Groups**:
  - `account-service-group`
  - `transaction-service-group`
  - `notification-service-group`
  - `customer-service-group`

## 3. Postgres
- **Image**: `postgres:16-alpine`
- **Database**: `banking`
- **User**: `user`
- **Password**: `password`
- **Initialization**:
  - Create database `banking`
  - Create user `user` with access to `banking`
  - Create schemas (if not using separate DBs per service for simplicity in dev, though separate DBs is better practice. Plan assumes schemas in one DB or distinct DBs):
    - `customer`
    - `account`
    - `transaction`

## 4. MongoDB
- **Image**: `mongo:7.0`
- **Database**: `audit_logs`
- **Collections**:
  - `transaction_events`
  - `account_events`
  - `customer_events`

## 5. Redis
- **Image**: `redis:7-alpine`
- **Configuration**: Default
- **Usage**:
  - DB 0: General caching
  - Keys:
    - `idempotency:{request_id}` (TTL: 24h)
    - `balance:{account_id}` (TTL: 30s)

## 6. Observability
- **Jaeger**: For distributed tracing.
- **OpenTelemetry Standard**: All services must expose metrics/traces.

## 7. Initialization Strategy
- **Postgres**: Use `/docker-entrypoint-initdb.d/init.sql` to create users/DBs/schemas.
- **Kafka**: Use a sidecar container or `command` in docker-compose to run `kafka-topics.sh` and create required topics on startup.
