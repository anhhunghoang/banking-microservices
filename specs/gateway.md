# API Gateway Specification

## Responsibility
Provides a single entry point for clients.

## Features

- JWT authentication
- Role-based authorization
- Rate limiting per user
- Correlation ID injection

---

## Rules

- Reject unauthenticated requests.
- Forward correlation ID to downstream services.
- Apply rate limiting before routing.

---

## Non-Functional

- High availability
- Low latency
