# Banking Microservices Demo System

A cloud-native, event-driven banking microservices demonstration using Java 21, Spring Boot, and Kafka.

## Quick Start (Infrastructure)

Ensure Docker is running, then start the infrastructure:

```bash
./infra/start.sh
```

## Documentation

All project documentation is located in the `docs/` folder:

- **[Setup Guide](docs/instructions/setup-guide.md)**: Detailed instructions on how to run the services.
- **[Architecture Plan](docs/instructions/plan.md)**: The overall technical design and roadmap.
- **[Code Structure](docs/instructions/code-structure.md)**: Explanation of the project layout.
- **[Java Coding Rules](docs/instructions/rules.md)**: Standards and TDD practices.
- **[Infrastructure Details](docs/instructions/infra.md)**: Deep dive into the Docker and Kafka setup.

## Services Overview

- **Customer Service**: Profile management and registration.
- **Account Service**: Balance management and ledger.
- **Transaction Service**: Processing deposits/withdrawals with Transactional Outbox.
- **Notification Service**: Simulated alerts and emails.
- **API Gateway**: Unified entry point for external traffic.

## Tech Stack

- **Java 21** & **Spring Boot 3.3.4**
- **Apache Kafka 4.1.1 (KRaft Mode)**
- **PostgreSQL 16** (Transactional Store)
- **MongoDB 7.0** (Audit Logs)
- **Redis 7** (Idempotency & Caching)
- **Jaeger / OpenTelemetry** (Distributed Tracing)
