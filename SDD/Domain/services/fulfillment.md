# Fulfillment (Shipments) — Domain Services — NexusMarket

## Introduction

The shipment lifecycle, which drives the last two steps of an order.

Part of the [Domain Services](../Domain%20Services.md) catalog, which holds the design conventions, the complete service tables and the collaboration diagram.

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
* Each step records one operation through `RegisterOperationAndAuditService` after saving, with `SHIPMENT` as the affected entity (see each service).

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
4. Create the shipment (`CREATED`, `createdAt` = now), save it and record it (`SHIPMENT_CREATION`).

## Business Rules

* Only `PAID` orders can be shipped.
* An order has at most one shipment.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `SHIPMENT_CREATION` | `SHIPMENT` — the new shipment | `orderId`, `newStatus` |

## Exceptions

In addition to the common exceptions:

| Exception | When |
| --------- | ---- |
| `DuplicateResourceException` | The shipment identifier is already used, or the order already has a shipment. |
| `OrderNotShippableException` | The order is not `PAID`. |

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

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `SHIPMENT_PACKING` | `SHIPMENT` — the shipment | `previousStatus`, `newStatus` |

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
4. Save the order and the shipment, and record the dispatch (`SHIPMENT_DISPATCH`). The stock outbound is covered by this operation; `ConfirmInventoryOutboundService` records none of its own.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `SHIPMENT_DISPATCH` | `SHIPMENT` — the shipment | `previousStatus`, `newStatus` (shipment), `orderId`, `newOrderStatus` |

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
3. Save the order and the shipment, and record the delivery (`SHIPMENT_DELIVERY`).

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `SHIPMENT_DELIVERY` | `SHIPMENT` — the shipment | `previousStatus`, `newStatus` (shipment), `orderId`, `newOrderStatus` |

## Exceptions

In addition to the common exceptions:

| Exception | When |
| --------- | ---- |
| `InvalidOrderStatusTransitionException` | The order is not `SHIPPED`. |
