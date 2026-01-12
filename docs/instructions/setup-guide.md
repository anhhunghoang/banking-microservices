# Infrastructure Setup & Run Guide

This guide describes how to initialize the banking microservices infrastructure and run the services.

## 1. Prerequisites
- Docker and Docker Compose
- Java 21
- MacOS (for provided scripts)

## 2. Infrastructure Setup (Docker)

The infrastructure consists of:
- **Postgres**: Relational DB (Port 5433 internally/externally)
- **Kafka**: Message Broker (Port 9092 for host, 29092 for docker)
- **Redis**: For idempotency (Port 6379)
- **MongoDB**: For read-model projections (Port 27017)
- **Jaeger**: For distributed tracing (Port 16686 UI)

### Initialize/Restart Infrastructure
I have provided a script that acts as the starting point for your environment. It handles the full lifecycle:
- Stops existing containers
- Deletes volumes (clean state)
- Starts all services
- Runs initialization scripts for DB and Kafka

```bash
# From the project root
./infra/start.sh
```

### Manual Verification
- **Kafka Topics**: 
  ```bash
  docker logs kafka-setup
  ```
- **Postgres Schemas**:
  ```bash
  docker exec -it postgres psql -U user -d banking -c "\dn"
  ```

---

## 3. Database & Kafka Configuration

### Postgres
- **Host**: `localhost`
- **Port**: `5433`
- **Database**: `banking`
- **Username**: `user`
- **Password**: `password`
- **Schemas**: `customer`, `account`, `transaction` (Set via `?currentSchema=` in JDBC URL)

### Kafka Topics Created
- `customers.events.created`
- `transactions.commands`
- `accounts.events`
- `transactions.events`
- `notifications.events`

---

## 4. Running the Services

Services should be started in the following order:

1. **Customer Service** (Port 8081)
   ```bash
   ./gradlew :customer-service:bootRun
   ```
2. **Account Service** (Port 8082)
   ```bash
   ./gradlew :account-service:bootRun
   ```
3. **Transaction Service** (Port 8083)
   ```bash
   ./gradlew :transaction-service:bootRun
   ```

---

## 5. Troubleshooting

### "Role 'user' does not exist" in Postgres
This usually happens if the Postgres container was started before the `init.sql` was mounted.
**Fix**: Run `./infra/start.sh` to wipe the volume and restart.

### Kafka Listener Issues
The application should connect to `localhost:9092`. The `init-topics` script inside Docker connects to `kafka:29092`.

### Outbox Events not publishing
Check the logs of `transaction-service`. The `OutboxProcessor` runs every 5 seconds to poll the `PENDING` events from the DB.
