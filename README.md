# 🏦 Digital Banking System

A **distributed digital banking system** built using **Spring Boot Microservices**, designed to simulate real-world banking operations such as account management, money transfers, fraud detection, OTP verification, payments, notifications, and transaction processing.

The system follows a **microservices architecture** with **event-driven communication**, **Kafka**, **Redis**, and the **Saga Pattern** to coordinate distributed transactions across multiple services.

---

## 🚀 Project Overview

Traditional banking transactions often involve multiple independent systems. For example, transferring money from one account to another may require:

1. Validating the transaction
2. Checking the source account
3. Deducting the amount
4. Checking for fraud
5. Performing OTP verification if required
6. Crediting the destination account
7. Processing payment if necessary
8. Sending notifications
9. Handling failures and compensating previous operations

Because these operations are distributed across multiple services, maintaining consistency is challenging.

This project solves this problem using:

* **Microservices Architecture**
* **Event-Driven Architecture**
* **Apache Kafka**
* **Redis**
* **Saga Pattern**
* **Compensating Transactions**
* **Fraud Detection**
* **OTP Verification**
* **Payment Gateway Integration**
* **API Gateway**
* **Rate Limiting**

---

# 🏗️ Architecture

```text
                         ┌──────────────────┐
                         │      Client      │
                         │ Web / Mobile App │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │   API Gateway    │
                         │ Rate Limiting    │
                         └────────┬─────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
              ▼                   ▼                   ▼
       ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
       │   Account   │     │ Transaction │     │   Payment   │
       │   Service   │     │   Service   │     │   Service   │
       └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
              │                   │                   │
              │                   ▼                   │
              │            ┌─────────────┐            │
              │            │    Fraud    │            │
              │            │  Detection  │            │
              │            └──────┬──────┘            │
              │                   │                   │
              └───────────────────┼───────────────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │      Kafka       │
                         │ Event Streaming  │
                         └────────┬─────────┘
                                  │
                ┌─────────────────┼─────────────────┐
                │                 │                 │
                ▼                 ▼                 ▼
        ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
        │ Notification│   │    Fraud    │   │   Payment   │
        │   Service   │   │   Service   │   │   Service   │
        └─────────────┘   └─────────────┘   └─────────────┘

                         ┌─────────────┐
                         │    Redis    │
                         │ Cache / OTP │
                         │ Rate Limit  │
                         └─────────────┘
```

---

# 🧩 Microservices

## 1. Account Service

Responsible for managing customer accounts and balances.

### Responsibilities

* Create bank accounts
* Generate unique account numbers
* Retrieve account details
* Maintain account balances
* Deduct balance
* Credit balance
* Validate sufficient funds
* Handle account-related operations

---

## 2. Transaction Service

The central service responsible for initiating and coordinating transactions.

### Responsibilities

* Create transactions
* Validate transaction requests
* Maintain transaction state
* Publish transaction events
* Coordinate the transaction workflow
* Communicate with fraud detection
* Handle transaction completion
* Trigger compensation when failures occur

### Example Transaction States

```text
INITIATED
    ↓
FRAUD_CHECK
    ↓
VERIFICATION_REQUIRED
    ↓
VERIFIED
    ↓
PROCESSING
    ↓
COMPLETED
```

Possible failure states:

```text
FAILED
CANCELLED
COMPENSATED
```

---

# 🛡️ 3. Fraud Detection Service

Responsible for identifying potentially suspicious transactions.

### Fraud checks can include:

* Transaction amount
* Transaction frequency
* Account activity
* Transaction patterns
* Multiple transactions within a short period
* Suspicious transaction behavior

If a transaction requires additional verification:

```text
Transaction
     ↓
Fraud Detection
     ↓
Suspicious
     ↓
OTP Required
     ↓
User Verification
     ↓
Continue / Reject
```

---

# 🔐 4. OTP Verification

OTP verification provides an additional security layer for transactions flagged by the fraud detection service.

Redis can be used to temporarily store OTP information.

Example:

```text
OTP Generated
     ↓
Stored in Redis
     ↓
Sent to User
     ↓
User submits OTP
     ↓
Validate OTP
     ↓
Delete OTP
```

---

# 🔔 5. Notification Service

Responsible for sending transaction-related notifications.

Examples:

* Transaction successful
* Transaction failed
* OTP generated
* Payment successful
* Payment failed
* Account created
* Suspicious transaction detected

