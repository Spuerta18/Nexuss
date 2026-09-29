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
        └── nexussMarket/
            │
            ├── Application.java
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
            │           └── mongodb/
            │               ├── documents/
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

## nexussMarket

Root package. Contains the entry point and holds all architectural components together.

## Application.java

- Spring Boot entry point (`@SpringBootApplication`).
- Initializes the application and starts the REST server.
- Dependency injection of the domain is **not** configured here: domain services are plain classes (no Spring annotations), so they are registered as beans in `infrastructure/config` (see [Infrastructure](#infrastructure)).

---

# Adapters

Adapters translate external requests into domain operations, and domain results into technology-specific representations. The domain never talks to external systems directly.

## Input Adapters — `adapters/in/rest`

- Receive HTTP requests.
- Validate incoming data.
- Convert Request DTOs into the use case `Command` records defined by each input port.
- Execute application use cases (input ports).
- Convert domain results into Response DTOs.

### Controllers
Expose REST endpoints and delegate execution to the domain. Controllers must never contain business rules.

### Requests
Represent incoming HTTP payloads. Perform structural/input validation only — never business validation.

### Responses
Represent outgoing HTTP payloads. Hide internal domain implementation and standardize API responses.

### Mappers
Convert Request DTO → use case `Command` and Domain Model → Response DTO, so the domain never depends on transport objects.

## Output Adapters — `adapters/out/persistence/mongodb`

Responsible for document-based persistence of all business data (users, sellers, buyers, warehouses, products, inventory, inventory movements, carts, orders, audit log).

- **Documents:** represent MongoDB collections (classes annotated with `@Document`), following the embedding/referencing strategy defined in the [Persistence Strategy](#persistence-strategy-embedding-vs-referencing) section below.
- **Repositories:** implement raw persistence operations (Spring Data MongoDB repositories, extending `MongoRepository`).
- **Mappers:** convert Domain Models into persistence documents and back. Value Objects are rebuilt with `fromCode(...)`, never with new instances, so the domain can compare them against its constants.
- **Adapters:** implement the Domain Output Ports, isolating MongoDB-specific details from the domain.

---

# Domain

The core of the application. Contains all business rules and must remain independent from any external technology (Spring, JPA, HTTP, REST, JSON, SQL).

## Models
Business entities: `User`, `Buyer`, `Seller`, `LogisticsOperator`, `Administrator`, `Supervisor`, `Warehouse`, `Product`, `InventoryItem`, `InventoryMovement`, `ShoppingCart`, `CartLine`, `Order`, `OrderLine`, `Shipment`, `Invoice`, `ReturnRequest`, `Refund`.

Entities protect their own invariants: e.g. `Order` only changes status through `advanceTo` / `cancel`, and `InventoryItem` only changes stock through `receive`, `reserve`, `releaseReservation`, `confirmOutbound`, `adjust`, and `markDamaged`.

## Value Objects
Immutable business concepts compared by value: `SystemRole`, `UserStatus`, `CustomerStatus`, `SellerStatus`, `ProductType`, `ProductStatus`, `OrderStatus`, `InventoryMovementType`, `WarehouseOwnerType`, `ShipmentStatus`, `ReturnStatus`, `RefundStatus`, `ProductVariant`.

## Enums
Fixed technical values without business metadata: `VariantAttributeType`, `NotificationChannel`.

## Services
Business logic that does not naturally belong to a single entity. Each input port has one service that implements it (`<Name>UseCase` → `<Name>Service`). In addition, three internal services are not exposed as use cases:
- `ReserveInventoryService` — reserves stock from the first active warehouse with enough available units (used by `ConfirmOrderService`).
- `AuditOperationService` — records which authenticated user performed which operation, through `AuditLogPort`.
- `NotifyUserService` — sends notifications through `NotificationPort`.

When a service needs another use case (e.g. `CancelOrderService` releasing reservations), it depends on the input port interface, not on the concrete service.

## Ports

Ports define the communication contracts owned by the domain.

### Input Ports (use cases)

Each use case is an interface with a nested `Command` record (its input) and a single `execute(Command)` method.

| Area       | Use cases |
| ---------- | --------- |
| Users      | `AuthenticateUserUseCase`, `RegisterBuyerUseCase`, `RegisterSellerUseCase`, `RegisterLogisticsOperatorUseCase`, `RegisterSupervisorUseCase`, `UpdateUserStatusUseCase`, `UpdateSellerStatusUseCase` |
| Warehouses | `RegisterWarehouseUseCase`, `DeactivateWarehouseUseCase` |
| Catalog    | `PublishProductUseCase`, `UpdateProductStatusUseCase`, `UpdateProductVariantsUseCase` |
| Inventory  | `RegisterInventoryInboundUseCase`, `RegisterInventoryReturnUseCase`, `AdjustInventoryUseCase`, `ReportDamagedInventoryUseCase`, `ReleaseInventoryReservationUseCase`, `ConfirmInventoryOutboundUseCase` |
| Carts      | `CreateShoppingCartUseCase`, `AddCartLineUseCase`, `UpdateCartLineQuantityUseCase`, `RemoveCartLineUseCase` |
| Orders     | `ConfirmOrderUseCase`, `AdvanceOrderStatusUseCase`, `CancelOrderUseCase` |

### Output Ports (dependencies)

| Port                               | Implemented by |
| ---------------------------------- | -------------- |
| `UserRepositoryPort`               | MongoDB adapter |
| `WarehouseRepositoryPort`          | MongoDB adapter |
| `ProductRepositoryPort`            | MongoDB adapter |
| `InventoryRepositoryPort`          | MongoDB adapter |
| `InventoryMovementRepositoryPort`  | MongoDB adapter |
| `CartRepositoryPort`               | MongoDB adapter |
| `OrderRepositoryPort`              | MongoDB adapter |
| `AuditLogPort`                     | MongoDB adapter |
| `PasswordHasherPort`               | Security adapter (`infrastructure/security`) |
| `TokenServicePort`                 | Security adapter (`infrastructure/security`, JWT) |
| `NotificationPort`                 | Notification adapter (not yet defined) |

## Exceptions
Business exceptions belong exclusively to the domain: `EntityNotFoundException`, `DuplicateResourceException`, `InsufficientStockException`, `InvalidOrderStatusTransitionException`, `InvalidShipmentStatusTransitionException`, `SellerNotAuthorizedException`, `OperationNotAllowedException`, `BusinessRuleViolationException`, `InvalidCredentialsException`. All of them extend `DomainException`.

---

# Infrastructure

Technical configuration required by the application. No business logic.

## Config
REST configuration, serialization, environment configuration, and **domain wiring**: a configuration class registers every domain service as a Spring bean, injecting the adapters that implement its output ports. This is the only place where the domain meets Spring, and it can only be completed once the output adapters exist.

## Database
MongoDB connection configuration (connection URI, database name), client initialization, and index configuration.

## Security
Authentication and authorization configuration (e.g. JWT configuration, authentication filters). It also contains:
- The adapter implementing `PasswordHasherPort` (e.g. with a Spring Security `PasswordEncoder`). The domain only stores `User.passwordHash` and never knows the algorithm.
- The filter/interceptor that calls `AuditOperationService` for every authenticated request, so every operation is traceable to its `User`.

---

# Persistence Strategy: Embedding vs. Referencing

MongoDB is a document database: it has no JOINs and no foreign key constraints. Every relationship between Domain Models must be explicitly decided as either **embedded** (nested inside the parent document) or **referenced** (stored as an ID, resolved with a separate query).

## Decision Criteria

- **Embed** when the child object has no independent lifecycle, is always read together with its parent, and is not shared across other documents.
- **Reference** when the related object is large, changes independently from the parent, or is shared/reused across many documents.

## Applied Decisions

| Relationship                      | Strategy        | Reason                                                                 |
| ---------------------------------- | ---------------- | -------------------------------------------------------------------------- |
| `Order` → `OrderLine`              | **Embed**        | Order lines have no independent lifecycle and are always read as part of their order. |
| `ShoppingCart` → `CartLine`        | **Embed**        | Cart lines only make sense inside their cart; never queried on their own.  |
| `Order` → `Buyer`                  | **Reference (ID)** | A buyer is a large, independent entity shared across many orders; embedding would duplicate buyer data in every order. |
| `ShoppingCart` → `Buyer`           | **Reference (ID)** | Same reasoning as above. |
| `Product` → `Seller`               | **Reference (ID)** | A seller is shared across many products; embedding would duplicate seller data in every product. |
| `InventoryItem` → `Product`        | **Reference (ID)** | A product is shared across multiple inventory records (one per warehouse); embedding would duplicate product data. |
| `InventoryItem` → `Warehouse`      | **Reference (ID)** | A warehouse holds many inventory records; embedding would duplicate warehouse data. |
| `Warehouse` → `Seller` (owner)     | **Reference (ID)** | A seller may own multiple warehouses; embedding would duplicate seller data. |
| `OrderLine` / `CartLine` → `Product` | **Reference (ID)**, with the snapshot fields `productName` and `unitPrice` (for `OrderLine` only) | The full product must not be duplicated, but an order must preserve the price and name exactly as they were at purchase time, even if the product changes later. |
| `OrderLine` → `Warehouse`          | **Reference (ID)** | Identifies the `InventoryItem` whose reservation must be released or dispatched; the warehouse is shared across many lines. |
| `InventoryMovement` → `Product` / `Warehouse` | **Reference (ID)**, own collection | Movements are an append-only history that grows without bound; embedding them in `InventoryItem` would make that document grow indefinitely. |
| `Buyer` → `carts` / `orders`, `Seller` → `warehouses` / `products` | **Not stored** | These lists are the inverse side of the references above (`ShoppingCart.buyerId`, `Order.buyerId`, `Warehouse.ownerId`, `Product.sellerId`). Storing them too would duplicate every relationship; they are loaded on demand by querying the other collection. |

## User Inheritance

All `User` specializations are stored in a single `users` collection. The `userType` field, holding the role code (`BUYER`, `SELLER`, …), acts as the discriminator: the mapper reads it to rebuild the right subclass. Specialization-only fields (e.g. `primaryAddress`, `sellerStatus`) are simply absent on documents of other roles. A single collection keeps `identifier` and `email` unique across all roles with one unique index each.

## Practical Consequence

When implementing `documents/` in the MongoDB adapter, an `Order` document contains its `OrderLine` list directly (embedded), but each `OrderLine` stores only a `productId` and a `warehouseId` (plus the frozen snapshot of `productName` and `unitPrice`) — never the full `Product` or `Warehouse` document. To resolve a complete `Product`, `Seller`, or `Buyer`, the corresponding repository must be queried separately by ID.

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
MongoDB Persistence Adapter
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