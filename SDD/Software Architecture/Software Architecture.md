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
            │   │       ├── mappers/
            │   │       └── exception/
            │   │
            │   ├── useCases/
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
            │   │   └── authorization/
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
- Its `@SpringBootApplication` component scan covers the whole `nexussMarket` package: Domain Services (`@Service`), use case adapters (`@Service`) and output adapters are found there. Only the authorization services and three internal services are registered explicitly, in `infrastructure/config` (see [Infrastructure](#infrastructure)).

---

# Adapters

Adapters translate external requests into domain operations, and domain results into technology-specific representations. The domain never talks to external systems directly.

## Input Adapters — `adapters/in/rest`

- Receive HTTP requests.
- Validate incoming data.
- Convert Request DTOs into the domain models each port method receives.
- Call the input port of the authenticated user's role, passing that user as the actor.
- Convert domain results into Response DTOs.

### Controllers
Expose REST endpoints and delegate execution to a role input port (`BuyerPort`, `SellerPort`, …). Controllers depend only on role ports, never on use case adapters or Domain Services, and must never contain business rules. No controller exists yet; the package only documents this design.

### Requests
Represent incoming HTTP payloads. Perform structural/input validation only — never business validation.

### Responses
Represent outgoing HTTP payloads. Hide internal domain implementation and standardize API responses.

### Mappers
Convert Request DTO → Domain Model and Domain Model → Response DTO, so the domain never depends on transport objects.

## Use Case Adapters — `adapters/useCases`

One class per role input port (`BuyerUseCaseImpl implements BuyerPort`, …). Each method only delegates to the corresponding Domain Service: no business rules, validations or branching. See [Use Case Adapters](../Adapters/Use-cases-adapters.md).

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
Business logic that does not naturally belong to a single entity. Each operation has one concrete service class, `<Name>Service`, annotated `@Service`, with a single `execute(...)` method that receives the actor and domain models. Services implement no interface; the role input ports reach them through the use case adapters. Besides the operation services there are:
- **Authorization services** (`domain/services/authorization`) — decide whether the actor may perform an operation.
- **Internal services**, used by other services or by the security layer:
  - `ReserveInventoryService` — reserves stock from the first active warehouse with enough available units (used by `ConfirmOrderService`).
  - `ReleaseInventoryReservationService` and `ConfirmInventoryOutboundService` — release or dispatch reserved stock (used by `ConfirmOrderService`, `CancelOrderService` and `DispatchShipmentService`).
  - `AuditOperationService` — records which authenticated user performed which operation, through `AuditLogPort`.
  - `NotifyUserService` — sends notifications through `NotificationPort`.

When a service needs another one (e.g. `CancelOrderService` releasing reservations), it injects the concrete service. See [Domain Services](../Domain/Domain%20Services.md).

## Ports

Ports define the communication contracts owned by the domain.

### Input Ports (one per role)

Each role has one input port listing every operation it can execute. Parameters are domain models, and every port except `PublicAccessPort` receives the authenticated `User` first. See [Input Ports](../Domain/Input-ports.md).

| Port | Role | Implemented by | Operations |
| ---- | ---- | -------------- | ---------- |
| `PublicAccessPort` | No authenticated user | `PublicAccessUseCaseImpl` | `authenticate`, `registerBuyer` |
| `BuyerPort` | `BUYER` | `BuyerUseCaseImpl` | `createShoppingCart`, `addCartLine`, `updateCartLineQuantity`, `removeCartLine`, `consultCart`, `confirmOrder`, `cancelOrder`, `consultOrder`, `listBuyerOrders`, `listPublishedProducts`, `consultProduct`, `requestReturn` |
| `SellerPort` | `SELLER` | `SellerUseCaseImpl` | `publishProduct`, `updateProductStatus`, `updateProductVariants`, `listSellerProducts`, `consultProduct`, `registerWarehouse`, `registerInventoryInbound`, `adjustInventory`, `consultInventory` |
| `AdministratorPort` | `ADMINISTRATOR` | `AdministratorUseCaseImpl` | `registerSeller`, `registerSupervisor`, `registerLogisticsOperator`, `updateSellerStatus`, `updateUserStatus`, `registerMarketplaceWarehouse`, `deactivateWarehouse`, `approveReturn`, `rejectReturn`, `processRefund`, `consultUser` |
| `LogisticsOperatorPort` | `LOGISTICS_OPERATOR` | `LogisticsOperatorUseCaseImpl` | `consultOrder`, `advanceOrderStatus`, `createShipment`, `packShipment`, `dispatchShipment`, `confirmDelivery`, `reportDamagedInventory`, `registerInventoryReturn`, `consultInventory` |
| `SupervisorPort` | `SUPERVISOR` | `SupervisorUseCaseImpl` | `consultOrder`, `consultInventory`, `consultProduct`, `consultUser`, `listPublishedProducts` |

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
| `ShipmentRepositoryPort`           | MongoDB adapter |
| `InvoiceRepositoryPort`            | MongoDB adapter |
| `ReturnRequestRepositoryPort`      | MongoDB adapter |
| `RefundRepositoryPort`             | MongoDB adapter |
| `AuditLogPort`                     | MongoDB adapter |
| `PasswordHasherPort`               | Security adapter (`infrastructure/security`) |
| `TokenServicePort`                 | Security adapter (`infrastructure/security`, JWT) |
| `NotificationPort`                 | `LoggingNotificationAdapter` (logs the message; no real channel yet) |

## Exceptions
Business exceptions belong exclusively to the domain: `EntityNotFoundException`, `DuplicateResourceException`, `InsufficientStockException`, `InvalidOrderStatusTransitionException`, `InvalidShipmentStatusTransitionException`, `SellerNotAuthorizedException`, `OperationNotAllowedException`, `BusinessRuleViolationException`, `InvalidCredentialsException`. All of them extend `DomainException`.

---

# Infrastructure

Technical configuration required by the application. No business logic.

## Config
REST configuration, serialization, environment configuration, and the part of the domain wiring that component scan does not cover. There are no per-use-case `*BeanConfiguration` classes: Domain Services and use case adapters are `@Service` classes found by component scan. Only two configuration classes remain:
- `AuthorizationBeanConfiguration` — registers the six authorization services, which are plain classes.
- `InternalServiceBeanConfiguration` — registers `ReserveInventoryService`, `AuditOperationService` and `NotifyUserService`, which are plain classes as well.

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
Role Input Port
        │
        ▼
UseCaseImpl (adapters/useCases)
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

The domain never depends on adapters or infrastructure; all dependencies point inward, toward the domain. The controller knows only the role input port; the use case adapter implements that port and calls the Domain Service, which reaches persistence only through output ports.

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
11. Controllers depend only on role input ports (`BuyerPort`, `SellerPort`, …), never on use case adapters or Domain Services.
12. Use case adapters (`*UseCaseImpl`) contain no logic: each method only delegates to one Domain Service.