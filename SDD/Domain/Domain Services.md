# Domain Services — NexusMarket

## Introduction

Domain Services contain the business logic of NexusMarket that does not naturally belong to a single entity: they load entities through output ports, coordinate them, enforce cross-entity rules, and persist the result.

Operation services are concrete Spring `@Service` classes that implement no interface. Their dependencies (output ports, and other services when needed) are received through the constructor and found by component scan. The authorization services and three internal services (`ReserveInventoryService`, `AuditOperationService`, `NotifyUserService`) are plain classes registered as beans in `infrastructure/config` (see [Software Architecture](../Software%20Architecture/Software%20Architecture.md)).

There are two kinds of services:

* **Operation services** — one per operation. Each exposes a single `execute(...)` method that receives domain models, with the authenticated `User actor` first when the operation must be authorized. They are reached from the [role input ports](Input-ports.md) through the [use case adapters](../Adapters/Use-cases-adapters.md), which only delegate to them.
* **Internal services** — not reached from any role port; they are injected into other services or into the security layer. The authorization services in `domain/services/authorization/` are internal services, and so are `ReleaseInventoryReservationService` and `ConfirmInventoryOutboundService`.

---

# Design Conventions

* **One operation, one service.** Each operation has one concrete service class with a single `execute(...)` method. When several role ports expose the same operation (e.g. `consultProduct`), they all delegate to the same service, so the rule is written once.
* **Services receive domain models.** A parameter is either a **reference** — a model carrying only the identifier of an existing entity, which the service loads through its output port (`Order orderReference`) — or a **new model** carrying the data of the entity to create (`Product product`). When creating, the service builds the stored entity itself, so the initial status, owner and timestamps are always set by the domain. Plain values (`int quantity`, `OrderStatus targetStatus`, `String password`) are separate parameters.
* **Authorized operations receive the actor.** Operations restricted to some users take the authenticated `User actor` as first parameter. The service authorizes the actor through an authorization service before it changes anything (see [Authorization Services](#authorization-services)). Only `AuthenticateUserService`, `RegisterBuyerService` and the internal services have no actor.
* **Entities protect their own invariants.** Services never change entity state through setters: they call the entity methods (`InventoryItem.receive`, `reserve`, `releaseReservation`, `confirmOutbound`, `adjust`, `markDamaged`; `Order.advanceTo`, `cancel`; `ShoppingCart.addLine`, `updateLineQuantity`, `removeLine`; `Product.changeStatus`, `replaceVariants`; `Warehouse.deactivate`). The rules of a single entity live in that entity (see [Domain Model](Domain%20Model.md)); services add authorization, loading and the rules that need more than one entity (e.g. the warehouse must be active, the seller must be `ACTIVE`).
* **Services inject other services directly.** When a service needs another one, it receives the concrete class (e.g. `CancelOrderService` → `ReleaseInventoryReservationService`). Services never depend on input ports: ports are the entry point from outside, not a way for the domain to call itself.
* **Every stock change records an `InventoryMovement`.** Each inventory service saves the `InventoryItem` and then an `InventoryMovement` through `InventoryMovementRepositoryPort`.
* **Lookups fail with `EntityNotFoundException`.** When an identifier refers to a user of the wrong role (e.g. a `Seller` reference whose identifier belongs to a `Buyer`), the service also throws `EntityNotFoundException`, so the role of other accounts is not revealed.
* **Invalid quantities** are rejected by the entities with `IllegalArgumentException`. `ShoppingCart` rethrows it with the cart and product in the message.
* **Money is a `Money` value.** Prices and totals carry their `Currency`; amounts of different currencies are never added together (see [Money](Domain%20Value%20Objects.md#money)).

---

# Service Catalog

`Role ports` lists the [role input ports](Input-ports.md) that expose the operation. A service's authorization may accept more roles than the ports currently expose (e.g. `ADMINISTRATOR` in the shipment services); the port decides what a role can reach, the service decides what it is allowed to do.

| Area       | Service | Role ports | Output ports / dependencies | Authorization |
| ---------- | ------- | ---------- | ---------------------------- | ------------- |
| Users      | `AuthenticateUserService` | `PublicAccessPort` | `UserRepositoryPort`, `PasswordHasherPort`, `TokenServicePort` | — |
| Users      | `RegisterBuyerService` | `PublicAccessPort` | `UserRepositoryPort`, `PasswordHasherPort` | — |
| Users      | `RegisterSellerService` | `AdministratorPort` | `UserRepositoryPort`, `PasswordHasherPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Users      | `RegisterLogisticsOperatorService` | `AdministratorPort` | `UserRepositoryPort`, `PasswordHasherPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Users      | `RegisterSupervisorService` | `AdministratorPort` | `UserRepositoryPort`, `PasswordHasherPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Users      | `UpdateUserStatusService` | `AdministratorPort` | `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Users      | `UpdateSellerStatusService` | `AdministratorPort` | `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Warehouses | `RegisterWarehouseService` | `AdministratorPort`, `SellerPort` | `WarehouseRepositoryPort`, `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`SELLER`, `ADMINISTRATOR`) + own rule by role |
| Warehouses | `DeactivateWarehouseService` | `AdministratorPort` | `WarehouseRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Catalog    | `PublishProductService` | `SellerPort` | `ProductRepositoryPort`, `UserRepositoryPort` | Own rule: the actor must be a `Seller` (it becomes the product's seller) |
| Catalog    | `UpdateProductStatusService` | `SellerPort` | `ProductRepositoryPort` | `AuthorizeProductOwnershipService` |
| Catalog    | `UpdateProductVariantsService` | `SellerPort` | `ProductRepositoryPort` | `AuthorizeProductOwnershipService` |
| Inventory  | `RegisterInventoryInboundService` | `SellerPort` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort`, `ProductRepositoryPort`, `WarehouseRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Inventory  | `RegisterInventoryReturnService` | `LogisticsOperatorPort` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Inventory  | `AdjustInventoryService` | `SellerPort` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Inventory  | `ReportDamagedInventoryService` | `LogisticsOperatorPort` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Carts      | `CreateShoppingCartService` | `BuyerPort` | `CartRepositoryPort`, `UserRepositoryPort` | Own rule: the actor must be a `Buyer` (it becomes the cart's owner) |
| Carts      | `AddCartLineService` | `BuyerPort` | `CartRepositoryPort`, `ProductRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeCartAccessService` |
| Carts      | `UpdateCartLineQuantityService` | `BuyerPort` | `CartRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeCartAccessService` |
| Carts      | `RemoveCartLineService` | `BuyerPort` | `CartRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeCartAccessService` |
| Orders     | `ConfirmOrderService` | `BuyerPort` | `CartRepositoryPort`, `OrderRepositoryPort`, `ReserveInventoryService`, `ReleaseInventoryReservationService` | `ValidateRoleAuthorizationService` (`BUYER`, `ADMINISTRATOR`), `AuthorizeCartAccessService` |
| Orders     | `AdvanceOrderStatusService` | `LogisticsOperatorPort` | `OrderRepositoryPort`, `InvoiceRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Orders     | `CancelOrderService` | `BuyerPort` | `OrderRepositoryPort`, `ReleaseInventoryReservationService` | `ValidateRoleAuthorizationService` (`BUYER`, `ADMINISTRATOR`), `AuthorizeOrderAccessService` |
| Shipments  | `CreateShipmentService` | `LogisticsOperatorPort` | `ShipmentRepositoryPort`, `OrderRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Shipments  | `PackShipmentService` | `LogisticsOperatorPort` | `ShipmentRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Shipments  | `DispatchShipmentService` | `LogisticsOperatorPort` | `ShipmentRepositoryPort`, `OrderRepositoryPort`, `ConfirmInventoryOutboundService` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Shipments  | `ConfirmDeliveryService` | `LogisticsOperatorPort` | `ShipmentRepositoryPort`, `OrderRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Returns    | `RequestReturnService` | `BuyerPort` | `ReturnRequestRepositoryPort`, `OrderRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeOrderAccessService` |
| Returns    | `ApproveReturnService` | `AdministratorPort` | `ReturnRequestRepositoryPort`, `RefundRepositoryPort`, `RegisterInventoryReturnService` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Returns    | `RejectReturnService` | `AdministratorPort` | `ReturnRequestRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Returns    | `ProcessRefundService` | `AdministratorPort` | `RefundRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Internal   | `ReserveInventoryService` | — | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | — |
| Internal   | `ReleaseInventoryReservationService` | — | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | — (no actor) |
| Internal   | `ConfirmInventoryOutboundService` | — | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | — (no actor) |
| Internal   | `AuditOperationService` | — | `AuditLogPort` | — |
| Internal   | `NotifyUserService` | — | `NotificationPort` | — |
| Authorization | `ValidateUserAuthorizationStatusService` | — | — | — |
| Authorization | `ValidateRoleAuthorizationService` | — | `ValidateUserAuthorizationStatusService` | — |
| Authorization | `AuthorizeOrderAccessService` | — | `ValidateUserAuthorizationStatusService` | — |
| Authorization | `AuthorizeProductOwnershipService` | — | `ValidateUserAuthorizationStatusService` | — |
| Authorization | `AuthorizeWarehouseOperationService` | — | `ValidateUserAuthorizationStatusService` | — |
| Authorization | `AuthorizeCartAccessService` | — | `ValidateUserAuthorizationStatusService` | — |

---

# Queries

Read-only operations. They follow the same conventions as every other service — `execute(User actor, …)` with domain model references — and never modify data. Supervisors, a read-only role, are allowed wherever the rule below says so.

| Area       | Service | Role ports | Output ports / dependencies | Authorization |
| ---------- | ------- | ---------- | ---------------------------- | ------------- |
| Catalog    | `ListPublishedProductsService` | `BuyerPort`, `SupervisorPort` | `ProductRepositoryPort` | `ValidateUserAuthorizationStatusService` (any active user) |
| Catalog    | `ConsultProductService` | `BuyerPort`, `SellerPort`, `SupervisorPort` | `ProductRepositoryPort` | `ValidateUserAuthorizationStatusService` (any active user); if not `PUBLISHED`, also `AuthorizeProductOwnershipService` |
| Catalog    | `ListSellerProductsService` | `SellerPort` | `ProductRepositoryPort` | `ValidateUserAuthorizationStatusService` + own rule: `Administrator`, `Supervisor`, or the `Seller` itself |
| Carts      | `ConsultCartService` | `BuyerPort` | `CartRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeCartAccessService` |
| Orders     | `ConsultOrderService` | `BuyerPort`, `LogisticsOperatorPort`, `SupervisorPort` | `OrderRepositoryPort` | `AuthorizeOrderAccessService` (administrators, supervisors, logistics operators, or the owning buyer) |
| Orders     | `ListBuyerOrdersService` | `BuyerPort` | `OrderRepositoryPort` | `ValidateUserAuthorizationStatusService` + own rule: `Administrator`, `Supervisor`, or the `Buyer` itself |
| Inventory  | `ConsultInventoryService` | `SellerPort`, `LogisticsOperatorPort`, `SupervisorPort` | `ProductRepositoryPort`, `InventoryRepositoryPort` | `Supervisor` / `LogisticsOperator`: `ValidateUserAuthorizationStatusService`; anyone else: `AuthorizeProductOwnershipService` (`Administrator` or owning `Seller`) |
| Users      | `ConsultUserService` | `AdministratorPort`, `SupervisorPort` | `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`, `SUPERVISOR`) |

| Service | Parameters | Returns | Notes |
| ------- | ---------- | ------- | ----- |
| `ListPublishedProductsService` | `actor` | `List<Product>` | Only `PUBLISHED` products. |
| `ConsultProductService` | `actor`, `Product productReference` | `Product` | A `PUBLISHED` product is visible to any active user; a `SUSPENDED` or `DISCONTINUED` one only to an `Administrator` or its owning `Seller`. `EntityNotFoundException` if it does not exist. |
| `ListSellerProductsService` | `actor`, `Seller seller` (reference) | `List<Product>` | Every status. Empty if the seller has no products. |
| `ConsultCartService` | `actor`, `ShoppingCart cartReference` | `ShoppingCart` | The role is checked first, then the cart is loaded and authorized: only its owning buyer can read it. |
| `ConsultOrderService` | `actor`, `Order orderReference` | `Order` | The order is loaded first, then authorized. |
| `ListBuyerOrdersService` | `actor`, `Buyer buyer` (reference) | `List<Order>` | Most recent first. |
| `ConsultInventoryService` | `actor`, `Product productReference` | `List<InventoryItem>` | One item per warehouse holding the product. The product is loaded first, then authorized. |
| `ConsultUserService` | `actor`, `User target` (reference) | `User` | Authorized by role before loading. `target` is the abstract `User`; see [Input Ports](Input-ports.md#abstract-user-parameters). |

The "own rule" of `ListSellerProductsService` and `ListBuyerOrdersService` compares the identifier of the referenced seller or buyer with the actor, not a loaded entity, so no existing authorization service fits; it is a private method of each service until another service needs it.

---

# Users

---

# AuthenticateUserService

## Description

Verifies a user's credentials and returns the authenticated `User` together with an access token issued for it.

## Parameters

`execute(String email, String password)`

| Parameter | Type   | Description |
| --------- | ------ | ----------- |
| email     | String | Email the user registered with. |
| password  | String | Plain password, checked with `PasswordHasherPort.matches`. It is never stored. |

**Returns:** `AuthenticationResult` (`user`, `token`)

## Flow

1. Find the user by `email`.
2. Check the password against `User.passwordHash`.
3. Check that the user's `status` is `ACTIVE`.
4. Issue a token for the user through `TokenServicePort.generateToken` and return it with the user. The domain never knows the token format (JWT, in `infrastructure/security`).

## Business Rules

* Only users whose `status` is `ACTIVE` can authenticate.
* An unknown email and a wrong password produce the same error, so accounts cannot be enumerated.

## Exceptions

| Exception | When |
| --------- | ---- |
| `InvalidCredentialsException` | Unknown email, missing password hash, or wrong password. |
| `OperationNotAllowedException` | The credentials are valid but the user is not `ACTIVE`. |

---

# RegisterBuyerService

## Description

Registers a new `Buyer`. Buyers self-register, so there is no actor.

## Parameters

`execute(Buyer buyer, String password)`

| Parameter | Type   | Description |
| --------- | ------ | ----------- |
| buyer     | Buyer  | New model: `identifier` (national ID or business tax ID), `fullName`, `email`, `primaryAddress`. Its status fields are ignored. |
| password  | String | Plain password, hashed with `PasswordHasherPort.hash`. |

**Returns:** `Buyer`

## Business Rules

* `identifier` and `email` must be unique across all users.
* A new buyer starts with `status = ACTIVE` and `commercialStatus = ENABLED`.

## Exceptions

| Exception | When |
| --------- | ---- |
| `DuplicateResourceException` | The `identifier` or the `email` is already used by another user. |

---

# RegisterSellerService

## Description

Registers a new `Seller` on behalf of an `Administrator`.

## Parameters

`execute(User actor, Seller seller, String password)`

| Parameter | Type   | Description |
| --------- | ------ | ----------- |
| actor     | User   | Administrator performing the registration. |
| seller    | Seller | New model: `identifier` (business tax ID), `fullName` (legal name), `email`. Its status fields are ignored. |
| password  | String | Plain password, hashed with `PasswordHasherPort.hash`. |

**Returns:** `Seller`

## Business Rules

* Sellers cannot self-register: only an active `ADMINISTRATOR` can register them, checked with `ValidateRoleAuthorizationService` before anything else.
* `identifier` and `email` must be unique across all users.
* A new seller starts with `status = ACTIVE` and `sellerStatus = ACTIVE`.

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is missing, not `ACTIVE`, or not an `ADMINISTRATOR`. |
| `DuplicateResourceException` | The `identifier` or the `email` is already used by another user. |

---

# RegisterLogisticsOperatorService / RegisterSupervisorService

## Description

Register a new `LogisticsOperator` or `Supervisor` on behalf of an `Administrator`. Both services follow the same flow.

## Parameters

`execute(User actor, LogisticsOperator logisticsOperator, String password)` / `execute(User actor, Supervisor supervisor, String password)`

| Parameter | Type   | Description |
| --------- | ------ | ----------- |
| actor     | User   | Administrator performing the registration. |
| logisticsOperator / supervisor | LogisticsOperator / Supervisor | New model: `identifier` (national ID), `fullName`, `email`. Its status is ignored. |
| password  | String | Plain password, hashed with `PasswordHasherPort.hash`. |

**Returns:** `LogisticsOperator` / `Supervisor`

## Business Rules

* Only an active `ADMINISTRATOR` can register staff, checked with `ValidateRoleAuthorizationService` before anything else.
* `identifier` and `email` must be unique across all users.
* A new user starts with `status = ACTIVE`.

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `ADMINISTRATOR`. |
| `DuplicateResourceException` | The `identifier` or the `email` is already used by another user. |

---

# UpdateUserStatusService

## Description

Changes the operational `UserStatus` of any user (e.g. blocks an account).

## Parameters

`execute(User actor, User target, UserStatus status)`

| Parameter | Type       | Description |
| --------- | ---------- | ----------- |
| actor     | User       | Administrator performing the change. |
| target    | User       | Reference to the user to update. It is the abstract `User`; see [Input Ports](Input-ports.md#abstract-user-parameters). |
| status    | UserStatus | New status. |

**Returns:** `User`

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `ADMINISTRATOR`. |
| `EntityNotFoundException` | No user exists with the target's identifier. |

---

# UpdateSellerStatusService

## Description

Changes the commercial `SellerStatus` of a seller. It is independent from the `UserStatus`: a seller can be able to log in but not allowed to publish.

## Parameters

`execute(User actor, Seller sellerReference, SellerStatus status)`

| Parameter       | Type         | Description |
| --------------- | ------------ | ----------- |
| actor           | User         | Administrator performing the change. |
| sellerReference | Seller       | Reference to the seller to update. |
| status          | SellerStatus | New seller status. |

**Returns:** `Seller`

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `ADMINISTRATOR`. |
| `EntityNotFoundException` | No user exists with the seller's identifier, or the user is not a `Seller`. |

---

# Warehouses

---

# RegisterWarehouseService

## Description

Registers a Marketplace-owned or Seller-owned warehouse. The kind of warehouse the actor may register depends on its role. It is reached from `AdministratorPort.registerMarketplaceWarehouse` and from `SellerPort.registerWarehouse`.

## Parameters

`execute(User actor, Warehouse warehouse)`

| Parameter | Type      | Description |
| --------- | --------- | ----------- |
| actor     | User      | Administrator or seller performing the registration. |
| warehouse | Warehouse | New model: `identifier`, `name`, `address`, `ownerType` (`MARKETPLACE` or `SELLER`) and, optionally, `owner`. |

**Returns:** `Warehouse`

## Flow

1. Authorize the actor as `SELLER` or `ADMINISTRATOR` with `ValidateRoleAuthorizationService`.
2. If the actor is an `ADMINISTRATOR` and `ownerType = MARKETPLACE`, save the warehouse without owner.
3. If the actor is a `SELLER`, `ownerType = SELLER`, and the warehouse has no owner or the actor as owner, load the actor as `Seller`, set it as `owner`, and save the warehouse.
4. Any other combination is rejected.

## Business Rules

* A Marketplace-owned warehouse can only be registered by an `Administrator`.
* A Seller-owned warehouse can only be registered by its own `Seller`, who is always set as the `owner`: a seller cannot register a warehouse for another seller, and an administrator cannot register Seller-owned warehouses.

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `SELLER` or `ADMINISTRATOR`, or the role does not match the `ownerType` (or the owner is another seller). |
| `EntityNotFoundException` | Seller-owned and the actor's identifier does not belong to a `Seller`. |

---

# DeactivateWarehouseService

## Description

Marks a warehouse as inactive through `Warehouse.deactivate()`. An inactive warehouse cannot receive inbound stock and is skipped when reserving stock.

## Parameters

`execute(User actor, Warehouse warehouseReference)`

| Parameter          | Type      | Description |
| ------------------ | --------- | ----------- |
| actor              | User      | User performing the change, authorized on the loaded warehouse. |
| warehouseReference | Warehouse | Reference to the warehouse to deactivate. |

**Returns:** `Warehouse`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No warehouse exists with the referenced identifier. |
| `OperationNotAllowedException` | The actor may not operate on that warehouse (see `AuthorizeWarehouseOperationService`). |

---

# Catalog

---

# PublishProductService

## Description

Creates a product in the catalog and publishes it immediately (`status = PUBLISHED`).

## Parameters

`execute(User actor, Product product)`

| Parameter | Type    | Description |
| --------- | ------- | ----------- |
| actor     | User    | Seller publishing the product. It is set as the product's `seller`. |
| product   | Product | New model: `identifier`, `name`, `productType` (`PHYSICAL` or `DIGITAL`), `price` (`Money`), `variants`. Its `status` and `seller` are ignored. |

**Returns:** `Product`

## Business Rules

* Only a `Seller` whose `sellerStatus` is `ACTIVE` can publish products, and always as the product's seller: a product cannot be published for another seller.
* **The price is required and must be greater than zero.** It is fixed when the product is created, together with the seller; `OrderLine` freezes it when an order is confirmed.

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The actor's identifier does not belong to a `Seller`. |
| `SellerNotAuthorizedException` | The seller's `sellerStatus` is not `ACTIVE`. |
| `BusinessRuleViolationException` | The product has no price, or its price is zero. |

---

# UpdateProductStatusService

## Description

Changes the `ProductStatus` of a product (e.g. withdraws it from sale) through `Product.changeStatus`. Only `PUBLISHED` products can be added to a cart or ordered.

## Parameters

`execute(User actor, Product productReference, ProductStatus status)`

| Parameter        | Type          | Description |
| ---------------- | ------------- | ----------- |
| actor            | User          | Owning seller or administrator. |
| productReference | Product       | Reference to the product to update. |
| status           | ProductStatus | New status. |

**Returns:** `Product`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No product exists with the referenced identifier. |
| `OperationNotAllowedException` | The actor is neither an active `Administrator` nor the seller who owns the product. |

---

# UpdateProductVariantsService

## Description

Replaces the list of variants of a product through `Product.replaceVariants`.

## Parameters

`execute(User actor, Product productReference, List<ProductVariant> variants)`

| Parameter        | Type                   | Description |
| ---------------- | ---------------------- | ----------- |
| actor            | User                   | Owning seller or administrator. |
| productReference | Product                | Reference to the product to update. |
| variants         | List\<ProductVariant\> | New list of variants. It replaces the previous one. |

**Returns:** `Product`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No product exists with the referenced identifier. |
| `OperationNotAllowedException` | The actor is neither an active `Administrator` nor the seller who owns the product. |

---

# Inventory

All inventory services act on the `InventoryItem` identified by the referenced product and warehouse, delegate the stock change to the entity, and record one `InventoryMovement`.

| Service | Entity method | Stock effect | Movement recorded |
| ------- | ------------- | ------------ | ----------------- |
| `RegisterInventoryInboundService` | `receive(quantity)` | available `+quantity` | `INBOUND`, `+quantity` |
| `RegisterInventoryReturnService` | `receive(quantity)` | available `+quantity` | `RETURN`, `+quantity` |
| `AdjustInventoryService` | `adjust(quantityDelta)` | available `±quantityDelta` | `ADJUSTMENT`, `quantityDelta` |
| `ReportDamagedInventoryService` | `markDamaged(quantity)` | available `−quantity`, damaged `+quantity` | `ADJUSTMENT`, `−quantity` |
| `ReserveInventoryService` (internal) | `reserve(quantity)` | available `−quantity`, reserved `+quantity` | `RESERVATION`, `+quantity` |
| `ReleaseInventoryReservationService` | `releaseReservation(quantity)` | reserved `−quantity`, available `+quantity` | `RESERVATION`, `−quantity` |
| `ConfirmInventoryOutboundService` | `confirmOutbound(quantity)` | reserved `−quantity` | `SALE_OUTBOUND`, `+quantity` |

> A released reservation is recorded as a negative `RESERVATION` movement, and damaged units as a negative `ADJUSTMENT`, because `InventoryMovementType` has no dedicated type for them.

## Common Parameters

Except for `AdjustInventoryService`, every inventory service receives `execute(User actor, Product product, Warehouse warehouse, int quantity)`:

| Parameter | Type      | Description |
| --------- | --------- | ----------- |
| actor     | User      | User performing the operation, authorized on the item's warehouse with `AuthorizeWarehouseOperationService`. |
| product   | Product   | Reference to the product of the inventory record. |
| warehouse | Warehouse | Reference to the warehouse of the inventory record. |
| quantity  | int       | Units to move. Must be greater than zero. |

`AdjustInventoryService` receives `int quantityDelta` (positive or negative) instead of `quantity`. The internal `ReleaseInventoryReservationService` and `ConfirmInventoryOutboundService` receive no actor: `execute(Product product, Warehouse warehouse, int quantity)`; the service that calls them has already authorized its own actor.

**Returns:** `InventoryItem`

## Common Business Rules

* Negative stock is forbidden: `InventoryItem` rejects any operation that would leave a negative quantity.
* Damaged units are kept apart from `availableQuantity`, so they can never be reserved.
* Every stock change is recorded as an `InventoryMovement`.

## Common Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No `InventoryItem` exists for the product in that warehouse. |
| `OperationNotAllowedException` | The actor may not operate on the item's warehouse (services with an actor only). |
| `InsufficientStockException` | The operation needs more units than are available (or reserved, for release and outbound). |
| `IllegalArgumentException` | `quantity` is zero or negative. |

---

# RegisterInventoryInboundService

## Description

Registers stock received in a warehouse.

## Flow

1. Find the `InventoryItem` for the product and warehouse. If none exists, check that the product is `PHYSICAL` and create the item with zero stock (this is the first time the product is stocked in that warehouse).
2. Check that the warehouse is active.
3. Call `receive(quantity)` and record an `INBOUND` movement.

## Business Rules

* An inactive warehouse cannot receive stock.
* Only `PHYSICAL` products are stocked. The product type is checked when the `InventoryItem` is created; restocking an existing item does not repeat the check.

## Exceptions

In addition to the common exceptions (except "no inventory item", since it is created):

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The item is new and the product or the warehouse does not exist. |
| `BusinessRuleViolationException` | The warehouse is inactive. |
| `BusinessRuleViolationException` | The item is new and the product is `DIGITAL`: digital products neither require nor allow inventory tracking. |

---

# RegisterInventoryReturnService

## Description

Returns units sent back by a buyer to the available stock of an existing inventory record, recording a `RETURN` movement. Used by `ApproveReturnService` for every line of an approved return.

---

# AdjustInventoryService

## Description

Applies a manual correction (positive or negative) to the available stock, e.g. after a physical count, recording an `ADJUSTMENT` movement.

---

# ReportDamagedInventoryService

## Description

Moves available units to damaged, so they can no longer be sold or reserved, recording a negative `ADJUSTMENT` movement.

---

# ReleaseInventoryReservationService

## Description

Returns previously reserved units to the available stock. Used by `CancelOrderService`, and by `ConfirmOrderService` to undo partial reservations.

---

# ConfirmInventoryOutboundService

## Description

Removes previously reserved units from the warehouse because they were sold and dispatched, recording a `SALE_OUTBOUND` movement. Used by `DispatchShipmentService` when an order ships.

---

# Carts

The cart rules live in `ShoppingCart` (see [Domain Model](Domain%20Model.md#shoppingcart)): one line per product, only `PUBLISHED` products, a single currency, positive quantities. The cart services only authorize the actor, load the cart (and the product), call the cart method and save the cart:

| Service | Cart method |
| ------- | ----------- |
| `AddCartLineService` | `ShoppingCart.addLine(product, quantity)` |
| `UpdateCartLineQuantityService` | `ShoppingCart.updateLineQuantity(product, quantity)` |
| `RemoveCartLineService` | `ShoppingCart.removeLine(product)` |

---

# CreateShoppingCartService

## Description

Creates an empty `ShoppingCart` for the buyer who asks for it.

## Parameters

`execute(User actor, ShoppingCart cart)`

| Parameter | Type         | Description |
| --------- | ------------ | ----------- |
| actor     | User         | Buyer owning the new cart. |
| cart      | ShoppingCart | New model: only its `identifier` is used. Its buyer and lines are ignored. |

**Returns:** `ShoppingCart`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The actor's identifier does not belong to a `Buyer`. |

---

# AddCartLineService

## Description

Adds a product to a cart. If the cart already has a line for that product, the quantities are merged into that line instead of creating a second one.

## Parameters

`execute(User actor, ShoppingCart cartReference, Product productReference, int quantity)`

| Parameter        | Type         | Description |
| ---------------- | ------------ | ----------- |
| actor            | User         | Buyer owning the cart. |
| cartReference    | ShoppingCart | Reference to the cart to update. |
| productReference | Product      | Reference to the product to add. |
| quantity         | int          | Units to add. |

**Returns:** `ShoppingCart`

## Flow

1. Authorize the actor as `BUYER`, load the cart and check it with `AuthorizeCartAccessService`.
2. Load the product.
3. Call `ShoppingCart.addLine(product, quantity)` and save the cart.

## Business Rules

* Only the buyer who owns the cart can change it: the role is restricted to `BUYER` with `ValidateRoleAuthorizationService` before loading anything, and the loaded cart is checked with `AuthorizeCartAccessService`.
* Enforced by `ShoppingCart.addLine`:
  * Only `PUBLISHED` products can be added to a cart.
  * A cart holds at most one line per product.
  * **Single currency:** every product in the cart must be priced in the same `Currency`; a product priced in another currency, or without a price, is rejected.
* A cart carries no commercial commitment: no stock is reserved when adding lines.

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `BUYER`, or the cart belongs to another buyer. |
| `EntityNotFoundException` | The cart or the product does not exist. |
| `BusinessRuleViolationException` | The product is not `PUBLISHED`, has no price, or is priced in another currency than the products already in the cart. |
| `IllegalArgumentException` | The resulting quantity is not valid for `CartLine`. |

---

# UpdateCartLineQuantityService

## Description

Replaces the quantity of an existing cart line through `ShoppingCart.updateLineQuantity`. Like every cart change, it is restricted to the buyer who owns the cart (`BUYER` role, then `AuthorizeCartAccessService`).

## Parameters

`execute(User actor, ShoppingCart cartReference, Product productReference, int quantity)`

| Parameter        | Type         | Description |
| ---------------- | ------------ | ----------- |
| actor            | User         | Buyer owning the cart. |
| cartReference    | ShoppingCart | Reference to the cart to update. |
| productReference | Product      | Reference to the product of the line. |
| quantity         | int          | New quantity. |

**Returns:** `ShoppingCart`

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `BUYER`, or the cart belongs to another buyer. |
| `EntityNotFoundException` | The cart does not exist, or it has no line for the product. |
| `IllegalArgumentException` | The quantity is not valid for `CartLine`. |

---

# RemoveCartLineService

## Description

Removes the line of a product from a cart through `ShoppingCart.removeLine`. Restricted to the buyer who owns the cart (`BUYER` role, then `AuthorizeCartAccessService`).

## Parameters

`execute(User actor, ShoppingCart cartReference, Product productReference)`

| Parameter        | Type         | Description |
| ---------------- | ------------ | ----------- |
| actor            | User         | Buyer owning the cart. |
| cartReference    | ShoppingCart | Reference to the cart to update. |
| productReference | Product      | Reference to the product whose line is removed. |

**Returns:** `ShoppingCart`

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `BUYER`, or the cart belongs to another buyer. |
| `EntityNotFoundException` | The cart does not exist, or it has no line for the product. |

---

# Orders

---

# ConfirmOrderService

## Description

Converts a shopping cart into an `Order` in `PENDING_PAYMENT`, reserving stock for every line. This is the point where the provisional selection becomes a commercial commitment.

## Parameters

`execute(User actor, ShoppingCart cartReference, Order newOrder)`

| Parameter     | Type         | Description |
| ------------- | ------------ | ----------- |
| actor         | User         | Buyer owning the cart, or an administrator. |
| cartReference | ShoppingCart | Reference to the cart to confirm. |
| newOrder      | Order        | New model: only its `identifier` is used. |

**Returns:** `Order`

## Flow

1. Authorize the actor as `BUYER` or `ADMINISTRATOR`, load the cart, and check it with `AuthorizeCartAccessService`.
2. Check that the buyer can purchase (`status = ACTIVE` and `commercialStatus = ENABLED`).
3. Check that the cart is not empty and that every product is `PUBLISHED`.
4. For each cart line, reserve stock with `ReserveInventoryService` and build an immutable `OrderLine` that freezes `productName` and `unitPrice` (`Money`) and records the `warehouse` the stock was reserved from. The order is created with all its lines at once; they cannot be changed afterwards.
5. If any reservation fails, release the reservations already made (through `ReleaseInventoryReservationService`) and rethrow the error. No order is created.
6. Save the order and delete the cart.

## Business Rules

* A buyer can only confirm an order when their `status` is `ACTIVE` and their `commercialStatus` is `ENABLED`.
* An empty cart cannot be confirmed.
* Only `PUBLISHED` products can be ordered.
* Either every line is reserved or none is: no stock is left reserved for an order that was never created.
* The order keeps the name and price of each product at the time of purchase, even if the product changes later.

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The cart does not exist. |
| `BusinessRuleViolationException` | The buyer is not active or is suspended from purchasing; the cart is empty; or a product is not `PUBLISHED`. |
| `InsufficientStockException` | No active warehouse has enough available stock for a line. |

---

# AdvanceOrderStatusService

## Description

Confirms the payment of an order, moving it from `PENDING_PAYMENT` to `PAID`, and issues its `Invoice`. The later steps of the lifecycle (`SHIPPED`, `DELIVERED`) are not reached through this service but through the [shipment lifecycle](#shipments).

## Parameters

`execute(User actor, Order orderReference, OrderStatus targetStatus)`

| Parameter      | Type        | Description |
| -------------- | ----------- | ----------- |
| actor          | User        | Logistics operator or administrator. |
| orderReference | Order       | Reference to the order to update. |
| targetStatus   | OrderStatus | Next status. In practice `PAID`: `SHIPPED` and `DELIVERED` are rejected. |

**Returns:** `Order`

## Flow

1. Authorize the actor (`LOGISTICS_OPERATOR` or `ADMINISTRATOR`).
2. Reject `SHIPPED` and `DELIVERED` as targets.
3. Load the order, call `Order.advanceTo(targetStatus)` and save it.
4. If the order is now `PAID`, issue its `Invoice` and save it through `InvoiceRepositoryPort`.

## Business Rules

* The lifecycle is strict and forward-only: no step can be skipped or reversed.
* `SHIPPED` and `DELIVERED` can only be reached through `DispatchShipmentService` and `ConfirmDeliveryService`, so an order cannot be marked as shipped without a shipment, or without its stock leaving the warehouse.
* `CANCELLED` cannot be reached by advancing; use `CancelOrderService`.
* A `DELIVERED` or `CANCELLED` order can never be modified.
* The invoice is issued automatically when the order becomes `PAID`; it has no operation of its own. Its `totalAmount` is a `Money`: the sum of `quantity × unitPrice` over the order lines (the prices frozen at confirmation, all in the same currency because the cart allows only one), its `buyer` is the order's buyer, and its identifier is `INV-<order identifier>`: an order is paid once, so it has exactly one invoice.

## Exceptions

| Exception | When |
| --------- | ---- |
| `BusinessRuleViolationException` | The target is `SHIPPED` or `DELIVERED`. |
| `EntityNotFoundException` | The order does not exist. |
| `InvalidOrderStatusTransitionException` | The target is not the next step, is `CANCELLED`, or the order is already `DELIVERED` / `CANCELLED`. |

---

# CancelOrderService

## Description

Cancels an order and releases the stock reserved for each line.

## Parameters

`execute(User actor, Order orderReference)`

| Parameter      | Type  | Description |
| -------------- | ----- | ----------- |
| actor          | User  | Buyer who placed the order, or an administrator. |
| orderReference | Order | Reference to the order to cancel. |

**Returns:** `Order`

## Flow

1. Authorize the actor as `BUYER` or `ADMINISTRATOR`, load the order, and check it with `AuthorizeOrderAccessService`.
2. Call `Order.cancel()` and release the reservation of every line through `ReleaseInventoryReservationService`.
3. Save the order.

## Business Rules

* An order can only be cancelled from `CART` or `PENDING_PAYMENT`, that is, before payment is confirmed.
* Once `CANCELLED`, the order cannot move to any other status.

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The order does not exist. |
| `InvalidOrderStatusTransitionException` | The order is `PAID` or later, or already `CANCELLED`. |

---

# Shipments

A paid order is delivered through a `Shipment`, whose lifecycle drives the last two steps of the order: dispatching the shipment moves the order to `SHIPPED`, and confirming its delivery moves it to `DELIVERED`.

| Shipment step | Service | Order effect | Stock effect |
| ------------- | ------- | ------------ | ------------ |
| `CREATED` | `CreateShipmentService` | — (must be `PAID`) | — |
| `CREATED` → `PACKED` | `PackShipmentService` | — | — |
| `PACKED` → `DISPATCHED` | `DispatchShipmentService` | `PAID` → `SHIPPED` | reserved → out |
| `DISPATCHED` → `DELIVERED` | `ConfirmDeliveryService` | `SHIPPED` → `DELIVERED` | — |

## Common Business Rules

* All four services are restricted to `LOGISTICS_OPERATOR` and `ADMINISTRATOR` through `ValidateRoleAuthorizationService`, checked before anything is loaded.
* Shipment transitions only happen through the entity (`pack`, `dispatch`, `confirmDelivery`); each records its own timestamp.
* When both the shipment and its order change, both are saved.

## Common Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `LOGISTICS_OPERATOR` or `ADMINISTRATOR`. |
| `EntityNotFoundException` | The shipment (or, when creating it, the order) does not exist. |
| `InvalidShipmentStatusTransitionException` | The shipment is not in the status the step requires. |

---

# CreateShipmentService

## Description

Registers the shipment of a paid order, in `CREATED`.

## Parameters

`execute(User actor, Shipment shipment)`

| Parameter | Type     | Description |
| --------- | -------- | ----------- |
| actor     | User     | Logistics operator or administrator. |
| shipment  | Shipment | New model: its `identifier` and a reference to the `order` to ship. |

**Returns:** `Shipment`

## Flow

1. Check that no shipment with that identifier exists.
2. Load the order and check that it is `PAID`.
3. Check that the order has no shipment yet.
4. Create the shipment (`CREATED`, `createdAt` = now) and save it.

## Business Rules

* Only `PAID` orders can be shipped.
* An order has at most one shipment.

## Exceptions

In addition to the common exceptions:

| Exception | When |
| --------- | ---- |
| `DuplicateResourceException` | The shipment identifier is already used, or the order already has a shipment. |
| `BusinessRuleViolationException` | The order is not `PAID`. |

---

# PackShipmentService

## Description

Marks the goods of a shipment as packed (`CREATED` → `PACKED`). The order is not affected.

## Parameters

`execute(User actor, Shipment shipmentReference)`

| Parameter         | Type     | Description |
| ----------------- | -------- | ----------- |
| actor             | User     | Logistics operator or administrator. |
| shipmentReference | Shipment | Reference to the shipment to pack. |

**Returns:** `Shipment`

---

# DispatchShipmentService

## Description

Dispatches a packed shipment (`PACKED` → `DISPATCHED`): the stock reserved for each order line leaves its warehouse, and the order moves to `SHIPPED`.

## Parameters

`execute(User actor, Shipment shipmentReference)`

| Parameter         | Type     | Description |
| ----------------- | -------- | ----------- |
| actor             | User     | Logistics operator or administrator. |
| shipmentReference | Shipment | Reference to the shipment to dispatch. |

**Returns:** `Shipment`

## Flow

1. Load the shipment; its order comes with it.
2. Call `Shipment.dispatch()` and `Order.advanceTo(SHIPPED)`. Both transitions are validated before any stock is touched.
3. Confirm the outbound of every order line through `ConfirmInventoryOutboundService`.
4. Save the order and the shipment.

## Exceptions

In addition to the common exceptions:

| Exception | When |
| --------- | ---- |
| `InvalidOrderStatusTransitionException` | The order is not `PAID`. |
| `InsufficientStockException` | A line has less reserved stock than its quantity (propagated from `ConfirmInventoryOutboundService`). |

---

# ConfirmDeliveryService

## Description

Confirms that a dispatched shipment reached the buyer (`DISPATCHED` → `DELIVERED`), and moves its order to `DELIVERED`.

## Parameters

`execute(User actor, Shipment shipmentReference)`

| Parameter         | Type     | Description |
| ----------------- | -------- | ----------- |
| actor             | User     | Logistics operator or administrator. |
| shipmentReference | Shipment | Reference to the shipment delivered. |

**Returns:** `Shipment`

## Flow

1. Load the shipment; its order comes with it.
2. Call `Shipment.confirmDelivery()` and `Order.advanceTo(DELIVERED)`.
3. Save the order and the shipment.

## Exceptions

In addition to the common exceptions:

| Exception | When |
| --------- | ---- |
| `InvalidOrderStatusTransitionException` | The order is not `SHIPPED`. |

---

# Returns and Refunds

A buyer can ask to return a delivered order through a `ReturnRequest`. Approving the request puts the returned stock back and creates a `Refund`, which an administrator then processes.

| Step | Service | Return request | Refund | Stock effect |
| ---- | ------- | -------------- | ------ | ------------ |
| Request | `RequestReturnService` | → `REQUESTED` | — | — |
| Approve | `ApproveReturnService` | `REQUESTED` → `APPROVED` | created `PENDING` | every line back to its warehouse |
| Reject | `RejectReturnService` | `REQUESTED` → `REJECTED` | — | — |
| Process refund | `ProcessRefundService` | — | `PENDING` → `PROCESSED` | — |

## Common Business Rules

* Each service authorizes the actor by role, through `ValidateRoleAuthorizationService`, before anything is loaded.
* The entities make their own transitions (`ReturnRequest.approve` / `reject`, `Refund.process`); a return request is decided exactly once and a refund is processed exactly once.

## Common Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not active or its role is not allowed. |
| `EntityNotFoundException` | The order, return request or refund does not exist. |
| `BusinessRuleViolationException` | The return request is already decided, or the refund already processed. |

---

# RequestReturnService

## Description

Lets a buyer request the return of one of its delivered orders.

## Parameters

`execute(User actor, ReturnRequest returnRequest)`

| Parameter     | Type          | Description |
| ------------- | ------------- | ----------- |
| actor         | User          | Buyer who placed the order. |
| returnRequest | ReturnRequest | New model: its `identifier`, a reference to the `order` to return, and the `reason` given by the buyer. |

**Returns:** `ReturnRequest`

## Flow

1. Authorize the actor as `BUYER`.
2. Load the order and check, through `AuthorizeOrderAccessService`, that it belongs to the actor.
3. Check that the order is `DELIVERED`, that the return request identifier is not used, and that the order has no return request yet.
4. Create the return request (`REQUESTED`, `requestedAt` = now, `buyer` = the order's buyer) and save it.

## Business Rules

* Only the buyer who placed the order may request its return; administrators and supervisors may not request it on the buyer's behalf.
* Only `DELIVERED` orders can be returned.
* An order has at most one return request.

## Exceptions

In addition to the common exceptions:

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The order belongs to another buyer. |
| `BusinessRuleViolationException` | The order is not `DELIVERED`. |
| `DuplicateResourceException` | The return request identifier is already used, or the order already has a return request. |

---

# ApproveReturnService

## Description

Approves a return request: every order line goes back to the warehouse it was shipped from, and a `PENDING` refund for the order total is created.

## Parameters

`execute(User actor, ReturnRequest returnRequestReference)`

| Parameter              | Type          | Description |
| ---------------------- | ------------- | ----------- |
| actor                  | User          | Administrator. |
| returnRequestReference | ReturnRequest | Reference to the return request to approve. |

**Returns:** `ReturnRequest`

## Flow

1. Authorize the actor as `ADMINISTRATOR`.
2. Load the return request and call `ReturnRequest.approve(actor)`, validated before any stock is touched.
3. For every order line, register the return of its quantity to its warehouse through `RegisterInventoryReturnService`, with the same actor.
4. Save the return request.
5. Create the refund (`PENDING`, identifier `REF-<return request identifier>`, amount = the order total) and save it through `RefundRepositoryPort`.

## Business Rules

* Only administrators may approve a return: an order can hold lines of several sellers, so no single seller has authority over the whole return.
* The refund amount is the same total the invoice charged: the sum of `quantity × unitPrice` over the order lines, as a `Money` in the order's currency.
* A return request has exactly one refund, so the refund identifier derives from the request's.

---

# RejectReturnService

## Description

Rejects a return request. Stock and refunds are not affected.

## Parameters

`execute(User actor, ReturnRequest returnRequestReference)`

| Parameter              | Type          | Description |
| ---------------------- | ------------- | ----------- |
| actor                  | User          | Administrator. |
| returnRequestReference | ReturnRequest | Reference to the return request to reject. |

**Returns:** `ReturnRequest`

## Business Rules

* Only administrators may reject a return, for the same reason as approving: an order can hold lines of several sellers.

---

# ProcessRefundService

## Description

Pays a pending refund back to the buyer.

## Parameters

`execute(User actor, Refund refundReference)`

| Parameter       | Type   | Description |
| --------------- | ------ | ----------- |
| actor           | User   | Administrator. |
| refundReference | Refund | Reference to the refund to process. |

**Returns:** `Refund`

## Business Rules

* Only administrators may process refunds.

---

# Internal Services

---

# ReserveInventoryService

## Description

Reserves stock for a product. Used by `ConfirmOrderService`.

## Operation

`InventoryItem reserve(String productId, int quantity)`

## Flow

1. Load every `InventoryItem` of the product.
2. Pick the **first** one whose warehouse is active and whose `availableQuantity` is at least `quantity`.
3. Call `reserve(quantity)`, save it, and record a `RESERVATION` movement.

## Business Rules

* Inactive warehouses are never used to reserve stock.
* Damaged units are not part of `availableQuantity`, so they are never reserved.
* A line is reserved from a single warehouse; its quantity is not split across warehouses.

## Exceptions

| Exception | When |
| --------- | ---- |
| `InsufficientStockException` | No active warehouse has enough available stock. |

---

# AuditOperationService

## Description

Records which authenticated user performed which operation, with the current timestamp, through `AuditLogPort`.

## Operation

`void record(String userId, String operation)`

## Business Rules

* Every authenticated operation must be traceable to the `User` who performed it. The security layer (`infrastructure/security`) calls this service for every authenticated request, so traceability does not depend on each service remembering to call it.

---

# NotifyUserService

## Description

Sends a message to a user through a `NotificationChannel`, via `NotificationPort`. It is meant to be injected into services that need to notify a user of an outcome.

## Operation

`void notify(String userId, NotificationChannel channel, String message)`

> **Note:** no service uses it yet. `NotificationPort` is backed by `LoggingNotificationAdapter`, which only logs the message (see [Persistence](../Adapters/Persistence.md)).

---

# Authorization Services

## Description

`domain/services/authorization/` holds internal services with a single purpose each: deciding whether the authenticated `User` (the *actor*) may perform an operation. Unlike the operation services, they are plain classes with a manual constructor and no framework annotations; they are registered as beans in `infrastructure/config/AuthorizationBeanConfiguration`.

Each one exposes an `execute(...)` method that returns normally when the actor is authorized and throws `OperationNotAllowedException` otherwise. Authentication (who the actor is) happens in the security layer; these services only decide what an authenticated actor may do, so the rules live in the domain and are testable without Spring.

## Services

| Service | Operation | Allows | Used by |
| ------- | --------- | ------ | ------- |
| `ValidateUserAuthorizationStatusService` | `execute(User actor)` | A present (non-`null`) actor whose `status` is `ACTIVE`. | Every other authorization service; `ListPublishedProductsService`, `ConsultProductService`, `ListSellerProductsService`, `ListBuyerOrdersService`, `ConsultInventoryService`. |
| `ValidateRoleAuthorizationService` | `execute(User actor, SystemRole... allowedRoles)` | An active actor whose role is one of `allowedRoles`. | `RegisterSellerService`, `RegisterLogisticsOperatorService`, `RegisterSupervisorService`, `UpdateUserStatusService`, `UpdateSellerStatusService` (`ADMINISTRATOR`); `RegisterWarehouseService` (`SELLER`, `ADMINISTRATOR`); `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService`, `ConsultCartService` (`BUYER`); `AdvanceOrderStatusService`, `CreateShipmentService`, `PackShipmentService`, `DispatchShipmentService`, `ConfirmDeliveryService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`); `ConfirmOrderService`, `CancelOrderService` (`BUYER`, `ADMINISTRATOR`); `ConsultUserService` (`ADMINISTRATOR`, `SUPERVISOR`); `RequestReturnService` (`BUYER`); `ApproveReturnService`, `RejectReturnService`, `ProcessRefundService` (`ADMINISTRATOR`). |
| `AuthorizeOrderAccessService` | `execute(User actor, Order order)` | An active `Administrator`, `Supervisor` or `LogisticsOperator`, or the `Buyer` who placed the order. | `CancelOrderService`, `ConsultOrderService`, `RequestReturnService`. |
| `AuthorizeProductOwnershipService` | `execute(User actor, Product product)` | An active `Administrator`, or the `Seller` who owns the product. | `UpdateProductStatusService`, `UpdateProductVariantsService`, `ConsultProductService` (non-published products only), `ConsultInventoryService`. |
| `AuthorizeWarehouseOperationService` | `execute(User actor, Warehouse warehouse)` | An active `Administrator` or `LogisticsOperator` on any warehouse, or the `Seller` who owns a Seller-owned warehouse. | `DeactivateWarehouseService`, `RegisterInventoryInboundService`, `RegisterInventoryReturnService`, `AdjustInventoryService`, `ReportDamagedInventoryService`. |
| `AuthorizeCartAccessService` | `execute(User actor, ShoppingCart cart)` | An active `Administrator` or `Supervisor`, or the `Buyer` who owns the cart. | `ConfirmOrderService`, `ConsultCartService`, `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService`. |

## Business Rules

* **The status check comes first.** Every authorization service calls `ValidateUserAuthorizationStatusService` before its own rule, so a missing, `BLOCKED` or `INACTIVE` actor is never authorized.
* **When authorization happens.** A service that authorizes by role alone does it before loading anything. A service that authorizes on an entity (order, cart, product, warehouse) loads that entity first — the rule needs it — and authorizes before changing anything. Inventory services authorize on the warehouse of the affected `InventoryItem`.
* **Ownership is compared by identifier** (`order.getBuyer()`, `cart.getBuyer()`, `product.getSeller()`, `warehouse.getOwner()` against the actor). A missing owner (e.g. a Marketplace-owned warehouse, which has no owning seller) is never matched, so only the roles allowed regardless of ownership get through.
* **Restrict the role first, then check access.** The access services allow `Supervisor` (and, for orders, `LogisticsOperator`) so that read operations can reuse them. Services that need a narrower rule first restrict the role with `ValidateRoleAuthorizationService` and then apply the access service, which at that point only lets through the roles left: `CancelOrderService` and `ConfirmOrderService` (`BUYER`, `ADMINISTRATOR`: administrators and the owning buyer), `RequestReturnService` and every cart service (`BUYER`: only the owning buyer).
* **Carts belong to their buyer.** Every cart operation except confirming an order — `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService` and `ConsultCartService` — is restricted to `BUYER`, so neither administrators nor supervisors can read or change a cart. `CreateShoppingCartService` already requires the actor to be a `Buyer`. `ConfirmOrderService` keeps allowing `ADMINISTRATOR`.
* **A buyer cannot advance its own order.** `AdvanceOrderStatusService` is restricted to `LOGISTICS_OPERATOR` and `ADMINISTRATOR`.
* **Logistics operators can consult orders.** `AuthorizeOrderAccessService` allows `LogisticsOperator` so that `ConsultOrderService` can serve `LogisticsOperatorPort.consultOrder`. This is a deliberate, scoped exception: no other rule of that service changed, and since `CancelOrderService` and `RequestReturnService` restrict the role before calling it, in practice it only affects `ConsultOrderService`.

## Authorization Changes in the Role-Based Refactor

Moving to one input port per role (see [Input Ports](Input-ports.md)) also tightened or adjusted these rules:

| Service | Before | After |
| ------- | ------ | ----- |
| `RegisterSellerService` | `administratorId` in the command had to reference an `Administrator`; the administrator's `status` was not checked. | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`): the actor must be an active administrator. |
| `RegisterSupervisorService`, `RegisterLogisticsOperatorService` | No authorization. | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`). |
| `RegisterWarehouseService` | `ownerType`, `sellerId` and `administratorId` in the command decided the owner. | `ValidateRoleAuthorizationService` (`SELLER`, `ADMINISTRATOR`), then by role: administrators register Marketplace-owned warehouses; sellers register Seller-owned warehouses owned by themselves. |
| `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService` | No authorization. | `ValidateRoleAuthorizationService` (`BUYER`), then `AuthorizeCartAccessService`. |
| `ConsultCartService` | `AuthorizeCartAccessService` (administrators, supervisors, owning buyer). | `ValidateRoleAuthorizationService` (`BUYER`), then `AuthorizeCartAccessService`: only the owning buyer. |
| `AuthorizeOrderAccessService` | Administrators, supervisors, owning buyer. | Also `LogisticsOperator`, so `ConsultOrderService` serves `LogisticsOperatorPort`. Scoped exception; no other rule changed. |
| `PublishProductService`, `CreateShoppingCartService` | `sellerId` / `buyerId` in the command named the owner. | The actor is the owner: it must be a `Seller` / `Buyer`. |

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is missing or not `ACTIVE`, its role is not allowed, or it does not own the order, cart, product or warehouse. |

---

# Service Collaboration

```text
ConfirmOrderService
   ├── ReserveInventoryService ──────────────> reserve stock for each line
   └── ReleaseInventoryReservationService ───> undo partial reservations on failure

AdvanceOrderStatusService
   └── InvoiceRepositoryPort ────────────────> issue the invoice when the order becomes PAID

DispatchShipmentService
   └── ConfirmInventoryOutboundService ──────> every line, when the order becomes SHIPPED

ApproveReturnService
   ├── RegisterInventoryReturnService ───────> every line back to its warehouse
   └── RefundRepositoryPort ─────────────────> create the PENDING refund

CancelOrderService
   └── ReleaseInventoryReservationService ───> release every line

Security layer (infrastructure/security)
   └── AuditOperationService ────────────────> every authenticated request
```

## Stock Lifecycle of an Order Line

```text
available ──reserve──> reserved ──confirmOutbound──> (leaves the warehouse)
    ▲                     │
    └─releaseReservation──┘
```

| Order event | Service | Stock effect |
| ----------- | ------- | ------------ |
| Order confirmed | `ConfirmOrderService` → `ReserveInventoryService` | available → reserved |
| Order cancelled | `CancelOrderService` → `ReleaseInventoryReservationService` | reserved → available |
| Order shipped | `DispatchShipmentService` → `ConfirmInventoryOutboundService` | reserved → out |
