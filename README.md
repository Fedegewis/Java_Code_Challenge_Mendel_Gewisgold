# Mendel Transaction Service

A RESTful transaction management service built with Spring Boot that provides in-memory storage for transactions with parent-child relationships and transitive sum calculations.

## Project Description

Mendel Transaction Service is a lightweight REST API designed to manage financial transactions with hierarchical (parent-child) relationships. It allows creating transactions, querying transactions by type, and calculating the transitive sum of a transaction and all its descendants.

**Note:** Java 21 is used, which satisfies the Java 11+ requirement of the challenge.

## Tech Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.3.0 |
| Build Tool | Maven | 3.x |
| Testing | Spring Boot Test + JUnit 5 | - |
| Container | Docker + Docker Compose | - |

## Requirements

- **Java 21** or higher (satisfies Java 11+ requirement)
- **Maven 3.6+** (for local development)
- **Docker & Docker Compose** (for containerized deployment)

## Architecture

### High-Level Architecture

```
+-----------------------------------------------------+
|                   Client Application                 |
+----------------------------+-------------------------+
                             | HTTP/REST
                             v
+-----------------------------------------------------+
|              TransactionController                   |
|  - Handles HTTP request mapping                     |
|  - Basic request body handling                       |
+----------------------------+-------------------------+
                             |
                             v
+-----------------------------------------------------+
|              TransactionServiceImpl                  |
|  - Business logic                                    |
|  - Field validation (amount, type, parent_id)       |
|  - Sum calculation (recursive)                     |
+----------------------------+-------------------------+
                             |
                             v
+-----------------------------------------------------+
|           InMemoryTransactionRepository             |
|  - ConcurrentHashMap for transaction storage        |
|  - O(1) lookups by ID                               |
|  - Indexed queries by type                          |
+-----------------------------------------------------+
```

### Data Model

```
Transaction {
    id: Long          # Unique identifier
    amount: Double    # Transaction amount (> 0)
    type: String      # Transaction category (required, non-blank)
    parentId: Long?   # Parent transaction ID (optional)
}
```

### Storage Design

The repository uses ConcurrentHashMap for thread-safe storage:

| Map | Type | Purpose |
|-----|------|---------|
| `transactions` | `ConcurrentHashMap<Long, Transaction>` | O(1) lookup by ID |
| `transactionsByType` | `ConcurrentHashMap<String, List<Transaction>>` | O(1) lookup by type |
| `parentToChildren` | `ConcurrentHashMap<Long, List<Long>>` | O(1) lookup of children |

**Note:** The maps themselves are thread-safe, but the List values stored within are mutable. The repository handles synchronization internally.

### Validation

Field validation (amount > 0, type not blank, parent existence, cycle detection) is performed in the `TransactionServiceImpl` layer. The controller handles HTTP request mapping and basic request body deserialization.

### Transitive Sum Algorithm

```
calculateSum(transaction):
    sum = transaction.amount
    for each childId in findChildrenIdsByParentId(transaction.id):
        child = findById(childId)
        if child != null:
            sum += calculateSum(child)
    return sum
```

- **Time Complexity**: O(n) where n is the total number of descendant transactions
- **Space Complexity**: O(h) where h is the tree height (recursion stack)

## API Endpoints

### 1. Root Endpoint

**GET** `/`

Returns a welcome message.

**Response:**
```
Mendel Transaction Service is running. Use /health for health check.
```

### 2. Create/Update Transaction

**PUT** `/transactions/{transactionId}`

Creates or updates a transaction with the specified ID.

