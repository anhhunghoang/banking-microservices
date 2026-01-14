# 🏦 System Overview: How it Works

This document provides a high-level narrative description of the Banking Microservices system, explaining the "Why" and "How" behind its design.

## 1. The Core Mission
The system is designed to simulate a real-world banking environment where **consistency** and **reliability** are non-negotiable. It solves the challenge of maintaining an accurate financial ledger across multiple distributed services using the **Saga Pattern** and **Transactional Outbox**.

## 2. The Service Landscape
The system is divided into focused bounded contexts:
- **Customer Service**: The "Source of Truth" for identity. It starts the journey by registering a person.
- **Account Service**: The "Ledger." It owns the money. It doesn't care about "Transactions" (like transfers), it only cares about "Balance Changes" (Credits/Debits) and ensuring no account goes below zero.
- **Transaction Service**: The "Orchestrator." It doesn't own money; it owns the **Intent**. It manages the state machine of a transfer from start to finish.

## 3. The "Life of a Transfer" (The Narrative)
When a user initiates a transfer of $100 from Alice to Bob:

1.  **The Intent**: The **Transaction Service** records a "PENDING" transaction. It persists this to its database and an "Outbox" table simultaneously.
2.  **The Command**: A background worker picks up the intent and sends a `TRANSFER_REQUESTED` command to Kafka.
3.  **The Reservation**: Alice's **Account Service** receives the command. It checks her balance. If she has $100, it **locks** (reserves) that money. It's not gone yet, but she can't spend it elsewhere.
4.  **The Credit**: Finding the reservation successful, the **Account Service** for Bob (the receiver) adds $100 to his balance.
5.  **The Completion**: Once Bob's balance is updated, the **Transaction Service** is notified via Kafka. It marks the transaction as "COMPLETED." Alice's reserved money is now officially "Debited."

## 4. What happens when things go wrong? (Resiliency)
Banks must handle failures gracefully:
- **Transient Failures**: If Bob's service is briefly down, Kafka will **retry** the credit 3 times.
- **Permanent Failures**: If Bob's account is frozen or doesn't exist, the system triggers a **Compensation**. A `REFUND_REQUESTED` event is sent back to Alice's service to "Unlock" her reserved $100. No money is lost.
- **Poison Messages**: If a message is fundamentally broken (e.g., bad data), it is moved to a **Dead Letter Topic (DLT)** for manual investigation, preventing it from blocking other users' transactions.

## 5. Visibility (How we watch it)
Because the system is asynchronous, we use **Distributed Tracing (Jaeger)**. Every request gets a "Passport" (Trace ID). As the $100 moves through Kafka and different databases, the Trace ID follows it, allowing us to see the entire world-spanning journey in a single timeline.
