# Banking Microservices - API Testing Commands

## Prerequisites
Make sure all services are running:
- Infrastructure: `./infra/start.sh`
- Customer Service: `./gradlew :customer-service:bootRun` (port 8081)
- Account Service: `./gradlew :account-service:bootRun` (port 8082)
- Transaction Service: `./gradlew :transaction-service:bootRun` (port 8083)

---

## 1. Create a Customer

```bash
curl -X POST http://localhost:8081/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nguyen Van A",
    "email": "nguyenvana@example.com"
  }'
```

**Expected Response:**
```json
{
  "id": "uuid-here",
  "name": "Nguyen Van A",
  "email": "nguyenvana@example.com",
  "status": "ACTIVE"
}
```

**Save the `id` for the next step!**

---

## 2. Create an Account

Replace `CUSTOMER_ID` with the ID from step 1:

```bash
curl -X POST http://localhost:8082/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUSTOMER_ID",
    "initialBalance": 0.00
  }'
```

**Expected Response:**
```json
{
  "id": "uuid-here",
  "customerId": "customer-uuid",
  "balance": 0.00,
  "status": "ACTIVE"
}
```

**Save the `id` for the next step!**

---

## 3. Make a Deposit

Replace `ACCOUNT_ID` with the ID from step 2:

```bash
curl -X POST http://localhost:8083/transactions/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACCOUNT_ID",
    "amount": 1000.00,
    "currency": "USD"
  }'
```

**Expected Response:**
```json
{
  "id": "uuid-here",
  "accountId": "account-uuid",
  "amount": 1000.00,
  "type": "DEPOSIT",
  "status": "PENDING"
}
```

---

## 4. Check Account Balance

Replace `ACCOUNT_ID` with your account ID:

```bash
curl -X GET http://localhost:8082/accounts/ACCOUNT_ID
```

**Expected Response:**
```json
{
  "id": "account-uuid",
  "customerId": "customer-uuid",
  "balance": 1000.00,
  "status": "ACTIVE"
}
```

---

## 5. Check Transaction Status

Replace `TRANSACTION_ID` with the ID from step 3:

```bash
curl -X GET http://localhost:8083/transactions/TRANSACTION_ID
```

**Expected Response:**
```json
{
  "id": "transaction-uuid",
  "accountId": "account-uuid",
  "amount": 1000.00,
  "type": "DEPOSIT",
  "status": "COMPLETED"
}
```

---

## Additional Operations

### Make a Withdrawal

```bash
curl -X POST http://localhost:8083/transactions/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACCOUNT_ID",
    "amount": 100.00,
    "currency": "USD"
  }'
```

### Make a Transfer

```bash
curl -X POST http://localhost:8083/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": "SOURCE_ACCOUNT_ID",
    "toAccountId": "DESTINATION_ACCOUNT_ID",
    "amount": 50.00,
    "currency": "USD"
  }'
```

---

## Automated Demo Script

For a complete automated demo, run:

```bash
chmod +x scripts/demo-deposit.sh
./scripts/demo-deposit.sh
```

This script will:
1. Create a customer
2. Create an account
3. Make a deposit
4. Check the balance
5. Verify transaction status

---

## Troubleshooting

### Transaction stays PENDING
- Wait 5-10 seconds (OutboxProcessor runs every 5 seconds)
- Check Kafka is running: `docker ps | grep kafka`
- Check account-service logs for event processing

### Connection Refused
- Ensure the service is running on the correct port
- Check `docker ps` for infrastructure status
- Verify no port conflicts

### Database Errors
- Restart infrastructure: `./infra/start.sh`
- Check PostgreSQL is running: `docker logs postgres`

---

## API Documentation

Each service exposes Swagger UI:
- Customer Service: http://localhost:8081/swagger-ui.html
- Account Service: http://localhost:8082/swagger-ui.html
- Transaction Service: http://localhost:8083/swagger-ui.html