**Request Body:**
```json
{
    "amount": 5000.00,
    "type": "cars",
    "parent_id": null
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| amount | Double | Yes | Must be greater than zero |
| type | String | Yes | Cannot be null or blank |
| parent_id | Long | No | Must reference an existing transaction |

**Responses:**

| Status | Description | Example |
|--------|-------------|---------|
| 200 OK | Transaction created/updated successfully | `{"status": "ok"}` |
| 400 Bad Request | Validation error | See Error Response format |

### 3. Get Transactions by Type

**GET** `/transactions/types/{type}`

Returns all transaction IDs matching the specified type.

**Responses:**

| Status | Description | Example |
|--------|-------------|---------|
| 200 OK | List of matching transaction IDs | `[1, 2, 3]` |
| 200 OK | No matches (empty array) | `[]` |

### 4. Get Transaction Sum

**GET** `/transactions/sum/{transactionId}`

Returns the sum of the transaction plus all its descendants.

**Responses:**

| Status | Description | Example |
|--------|-------------|---------|
| 200 OK | Transitive sum calculated | `{"sum": 15000.0}` |
| 404 Not Found | Transaction does not exist | See Error Response format |

### 5. Health Check

**GET** `/health`

Returns service health status.

**Response:**
```
OK
```

## Error Response Format

All error responses follow this format:

```json
{
    "status": 400,
    "message": "Amount must be greater than zero",
    "timestamp": "2024-01-15T10:30:00"
}
```

| Field | Type | Description |
|-------|------|-------------|
| status | int | HTTP status code |
| message | String | Human-readable error message |
| timestamp | String | ISO-8601 formatted datetime |

## Error Handling

| Error Condition | HTTP Status | Error Message |
|----------------|-------------|---------------|
| Amount is null | 400 | "Amount is required" |
| Amount <= 0 | 400 | "Amount must be greater than zero" |
| Type is null or blank | 400 | "Type is required" |
| Parent ID references non-existent transaction | 400 | "Parent transaction with id {id} not found" |
| Transaction cannot be its own parent | 400 | "Transaction cannot be its own parent" |
| Setting parent would create cycle | 400 | "Setting this parent would create a cycle" |
| Malformed JSON | 400 | "Invalid request body" |
| Transaction not found (sum) | 404 | "Transaction with id {id} not found" |

## Request/Response Examples

### Root Endpoint

```bash
curl http://localhost:8080/
```

**Response:**
```
Mendel Transaction Service is running. Use /health for health check.
```

### Create a Parent Transaction

```bash
curl -X PUT http://localhost:8080/transactions/1 \
  -H "Content-Type: application/json" \
  -d '{"amount": 5000.00, "type": "cars"}'
```

**Response:**
```json
{"status": "ok"}
```

### Create a Child Transaction

```bash
curl -X PUT http://localhost:8080/transactions/2 \
  -H "Content-Type: application/json" \
  -d '{"amount": 3000.00, "type": "debit", "parent_id": 1}'
```

**Response:**
```json
{"status": "ok"}
```

### Create a Grandchild Transaction

```bash
curl -X PUT http://localhost:8080/transactions/3 \
  -H "Content-Type: application/json" \
  -d '{"amount": 2000.00, "type": "debit", "parent_id": 2}'
```

**Response:**
```json
{"status": "ok"}
```

### Get Transactions by Type

```bash
curl http://localhost:8080/transactions/types/debit
```

**Response:**
```json
[2, 3]
```

### Get Transitive Sum

```bash
curl http://localhost:8080/transactions/sum/1
```

**Response:**
```json
{"sum": 10000.0}
```

### Error: Non-Existent Parent

```bash
curl -X PUT http://localhost:8080/transactions/10 \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "type": "test", "parent_id": 999}'
```

**Response (400 Bad Request):**
```json
{"status": 400, "message": "Parent transaction with id 999 not found", "timestamp": "2024-01-15T10:30:00"}
```

### Error: Self-Reference Parent

```bash
curl -X PUT http://localhost:8080/transactions/5 \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "type": "test", "parent_id": 5}'
```

**Response (400 Bad Request):**
```json
{"status": 400, "message": "Transaction cannot be its own parent", "timestamp": "2024-01-15T10:30:00"}
```

### Error: Missing Transaction

```bash
curl http://localhost:8080/transactions/sum/999
```

**Response (404 Not Found):**
```json
{"status": 404, "message": "Transaction with id 999 not found", "timestamp": "2024-01-15T10:30:00"}
```

## How to Run Locally

### Prerequisites
- Java 21 installed
- Maven 3.6+ installed

### Steps

1. **Clone and navigate to the project:**
   ```bash
   cd mendel-transaction-service
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **The service will start on http://localhost:8080**

### Run Tests

```bash
mvn test
```

## How to Run with Docker

