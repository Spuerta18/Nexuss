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

> **Addition:** `CANCELLED` was not part of the original allowed values. It was added because cancelling an order needs a terminal status distinct from `CART` (which means "provisional selection", not "voided order"). It is reached exclusively through `CancelOrderService`, never through `AdvanceOrderStatusService` — the forward-only lifecycle (`CART` → `PENDING_PAYMENT` → `PAID` → `SHIPPED` → `DELIVERED`) never transitions into or out of `CANCELLED`. Once an order reaches `CANCELLED`, it cannot move to any other status.

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