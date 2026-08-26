# Software Architecture — NexusMarket

## Overview

NexusMarket follows a **Hexagonal Architecture (Ports and Adapters)** combined with **Domain-Driven Design (DDD)** principles.

The goal of this architecture is to keep the marketplace business rules independent from frameworks, databases, communication protocols, and infrastructure concerns, so the domain can evolve and be tested without depending on any specific technology.

---

# Architectural Principles

- Domain-first design.
- Separation of concerns.
- Dependency inversion.
- Technology independence.
- High cohesion, low coupling.
- Explicit boundaries between layers.

The domain contains all business rules and never depends on external technologies.

---

# Architecture Layers

```
Application
│
├── Adapters
│
├── Domain
│
└── Infrastructure
```

---

# Package Structure

```text
src/
└── main/
    └── java/
        └── application/
            │
            ├── App.java
            │
            ├── adapters/
            │   │
            │   ├── in/
            │   │   └── rest/
            │   │       ├── controllers/
            │   │       ├── requests/
            │   │       ├── responses/
            │   │       └── mappers/
            │   │
            │   └── out/
            │       └── persistence/
            │           └── mysql/
            │               ├── entities/
            │               ├── repositories/
            │               ├── adapters/
            │               └── mappers/
            │
            ├── domain/
            │   ├── models/
            │   ├── valueobjects/
            │   ├── enums/
            │   ├── services/
            │   ├── exceptions/
            │   └── ports/
            │       ├── in/
            │       └── out/
            │
            └── infrastructure/
                ├── config/
                ├── database/
                └── security/
```

---

# Layer Responsibilities

## Application

Root package. Contains the entry point and holds all architectural components together.

## App.java

- Initializes the application.
- Loads infrastructure.
- Configures dependency injection.
- Starts the REST server.

---

# Adapters

Adapters translate external requests into domain operations, and domain results into technology-specific representations. The domain never talks to external systems directly.

## Input Adapters — `adapters/in/rest`

- Receive HTTP requests.
- Validate incoming data.
- Convert Request DTOs into Domain Models.
- Execute application use cases (input ports).
- Convert domain results into Response DTOs.

### Controllers
Expose REST endpoints and delegate execution to the domain. Controllers must never contain business rules.

### Requests
Represent incoming HTTP payloads. Perform structural/input validation only — never business validation.

### Responses
Represent outgoing HTTP payloads. Hide internal domain implementation and standardize API responses.

### Mappers
Convert Request DTO ↔ Domain Model and Domain Model ↔ Response DTO, so the domain never depends on transport objects.

## Output Adapters — `adapters/out/persistence/mysql`

Responsible for relational persistence of all business data (users, sellers, buyers, warehouses, products, inventory, carts, orders).

- **Entities:** represent relational database tables.
- **Repositories:** implement raw persistence operations (Spring Data JPA repositories).
- **Mappers:** convert Domain Models into persistence entities and back.
- **Adapters:** implement the Domain Output Ports, isolating JPA-specific details from the domain.

---

# Domain

The core of the application. Contains all business rules and must remain independent from any external technology (Spring, JPA, HTTP, REST, JSON, SQL).

## Models
Business entities: `User`, `Buyer`, `Seller`, `LogisticsOperator`, `Administrator`, `Supervisor`, `Warehouse`, `Product`, `InventoryItem`, `ShoppingCart`, `Order`.

## Value Objects
Immutable business concepts compared by value: `SystemRole`, `UserStatus`, `CustomerStatus`, `SellerStatus`, `ProductType`, `ProductStatus`, `OrderStatus`, `InventoryMovementType`, `WarehouseOwnerType`.

## Enums
Fixed technical values without business metadata: `VariantAttributeType`, `NotificationChannel`.

## Services
Business logic that does not naturally belong to a single entity. Examples:
- `RegisterSellerService`
- `PublishProductService`
- `ReserveInventoryService`
- `ConfirmOrderService`
- `AdvanceOrderStatusService`

## Ports

Ports define the communication contracts owned by the domain.

### Input Ports (use cases)
- `RegisterBuyerUseCase`
- `RegisterSellerUseCase`
- `PublishProductUseCase`
- `AddCartLineUseCase`
- `ConfirmOrderUseCase`
- `AdvanceOrderStatusUseCase`

### Output Ports (dependencies)
- `UserRepositoryPort`
- `ProductRepositoryPort`
- `InventoryRepositoryPort`
- `OrderRepositoryPort`
- `WarehouseRepositoryPort`
- `NotificationPort`

## Exceptions
Business exceptions belong exclusively to the domain. Examples: `InsufficientStockException`, `InvalidOrderStatusTransitionException`, `SellerNotAuthorizedException`.

---

# Infrastructure

Technical configuration required by the application. No business logic.

## Config
REST configuration, serialization, environment configuration.

## Database
MySQL connection and initialization configuration, connection pooling.

## Security
Authentication and authorization configuration (e.g. JWT configuration, password encoding, authentication filters).

---

# Dependency Flow

```
REST Controller
        │
        ▼
Input Port
        │
        ▼
Domain Service
        │
        ▼
Output Port
        │
        ▼
MySQL Persistence Adapter
        │
        ▼
Database
```

The domain never depends on adapters or infrastructure; all dependencies point inward, toward the domain.

---

# Architectural Constraints

1. Business logic belongs exclusively to the Domain layer.
2. Controllers must not contain business rules.
3. DTOs must never enter the Domain layer.
4. Persistence entities must never be exposed through the API.
5. Communication between technologies and the Domain occurs only through Ports.
6. Adapters implement Ports but never define business rules.
7. Infrastructure depends on the Domain, never the opposite.
8. Every dependency must point toward the Domain.
9. Business entities must remain framework-independent.
10. The Domain must be fully testable without requiring infrastructure components.