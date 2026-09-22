# Microservices Architecture — Q&A Notes

## Index

1. [Idempotency in APIs](#1-idempotency-in-apis)
2. [Database-per-Service Principle](#2-database-per-service-principle)
3. [Saga Pattern for Distributed Transactions](#3-saga-pattern-for-distributed-transactions)
4. [Migrating a Monolith to Microservices (Strangler Fig Pattern)](#4-migrating-a-monolith-to-microservices-strangler-fig-pattern)

---

## 1. Idempotency in APIs

- **Definition:** Making the same API request multiple times has the same effect as making it once.
- **Why it matters:** Prevents duplicate operations when clients retry requests after network failures.

**[⬆ Back to Index](#index)**

---

## 2. Database-per-Service Principle

- **Rule:** Each microservice owns its data — no other service touches that database directly.
- **How to access it:** Through APIs or events, never direct DB access.
- **Benefits:**
  - Independent scaling
  - Independent schema changes
  - Freedom to pick different database technologies per service
  - One service's DB changes don't break another service
- **Trade-off:** Introduces distributed transactions and eventual consistency, handled via Saga and event-driven communication.

**[⬆ Back to Index](#index)**

---

## 3. Saga Pattern for Distributed Transactions

- **Problem it solves:** No single DB transaction can span multiple microservices (each owns its own DB).
- **Approach:** Break one big transaction into multiple local transactions.
- **On failure:** Run compensating transactions to undo prior steps.
- **Example flow (Order):**
  1. Order Service creates order
  2. Payment Service processes payment
  3. Inventory Service reserves product
  4. If inventory reservation fails → refund via Payment Service + cancel order via Order Service
- **Two implementation styles:**
  - **Choreography** — services communicate via events (no central controller)
  - **Orchestration** — a central Saga orchestrator drives the workflow
- **Goal:** Eventual consistency across services.

**[⬆ Back to Index](#index)**

---

## 4. Migrating a Monolith to Microservices (Strangler Fig Pattern)

- **Step 1 — Understand the monolith:** Identify business domains / bounded contexts (e.g., Order, Payment, Customer, Inventory).
- **Step 2 — Map dependencies:** Identify inter-module dependencies and define clear service boundaries.
- **Step 3 — Extract one module first:**
  - Pick a relatively independent module
  - Build it as a separate Spring Boot microservice
  - Give it ownership of its own data
  - Expose APIs/events for communication with the remaining monolith
- **Step 4 — Shift traffic gradually:** Route traffic from monolith to the new service incrementally.
- **Step 5 — Validate, then repeat:** Once proven in production, repeat for the next module until the monolith shrinks or disappears.
- **Supporting infrastructure to introduce along the way:**
  - API Gateway
  - Centralized configuration
  - Authentication
  - Monitoring, logging, tracing
  - Asynchronous communication where appropriate
- **Cross-service transactions:** Use Saga pattern instead of a single DB transaction.

**[⬆ Back to Index](#index)**