The service consumes events from Kafka and processes notifications asynchronously.

---

# 💳 6. Payment Service

Responsible for external payment processing.

### Responsibilities

* Create payment orders
* Communicate with payment gateway
* Track payment status
* Handle payment success
* Handle payment failure
* Process webhooks
* Update transaction status

### Payment Flow

```text
Transaction Service
        ↓
Payment Service
        ↓
Create Payment Order
        ↓
Payment Gateway
        ↓
Customer Payment
        ↓
Webhook
   ┌────┴────┐
   ↓         ↓
Success    Failure
   ↓         ↓
Complete   Compensate
Transaction Transaction
```

---

# 🌐 7. API Gateway

The API Gateway acts as the single entry point for clients.

### Responsibilities

* Route requests
* Rate limiting
* Request filtering
* Authentication/authorization integration
* Protect backend services
* Centralized API entry point

```text
Client
  ↓
API Gateway
  ↓
Microservices
```

---

# 🔄 Saga Pattern

Since the application consists of multiple microservices, a transaction cannot simply use one traditional database transaction across all services.

The project therefore uses the **Saga Pattern**.

A transaction is divided into multiple local transactions.

Example:

```text
Transaction Started
       ↓
Debit Source Account
       ↓
Fraud Check
       ↓
OTP Verification
       ↓
Credit Destination Account
       ↓
Payment Processing
       ↓
Transaction Completed
```

If something fails:

```text
Payment Failed
      ↓
Compensation Triggered
      ↓
Reverse Previous Operations
      ↓
Transaction Failed
```

This allows the system to maintain consistency without requiring a distributed database transaction.

---

# 📡 Event-Driven Architecture

Services communicate asynchronously using **Apache Kafka**.

Example:

```text
Transaction Service
        │
        │ TransactionInitiated
        ▼
      Kafka
        │
        ├──────────────► Fraud Service
        │
        ├──────────────► Notification Service
        │
        └──────────────► Payment Service
```

This reduces direct coupling between services and allows services to process events independently.

---

# 📨 Kafka Events

Some example events used by the system:

```text
TransactionInitiated
TransactionVerified
TransactionFailed
TransactionCompleted
FraudCheckRequired
OtpRequired
PaymentCreated
PaymentSuccessful
PaymentFailed
NotificationRequested
```

Kafka topics can be organized around these events.

Example:

```text
transaction-events
fraud-events
payment-events
notification-events
```

---

# ⚡ Redis

Redis is used for fast, temporary data storage and caching.

### Use cases

* OTP storage
* OTP expiration
* Rate limiting
* Frequently accessed data
* Temporary transaction information
* Distributed caching

Example OTP:

```text
Key:
otp:user:12345

Value:
829341

TTL:
5 minutes
```

---

# 🗄️ Database

Each microservice can maintain its own database following the **Database-per-Service** pattern.

```text
Account Service
      ↓
Account Database

Transaction Service
      ↓
Transaction Database

Fraud Service
      ↓
Fraud Database

Payment Service
      ↓
Payment Database
```

This keeps services loosely coupled and allows each service to evolve independently.

---

# 🛠️ Tech Stack

### Backend

* Java
* Spring Boot 4.1.1
* Spring Web
* Spring Data JPA
* Spring Security

### Microservices

* Spring Boot
* REST APIs
* API Gateway

### Messaging

* Apache Kafka
* Zookeeper

### Database

* PostgreSQL

### Caching

* Redis

### Containerization

* Docker
* Docker Compose

### Payment

* Payment Gateway
* Webhooks

### Architecture Patterns

* Microservices
* Event-Driven Architecture
* Saga Pattern
* Compensating Transactions
* Database-per-Service

---

# 🐳 Docker Infrastructure

The project uses Docker Compose to run infrastructure services locally.

```text
Docker Compose
│
├── Redis
│
├── Kafka
│
└── Zookeeper
```

Start infrastructure:

```bash
docker compose up -d
```

Check running containers:

```bash
docker ps
```

Stop containers:

```bash
docker compose down
```

---

# ⚙️ Kafka Configuration

Example configuration when Spring Boot runs locally:

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

When Spring Boot services also run inside Docker:

```properties
spring.kafka.bootstrap-servers=kafka:29092
```

---

# ⚡ Redis Configuration

When Spring Boot runs locally:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

When Spring Boot runs inside Docker:

```properties
spring.data.redis.host=redis
spring.data.redis.port=6379
```

