# Code Structure

This document describes the physical and logical structure of the Banking Microservices Demo System repository.

---

## 1. Repository Root

```
banking-microservices/
├── common-lib/
├── account-service/
├── transaction-service/
├── customer-service/
├── notification-service/
├── api-gateway/
├── infra/
├── scripts/
├── specs/
├── docs/
├── .cursor/
├── build.gradle
└── README.md
```

---

## 2. common-lib

Shared library for cross-cutting concerns.

```
common-lib/
├── correlation/ # Correlation ID propagation
├── security/ # JWT utilities, auth helpers
├── events/ # Event envelope, base schemas
├── exception/ # Standardized exception models
└── util/ # Date, UUID, helper functions
```

---

## 3. account-service

Responsible for account lifecycle and balance management.

```
account-service/
├── config/ # JPA, Kafka, Redis configuration
├── controller/ # REST APIs
├── service/ # Business logic
├── repository/ # JPA repositories
├── model/ # Account entity
├── event/ # Kafka producers and consumers
└── dto/ # Request/response models
```

---

## 4. transaction-service

Manages transactions and saga orchestration.

```
transaction-service/
├── config/ # Kafka, Redis, Resilience configs
├── controller/ # Deposit, Withdraw, Transfer APIs
├── service/ # TransactionService, SagaCoordinator
├── repository/ # Transaction and Outbox storage
├── model/ # Transaction and Outbox entities
├── event/ # Producers and consumers
└── dto/ # API requests
```

---

## 5. customer-service

Manages customer profiles.

```
customer-service/
├── controller/
├── service/
├── repository/
├── model/
└── dto/
```

---

## 6. notification-service

Consumes events and sends notifications.

```
notification-service/
├── event/ # Kafka consumers
└── service/ # Email/SMS simulation
```

---

## 7. api-gateway

Entry point for external traffic.

```
api-gateway/
├── config/ # Security, routing, rate limiting
└── filter/ # Correlation, auth filters
```

---

## 8. infra

Infrastructure definitions.

```
infra/
├── docker-compose.yml
├── kafka/
│   └── topics.sh
├── prometheus/
└── grafana/
```

---

## 9. scripts

Demo and testing helpers.

```
scripts/
└── demo-transfer.sh
```

---

## 10. specs

Domain and protocol specifications.

```
specs/
├── _index.md
├── customers.md
├── accounts.md
├── transactions.md
├── notifications.md
├── events.md
├── gateway.md
└── adr/
    ├── 0001-event-driven.md
    ├── 0002-outbox-pattern.md
    └── 0003-saga-choreography.md
```

---

## 11. docs

Architecture and decision documentation.

```
docs/
├── code-structure.md
└── diagrams/
    ├── sequence-create-customer.puml
    ├── sequence-create-account.puml
    ├── sequence-deposit.puml
    ├── sequence-withdraw.puml
    ├── sequence-transfer.puml
    └── sequence-notification.puml
```

---

## 12. .cursor

AI assistant instructions.

```
.cursor/
├── rules.md
└── templates.md
```

---

## 13. Design Principles

- Clear bounded contexts per service
- No shared databases
- Event-first communication
- Idempotent and resilient design
- Infrastructure as code
- Production-grade observability and security

---

## 14. Ownership

Each service is owned independently and can be deployed separately.

---

## 15. Summary

This structure supports:

- Independent scaling and deployment
- High cohesion, low coupling
- Clear documentation and discoverability
