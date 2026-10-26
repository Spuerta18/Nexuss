# Domain Value Objects — NexusMarket

## Introduction

This document defines the controlled business concepts of NexusMarket that are represented as Value Objects, and the fixed technical values represented as primitive enumerations.

A Value Object is used whenever a concept carries business meaning that may require metadata, description, or governance (a fixed catalog of allowed values). A primitive enumeration is used instead when the concept is a purely technical flag with no business metadata.

---

# DomainCatalog (Base concept)

## Description

Represents the common shape of every business catalog defined below: a controlled, named set of allowed values with a code, a display name, and a description. Business entities reference these Value Objects instead of raw strings.

## Attributes

| Attribute    | Type   | Description                          |
| ------------- | ------ | ---------------------------------------- |
| code          | String | Unique code identifying the value.         |
| name          | String | Human-readable name.                        |
| description   | String | Explanation of the value's business meaning. |

## Rules

* Each catalog exposes its allowed values as constants, a `values()` list, and `fromCode(code)`, which returns the constant with that code (or `null` if unknown).
* Persistence mappers must rebuild values with `fromCode`, never with a new instance, so rebuilt entities hold the same constants that the domain compares against.

---

# SystemRole

## Description

Represents the unique business role assigned to a `User`.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code                 | Name                  | Description                                                  |
| --------------------- | ---------------------- | ------------------------------------------------------------------ |
| BUYER                 | Buyer                  | Purchases products published in the marketplace.                     |
| SELLER                | Seller                 | Registers and manages products for sale.                              |
| LOGISTICS_OPERATOR    | Logistics Operator     | Operates warehouses and manages dispatches.                            |
| ADMINISTRATOR         | Administrator          | Manages sellers and Marketplace-owned warehouses.                      |
| SUPERVISOR            | Supervisor             | Read-only operational monitoring role.                                 |

---

# UserStatus

## Description

Represents the operational status of a `User` with respect to system access.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code      | Name      | Description                                     |
| ---------- | --------- | ---------------------------------------------------- |
| ACTIVE     | Active    | The user may access and operate the system.            |
| BLOCKED    | Blocked   | The user's access has been suspended.                   |
| INACTIVE   | Inactive  | The user is registered but not currently operational.    |

---

# CustomerStatus

## Description

Represents the commercial condition of a `Buyer` with respect to making purchases. Independent from `UserStatus`.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code       | Name         | Description                                     |
| ----------- | ------------ | ------------------------------------------------- |
| ENABLED     | Enabled      | The buyer may place orders normally.                  |
| SUSPENDED   | Suspended    | The buyer is temporarily restricted from purchasing.  |

---

# SellerStatus

## Description

Represents the operational condition of a `Seller` within the marketplace.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code       | Name         | Description                                        |
| ----------- | ------------ | ---------------------------------------------------- |
| ACTIVE      | Active       | The seller may publish and sell products.               |
| SUSPENDED   | Suspended    | The seller is temporarily restricted from selling.       |

---

# WarehouseOwnerType

## Description

Represents the ownership classification of a `Warehouse`.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code          | Name           | Description                                  |
| -------------- | -------------- | ------------------------------------------------- |
| MARKETPLACE    | Marketplace    | The warehouse is owned and operated by the platform. |
| SELLER         | Seller         | The warehouse is owned and operated by a seller.       |

---

# ProductType

## Description

Represents whether a product requires physical logistics or is delivered electronically.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code       | Name       | Description                                        |
| ----------- | ---------- | ------------------------------------------------------ |
| PHYSICAL    | Physical   | Requires inventory tracking and shipping.                 |
| DIGITAL     | Digital    | Delivered immediately upon payment confirmation.           |

---

# ProductStatus

## Description

Represents the publication state of a `Product` in the catalog.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code            | Name            | Description                                    |
| ---------------- | --------------- | ------------------------------------------------- |
| PUBLISHED         | Published        | Visible and purchasable in the public catalog.      |
| SUSPENDED         | Suspended        | Temporarily hidden from the catalog.                 |
| DISCONTINUED      | Discontinued     | Permanently removed from sale.                        |

