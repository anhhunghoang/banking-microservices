# Notification Service Specification

## Responsibility
Sends notifications about transaction outcomes.

## Events Inbound
- TransactionCompleted
- TransactionFailed

---

## Notification Channels

- Console log
- Mock email sender

---

## Business Rules

- Notifications are best-effort.
- Failures do not affect transaction outcome.

---

## Non-Functional

- No retries impacting business logic.
- Observability only.