---

# 🔐 Security Considerations

The system is designed with several security mechanisms:

* Authentication and authorization
* OTP verification
* Fraud detection
* Rate limiting
* Secure API Gateway
* Input validation
* Transaction validation
* Payment webhook verification
* Sensitive configuration through environment variables

Secrets should **not** be committed to Git.

Example:

```properties
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
PAYMENT_KEY=${PAYMENT_KEY}
PAYMENT_SECRET=${PAYMENT_SECRET}
```

---

# 📊 Transaction Flow

A simplified money-transfer flow:

```text
                 User
                  │
                  ▼
             API Gateway
                  │
                  ▼
         Transaction Service
                  │
                  ▼
        Create Transaction
                  │
                  ▼
                Kafka
                  │
                  ▼
          Fraud Detection
             │         │
          Safe       Suspicious
             │         │
             │         ▼
             │     OTP Required
             │         │
             │      Verify
             │         │
             └────┬────┘
                  ▼
          Account Service
                  │
           ┌──────┴──────┐
           ▼             ▼
       Deduct          Credit
       Source          Target
           │             │
           └──────┬──────┘
                  ▼
          Payment Service
                  │
                  ▼
          Payment Gateway
                  │
             ┌────┴────┐
             ▼         ▼
          Success    Failure
             │         │
             ▼         ▼
         Complete   Compensate
             │
             ▼
       Notification
             │
             ▼
            User
```

---

# 🔄 Compensation Flow

If a later operation fails, previously completed operations can be compensated.

Example:

```text
Debit ₹1000
    ↓
Fraud Check ✓
    ↓
OTP ✓
    ↓
Payment ✗
    ↓
Compensation
    ↓
Credit ₹1000 back
    ↓
Transaction FAILED
```

This is one of the important concepts demonstrated by this project.

---

# 📁 Project Structure

```text
digital-banking-system/
│
├── api-gateway/
│
├── account-service/
│
├── transaction-service/
│
├── fraud-detection-service/
│
├── notification-service/
│
├── payment-service/
│
├── docker-compose.yml
│
└── README.md
```

Each service follows a typical Spring Boot structure:

```text
account-service/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ...
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── Dockerfile
└── pom.xml
```

---

# 🧪 Testing

The system can be tested using:

* Postman
* REST clients
* Unit tests
* Integration tests
* Kafka event testing

Example API:

```http
POST /api/v1/accounts
```

```http
GET /api/v1/accounts/{accountNumber}
```

```http
POST /api/v1/transactions
```

```http
GET /api/v1/transactions/{transactionId}
```

---

# 🚦 Running the Project

### 1. Clone the repository

```bash
git clone <repository-url>
cd digital-banking-system
```

### 2. Start infrastructure

```bash
docker compose up -d
```

### 3. Start PostgreSQL databases

Configure the required PostgreSQL databases for each service.

### 4. Configure environment variables

```text
DB_USERNAME
DB_PASSWORD
KAFKA_BOOTSTRAP_SERVERS
REDIS_HOST
PAYMENT_KEY
PAYMENT_SECRET
```

### 5. Start microservices

Start:

```text
API Gateway
Account Service
Transaction Service
Fraud Detection Service
Notification Service
Payment Service
```

### 6. Test APIs

Use Postman or another REST client to test the complete transaction flow.

---

# 📈 Future Improvements

Possible future improvements include:

* JWT authentication
* OAuth2 authentication
* Service discovery
* Centralized configuration
* Distributed tracing
* Prometheus monitoring
* Grafana dashboards
* Elasticsearch/Kibana logging
* Kubernetes deployment
* CI/CD pipeline
* Multi-region deployment
* Advanced fraud detection using machine learning
* Real-time transaction monitoring

---

# 🎯 Learning Objectives

This project demonstrates practical implementation of:

* Spring Boot Microservices
* REST API development
* Kafka event-driven architecture
* Redis caching
* Saga Pattern
* Distributed transactions
* Compensation mechanisms
* Fraud detection
* OTP verification
* Payment gateway integration
* Webhook handling
* API Gateway
* Rate limiting
* Docker and containerization
* PostgreSQL
* Distributed-system design

---

# 👨‍💻 Author

**Yuvi Chib**

## ⭐ Project Status

🚧 **Under Development**

The project is being developed incrementally, starting with the core banking services and gradually integrating Kafka, Redis, fraud detection, payments, notifications, and distributed transaction handling.