---

# InventoryMovementType

## Description

Represents the type of stock movement applied to an `InventoryItem`.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code             | Name              | Description                                   |
| ----------------- | ----------------- | ---------------------------------------------------- |
| INBOUND            | Inbound            | Stock added to the warehouse.                          |
| RESERVATION        | Reservation        | Stock reserved for a pending order.                     |
| SALE_OUTBOUND      | Sale Outbound      | Stock removed due to a completed sale.                  |
| ADJUSTMENT         | Adjustment         | Manual correction of recorded stock.                     |
| RETURN             | Return             | Stock added back due to a returned product.               |

---

# OrderStatus

## Description

Represents the current stage of an `Order` in its lifecycle.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code               | Name                | Description                                       |
| ------------------- | ------------------- | -------------------------------------------------------- |
| CART                 | Cart                 | Provisional selection, not yet a binding commitment.        |
| PENDING_PAYMENT      | Pending Payment      | Awaiting financial confirmation.                              |
| PAID                 | Paid                 | Payment confirmed; fulfillment process begins.                |
| SHIPPED              | Shipped              | The order has left the warehouse.                             |
| DELIVERED            | Delivered            | The order has been successfully completed. Cannot be modified.|
| CANCELLED            | Cancelled            | The order was cancelled before payment was confirmed.         |

> **Addition:** `CANCELLED` was not part of the original allowed values. It was added because cancelling an order needs a terminal status distinct from `CART` (which means "provisional selection", not "voided order"). It is reached exclusively through `Order.cancel()` (used by `CancelOrderService`), never through `Order.advanceTo()` (used by `AdvanceOrderStatusService`), and only from `CART` or `PENDING_PAYMENT` — the forward-only lifecycle (`CART` → `PENDING_PAYMENT` → `PAID` → `SHIPPED` → `DELIVERED`) never transitions into or out of `CANCELLED`. Once an order reaches `CANCELLED`, it cannot move to any other status.

---

# ShipmentStatus

## Description

Represents the current stage of a `Shipment`. The lifecycle is forward-only, one step at a time, in the order below.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code       | Name       | Description                                                   |
| ---------- | ---------- | ------------------------------------------------------------- |
| CREATED    | Created    | The shipment has been registered for the order.               |
| PACKED     | Packed     | The goods have been packed and are ready to leave the warehouse. |
| DISPATCHED | Dispatched | The shipment has left the warehouse.                          |
| DELIVERED  | Delivered  | The shipment has been delivered to the buyer. Final status.   |

---

# ReturnStatus

## Description

Represents the decision state of a `ReturnRequest`. A request is decided exactly once.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code      | Name      | Description                                                  |
| --------- | --------- | ------------------------------------------------------------ |
| REQUESTED | Requested | The buyer has requested the return; awaiting a decision.     |
| APPROVED  | Approved  | The return has been accepted. Final status.                  |
| REJECTED  | Rejected  | The return has been refused. Final status.                   |

---

# RefundStatus

## Description

Represents the processing state of a `Refund`. A refund is processed exactly once.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code      | Name      | Description                                                 |
| --------- | --------- | ----------------------------------------------------------- |
| PENDING   | Pending   | The refund has been created and awaits processing.          |
| PROCESSED | Processed | The refund has been paid back to the buyer. Final status.   |

> `Invoice` has no status Value Object: it is issued once and has no lifecycle of its own.

---

# Currency

## Description

Represents the currency a `Money` amount is expressed in. Codes follow ISO 4217.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code | Name           | Description                              |
| ---- | -------------- | ---------------------------------------- |
| COP  | Colombian Peso | Official currency of Colombia.           |
| USD  | US Dollar      | Official currency of the United States.  |

---

# OperationType

## Description

