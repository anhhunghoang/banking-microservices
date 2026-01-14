# Transaction Service Specification

## Responsibility
Orchestrates multi-service sagas (Deposit, Withdrawal, Transfer) and manages the Transactional Outbox.

## API
### POST /transactions/deposit
### POST /transactions/withdraw
### POST /transactions/transfer

## Saga State Machine
- **PENDING**: Transaction created, outbox event saved.
- **COMPLETED**: Received success event from Account Service.
- **FAILED**: Received failure event or compensation succeeded.

---

## Transactional Outbox Pattern
1. Business logic updates `transactions` table.
2. `OutboxEvent` is saved in the **same DB transaction**.
3. `OutboxProcessor` polls `PENDING` events every 5s and publishes to Kafka.
4. On Kafka ACK, event status is marked `PROCESSED`.

---

## Messaging
### Outbound (Commands)
- Topic: `transactions.commands`
- Events: `DEPOSIT_REQUESTED`, `WITHDRAW_REQUESTED`, `TRANSFER_REQUESTED`, `REFUND_REQUESTED`.

### Inbound (Events)
- Topic: `accounts.events`
- Listens for: `MONEY_RESERVED`, `MONEY_CREDITED`, `MONEY_DEBITED`, `RESERVATION_FAILED`, `REFUND_COMPLETED`.

---

## Error Handling
- Processes DLT messages via `TransactionDltListener`.
- Automatically moves failed processing to `accounts.events.DLT` after 3 retries.
