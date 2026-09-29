# Persistence Adapters (MongoDB) — NexusMarket

## Introduction

The persistence adapters implement the domain output ports on top of MongoDB with Spring Data MongoDB. They translate between Domain Models and MongoDB documents, and they are the only place in the application that knows how business data is stored.

The domain is never changed to accommodate persistence: documents, mappers and Spring Data repositories live in `adapters/out/persistence/mongodb`, and the domain only sees its own output ports (see [Software Architecture](../Software%20Architecture/Software%20Architecture.md) and [Domain Services](../Domain/Domain%20Services.md)).

```text
adapters/out/persistence/mongodb/
├── documents/     @Document classes, one per aggregate root (+ audit log)
├── mappers/       Domain Model <-> Document conversion
├── repositories/  Spring Data MongoRepository<XxxDocument, String> interfaces
└── adapters/      One class per output port, delegating to repository + mapper
```

---

# Design Conventions

* **One document per aggregate root.** Each aggregate root has its own collection. Child objects without an independent lifecycle are embedded; everything else is referenced by ID (see [Collections](#collections)).
* **The domain identifier is the document `_id`.** Every aggregate root receives its identifier from the use case `Command` (e.g. `cartId`, `orderId`, `identifier`), so documents never rely on MongoDB-generated IDs. The only exceptions are the append-only collections (`inventory_movements`, `audit_logs`), whose domain objects have no identifier; there `_id` is a MongoDB-generated `ObjectId`.
* **References are stored as plain `String` IDs** named `<relation>Id` (`sellerId`, `buyerId`, `productId`, `warehouseId`, `ownerId`). DBRefs and `@DocumentReference` are not used: resolving references is the adapter's job, so it stays explicit and can be batched.
* **Value Objects are stored as their `code`** and rebuilt with `fromCode(...)`, never with new instances, so the domain can compare them against its constants (`status == OrderStatus.PAID`). A stored code the catalog does not know is corrupt data and makes the mapper fail with `IllegalStateException` naming the code, instead of rebuilding the entity with a `null` value. Primitive enums (`VariantAttributeType`, `NotificationChannel`) are stored by `name()`.
* **Money is stored as `Decimal128`** (`@Field(targetType = FieldType.DECIMAL128)`), never as `double` or as a string, so prices keep their exact scale and can be compared/aggregated in queries.
* **Dates** (`LocalDateTime`) are stored as BSON dates by Spring Data's default conversion.
* **Inverse lists are not stored.** `Buyer.carts` / `orders` and `Seller.warehouses` / `products` are the inverse side of `ShoppingCart.buyerId`, `Order.buyerId`, `Warehouse.ownerId` and `Product.sellerId`. They are rebuilt empty; loading them is a query on the other collection.
* **Documents are persistence types only.** They are never returned by an adapter nor exposed through the API (architectural constraints 4 and 5).

---

# Collections

| Collection            | Document                    | `_id`                           | Embedded                            | References (by ID)                                   |
| --------------------- | --------------------------- | ------------------------------- | ----------------------------------- | ---------------------------------------------------- |
| `users`               | `UserDocument`              | `User.identifier`               | —                                   | —                                                    |
| `warehouses`          | `WarehouseDocument`         | `Warehouse.identifier`          | —                                   | `ownerId` → `users` (only when owner type is `SELLER`) |
| `products`            | `ProductDocument`           | `Product.identifier`            | `variants` (`ProductVariant` value objects) | `sellerId` → `users`                          |
| `inventory_items`     | `InventoryItemDocument`     | `"<productId>:<warehouseId>"`   | —                                   | `productId` → `products`, `warehouseId` → `warehouses` |
| `inventory_movements` | `InventoryMovementDocument` | generated `ObjectId`            | —                                   | `productId` → `products`, `warehouseId` → `warehouses` |
| `shopping_carts`      | `ShoppingCartDocument`      | `ShoppingCart.identifier`       | `lines` (`CartLine`)                | `buyerId` → `users`; each line `productId` → `products` |
| `orders`              | `OrderDocument`             | `Order.identifier`              | `lines` (`OrderLine`)               | `buyerId` → `users`; each line `productId` → `products`, `warehouseId` → `warehouses` |
| `shipments`           | `ShipmentDocument`          | `Shipment.identifier`           | —                                   | `orderId` → `orders` (unique)                        |
| `invoices`            | `InvoiceDocument`           | `Invoice.identifier` (`INV-<orderId>`) | —                            | `orderId` → `orders` (unique), `buyerId` → `users`   |
| `return_requests`     | `ReturnRequestDocument`     | `ReturnRequest.identifier`      | —                                   | `orderId` → `orders` (unique), `buyerId` → `users`, `decidedById` → `users` (once decided) |
| `refunds`             | `RefundDocument`            | `Refund.identifier` (`REF-<returnRequestId>`) | —                     | `returnRequestId` → `return_requests` (unique)       |
| `audit_logs`          | `AuditLogDocument`          | generated `ObjectId`            | —                                   | `userId` → `users` (not resolved)                    |

The embed-vs-reference decisions are exactly those of the [Persistence Strategy](../Software%20Architecture/Software%20Architecture.md#persistence-strategy-embedding-vs-referencing): `Order` embeds `OrderLine`, `ShoppingCart` embeds `CartLine`, and every other relationship is a reference by ID. `ProductVariant` is embedded inside `products` because it is a value object with no identity of its own — it is part of the product's state, not a relationship.

`Shipment`, `Invoice`, `ReturnRequest` and `Refund` each get their own collection and reference their parent by ID; none of them is embedded. They have their own identity and lifecycle (a shipment moves through four steps, a return request is decided, a refund is processed), they are loaded and updated on their own, and embedding them would make `orders` (or `return_requests`) change shape as the fulfilment and return processes advance. The parent does not reference them back: `Order` has no `shipmentId` or `invoiceId`, and they are found through their `orderId` / `returnRequestId`. Because each parent has at most one of each, those reference fields carry a unique index.

## Document Shapes

### `users`

```json
{
  "_id": "1020304050",
  "userType": "BUYER",
  "fullName": "Ana Gómez",
  "email": "ana@example.com",
  "status": "ACTIVE",
  "passwordHash": "$2a$10$...",

  "primaryAddress": "Cra 1 # 2-3",          // BUYER only
  "additionalAddresses": ["..."],           // BUYER only
  "commercialStatus": "ENABLED",            // BUYER only
  "sellerStatus": "ACTIVE"                  // SELLER only
}
```

Indexes: unique on `email`. See [User Hierarchy](#user-hierarchy).

### `warehouses`

```json
{ "_id": "WH-01", "name": "Bodega Norte", "address": "...", "ownerType": "SELLER", "ownerId": "900123456", "active": true }
```

`ownerId` is absent for `MARKETPLACE` warehouses.

### `products`

```json
{
  "_id": "P-001", "name": "Camiseta", "productType": "PHYSICAL", "status": "PUBLISHED",
  "sellerId": "900123456", "price": NumberDecimal("49900.00"),
  "variants": [ { "attribute": "COLOR", "value": "red" }, { "attribute": "SIZE", "value": "M" } ]
}
```

Indexes: `sellerId` (backs `ProductRepositoryPort.findBySellerId`) and `status` (backs `ProductRepositoryPort.findAllPublished`).

### `inventory_items`

```json
{ "_id": "P-001:WH-01", "productId": "P-001", "warehouseId": "WH-01",
  "availableQuantity": 40, "reservedQuantity": 5, "damagedQuantity": 1 }
```

`InventoryItem` has no identifier in the domain: it is identified by `product` + `warehouse`. The `_id` is derived deterministically from both IDs, so `save` is always an upsert of the same record and there can never be two records for the same pair. A unique compound index on `(productId, warehouseId)` enforces the same rule at database level, and `productId` alone backs `findAllByProductId`.

### `inventory_movements`

```json
{ "_id": ObjectId("..."), "productId": "P-001", "warehouseId": "WH-01",
  "type": "RESERVATION", "quantity": 2, "occurredAt": ISODate("2026-09-29T10:15:00Z") }
```

Append-only: the adapter only inserts. Indexed by `(productId, warehouseId, occurredAt)`; `findAllByProductIdAndWarehouseId` returns the history in chronological order.

### `shopping_carts`

```json
{ "_id": "C-77", "buyerId": "1020304050",
  "lines": [ { "productId": "P-001", "quantity": 2 } ] }
```

Indexes: `buyerId` (backs `CartRepositoryPort.findByBuyerId`). The cart is deleted by `ConfirmOrderService` once its order is saved.

### `orders`

```json
{
  "_id": "O-500", "buyerId": "1020304050", "status": "PENDING_PAYMENT",
  "createdAt": ISODate("2026-09-29T10:15:00Z"),
  "lines": [
    { "productId": "P-001", "productName": "Camiseta", "quantity": 2,
      "unitPrice": NumberDecimal("49900.00"), "warehouseId": "WH-01" }
  ]
}
```

Indexes: `buyerId` (backs `OrderRepositoryPort.findAllByBuyerId`, which returns the most recent first). `productName` and `unitPrice` are the frozen snapshot taken at confirmation time; they are never refreshed from `products`.

### `shipments`

```json
{ "_id": "SH-01", "orderId": "O-500", "status": "DISPATCHED",
  "createdAt": ISODate("2026-09-30T08:00:00Z"), "packedAt": ISODate("2026-09-30T09:00:00Z"),
  "dispatchedAt": ISODate("2026-09-30T11:00:00Z") }
```

Step timestamps (`packedAt`, `dispatchedAt`, `deliveredAt`) are absent until the step happens. Indexes: unique on `orderId` (backs `ShipmentRepositoryPort.findByOrderId`; an order has at most one shipment).

### `invoices`

```json
{ "_id": "INV-O-500", "orderId": "O-500", "buyerId": "1020304050",
  "totalAmount": NumberDecimal("99900.50"), "issuedAt": ISODate("2026-09-29T12:00:00Z") }
```

Written once, when the order becomes `PAID`, and never updated. Indexes: unique on `orderId` (backs `InvoiceRepositoryPort.findByOrderId`), and `buyerId`.

### `return_requests`

```json
{ "_id": "RR-01", "orderId": "O-500", "buyerId": "1020304050", "reason": "Wrong size",
  "status": "APPROVED", "requestedAt": ISODate("2026-10-02T10:00:00Z"),
  "decidedAt": ISODate("2026-10-02T15:00:00Z"), "decidedById": "80011223" }
```

`decidedAt` and `decidedById` are absent while the request is `REQUESTED`. `decidedById` may point to a user of any role, so it is resolved as a plain `User`, not as a specific subclass. Indexes: unique on `orderId` (backs `ReturnRequestRepositoryPort.findByOrderId`), and `buyerId`.

### `refunds`

```json
{ "_id": "REF-RR-01", "returnRequestId": "RR-01", "amount": NumberDecimal("99900.50"),
  "status": "PENDING", "createdAt": ISODate("2026-10-02T15:00:00Z") }
```

`processedAt` is absent until the refund is processed. Indexes: unique on `returnRequestId` (backs `RefundRepositoryPort.findByReturnRequestId`).

---

# User Hierarchy

MongoDB has no table inheritance, and the domain `User` is an abstract class with five specializations (`Buyer`, `Seller`, `LogisticsOperator`, `Administrator`, `Supervisor`). All of them are stored in the single `users` collection as one `UserDocument` type:

* **Discriminator — `userType`.** Holds the `SystemRole` code of the user (`BUYER`, `SELLER`, `LOGISTICS_OPERATOR`, `ADMINISTRATOR`, `SUPERVISOR`). In the domain the role is fixed by the subclass (each constructor passes its own `SystemRole`), so the role and the subtype are the same piece of information; storing it once as `userType` avoids two fields that could disagree. `UserMapper.toDomain` switches on `userType` to instantiate the right subclass, and the rebuilt entity's `role` comes from that constructor.
* **Shared fields** (`fullName`, `email`, `status`, `passwordHash`) are present on every document.
* **Specialization-only fields** are optional on the document and simply absent (not written as `null`) on users of other types:

  | Field                 | Present for |
  | --------------------- | ----------- |
  | `primaryAddress`      | `BUYER`     |
  | `additionalAddresses` | `BUYER`     |
  | `commercialStatus`    | `BUYER`     |
  | `sellerStatus`        | `SELLER`    |

* **Why one collection.** `UserRepositoryPort` looks users up by `identifier` and `email` regardless of their role (`AuthenticateUserService`, duplicate checks during registration), and both must be unique across all roles. One collection gives one `_id` and one unique `email` index that cover every role; one collection per role would need cross-collection uniqueness checks that MongoDB cannot enforce.
* **Unknown `userType`.** A document whose `userType` is not a known role cannot be rebuilt and makes the mapper fail with `IllegalStateException`; it indicates corrupt data, not a business error.

---

# Reference Resolution

The domain works with object graphs (`ShoppingCart.getBuyer().getCommercialStatus()`, `CartLine.getProduct().getPrice()`, `InventoryItem.getWarehouse().isActive()`), while documents only store IDs. Services rely on those graphs being complete — e.g. `ConfirmOrderService` checks the buyer's statuses, each product's status, name and price, and `ReserveInventoryService` checks each warehouse is active — so adapters return **fully hydrated** entities.

* **Mappers are pure and static.** `toDocument(entity)` extracts the IDs of referenced entities. `toDomain(document, ...)` receives the referenced entities it needs, already resolved (e.g. `ProductMapper.toDomain(ProductDocument, Seller)`, `OrderMapper.toDomain(OrderDocument, Buyer, Map<String, Product>, Map<String, Warehouse>)`). Mappers never query the database.
* **Adapters resolve references** before calling the mapper, through a shared `MongoReferenceResolver` component that loads users, products, warehouses, orders and return requests by ID (products with their seller, warehouses with their owner, orders with their buyer and the products and warehouses of their lines, return requests with their order, buyer and deciding user). Every adapter that needs an `Order` — `orders` itself, `shipments`, `invoices`, `return_requests` — builds it through the same resolver, so an order is hydrated the same way everywhere. Collections of references are loaded with a single `findAllById` per collection, so hydrating an order with *n* lines costs a fixed number of queries, not *n*. Lists (`findAllByBuyerId`, `findByBuyerId`, `findBySellerId`, `findAllPublished`) resolve the references of all their documents together, so a list of any size also costs a fixed number of queries.
* **Hydration depth.** Every reference an aggregate holds is resolved one level, plus the references of `Product` (→ `Seller`) and `Warehouse` (→ owner `Seller`). An `Order` referenced by a shipment, invoice or return request is always fully hydrated, and so is the `ReturnRequest` referenced by a refund. Inverse lists (`Seller.products`, `Buyer.orders`, …) are not loaded.
* **Dangling references** (an ID pointing to a document that no longer exists) make the resolver fail with `IllegalStateException`. No use case deletes users, products or warehouses, so this can only happen through data corruption.
* **`save` returns the same instance it received.** Identifiers come from the domain and references do not change when saving, so re-reading the document would return an equivalent object at the cost of re-resolving every reference.

---

# Repositories

Each document has a Spring Data interface `<Aggregate>MongoRepository extends MongoRepository<XxxDocument, String>` (`UserMongoRepository`, `ProductMongoRepository`, …, `AuditLogMongoRepository`). The `Mongo` infix keeps them apart from the domain's `<Name>RepositoryPort` interfaces. They only declare the derived queries the ports need (`findByEmail`, `existsByEmail`, `findBySellerId`, `findByStatus`, `findAllByProductId`, `findAllByProductIdAndWarehouseIdOrderByOccurredAtAsc`, `findAllByBuyerIdOrderByCreatedAtDesc` on orders, `findAllByBuyerId` on carts, `findByOrderId` on shipments, invoices and return requests, `findByReturnRequestId` on refunds); they are package-internal plumbing and are never injected outside `adapters/out/persistence/mongodb`.

Spring Data MongoDB comes from `spring-boot-starter-data-mongodb`. In Spring Boot 4, `spring-boot-starter-mongodb` only provides the MongoDB driver, without `@Document` or `MongoRepository`.

---

# Adapters

| Output port                        | Adapter                                | Collection(s)         |
| ---------------------------------- | -------------------------------------- | --------------------- |
| `UserRepositoryPort`               | `MongoUserRepositoryAdapter`           | `users`               |
| `WarehouseRepositoryPort`          | `MongoWarehouseRepositoryAdapter`      | `warehouses`          |
| `ProductRepositoryPort`            | `MongoProductRepositoryAdapter`        | `products`            |
| `InventoryRepositoryPort`          | `MongoInventoryRepositoryAdapter`      | `inventory_items`     |
| `InventoryMovementRepositoryPort`  | `MongoInventoryMovementRepositoryAdapter` | `inventory_movements` |
| `CartRepositoryPort`               | `MongoCartRepositoryAdapter`           | `shopping_carts`      |
| `OrderRepositoryPort`              | `MongoOrderRepositoryAdapter`          | `orders`              |
| `ShipmentRepositoryPort`           | `MongoShipmentRepositoryAdapter`       | `shipments`           |
| `InvoiceRepositoryPort`            | `MongoInvoiceRepositoryAdapter`        | `invoices`            |
| `ReturnRequestRepositoryPort`      | `MongoReturnRequestRepositoryAdapter`  | `return_requests`     |
| `RefundRepositoryPort`             | `MongoRefundRepositoryAdapter`         | `refunds`             |
| `AuditLogPort`                     | `MongoAuditLogAdapter`                 | `audit_logs`          |
| `NotificationPort`                 | `LoggingNotificationAdapter`           | — (log only)          |

Persistence adapters are annotated `@Repository`, so Spring translates MongoDB driver exceptions into its `DataAccessException` hierarchy. They are picked up by component scanning; the domain services that consume them are wired in `infrastructure/config` (next phase).

## Out of Scope: `PasswordHasherPort`

`PasswordHasherPort` is **not** a persistence port: it hashes and verifies credentials and stores nothing. Its adapter belongs to `infrastructure/security` (BCrypt through a Spring Security `PasswordEncoder`). The persistence layer only stores the resulting `User.passwordHash` as an opaque string and never knows the algorithm.

## `NotificationPort` (placeholder)

There is no real notification channel yet ([Domain Services](../Domain/Domain%20Services.md)). `LoggingNotificationAdapter` implements `NotificationPort` by writing the notification to the application log, so services that notify users can be wired and exercised. It lives next to the persistence adapters for now and should move to its own `adapters/out/notification` package when a real channel (email, SMS, push) is implemented.

---

# Cross-Cutting: Audit Log

`AuditLogPort` is also backed by MongoDB, but it is documented apart from the aggregates because it is **transversal, not domain data**: it does not persist any Domain Model, no use case reads it, and it is written for every authenticated request by the security layer through `AuditOperationService`.

* **Collection:** `audit_logs`, one document per recorded operation.

  ```json
  { "_id": ObjectId("..."), "userId": "1020304050", "operation": "POST /orders", "occurredAt": ISODate("...") }
  ```

* **Append-only.** The port only exposes `record(userId, operation, occurredAt)`; the adapter only inserts, and entries are never updated or deleted by the application.
* **No domain entity.** Because the port receives plain values, `AuditLogMapper` only has `toDocument(userId, operation, occurredAt)`. A `toDomain` would have no domain type to return; it will be added if an audit query port is ever introduced.
* **`userId` is not resolved** against `users`: the log must remain valid and cheap to write even if the user later changes, and it is never hydrated.
* **Indexes:** `(userId, occurredAt)` for per-user traceability queries.

---

# Known Gaps

* **Index creation is not enabled yet.** Indexes are declared on the documents (`@Indexed`, `@CompoundIndex`), but Spring Data MongoDB does not create them automatically unless `spring.data.mongodb.auto-index-creation=true` or they are created explicitly in `infrastructure/database`. Until then the unique `email` index is not enforced by the database (the services still check duplicates), and neither are the unique `orderId` / `returnRequestId` indexes of `shipments`, `invoices`, `return_requests` and `refunds` (the services check them with `findByOrderId` before creating, which two concurrent requests can both pass).
* **No multi-document transactions.** `ConfirmOrderService` reserves stock in several `inventory_items`, saves the order and deletes the cart as separate writes. The same applies to `AdvanceOrderStatusService` (order, then invoice), `DispatchShipmentService` (stock of every line, order, shipment) and `ApproveReturnService` (stock of every line, return request, refund). MongoDB transactions require a replica set and a `MongoTransactionManager`, to be decided in `infrastructure/`.
* **No optimistic locking on stock.** Two concurrent reservations on the same `InventoryItem` can overwrite each other (last write wins). Adding `@Version` needs the version to travel with the domain object, which the current domain model does not carry.
* **`_class` type hints.** Spring Data writes a `_class` field on every document. It is harmless (the `userType` discriminator does not depend on it) and can be disabled with a custom `MappingMongoConverter` in `infrastructure/database`.
