# Carts and Orders — Domain Services — NexusMarket

## Introduction

Shopping carts and the order lifecycle up to payment. The cart rules live in `ShoppingCart` (see [Domain Model](../Domain%20Model.md#shoppingcart)).

Part of the [Domain Services](../Domain%20Services.md) catalog, which holds the design conventions, the complete service tables and the collaboration diagram.

---

# Carts

The cart rules live in `ShoppingCart` (see [Domain Model](../Domain%20Model.md#shoppingcart)): one line per product, only `PUBLISHED` products, a single currency, positive quantities. The cart services only authorize the actor, load the cart (and the product), call the cart method, save the cart and record the operation:

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

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `CART_CREATION` | `SHOPPING_CART` — the new cart | `buyerId` |

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

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `CART_LINE_ADDITION` | `SHOPPING_CART` — the cart | `productId`, `quantity` (units added) |

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `BUYER`, or the cart belongs to another buyer. |
| `EntityNotFoundException` | The cart or the product does not exist. |
| `ProductNotPurchasableException` | The product is not `PUBLISHED`, or has no price. |
| `CurrencyMismatchException` | The product is priced in another currency than the products already in the cart. |
| `InvalidQuantityException` | The resulting quantity is not positive. |

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

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `CART_LINE_QUANTITY_UPDATE` | `SHOPPING_CART` — the cart | `productId`, `newQuantity` |

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `BUYER`, or the cart belongs to another buyer. |
| `EntityNotFoundException` | The cart does not exist, or it has no line for the product. |
| `InvalidQuantityException` | The quantity is not positive. |

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

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `CART_LINE_REMOVAL` | `SHOPPING_CART` — the cart | `productId` |

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
6. Save the order, delete the cart and record the confirmation (`ORDER_CONFIRMATION`). The reservations are covered by this operation; `ReserveInventoryService` records none of its own.

## Business Rules

* A buyer can only confirm an order when their `status` is `ACTIVE` and their `commercialStatus` is `ENABLED`.
* An empty cart cannot be confirmed.
* Only `PUBLISHED` products can be ordered.
* Either every line is reserved or none is: no stock is left reserved for an order that was never created.
* The order keeps the name and price of each product at the time of purchase, even if the product changes later.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `ORDER_CONFIRMATION` | `ORDER` — the new order | `cartId`, `newStatus`, `lineCount` |

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The cart does not exist. |
| `OperationNotAllowedException` | The actor is not an active `BUYER` or `ADMINISTRATOR`, or the cart belongs to another buyer. |
| `BuyerNotEnabledException` | The buyer is not `ACTIVE`, or is suspended from purchasing (`commercialStatus` is not `ENABLED`). |
| `EmptyCartException` | The cart has no lines. |
| `ProductNotPurchasableException` | A product of the cart is not `PUBLISHED`. |
| `InsufficientStockException` | No active warehouse has enough available stock for a line. |

---

# AdvanceOrderStatusService

## Description

Confirms the payment of an order, moving it from `PENDING_PAYMENT` to `PAID`, and issues its `Invoice`. The later steps of the lifecycle (`SHIPPED`, `DELIVERED`) are not reached through this service but through the [shipment lifecycle](fulfillment.md#shipments).

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
5. Record the change (`ORDER_STATUS_ADVANCE`).

## Business Rules

* The lifecycle is strict and forward-only: no step can be skipped or reversed.
* `SHIPPED` and `DELIVERED` can only be reached through `DispatchShipmentService` and `ConfirmDeliveryService`, so an order cannot be marked as shipped without a shipment, or without its stock leaving the warehouse.
* `CANCELLED` cannot be reached by advancing; use `CancelOrderService`.
* A `DELIVERED` or `CANCELLED` order can never be modified.
* The invoice is issued automatically when the order becomes `PAID`; it has no operation of its own. Its `totalAmount` is a `Money`: the sum of `quantity × unitPrice` over the order lines (the prices frozen at confirmation, all in the same currency because the cart allows only one), its `buyer` is the order's buyer, and its identifier is `INV-<order identifier>`: an order is paid once, so it has exactly one invoice.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `ORDER_STATUS_ADVANCE` | `ORDER` — the order | `previousStatus`, `newStatus`, `invoiceId` (only when the invoice is issued) |

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `LOGISTICS_OPERATOR` or `ADMINISTRATOR`. |
| `OrderStatusRequiresShipmentException` | The target is `SHIPPED` or `DELIVERED`. |
| `EntityNotFoundException` | The order does not exist. |
| `InvalidOrderStatusTransitionException` | The target is not the next step, is `CANCELLED`, or the order is already `DELIVERED` / `CANCELLED`. |
| `EmptyOrderException` | The order has no lines, so its invoice cannot be totalled. |

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
3. Save the order and record the cancellation (`ORDER_CANCELLATION`).

## Business Rules

* An order can only be cancelled from `CART` or `PENDING_PAYMENT`, that is, before payment is confirmed.
* Once `CANCELLED`, the order cannot move to any other status.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `ORDER_CANCELLATION` | `ORDER` — the order | `previousStatus`, `newStatus` |

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The order does not exist. |
| `InvalidOrderStatusTransitionException` | The order is `PAID` or later, or already `CANCELLED`. |