### Prerequisites
- Docker installed
- Docker Compose installed

### Steps

1. **Build and start the container:**
   ```bash
   docker compose up --build
   ```

2. **Verify the service is running:**
   ```bash
   curl http://localhost:8080/health
   ```

3. **Stop the service:**
   ```bash
   docker compose down
   ```

## Design Decisions

### 1. In-Memory Storage

**Why:** The challenge specification explicitly requires "no SQL" and "in-memory repository."

**Benefits:**
- Zero latency data access
- No database setup required
- Simple deployment

**Trade-offs:**
- Data is lost when the service restarts
- Not suitable for production with persistent data requirements

### 2. Parent-Child Map Structure

**Why:** Efficient traversal from parent to children is essential for transitive sum calculation.

**Structure:**
```java
ConcurrentHashMap<Long, List<Long>> parentToChildren  // parentId -> List<childIds>
```

**Benefits:**
- O(1) lookup for children of any transaction
- Efficient for deep hierarchies
- Easy to maintain when transactions are updated

### 3. ConcurrentHashMap

**Why:** The service may handle concurrent requests in a production environment.

**Benefits:**
- Concurrent reads and writes are safe without external synchronization
- High performance under concurrent access
- Repository operations are atomic at the map level

**Note:** While the maps are thread-safe, the List values stored within are mutable. The repository uses proper synchronization to maintain consistency.

### 4. Transitive Sum Calculation

**Why:** The challenge requires calculating the sum of a transaction and all its descendants.

**Algorithm:** Depth-first recursive traversal

**Benefits:**
- Simple and clear logic
- Visits each descendant exactly once
- Handles arbitrarily deep hierarchies

**Limitation:** For extremely deep hierarchies, could hit stack overflow. Production systems might use iterative approaches.

### 5. Cycle Prevention

**Why:** Parent-child relationships must not create cycles (A->B->C->A). Cycles would cause infinite recursion in sum calculation and corrupt data integrity.

**Implementation:** Before setting a parent, the service traces the ancestor chain to verify the new parent is not a descendant of the transaction being updated.

**Algorithm:**
```
wouldCreateCycle(transactionId, parentId):
    current = parentId
    while current != null:
        if current == transactionId:
            return true
        current = getParent(current)
    return false
```

## Project Structure

```
mendel-transaction-service/
|-- src/
|   |-- main/
|   |   |-- java/com/challenge/mendel/
|   |   |   |-- MendelTransactionServiceApplication.java
|   |   |   |-- controller/
|   |   |   |   |-- TransactionController.java
|   |   |   |   |-- HealthController.java
|   |   |   |-- service/
|   |   |   |   |-- TransactionService.java
|   |   |   |   |-- TransactionServiceImpl.java
|   |   |   |-- repository/
|   |   |   |   |-- TransactionRepository.java
|   |   |   |   |-- InMemoryTransactionRepository.java
|   |   |   |-- domain/
|   |   |   |   |-- Transaction.java
|   |   |   |-- dto/
|   |   |   |   |-- UpdateTransactionRequest.java
|   |   |   |   |-- TransactionResponse.java
|   |   |   |   |-- SumTransactionResponse.java
|   |   |   |   |-- ErrorResponse.java
|   |   |   |-- exception/
|   |   |   |   |-- GlobalExceptionHandler.java
|   |   |   |   |-- TransactionNotFoundException.java
|   |   |   |   |-- ParentTransactionNotFoundException.java
|   |   |   |   |-- InvalidParentTransactionException.java
|   |   |   |   |-- InvalidTransactionRequestException.java
|   |   |   |   |-- ValidationException.java
|   |   |-- resources/
|   |-- test/
|       |-- java/com/challenge/mendel/
|       |   |-- controller/
|       |   |   |-- TransactionControllerTest.java
|       |   |   |-- TransactionControllerIntegrationTest.java
|       |   |-- service/
|       |   |   |-- TransactionServiceImplTest.java
|       |   |-- repository/
|       |   |   |-- InMemoryTransactionRepositoryTest.java
|-- Dockerfile
|-- docker-compose.yml
|-- pom.xml
|-- README.md
```

## License

This project is part of a coding challenge.