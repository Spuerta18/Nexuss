# Inventory and Warehouses — Domain Services — NexusMarket

## Introduction

Warehouses, stock changes and stock reservation. Every stock change goes through an `InventoryItem` method and records an `InventoryMovement`.

Part of the [Domain Services](../Domain%20Services.md) catalog, which holds the design conventions, the complete service tables and the collaboration diagram.

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

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `WAREHOUSE_REGISTRATION` | `WAREHOUSE` — the new warehouse | `ownerType`, `ownerId` (only for Seller-owned warehouses) |

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

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `WAREHOUSE_DEACTIVATION` | `WAREHOUSE` — the warehouse | `previousActive`, `newActive` |

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No warehouse exists with the referenced identifier. |
| `OperationNotAllowedException` | The actor may not operate on that warehouse (see `AuthorizeWarehouseOperationService`). |

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
* The four services with an actor (inbound, return, adjustment, damage report) also record an operation through `RegisterOperationAndAuditService` after saving, with `INVENTORY_ITEM` as the affected entity and `productId:warehouseId` (`InventoryItem.getIdentifier()`) as its identifier; see each service. The internal stock services (`ReserveInventoryService`, `ReleaseInventoryReservationService`, `ConfirmInventoryOutboundService`) record none: the order confirmation, cancellation or shipment dispatch that calls them is audited instead.

## Common Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No `InventoryItem` exists for the product in that warehouse. |
| `OperationNotAllowedException` | The actor may not operate on the item's warehouse (services with an actor only). |
| `InsufficientStockException` | The operation needs more units than are available (or reserved, for release and outbound). |
| `InvalidQuantityException` | `quantity` is zero or negative (rejected by `InventoryItem`). |

---

# RegisterInventoryInboundService

## Description

Registers stock received in a warehouse.

## Flow

1. Find the `InventoryItem` for the product and warehouse. If none exists, check that the product is `PHYSICAL` and create the item with zero stock (this is the first time the product is stocked in that warehouse).
2. Check that the warehouse is active.
3. Call `receive(quantity)`, record an `INBOUND` movement, and record the operation (`INVENTORY_INBOUND`).

## Business Rules

* An inactive warehouse cannot receive stock.
* Only `PHYSICAL` products are stocked. The product type is checked when the `InventoryItem` is created; restocking an existing item does not repeat the check.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `INVENTORY_INBOUND` | `INVENTORY_ITEM` — `productId:warehouseId` (`InventoryItem.getIdentifier()`) | `productId`, `warehouseId`, `quantity`, `previousAvailableQuantity`, `newAvailableQuantity` |

## Exceptions

In addition to the common exceptions (except "no inventory item", since it is created):

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The item is new and the product or the warehouse does not exist. |
| `InactiveWarehouseException` | The warehouse is inactive. |
| `InventoryNotTrackedException` | The item is new and the product is `DIGITAL`: digital products neither require nor allow inventory tracking. |

---

# RegisterInventoryReturnService

## Description

Returns units sent back by a buyer to the available stock of an existing inventory record, recording a `RETURN` movement. Used by `ApproveReturnService` for every line of an approved return.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving; when called by `ApproveReturnService`, one per order line, besides the `RETURN_APPROVAL` of the request.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `INVENTORY_RETURN` | `INVENTORY_ITEM` — `productId:warehouseId` (`InventoryItem.getIdentifier()`) | `productId`, `warehouseId`, `quantity`, `previousAvailableQuantity`, `newAvailableQuantity` |

---

# AdjustInventoryService

## Description

Applies a manual correction (positive or negative) to the available stock, e.g. after a physical count, recording an `ADJUSTMENT` movement.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `INVENTORY_ADJUSTMENT` | `INVENTORY_ITEM` — `productId:warehouseId` (`InventoryItem.getIdentifier()`) | `productId`, `warehouseId`, `quantityDelta`, `previousAvailableQuantity`, `newAvailableQuantity` |

---

# ReportDamagedInventoryService

## Description

Moves available units to damaged, so they can no longer be sold or reserved, recording a negative `ADJUSTMENT` movement.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `INVENTORY_DAMAGE_REPORT` | `INVENTORY_ITEM` — `productId:warehouseId` (`InventoryItem.getIdentifier()`) | `productId`, `warehouseId`, `quantity`, `previousDamagedQuantity`, `newDamagedQuantity` |

---

# ReleaseInventoryReservationService

## Description

Returns previously reserved units to the available stock. Used by `CancelOrderService`, and by `ConfirmOrderService` to undo partial reservations.

---

# ConfirmInventoryOutboundService

## Description

Removes previously reserved units from the warehouse because they were sold and dispatched, recording a `SALE_OUTBOUND` movement. Used by `DispatchShipmentService` when an order ships.

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