Represents the kind of state-changing business operation recorded as an `Operation` and its `AuditLog`. There is one code per operation that changes the state of the platform; reads are never recorded. Each code is recorded by exactly one Domain Service (see [Audit](services/audit.md#recorded-operations)).

## Inherits From

`DomainCatalog`

## Allowed Values

**Users and sessions**

| Code                            | Name                            | Description |
| ------------------------------- | ------------------------------- | ----------- |
| BUYER_REGISTRATION              | Buyer Registration              | A buyer registered themselves. |
| SELLER_REGISTRATION             | Seller Registration             | An administrator registered a seller. |
| SUPERVISOR_REGISTRATION         | Supervisor Registration         | An administrator registered a supervisor. |
| LOGISTICS_OPERATOR_REGISTRATION | Logistics Operator Registration | An administrator registered a logistics operator. |
| USER_LOGIN                      | User Login                      | A user logged in successfully and received a token. |
| USER_LOGOUT                     | User Logout                     | A user logged out; every token issued before is revoked. |
| PASSWORD_CHANGE                 | Password Change                 | A user changed their password; every token issued before is revoked. |
| USER_STATUS_CHANGE              | User Status Change              | An administrator changed the operational status of a user. |
| SELLER_STATUS_CHANGE            | Seller Status Change            | An administrator changed the commercial status of a seller. |

**Catalog**

| Code                    | Name                    | Description |
| ----------------------- | ----------------------- | ----------- |
| PRODUCT_PUBLICATION     | Product Publication     | A seller published a product. |
| PRODUCT_STATUS_CHANGE   | Product Status Change   | The publication status of a product changed. |
| PRODUCT_VARIANTS_UPDATE | Product Variants Update | The variants of a product were replaced. |

**Warehouses and inventory**

| Code                    | Name                    | Description |
| ----------------------- | ----------------------- | ----------- |
| WAREHOUSE_REGISTRATION  | Warehouse Registration  | A warehouse was registered. |
| WAREHOUSE_DEACTIVATION  | Warehouse Deactivation  | A warehouse was deactivated. |
| INVENTORY_INBOUND       | Inventory Inbound       | Stock was received in a warehouse. |
| INVENTORY_ADJUSTMENT    | Inventory Adjustment    | The available stock of a warehouse was corrected. |
| INVENTORY_DAMAGE_REPORT | Inventory Damage Report | Units were reported as damaged and left the sellable stock. |
| INVENTORY_RETURN        | Inventory Return        | Returned units went back to the sellable stock of a warehouse. |

**Carts and orders**

| Code                      | Name                      | Description |
| ------------------------- | ------------------------- | ----------- |
| CART_CREATION             | Cart Creation             | A buyer created a shopping cart. |
| CART_LINE_ADDITION        | Cart Line Addition        | A product was added to a cart. |
| CART_LINE_QUANTITY_UPDATE | Cart Line Quantity Update | The quantity of a cart line changed. |
| CART_LINE_REMOVAL         | Cart Line Removal         | A product was removed from a cart. |
| ORDER_CONFIRMATION        | Order Confirmation        | A cart was turned into an order and its stock reserved. |
| ORDER_CANCELLATION        | Order Cancellation        | An order was cancelled and its reserved stock released. |
| ORDER_STATUS_ADVANCE      | Order Status Advance      | An order moved to its next status outside the shipment lifecycle. |

**Shipments**

| Code              | Name              | Description |
| ----------------- | ----------------- | ----------- |
| SHIPMENT_CREATION | Shipment Creation | The shipment of a paid order was created. |
| SHIPMENT_PACKING  | Shipment Packing  | The goods of a shipment were packed. |
| SHIPMENT_DISPATCH | Shipment Dispatch | A shipment left the warehouse and its order moved to `SHIPPED`. |
| SHIPMENT_DELIVERY | Shipment Delivery | A shipment reached the buyer and its order moved to `DELIVERED`. |

**Returns and refunds**

| Code              | Name              | Description |
| ----------------- | ----------------- | ----------- |
| RETURN_REQUEST    | Return Request    | A buyer requested the return of an order. |
| RETURN_APPROVAL   | Return Approval   | An administrator approved a return request and a refund was created. |
| RETURN_REJECTION  | Return Rejection  | An administrator rejected a return request. |
| REFUND_PROCESSING | Refund Processing | A pending refund was paid to the buyer. |

---

# AffectedEntityType

## Description

Represents the kind of entity changed by an `Operation`. Together with the entity's identifier (`affectedEntityId`) it tells which record an operation and its audit log refer to.

## Inherits From

`DomainCatalog`

## Allowed Values

| Code           | Name           | Description |
| -------------- | -------------- | ----------- |
| USER           | User           | A user of any role. |
| PRODUCT        | Product        | A product of the catalog. |
| WAREHOUSE      | Warehouse      | A Marketplace-owned or Seller-owned warehouse. |
| INVENTORY_ITEM | Inventory Item | The stock of one product in one warehouse; identified as `productId:warehouseId`. |
| SHOPPING_CART  | Shopping Cart  | A buyer's shopping cart. |
| ORDER          | Order          | An order placed by a buyer. |
| SHIPMENT       | Shipment       | The shipment of an order. |
| RETURN_REQUEST | Return Request | A buyer's request to return an order. |
| REFUND         | Refund         | The refund of an approved return. |

---

# Money

## Description

Represents an amount of money in a given `Currency`. It is used for every monetary attribute of the domain: `Product.price`, `OrderLine.unitPrice`, `Invoice.totalAmount` and `Refund.amount`. Unlike the catalogs above, it does not inherit from `DomainCatalog`: its values are not a fixed list.

## Attributes

| Attribute | Type       | Description                         |
| --------- | ---------- | ----------------------------------- |
| amount    | BigDecimal | The amount. Never negative.         |
| currency  | Currency   | Currency the amount is expressed in. |

## Operations

| Operation | Description |
| --------- | ----------- |
| `Money.of(amount, currency)` | Creates a money value. |
| `Money.zero(currency)` | Zero in the given currency. |
| `isPositive()` / `isZero()` | Whether the amount is greater than zero / exactly zero. |
| `add(other)` | Sum of both amounts. Both must be in the same currency. |
| `subtract(other)` | Difference of both amounts. Both must be in the same currency, and the result must not be negative. |

## Rules

* Immutable. A `null` amount, a `null` currency, a negative amount (also as the result of `subtract`) or a `null` operand fail with `InvalidMoneyException`; combining two different currencies in `add` / `subtract` fails with `CurrencyMismatchException`.
* **Equality by value, ignoring scale:** two values are equal when they have the same currency and their amounts compare as equal (`compareTo`), so `10.0 COP` equals `10.00 COP`. The hash code uses the amount without trailing zeros, consistent with that equality.
* Persisted as an embedded `{ amount: Decimal128, currency: "<code>" }` subdocument (see [Persistence](../Adapters/Persistence.md)).

---

# ProductVariant

## Description

Represents one variation of a `Product`, such as color "red" or size "M". Unlike the catalogs above, its values are not a fixed list: the `value` is free text chosen by the seller, while the `attribute` is limited to `VariantAttributeType`.

## Attributes

| Attribute   | Type                 | Description                                   |
| ------------ | -------------------- | ---------------------------------------------- |
| attribute    | VariantAttributeType | Dimension the variant varies by (COLOR, SIZE, MODEL). |
| value        | String               | Concrete value of that dimension (e.g. "red", "M"). |

## Rules

* Immutable and compared by value. Both attributes are required.

---

# Primitive Enumerations

The following concepts are simple enumerations because they represent fixed technical values without requiring business catalog metadata or behavior.

---

## VariantAttributeType

### Description

Represents the fixed technical dimension a product variant can vary by.

### Values

* COLOR
* SIZE
* MODEL

---

## NotificationChannel

### Description

Represents the communication channel used by the system to notify a user.

### Values

* EMAIL
* SMS
* PUSH_NOTIFICATION