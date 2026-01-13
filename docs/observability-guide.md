# Observability & API Testing Guide

## Overview
The banking microservices system provides two powerful UIs for testing and monitoring:

1. **Swagger UI** - Interactive API documentation and testing (already implemented)
2. **Jaeger UI** - Distributed tracing visualization (OpenTelemetry integration in progress)

---

## 🎯 Swagger UI - API Testing (READY TO USE)

### What is Swagger UI?
Swagger UI provides an interactive web interface where you can:
- View all available API endpoints
- See request/response schemas
- **Test APIs directly from the browser** (no curl needed!)
- Generate code samples
- View API documentation

### Access Swagger UI

Each service has its own Swagger UI:

| Service | Swagger UI URL | Port |
|---------|---------------|------|
| **Customer Service** | http://localhost:8081/swagger-ui.html | 8081 |
| **Account Service** | http://localhost:8082/swagger-ui.html | 8082 |
| **Transaction Service** | http://localhost:8083/swagger-ui.html | 8083 |

### How to Use Swagger UI

1. **Start the service**:
   ```bash
   ./gradlew :customer-service:bootRun
   ```

2. **Open Swagger UI** in your browser:
   ```
   http://localhost:8081/swagger-ui.html
   ```

3. **Test an API**:
   - Click on an endpoint (e.g., `POST /customers`)
   - Click "Try it out"
   - Fill in the request body:
     ```json
     {
       "name": "Nguyen Van A",
       "email": "nguyenvana@example.com"
     }
     ```
   - Click "Execute"
   - See the response immediately!

### Example: Complete Flow via Swagger UI

#### Step 1: Create Customer (Customer Service Swagger)
1. Go to http://localhost:8081/swagger-ui.html
2. POST /customers → Try it out
3. Request body:
   ```json
   {
     "name": "Test User",
     "email": "test@example.com"
   }
   ```
4. Execute → Copy the `id` from response

#### Step 2: Create Account (Account Service Swagger)
1. Go to http://localhost:8082/swagger-ui.html
2. POST /accounts → Try it out
3. Request body:
   ```json
   {
     "customerId": "paste-customer-id-here",
     "initialBalance": 0.00
   }
   ```
4. Execute → Copy the `id` from response

#### Step 3: Make Deposit (Transaction Service Swagger)
1. Go to http://localhost:8083/swagger-ui.html
2. POST /transactions/deposit → Try it out
3. Request body:
   ```json
   {
     "accountId": "paste-account-id-here",
     "amount": 1000.00,
     "currency": "USD"
   }
   ```
4. Execute → See transaction created!

---

## 🔍 Jaeger UI - Distributed Tracing (IN PROGRESS)

### What is Jaeger?
Jaeger provides distributed tracing to help you:
- **Visualize request flow** across all microservices
- **Identify bottlenecks** and slow operations
- **Debug issues** by seeing the complete request path
- **Monitor performance** of each service

### Architecture
```
Request → Customer Service → Kafka → Account Service
              ↓                          ↓
         OpenTelemetry              OpenTelemetry
              ↓                          ↓
              └──────→ Jaeger ←──────────┘
                         ↓
                    Jaeger UI
```

### Access Jaeger UI

Once OpenTelemetry is fully configured:

**Jaeger UI**: http://localhost:16686

### What You'll See in Jaeger

1. **Service List**: All your microservices
2. **Trace Timeline**: Visual representation of request flow
3. **Span Details**: Time spent in each service/operation
4. **Dependencies**: Service dependency graph

### Example Trace Visualization

When you create a deposit via Swagger UI, Jaeger will show:

```
Trace: Create Deposit
├─ customer-service: POST /customers (120ms)
│  ├─ DB: Insert customer (45ms)
│  └─ Kafka: Publish CustomerCreated (15ms)
├─ account-service: POST /accounts (95ms)
│  ├─ DB: Insert account (40ms)
│  └─ Kafka: Publish AccountCreated (10ms)
└─ transaction-service: POST /transactions/deposit (150ms)
   ├─ DB: Insert transaction (50ms)
   ├─ DB: Insert outbox event (30ms)
   └─ Kafka: Publish DepositRequested (20ms)
```

---

## 🚀 Current Status

### ✅ Already Implemented
- **Swagger UI** for all 3 services
- **Jaeger** container in docker-compose
- **OpenAPI annotations** on all controllers

### 🔄 In Progress
- **OpenTelemetry dependencies** (customer-service: ✅ added)
- **Tracing configuration** (customer-service: ✅ configured)
- **Account-service** OpenTelemetry setup (pending)
- **Transaction-service** OpenTelemetry setup (pending)

---

## 📊 Comparison: Swagger UI vs Jaeger UI

| Feature | Swagger UI | Jaeger UI |
|---------|-----------|-----------|
| **Purpose** | API testing & documentation | Distributed tracing & monitoring |
| **Use Case** | Test individual endpoints | Debug cross-service flows |
| **View** | Single service at a time | All services together |
| **Data** | Request/Response | Timing & dependencies |
| **Best For** | Development & testing | Performance optimization |

---

## 🎓 When to Use Each Tool

### Use Swagger UI When:
- ✅ Testing a single API endpoint
- ✅ Exploring available APIs
- ✅ Generating sample requests
- ✅ Quick manual testing without curl

### Use Jaeger UI When:
- ✅ Debugging slow requests
- ✅ Understanding service dependencies
- ✅ Finding bottlenecks
- ✅ Monitoring production performance
- ✅ Analyzing distributed transactions

---

## 🔧 Next Steps to Complete OpenTelemetry

To finish the OpenTelemetry integration:

1. **Add dependencies** to account-service and transaction-service
2. **Configure tracing** in their application.yml files
3. **Restart all services**
4. **Make API calls** via Swagger UI
5. **View traces** in Jaeger UI at http://localhost:16686

---

## 💡 Pro Tips

### Swagger UI Tips:
1. **Use "Authorize"** button if APIs require authentication
2. **Copy response IDs** to use in subsequent requests
3. **Check "Schemas"** section for data models
4. **Use "Try it out"** for quick testing

### Jaeger UI Tips:
1. **Filter by service** to focus on specific microservice
2. **Sort by duration** to find slow requests
3. **Compare traces** to identify performance regressions
4. **Use tags** to search for specific operations

---

## 🎯 Quick Start

1. **Start infrastructure**:
   ```bash
   ./infra/start.sh
   ```

2. **Start services**:
   ```bash
   ./gradlew :customer-service:bootRun
   ./gradlew :account-service:bootRun
   ./gradlew :transaction-service:bootRun
   ```

3. **Test with Swagger UI**:
   - Open http://localhost:8081/swagger-ui.html
   - Create a customer
   - Create an account
   - Make a deposit

4. **View traces in Jaeger** (once OpenTelemetry is complete):
   - Open http://localhost:16686
   - Select service: "customer-service"
   - Click "Find Traces"
   - Click on a trace to see details

---

## Summary

You now have **two powerful UIs**:

1. **Swagger UI** (✅ Ready) - Test APIs interactively
2. **Jaeger UI** (🔄 In Progress) - Visualize distributed traces

**No curl commands needed!** Just use Swagger UI to test all APIs from your browser.
