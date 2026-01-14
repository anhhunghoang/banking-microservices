# Account Service Specification

## Responsibility
Manages bank accounts, balances, and enforces core ledger integrity.

## API
### POST /accounts
Create an account for a customer with an initial balance.

### GET /accounts/{id}
Fetch full account details including status and version.

---

## Data Model
- **Account**:
    - `id` (UUID)
    - `customer_id` (UUID)
    - `balance` (BigDecimal)
    - `status` (ACTIVE, FROZEN)
    - `version` (Optimistic Locking)
    - `created_at`

---

## Messaging (Kafka)
Uses `common-lib` constants for consistency.

### Inbound (Commands)
- `DEPOSIT_REQUESTED` (Topic: `transactions.commands`)
- `WITHDRAW_REQUESTED` (Topic: `transactions.commands`)
- `TRANSFER_REQUESTED` (Topic: `transactions.commands`)
- `REFUND_REQUESTED` (Topic: `transactions.commands`)

### Outbound (Events)
- `ACCOUNT_CREATED` (Topic: `accounts.events`)
- `MONEY_RESERVED`
- `MONEY_CREDITED`
- `MONEY_DEBITED`
- `RESERVATION_FAILED`
- `REFUND_COMPLETED`

---

## Business Rules Enforcement
- **Rule R1**: Balance must never be negative.
- **Rule R2**: All state-changing operations are rejected if account status is `FROZEN`.
- **Rule R3**: Operations are idempotent via `event_id` stored in `processed_events`.
- **Rule R4**: Optimistic locking ensures data integrity during concurrent updates.

---

## Resiliency
- **Retries**: 3 retries for transient failures.
- **DLQ**: Permanently failed commands move to `transactions.commands.DLT`.
- **Observation**: `AccountDltListener` logs DLT messages with original exception headers.
