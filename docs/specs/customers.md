# Customer Service Specification

## Responsibility
Manages customer profiles and identity within the banking system.

## API

### POST /customers
Create a new customer.

Request:
- name
- email

Response:
- id
- status

### GET /customers/{id}
Fetch customer profile.

---

## Data Model

Customer:
- id (UUID)
- name (string)
- email (string, unique)
- status (ACTIVE, INACTIVE)
- created_at
- updated_at

---

## Events

Outbound:
- CustomerCreated

---

## Business Rules

- Email must be unique across customers.
- Customers cannot be physically deleted.
- Customers can be deactivated but remain in audit history.

---

## Failure Handling

| Scenario | Handling |
|----------|-----------|
Duplicate email | Reject with 409 Conflict |
Invalid input | Reject with 400 Bad Request |

---

## Non-Functional

- Must be stateless.
- Must emit correlation ID in events.
