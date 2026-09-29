# Domain Services — NexusMarket

## Introduction

Domain Services contain the business logic of NexusMarket that does not naturally belong to a single entity: they load entities through output ports, coordinate them, enforce cross-entity rules, and persist the result.

Services are plain Java classes with no framework annotations. Their dependencies (output ports, and other use cases when needed) are received through the constructor and wired as Spring beans in `infrastructure/config` (see [Software Architecture](../Software%20Architecture/Software%20Architecture.md)).

There are two kinds of services:

* **Use case services** — each implements exactly one input port (`<Name>UseCase` → `<Name>Service`) and is invoked by input adapters through its `execute(Command)` method — or `execute(User actor, Command)` when the use case must be authorized (see [Authorization Services](#authorization-services)).
* **Internal services** — not exposed as use cases; they are injected into other services or into the security layer. The authorization services in `domain/services/authorization/` are internal services.

---

# Design Conventions

* **One use case, one service.** Every input port is an interface with a nested `Command` record and a single `execute(Command)` method. Its service is the only implementation.
* **Authorized use cases receive the actor.** Use cases restricted to some users take the authenticated `User` as first parameter, `execute(User actor, Command)`; the actor is never part of the `Command`. The service authorizes the actor through an authorization service before it changes anything (see [Authorization Services](#authorization-services)).
* **Entities protect their own invariants.** Services never modify stock or order status directly: they call the entity methods (`InventoryItem.receive`, `reserve`, `releaseReservation`, `confirmOutbound`, `adjust`, `markDamaged`; `Order.advanceTo`, `cancel`). Services add the rules that need more than one entity (e.g. the warehouse must be active, the seller must be `ACTIVE`).
* **Services depend on ports, never on concrete services.** When a service needs another use case, it depends on the input port interface (e.g. `CancelOrderService` → `ReleaseInventoryReservationUseCase`). Internal services, which have no input port, are injected directly.
* **Every stock change records an `InventoryMovement`.** Each inventory service saves the `InventoryItem` and then an `InventoryMovement` through `InventoryMovementRepositoryPort`.
* **Lookups fail with `EntityNotFoundException`.** When an ID refers to a user of the wrong role (e.g. a `sellerId` that belongs to a `Buyer`), the service also throws `EntityNotFoundException`, so the role of other accounts is not revealed.
* **Invalid quantities** are rejected by the entities with `IllegalArgumentException`. Cart services rethrow it with the cart and product in the message.

---

# Service Catalog

| Area       | Service | Input port | Output ports / dependencies | Authorization |
| ---------- | ------- | ---------- | ---------------------------- | ------------- |
| Users      | `AuthenticateUserService` | `AuthenticateUserUseCase` | `UserRepositoryPort`, `PasswordHasherPort`, `TokenServicePort` | — |
| Users      | `RegisterBuyerService` | `RegisterBuyerUseCase` | `UserRepositoryPort`, `PasswordHasherPort` | — |
| Users      | `RegisterSellerService` | `RegisterSellerUseCase` | `UserRepositoryPort`, `PasswordHasherPort` | — |
| Users      | `RegisterLogisticsOperatorService` | `RegisterLogisticsOperatorUseCase` | `UserRepositoryPort`, `PasswordHasherPort` | — |
| Users      | `RegisterSupervisorService` | `RegisterSupervisorUseCase` | `UserRepositoryPort`, `PasswordHasherPort` | — |
| Users      | `UpdateUserStatusService` | `UpdateUserStatusUseCase` | `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Users      | `UpdateSellerStatusService` | `UpdateSellerStatusUseCase` | `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Warehouses | `RegisterWarehouseService` | `RegisterWarehouseUseCase` | `WarehouseRepositoryPort`, `UserRepositoryPort` | — |
| Warehouses | `DeactivateWarehouseService` | `DeactivateWarehouseUseCase` | `WarehouseRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Catalog    | `PublishProductService` | `PublishProductUseCase` | `ProductRepositoryPort`, `UserRepositoryPort` | — |
| Catalog    | `UpdateProductStatusService` | `UpdateProductStatusUseCase` | `ProductRepositoryPort` | `AuthorizeProductOwnershipService` |
| Catalog    | `UpdateProductVariantsService` | `UpdateProductVariantsUseCase` | `ProductRepositoryPort` | `AuthorizeProductOwnershipService` |
| Inventory  | `RegisterInventoryInboundService` | `RegisterInventoryInboundUseCase` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort`, `ProductRepositoryPort`, `WarehouseRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Inventory  | `RegisterInventoryReturnService` | `RegisterInventoryReturnUseCase` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Inventory  | `AdjustInventoryService` | `AdjustInventoryUseCase` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Inventory  | `ReportDamagedInventoryService` | `ReportDamagedInventoryUseCase` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Inventory  | `ReleaseInventoryReservationService` | `ReleaseInventoryReservationUseCase` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | — |
| Inventory  | `ConfirmInventoryOutboundService` | `ConfirmInventoryOutboundUseCase` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | — |
| Carts      | `CreateShoppingCartService` | `CreateShoppingCartUseCase` | `CartRepositoryPort`, `UserRepositoryPort` | — |
| Carts      | `AddCartLineService` | `AddCartLineUseCase` | `CartRepositoryPort`, `ProductRepositoryPort` | — |
| Carts      | `UpdateCartLineQuantityService` | `UpdateCartLineQuantityUseCase` | `CartRepositoryPort` | — |
| Carts      | `RemoveCartLineService` | `RemoveCartLineUseCase` | `CartRepositoryPort` | — |
| Orders     | `ConfirmOrderService` | `ConfirmOrderUseCase` | `CartRepositoryPort`, `OrderRepositoryPort`, `ReserveInventoryService`, `ReleaseInventoryReservationUseCase` | `ValidateRoleAuthorizationService` (`BUYER`, `ADMINISTRATOR`), `AuthorizeCartAccessService` |
| Orders     | `AdvanceOrderStatusService` | `AdvanceOrderStatusUseCase` | `OrderRepositoryPort`, `InvoiceRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Orders     | `CancelOrderService` | `CancelOrderUseCase` | `OrderRepositoryPort`, `ReleaseInventoryReservationUseCase` | `ValidateRoleAuthorizationService` (`BUYER`, `ADMINISTRATOR`), `AuthorizeOrderAccessService` |
| Shipments  | `CreateShipmentService` | `CreateShipmentUseCase` | `ShipmentRepositoryPort`, `OrderRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Shipments  | `PackShipmentService` | `PackShipmentUseCase` | `ShipmentRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Shipments  | `DispatchShipmentService` | `DispatchShipmentUseCase` | `ShipmentRepositoryPort`, `OrderRepositoryPort`, `ConfirmInventoryOutboundUseCase` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Shipments  | `ConfirmDeliveryService` | `ConfirmDeliveryUseCase` | `ShipmentRepositoryPort`, `OrderRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Returns    | `RequestReturnService` | `RequestReturnUseCase` | `ReturnRequestRepositoryPort`, `OrderRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeOrderAccessService` |
| Returns    | `ApproveReturnService` | `ApproveReturnUseCase` | `ReturnRequestRepositoryPort`, `RefundRepositoryPort`, `RegisterInventoryReturnUseCase` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Returns    | `RejectReturnService` | `RejectReturnUseCase` | `ReturnRequestRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Returns    | `ProcessRefundService` | `ProcessRefundUseCase` | `RefundRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Internal   | `ReserveInventoryService` | — | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | — |
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

Read-only use cases. Each input port declares a nested `Query` record (instead of a `Command`) and a single `execute(User actor, Query query)` method; the service authorizes the actor and never modifies data. Supervisors, a read-only role, are allowed wherever the rule below says so.

| Area       | Service | Input port | Output ports / dependencies | Authorization |
| ---------- | ------- | ---------- | ---------------------------- | ------------- |
| Catalog    | `ListPublishedProductsService` | `ListPublishedProductsUseCase` | `ProductRepositoryPort` | `ValidateUserAuthorizationStatusService` (any active user) |
| Catalog    | `ConsultProductService` | `ConsultProductUseCase` | `ProductRepositoryPort` | `ValidateUserAuthorizationStatusService` (any active user); if not `PUBLISHED`, also `AuthorizeProductOwnershipService` |
| Catalog    | `ListSellerProductsService` | `ListSellerProductsUseCase` | `ProductRepositoryPort` | `ValidateUserAuthorizationStatusService` + own rule: `Administrator`, `Supervisor`, or the `Seller` whose id is `sellerId` |
| Carts      | `ConsultCartService` | `ConsultCartUseCase` | `CartRepositoryPort` | `AuthorizeCartAccessService` |
| Orders     | `ConsultOrderService` | `ConsultOrderUseCase` | `OrderRepositoryPort` | `AuthorizeOrderAccessService` (supervisors allowed) |
| Orders     | `ListBuyerOrdersService` | `ListBuyerOrdersUseCase` | `OrderRepositoryPort` | `ValidateUserAuthorizationStatusService` + own rule: `Administrator`, `Supervisor`, or the `Buyer` whose id is `buyerId` |
| Inventory  | `ConsultInventoryService` | `ConsultInventoryUseCase` | `ProductRepositoryPort`, `InventoryRepositoryPort` | `Supervisor` / `LogisticsOperator`: `ValidateUserAuthorizationStatusService`; anyone else: `AuthorizeProductOwnershipService` (`Administrator` or owning `Seller`) |
| Users      | `ConsultUserService` | `ConsultUserUseCase` | `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`, `SUPERVISOR`) |

| Query | Query fields | Returns | Notes |
| ----- | ------------ | ------- | ----- |
| `ListPublishedProductsUseCase` | — | `List<Product>` | Only `PUBLISHED` products. |
| `ConsultProductUseCase` | `productId` | `Product` | A `PUBLISHED` product is visible to any active user; a `SUSPENDED` or `DISCONTINUED` one only to an `Administrator` or its owning `Seller`. `EntityNotFoundException` if it does not exist. |
| `ListSellerProductsUseCase` | `sellerId` | `List<Product>` | Every status. Empty if the seller has no products. |
| `ConsultCartUseCase` | `cartId` | `ShoppingCart` | The cart is loaded first, then authorized. |
| `ConsultOrderUseCase` | `orderId` | `Order` | The order is loaded first, then authorized. |
| `ListBuyerOrdersUseCase` | `buyerId` | `List<Order>` | Most recent first. |
| `ConsultInventoryUseCase` | `productId` | `List<InventoryItem>` | One item per warehouse holding the product. The product is loaded first, then authorized. |
| `ConsultUserUseCase` | `userId` | `User` | Authorized by role before loading. |

The "own rule" of `ListSellerProductsService` and `ListBuyerOrdersService` compares an ID from the query with the actor, not a loaded entity, so no existing authorization service fits; it is a private method of each service until another use case needs it.

---

# Users

---

# AuthenticateUserService

## Description

Verifies a user's credentials and returns the authenticated `User` together with an access token issued for it.

## Command

| Field    | Type   | Description |
| -------- | ------ | ----------- |
| email    | String | Email the user registered with. |
| password | String | Plain password, checked with `PasswordHasherPort.matches`. It is never stored. |

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

Registers a new `Buyer`. Buyers self-register.

## Command

| Field          | Type   | Description |
| -------------- | ------ | ----------- |
| identifier     | String | National ID or business tax ID. |
| fullName       | String | Full name. |
| email          | String | Primary email. |
| primaryAddress | String | Default delivery address. |
| password       | String | Plain password, hashed with `PasswordHasherPort.hash`. |

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

## Command

| Field           | Type   | Description |
| --------------- | ------ | ----------- |
| identifier      | String | Business tax ID. |
| fullName        | String | Legal name. |
| email           | String | Primary email. |
| password        | String | Plain password, hashed with `PasswordHasherPort.hash`. |
| administratorId | String | Administrator performing the registration. |

**Returns:** `Seller`

## Business Rules

* Sellers cannot self-register: `administratorId` must reference an existing `Administrator`.
* `identifier` and `email` must be unique across all users.
* A new seller starts with `status = ACTIVE` and `sellerStatus = ACTIVE`.

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | `administratorId` is missing or does not belong to an `Administrator`. |
| `DuplicateResourceException` | The `identifier` or the `email` is already used by another user. |

---

# RegisterLogisticsOperatorService / RegisterSupervisorService

## Description

Register a new `LogisticsOperator` or `Supervisor`. Both services follow the same flow.

## Command

| Field      | Type   | Description |
| ---------- | ------ | ----------- |
| identifier | String | National ID. |
| fullName   | String | Full name. |
| email      | String | Primary email. |
| password   | String | Plain password, hashed with `PasswordHasherPort.hash`. |

**Returns:** `LogisticsOperator` / `Supervisor`

## Business Rules

* `identifier` and `email` must be unique across all users.
* A new user starts with `status = ACTIVE`.

## Exceptions

| Exception | When |
| --------- | ---- |
| `DuplicateResourceException` | The `identifier` or the `email` is already used by another user. |

---

# UpdateUserStatusService

## Description

Changes the operational `UserStatus` of any user (e.g. blocks an account).

## Command

| Field  | Type       | Description |
| ------ | ---------- | ----------- |
| userId | String     | User to update. |
| status | UserStatus | New status. |

**Returns:** `User`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No user exists with `userId`. |

---

# UpdateSellerStatusService

## Description

Changes the commercial `SellerStatus` of a seller. It is independent from the `UserStatus`: a seller can be able to log in but not allowed to publish.

## Command

| Field    | Type         | Description |
| -------- | ------------ | ----------- |
| sellerId | String       | Seller to update. |
| status   | SellerStatus | New seller status. |

**Returns:** `Seller`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No user exists with `sellerId`, or the user is not a `Seller`. |

---

# Warehouses

---

# RegisterWarehouseService

## Description

Registers a Marketplace-owned or Seller-owned warehouse.

## Command

| Field           | Type               | Description |
| --------------- | ------------------ | ----------- |
| identifier      | String             | Unique identifier of the warehouse. |
| name            | String             | Warehouse name. |
| address         | String             | Physical address. |
| ownerType       | WarehouseOwnerType | `MARKETPLACE` or `SELLER`. |
| sellerId        | String             | Owning seller. Required when `ownerType = SELLER`. |
| administratorId | String             | Administrator performing the registration. Required when `ownerType = MARKETPLACE`. |

**Returns:** `Warehouse`

## Business Rules

* A Marketplace-owned warehouse can only be registered by an `Administrator`.
* A Seller-owned warehouse must reference its owning `Seller`, which is set as the warehouse `owner`.

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | Marketplace-owned and `administratorId` is missing or not an `Administrator`; or Seller-owned and `sellerId` is missing. |
| `EntityNotFoundException` | Seller-owned and `sellerId` does not belong to a `Seller`. |

---

# DeactivateWarehouseService

## Description

Marks a warehouse as inactive. An inactive warehouse cannot receive inbound stock and is skipped when reserving stock.

## Command

| Field       | Type   | Description |
| ----------- | ------ | ----------- |
| warehouseId | String | Warehouse to deactivate. |

**Returns:** `Warehouse`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No warehouse exists with `warehouseId`. |

---

# Catalog

---

# PublishProductService

## Description

Creates a product in the catalog and publishes it immediately (`status = PUBLISHED`).

## Command

| Field       | Type                   | Description |
| ----------- | ---------------------- | ----------- |
| identifier  | String                 | Unique identifier of the product. |
| name        | String                 | Product name. |
| productType | ProductType            | `PHYSICAL` or `DIGITAL`. |
| price       | BigDecimal             | Unit price. |
| sellerId    | String                 | Seller publishing the product. |
| variants    | List\<ProductVariant\> | Variants of the product (size, color, …). |

**Returns:** `Product`

## Business Rules

* Only a `Seller` whose `sellerStatus` is `ACTIVE` can publish products.

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | `sellerId` does not belong to a `Seller`. |
| `SellerNotAuthorizedException` | The seller's `sellerStatus` is not `ACTIVE`. |

---

# UpdateProductStatusService

## Description

Changes the `ProductStatus` of a product (e.g. withdraws it from sale). Only `PUBLISHED` products can be added to a cart or ordered.

## Command

| Field     | Type          | Description |
| --------- | ------------- | ----------- |
| productId | String        | Product to update. |
| status    | ProductStatus | New status. |

**Returns:** `Product`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No product exists with `productId`. |

---

# UpdateProductVariantsService

## Description

Replaces the list of variants of a product.

## Command

| Field     | Type                   | Description |
| --------- | ---------------------- | ----------- |
| productId | String                 | Product to update. |
| variants  | List\<ProductVariant\> | New list of variants. It replaces the previous one. |

**Returns:** `Product`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No product exists with `productId`. |

---

# Inventory

All inventory services act on the `InventoryItem` identified by `productId` + `warehouseId`, delegate the stock change to the entity, and record one `InventoryMovement`.

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

## Common Command

Except for `AdjustInventoryUseCase`, every inventory use case receives:

| Field       | Type    | Description |
| ----------- | ------- | ----------- |
| productId   | String  | Product of the inventory record. |
| warehouseId | String  | Warehouse of the inventory record. |
| quantity    | int     | Units to move. Must be greater than zero. |

`AdjustInventoryUseCase` receives `quantityDelta` (positive or negative) instead of `quantity`.

**Returns:** `InventoryItem`

## Common Business Rules

* Negative stock is forbidden: `InventoryItem` rejects any operation that would leave a negative quantity.
* Damaged units are kept apart from `availableQuantity`, so they can never be reserved.
* Every stock change is recorded as an `InventoryMovement`.

## Common Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No `InventoryItem` exists for the product in that warehouse. |
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

---

# CreateShoppingCartService

## Description

Creates an empty `ShoppingCart` for a buyer.

## Command

| Field   | Type   | Description |
| ------- | ------ | ----------- |
| cartId  | String | Identifier of the new cart. |
| buyerId | String | Buyer owning the cart. |

**Returns:** `ShoppingCart`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | `buyerId` does not belong to a `Buyer`. |

---

# AddCartLineService

## Description

Adds a product to a cart. If the cart already has a line for that product, the quantities are merged into that line instead of creating a second one.

## Command

| Field     | Type   | Description |
| --------- | ------ | ----------- |
| cartId    | String | Cart to update. |
| productId | String | Product to add. |
| quantity  | int    | Units to add. |

**Returns:** `ShoppingCart`

## Business Rules

* Only `PUBLISHED` products can be added to a cart.
* A cart holds at most one line per product.
* A cart carries no commercial commitment: no stock is reserved when adding lines.

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The cart or the product does not exist. |
| `BusinessRuleViolationException` | The product is not `PUBLISHED`. |
| `IllegalArgumentException` | The resulting quantity is not valid for `CartLine`. |

---

# UpdateCartLineQuantityService

## Description

Replaces the quantity of an existing cart line.

## Command

| Field     | Type   | Description |
| --------- | ------ | ----------- |
| cartId    | String | Cart to update. |
| productId | String | Product of the line. |
| quantity  | int    | New quantity. |

**Returns:** `ShoppingCart`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The cart does not exist, or it has no line for the product. |
| `IllegalArgumentException` | The quantity is not valid for `CartLine`. |

---

# RemoveCartLineService

## Description

Removes the line of a product from a cart.

## Command

| Field     | Type   | Description |
| --------- | ------ | ----------- |
| cartId    | String | Cart to update. |
| productId | String | Product whose line is removed. |

**Returns:** `ShoppingCart`

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The cart does not exist, or it has no line for the product. |

---

# Orders

---

# ConfirmOrderService

## Description

Converts a shopping cart into an `Order` in `PENDING_PAYMENT`, reserving stock for every line. This is the point where the provisional selection becomes a commercial commitment.

## Command

| Field   | Type   | Description |
| ------- | ------ | ----------- |
| cartId  | String | Cart to confirm. |
| orderId | String | Identifier of the new order. |

**Returns:** `Order`

## Flow

1. Load the cart.
2. Check that the buyer can purchase (`status = ACTIVE` and `commercialStatus = ENABLED`).
3. Check that the cart is not empty and that every product is `PUBLISHED`.
4. For each cart line, reserve stock with `ReserveInventoryService` and build an `OrderLine` that freezes `productName` and `unitPrice` and records the `warehouse` the stock was reserved from.
5. If any reservation fails, release the reservations already made (through `ReleaseInventoryReservationUseCase`) and rethrow the error. No order is created.
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

## Command

| Field        | Type        | Description |
| ------------ | ----------- | ----------- |
| orderId      | String      | Order to update. |
| targetStatus | OrderStatus | Next status. In practice `PAID`: `SHIPPED` and `DELIVERED` are rejected. |

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
* The invoice is issued automatically when the order becomes `PAID`; it has no use case of its own. Its `totalAmount` is the sum of `quantity × unitPrice` over the order lines (the prices frozen at confirmation), its `buyer` is the order's buyer, and its identifier is `INV-<orderId>`: an order is paid once, so it has exactly one invoice.

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

## Command

| Field   | Type   | Description |
| ------- | ------ | ----------- |
| orderId | String | Order to cancel. |

**Returns:** `Order`

## Flow

1. Load the order and call `Order.cancel()`.
2. Release the reservation of every line through `ReleaseInventoryReservationUseCase`.
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

## Command

| Field      | Type   | Description |
| ---------- | ------ | ----------- |
| shipmentId | String | Identifier of the new shipment. |
| orderId    | String | Order to ship. |

**Returns:** `Shipment`

## Flow

1. Check that no shipment with `shipmentId` exists.
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
| `DuplicateResourceException` | `shipmentId` is already used, or the order already has a shipment. |
| `BusinessRuleViolationException` | The order is not `PAID`. |

---

# PackShipmentService

## Description

Marks the goods of a shipment as packed (`CREATED` → `PACKED`). The order is not affected.

## Command

| Field      | Type   | Description |
| ---------- | ------ | ----------- |
| shipmentId | String | Shipment to pack. |

**Returns:** `Shipment`

---

# DispatchShipmentService

## Description

Dispatches a packed shipment (`PACKED` → `DISPATCHED`): the stock reserved for each order line leaves its warehouse, and the order moves to `SHIPPED`.

## Command

| Field      | Type   | Description |
| ---------- | ------ | ----------- |
| shipmentId | String | Shipment to dispatch. |

**Returns:** `Shipment`

## Flow

1. Load the shipment; its order comes with it.
2. Call `Shipment.dispatch()` and `Order.advanceTo(SHIPPED)`. Both transitions are validated before any stock is touched.
3. Confirm the outbound of every order line through `ConfirmInventoryOutboundUseCase`.
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

## Command

| Field      | Type   | Description |
| ---------- | ------ | ----------- |
| shipmentId | String | Shipment delivered. |

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

## Command

| Field           | Type   | Description |
| --------------- | ------ | ----------- |
| returnRequestId | String | Identifier of the new return request. |
| orderId         | String | Order to return. |
| reason          | String | Reason given by the buyer. |

**Returns:** `ReturnRequest`

## Flow

1. Authorize the actor as `BUYER`.
2. Load the order and check, through `AuthorizeOrderAccessService`, that it belongs to the actor.
3. Check that the order is `DELIVERED`, that `returnRequestId` is not used, and that the order has no return request yet.
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
| `DuplicateResourceException` | `returnRequestId` is already used, or the order already has a return request. |

---

# ApproveReturnService

## Description

Approves a return request: every order line goes back to the warehouse it was shipped from, and a `PENDING` refund for the order total is created.

## Command

| Field           | Type   | Description |
| --------------- | ------ | ----------- |
| returnRequestId | String | Return request to approve. |

**Returns:** `ReturnRequest`

## Flow

1. Authorize the actor as `ADMINISTRATOR`.
2. Load the return request and call `ReturnRequest.approve(actor)`, validated before any stock is touched.
3. For every order line, register the return of its quantity to its warehouse through `RegisterInventoryReturnUseCase`.
4. Save the return request.
5. Create the refund (`PENDING`, identifier `REF-<returnRequestId>`, amount = the order total) and save it through `RefundRepositoryPort`.

## Business Rules

* Only administrators may approve a return: an order can hold lines of several sellers, so no single seller has authority over the whole return.
* The refund amount is the same total the invoice charged: the sum of `quantity × unitPrice` over the order lines.
* A return request has exactly one refund, so the refund identifier derives from the request's.

---

# RejectReturnService

## Description

Rejects a return request. Stock and refunds are not affected.

## Command

| Field           | Type   | Description |
| --------------- | ------ | ----------- |
| returnRequestId | String | Return request to reject. |

**Returns:** `ReturnRequest`

## Business Rules

* Only administrators may reject a return, for the same reason as approving: an order can hold lines of several sellers.

---

# ProcessRefundService

## Description

Pays a pending refund back to the buyer.

## Command

| Field    | Type   | Description |
| -------- | ------ | ----------- |
| refundId | String | Refund to process. |

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

* Every authenticated operation must be traceable to the `User` who performed it. The security layer (`infrastructure/security`) calls this service for every authenticated request, so traceability does not depend on each use case remembering to call it.

---

# NotifyUserService

## Description

Sends a message to a user through a `NotificationChannel`, via `NotificationPort`. It is meant to be injected into services that need to notify a user of an outcome.

## Operation

`void notify(String userId, NotificationChannel channel, String message)`

> **Note:** no service uses it yet, and the adapter that implements `NotificationPort` is not yet defined.

---

# Authorization Services

## Description

`domain/services/authorization/` holds internal services with a single purpose each: deciding whether the authenticated `User` (the *actor*) may perform an operation. Like every other domain service they are plain classes with a manual constructor and no framework annotations; they are wired as beans in `infrastructure/config`.

Each one exposes an `execute(...)` method that returns normally when the actor is authorized and throws `OperationNotAllowedException` otherwise. Authentication (who the actor is) happens in the security layer; these services only decide what an authenticated actor may do, so the rules live in the domain and are testable without Spring.

## Services

| Service | Operation | Allows | Used by |
| ------- | --------- | ------ | ------- |
| `ValidateUserAuthorizationStatusService` | `execute(User actor)` | A present (non-`null`) actor whose `status` is `ACTIVE`. | Every other authorization service; `ListPublishedProductsService`, `ConsultProductService`, `ListSellerProductsService`, `ListBuyerOrdersService`, `ConsultInventoryService`. |
| `ValidateRoleAuthorizationService` | `execute(User actor, SystemRole... allowedRoles)` | An active actor whose role is one of `allowedRoles`. | `UpdateUserStatusService`, `UpdateSellerStatusService` (`ADMINISTRATOR`); `AdvanceOrderStatusService`, `CreateShipmentService`, `PackShipmentService`, `DispatchShipmentService`, `ConfirmDeliveryService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`); `ConfirmOrderService`, `CancelOrderService` (`BUYER`, `ADMINISTRATOR`); `ConsultUserService` (`ADMINISTRATOR`, `SUPERVISOR`); `RequestReturnService` (`BUYER`); `ApproveReturnService`, `RejectReturnService`, `ProcessRefundService` (`ADMINISTRATOR`). |
| `AuthorizeOrderAccessService` | `execute(User actor, Order order)` | An active `Administrator` or `Supervisor`, or the `Buyer` who placed the order. | `CancelOrderService`, `ConsultOrderService`, `RequestReturnService`. |
| `AuthorizeProductOwnershipService` | `execute(User actor, Product product)` | An active `Administrator`, or the `Seller` who owns the product. | `UpdateProductStatusService`, `UpdateProductVariantsService`, `ConsultProductService` (non-published products only), `ConsultInventoryService`. |
| `AuthorizeWarehouseOperationService` | `execute(User actor, Warehouse warehouse)` | An active `Administrator` or `LogisticsOperator` on any warehouse, or the `Seller` who owns a Seller-owned warehouse. | `DeactivateWarehouseService`, `RegisterInventoryInboundService`, `RegisterInventoryReturnService`, `AdjustInventoryService`, `ReportDamagedInventoryService`. |
| `AuthorizeCartAccessService` | `execute(User actor, ShoppingCart cart)` | An active `Administrator` or `Supervisor`, or the `Buyer` who owns the cart. | `ConfirmOrderService`, `ConsultCartService`. |

## Business Rules

* **The status check comes first.** Every authorization service calls `ValidateUserAuthorizationStatusService` before its own rule, so a missing, `BLOCKED` or `INACTIVE` actor is never authorized.
* **When authorization happens.** A service that authorizes by role alone does it before loading anything. A service that authorizes on an entity (order, cart, product, warehouse) loads that entity first — the rule needs it — and authorizes before changing anything. Inventory services authorize on the warehouse of the affected `InventoryItem`.
* **Ownership is compared by identifier** (`order.getBuyer()`, `cart.getBuyer()`, `product.getSeller()`, `warehouse.getOwner()` against the actor). A missing owner (e.g. a Marketplace-owned warehouse, which has no owning seller) is never matched, so only the roles allowed regardless of ownership get through.
* **Supervisors consult, never change.** The access services allow `Supervisor` so that read operations can reuse them. Use cases that modify an order or a cart (`CancelOrderService`, `ConfirmOrderService`) first restrict the role to `BUYER` / `ADMINISTRATOR` with `ValidateRoleAuthorizationService`, and then apply the access service, which at that point only lets administrators and the owning buyer through.
* **A buyer cannot advance its own order.** `AdvanceOrderStatusService` is restricted to `LOGISTICS_OPERATOR` and `ADMINISTRATOR`.

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is missing or not `ACTIVE`, its role is not allowed, or it does not own the order, cart, product or warehouse. |

---

# Service Collaboration

```text
ConfirmOrderService
   ├── ReserveInventoryService ──────────────> reserve stock for each line
   └── ReleaseInventoryReservationUseCase ───> undo partial reservations on failure

AdvanceOrderStatusService
   └── InvoiceRepositoryPort ────────────────> issue the invoice when the order becomes PAID

DispatchShipmentService
   └── ConfirmInventoryOutboundUseCase ──────> every line, when the order becomes SHIPPED

ApproveReturnService
   ├── RegisterInventoryReturnUseCase ───────> every line back to its warehouse
   └── RefundRepositoryPort ─────────────────> create the PENDING refund

CancelOrderService
   └── ReleaseInventoryReservationUseCase ───> release every line

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
