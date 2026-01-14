# 🏦 Banking Microservices Demo

A production-grade, event-driven banking demonstration built with **Java 21**, **Spring Boot 3**, and **Apache Kafka**. This project showcases the **Outbox Pattern**, **Saga Choreography**, and **Distributed Tracing**.

---

## 🚀 Quick Start

1.  **Start Infrastructure**: Ensure Docker is running.
    ```bash
    ./infra/start.sh
    ```
2.  **Run Services**: Launch the microservices (Customer, Account, Transaction).
    ```bash
    ./gradlew :[service-name]:bootRun
    ```

---

### 📖 Essential Documentation

To make the project easier to navigate, we have consolidated our guides:

#### 🌟 [System Overview](docs/SYSTEM_OVERVIEW.md)
*The "Story" of the system—What it does and how a transaction flows.*

#### 📐 [Architecture & Specifications](docs/ARCHITECTURE.md)
*Core logic, Business Rules, and Technical Standards.*
- Saga Choreography & Compensation.
- Resiliency (Retries & DLQ).
- Idempotency & Exactly-Once Processing.
- Centralized Constants & Serialization Standards.

### 🛠️ [Development & Setup Guide](docs/DEVELOPMENT_GUIDE.md)
*How to build, run, and debug the system.*
- Infrastructure Setup details.
- Distributed Tracing with Jaeger.
- API Testing & Swagger.
- **Bug History & Prevention Checklist**.

---

## 🏗️ Services Overview

| Service | Port | Responsibility |
| :--- | :--- | :--- |
| **Customer Service** | `8081` | Manages customer profiles and registration. |
| **Account Service** | `8082` | Manages balances, versions, and business rules. |
| **Transaction Service** | `8083` | Orchestrates sagas and persists transaction states. |
| **API Gateway** | `8080` | Unified entry point and rate limiting. |

---

## 🛠️ Tech Stack
- **Languages**: Java 21
- **Messaging**: Kafka (KRaft mode)
- **Databases**: PostgreSQL (Relational), MongoDB (Audit), Redis (Idempotency)
- **Observability**: OpenTelemetry, Jaeger, Micrometer Tracing
- **Patterns**: Transactional Outbox, Saga, Idempotent Consumer
