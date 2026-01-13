# Cursor Implementation Plan for Banking Microservices Demo

**Updated:** January 2026

---

## Overview

This plan guides Cursor to generate the full mono-repo Banking Microservices system with:

- Multi-module Spring Boot (Gradle Groovy DSL) using Java 21
- Services: Account, Transaction, Customer, Notification, API Gateway
- Event-driven communication (Kafka in KRaft mode – no ZooKeeper)
- Outbox pattern for reliable event publishing
- Saga choreography for transfer (event-based compensating transactions)
- Redis for caching (balance) and idempotency
- Postgres for relational storage (ACID transactions, optimistic locking)
- MongoDB for audit logs
- Docker Compose infra (KRaft Kafka + Redis + Postgres + Mongo + Jaeger for tracing)
- Sequence diagrams (PlantUML), ADRs, and demo scripts
- Observability: OpenTelemetry Java Agent (zero-code tracing) + Jaeger

**Note:** Current date context is January 2026 – Use Kafka 4.1.1+ in KRaft mode (ZooKeeper removed since 4.0).

---

## 🏁 Current Status & Progress Tracking

### Core Services
- [x] **common-lib**: DTOs, events, exceptions implemented.
- [x] **customer-service**: CRUD, JPA Auditing, Schema isolation implemented.
- [x] **account-service**: CRUD, Event Listener, Saga response events implemented. 
- [x] **transaction-service**: Deposit, Withdrawal, Transfer, Outbox Pattern, Saga coordination implemented.
- [ ] **notification-service**: *Pending*
- [ ] **api-gateway**: *Pending*

### Infrastructure
- [x] **Docker Compose**: Kafka (KRaft), Postgres, Jaeger, Adminer, Schema initializers.
- [x] **Kafka Topics**: Auto-initialized via `init-topics.sh`.
- [x] **Distributed Tracing**: OpenTelemetry integration (Customer-service: ✅, others: 🔄).

### Recent Critical Fixes (Jan 13, 2026)
- **Serialization Match**: Fixed double-serialization issue in `transaction-service` by changing Kafka `value-serializer` to `StringSerializer`.
- **Balance Update Logic**: Enhanced `account-service` to correctly update balances AND emit response events (`MoneyCredited`, etc.) to complete the Saga flow.
- **Robust Listener**: Improved `AccountEventListener` to handle payload mapping issues using `ObjectMapper`.

---

## 1. Project Setup

### 1.1 Root Structure

- Root folder: `banking-microservices/`
- Create Gradle multi-module project (Groovy DSL)

**Directory structure:**
```
banking-microservices/
├── common-lib/                  # DTOs, events, utils
├── account-service/
├── transaction-service/
├── customer-service/
├── notification-service/
├── api-gateway/
├── infra/                       # docker-compose.yml, otel-collector-config.yaml
├── specs/
├── docs/
│   └── diagrams/               # PlantUML sequence + C4
├── scripts/                     # demo scripts
└── .cursor/
```

### 1.2 Gradle Configuration

**settings.gradle:**
```groovy
rootProject.name = 'banking-microservices'
include 'common-lib'
include 'account-service'
include 'transaction-service'
include 'customer-service'
include 'notification-service'
include 'api-gateway'
```

**Root build.gradle (Groovy – lock versions):**
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.4' apply false
    id 'io.spring.dependency-management' version '1.1.6' apply false
}

allprojects {
    group = 'com.banking'
    version = '0.0.1-SNAPSHOT'

    repositories { 
        mavenCentral() 
    }

    java {
        toolchain { 
            languageVersion = JavaLanguageVersion.of(21) 
        }
    }
}
```

### 1.3 Additional Files

- Add `.gitignore`: IntelliJ, Gradle, Docker, Spring Boot logs, temp files, opentelemetry-javaagent.jar (if downloaded)

---

## 2. Cursor Rules

1. **Service Structure**: Each service must contain `Application.java`, `config/`, `controller/`, `service/`, `repository/`, `model/`, `dto/`, `event/`

2. **Outbox Pattern**: Transaction-service implements Outbox pattern with `@Scheduled` polling

3. **Saga Choreography**: Transfer flow uses Saga choreography (event-driven, compensating actions)

4. **Idempotency**: All POST requests require idempotency key (`Idempotency-Key: UUID`) checked in Redis

5. **API Gateway**: Spring Cloud Gateway handles JWT auth, RBAC, correlation ID propagation, rate limiting (Bucket4j + Redis)

6. **Unit Tests**: Generate unit test skeletons (JUnit 5 + Mockito) for services

7. **Integration Tests**: Integration tests use Testcontainers (Postgres, Redis, Kafka KRaft, MongoDB)

8. **Kafka KRaft**: Kafka uses KRaft mode (no ZooKeeper) – topics & events defined in `specs/events.md`

9. **Documentation**: Documentation includes C4/sequence diagrams (PlantUML), ADRs, README

10. **Observability**: Use OpenTelemetry Java Agent (auto-instrument HTTP/Kafka/JDBC) + Jaeger for tracing

---

## 3. Cursor Templates

### 3.1 Spring Boot Application Template

```java
@SpringBootApplication
public class {{ServiceName}}Application {
    public static void main(String[] args) {
        SpringApplication.run({{ServiceName}}Application.class, args);
    }
}
```

### 3.2 REST Controller Template

```java
@RestController
@RequestMapping("/api/v1/{{entity}}")
public class {{Entity}}Controller {
    private final {{Entity}}Service service;

