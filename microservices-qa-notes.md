# Microservices Architecture — Q&A Notes

## Index

1. [Migrating a Monolith to Microservices (Strangler Fig Pattern)](#1-migrating-a-monolith-to-microservices-strangler-fig-pattern)
2. [Saga Pattern for Distributed Transactions](#2-saga-pattern-for-distributed-transactions)
3. [Database-per-Service Principle](#3-database-per-service-principle)
4. [Idempotency in APIs](#4-idempotency-in-apis)

---

## 1. Migrating a Monolith to Microservices (Strangler Fig Pattern)

I would migrate a monolithic application to microservices incrementally using the Strangler Fig pattern. First, I would understand the existing application and identify business domains or bounded contexts such as Order, Payment, Customer, and Inventory. Then I would identify the dependencies between these modules and define clear service boundaries.

I would start with one relatively independent module, extract it into a separate Spring Boot microservice with ownership of its data, and expose APIs or events for communication with the remaining monolith. I would gradually route traffic from the monolith to the new service. After validating the new service in production, I would repeat the process for other modules until the monolith is reduced or eventually removed.

During the migration, I would also introduce API Gateway, centralized configuration, authentication, monitoring, logging, tracing, and asynchronous communication where appropriate. For transactions that span multiple services, I would use patterns such as Saga rather than a single database transaction.

**[⬆ Back to Index](#index)**

---

## 2. Saga Pattern for Distributed Transactions

Saga Pattern is used to manage distributed transactions in microservices. Since each microservice has its own database, we cannot use a single database transaction across multiple services. So, we divide the overall business transaction into multiple local transactions. If one transaction fails, we execute compensating transactions to undo the changes made by the previous services.

For example, in an order process, the Order Service creates an order, the Payment Service processes the payment, and the Inventory Service reserves the product. If inventory reservation fails after payment succeeds, the Saga can trigger a refund in the Payment Service and cancel the order in the Order Service.

Saga can be implemented using either **choreography**, where services communicate through events, or **orchestration**, where a central Saga orchestrator manages the workflow. The main goal is to achieve eventual consistency across microservices.

**[⬆ Back to Index](#index)**

---

## 3. Database-per-Service Principle

Each microservice should own its data so that services remain loosely coupled and independently deployable. A service should access another service's data through APIs or events rather than directly accessing its database. This allows independent scaling, schema changes, and even database technology choices.

Separate databases also prevent one service's database changes from directly impacting another service. It does introduce challenges such as distributed transactions and eventual consistency, which are commonly handled using patterns like Saga and event-driven communication.

**[⬆ Back to Index](#index)**

---

## 4. Idempotency in APIs

Idempotency is the property where making the same API request multiple times has the same effect on the system as making it once. It is particularly useful for preventing duplicate operations when clients retry requests because of network failures.

**[⬆ Back to Index](#index)**