    public {{Entity}}Controller({{Entity}}Service service) { 
        this.service = service; 
    }

    @PostMapping
    public ResponseEntity<{{Entity}}Response> create(
            @Valid @RequestBody {{CreateEntity}}Request req,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        // validate & process
    }
    // Other endpoints...
}
```

### 3.3 Kafka Producer/Consumer Template

**Producer:**
```java
@Component
public class {{Event}}Producer {
    private final KafkaTemplate<String, {{Event}}> kafkaTemplate;

    public void send({{Event}} event) {
        kafkaTemplate.send("{{topic}}", event.getCorrelationId(), event);
    }
}
```

**Consumer (idempotent):**
```java
@Component
@KafkaListener(topics = "{{topic}}", groupId = "{{groupId}}")
public void consume(@Payload {{Event}} event, Acknowledgment ack) {
    // Check idempotency in Redis/DB -> process -> ack.acknowledge()
}
```

### 3.4 Entity Template

```java
@Entity
public class {{Entity}} {
    @Id 
    private UUID id;
    
    @Version 
    private Long version; // optimistic locking
    
    // fields...
}
```

---

## 4. Specs-based Generation

Cursor reads `specs/*.md` to generate:

- `specs/customers.md` → CustomerService
- `specs/accounts.md` → AccountService
- `specs/transactions.md` → TransactionService (outbox + saga init)
- `specs/notifications.md` → NotificationService (Kafka consumer only)
- `specs/events.md` → Event schemas (DTOs in common-lib), topics (e.g., `banking.transaction.transfer-requested.v1`)

---

## 5. Infrastructure Generation

### 5.1 Docker Compose

**infra/docker-compose.yml**: Postgres, Mongo, Redis, Kafka (KRaft single broker), Jaeger, OpenTelemetry Collector

**Example Kafka KRaft (single combined mode):**
```yaml
kafka:
  image: apache/kafka:4.1.1
  container_name: kafka
  ports:
    - "9092:9092"
  environment:
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
    KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
    CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk  # random UUID
  volumes:
    - kafka_data:/tmp/kraft-combined-logs
```

### 5.2 OpenTelemetry

Mount OpenTelemetry Java Agent jar + env vars for OTEL exporter to Jaeger/Collector

---

## 6. Testing Generation

- **Unit tests**: Service layer (JUnit + Mockito)
- **Integration tests**: Testcontainers (Kafka KRaft support in latest versions)

---

## 7. Documentation Generation

- `docs/diagrams/` → PlantUML files (`sequence-create-account.puml`, `sequence-transfer.puml`, etc.)
- `docs/adr/` → `0001-event-driven.md`, `0002-outbox.md`, `0003-saga-choreography.md`
- `docs/code-structure.md`
- `README.md`: Setup (Java 21, Gradle), run (docker compose up), demo flows

---

## 8. Demo Scripts

**scripts/demo-transfer.sh**: curl deposit → transfer → check balance/logs/traces in Jaeger

---

## 9. Execution Steps for Cursor

1. Load `.cursor/rules.md` and `.cursor/templates.md`
2. Load `specs/*.md`
3. Generate per service: `build.gradle`, packages/classes, Kafka code, APIs, tests
4. Generate infra (KRaft Kafka, OTEL agent)
5. Generate diagrams, ADRs, README
6. Output: ready-to-run repo

---

## 10. Expected Outcome

✅ Full modern banking microservices mono-repo with:
- KRaft Kafka
- Java 21
- OTEL tracing
- Resilient architecture
- Complete documentation
