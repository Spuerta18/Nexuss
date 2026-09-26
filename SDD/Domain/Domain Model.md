# Domain Model — NexusMarket

## Introduction

The Domain Model represents the core business entities of NexusMarket, a digital marketplace that acts as a commercial intermediary between buyers and sellers. These entities encapsulate the business rules, data, relationships, and lifecycle concepts described in the NexusMarket Functional Business Specification.

The model follows Object-Oriented Design and Domain-Driven Design (DDD) principles. Inheritance is used to represent genuine role specialization, while explicit object relationships are preferred over generic identifier fields.

The model distinguishes between:

* **Users**, which represent any identifiable participant authorized to interact with the system.
* **Buyers**, which represent users who purchase products.
* **Sellers**, which represent users who commercialize products.
* **Warehouses**, which represent physical locations where inventory is managed.
* **Products**, which represent the items published in the catalog.
* **Inventory**, which represents the stock of a product distributed across warehouses.
* **Shopping Carts**, which represent a buyer's provisional product selection.
* **Orders**, which represent a confirmed commercial commitment and its fulfillment lifecycle.

> **Scope note:** the source specification only details the business rules and attributes for the domains **Users, Buyers, Sellers, Warehouses, Catalog, Inventory, and Orders**. Objectives referring to Invoicing, Shipping/Logistics execution, Returns/Refunds, and Administrative Reporting are named in the specification's objectives list but are not developed with attributes or rules in the source document. They are therefore **out of scope for this model** and should be specified in a future iteration before being implemented.

---

# Domain Class Hierarchy

```text
User (Abstract)
├── Buyer
├── Seller
├── LogisticsOperator
├── Administrator
└── Supervisor

Warehouse

Product

InventoryItem

ShoppingCart

Order
```

---

# Domain Relationships

```text
User
   │
   ├── Buyer
   ├── Seller
   ├── LogisticsOperator
   ├── Administrator
   └── Supervisor

Administrator
   └── registers ───────────────> Seller
   └── registers ───────────────> Warehouse (Marketplace-owned)

Seller
   ├── owns ─────────────────────> Warehouse (Seller-owned)
   └── publishes ────────────────> Product

Product
   └── stocked in ───────────────> InventoryItem ──> Warehouse

Buyer
   ├── owns ─────────────────────> ShoppingCart
   └── places ───────────────────> Order

ShoppingCart
   └── contains ─────────────────> CartLine ──> Product

Order
   ├── created from ─────────────> ShoppingCart
   ├── contains ─────────────────> OrderLine ──> Product
   └── reserves/consumes ────────> InventoryItem
```

---

# Entities

---

# User (Abstract)

## Description

Represents any person or system identity authorized to interact with NexusMarket.

This abstract class centralizes the identity and contact information shared by all participants. Each user has exactly one role, which determines their responsibilities and the information they are allowed to access.

This class cannot be instantiated directly.

## Attributes

| Attribute      | Type       | Description                                                                 |
| -------------- | ---------- | ----------------------------------------------------------------------------- |
| identifier     | String     | Unique identifier of the user (national ID or business tax ID).               |
| fullName       | String     | Full name of the person or legal name of the business.                       |
| email          | String     | Primary email address. Unique across the platform.                            |
| role           | SystemRole | Business role that defines the user's responsibilities within the system.     |
| status         | UserStatus | Current operational status of the user (Active, Blocked, etc.).               |

## Relationships

* A `User` is specialized into exactly one of `Buyer`, `Seller`, `LogisticsOperator`, `Administrator`, or `Supervisor`.
* `role` and `status` belong to `User` because they represent the identity and operational meaning of the participant, independent of their specialization.

## Business Rules

* A user has exactly one role within the system (RG-02).
* A user cannot manage information outside the scope of their role (RG-03).
* `identifier` and `email` must be unique across the platform.

---

# Buyer

## Description

Represents a user who purchases products published in the marketplace.

A buyer never manages information belonging to other buyers, nor manages inventory or seller data.

## Inherits From

`User`

## Attributes

| Attribute            | Type                     | Description                                          |
| --------------------- | ------------------------ | ------------------------------------------------------ |
| primaryAddress        | String                   | Default delivery address.                              |
| additionalAddresses   | List\<String\>           | Optional secondary delivery addresses.                  |
| commercialStatus      | CustomerStatus           | Condition of the buyer with respect to making purchases (e.g. able to buy, suspended). |
| carts                 | List\<ShoppingCart\>     | Shopping carts belonging to the buyer. Empty by default. |
| orders                | List\<Order\>            | Orders placed by the buyer. Empty by default.           |

## Relationships

* A buyer owns zero or more `ShoppingCart` instances.
* A buyer places zero or more `Order` instances.
* `carts` and `orders` are not populated by default; they are loaded on demand.

---

# Seller

## Description

Represents a user responsible for publishing and managing products in the marketplace.

Sellers cannot self-register; they are incorporated into the platform by an `Administrator`.

## Inherits From

`User`

## Attributes

| Attribute        | Type                 | Description                                                  |
| ----------------- | --------------------- | --------------------------------------------------------------- |
| sellerStatus       | SellerStatus          | Current operational status of the seller (Active, Suspended, etc.). |
| warehouses         | List\<Warehouse\>     | Warehouses owned by the seller. Empty by default.                |
| products           | List\<Product\>       | Products published by the seller. Empty by default.              |

## Relationships

* A seller owns zero or more `Warehouse` instances.
* A seller publishes zero or more `Product` instances.
* A seller is registered by exactly one `Administrator` (recorded as an audit/operational fact, not as a persistent domain reference).

## Business Rules

* Sellers must be registered by an `Administrator`; self-registration is not allowed.

---

# LogisticsOperator

## Description

Represents a user responsible for the physical operation of warehouses and dispatches.

## Inherits From

`User`

## Relationships

* A `LogisticsOperator` operates on `Warehouse` and `Order` entities during the dispatch and shipping steps of the order lifecycle.

---

# Administrator

## Description

Represents a user responsible for administering sellers and Marketplace-owned warehouses.

## Inherits From

`User`

## Relationships

* An `Administrator` registers `Seller` instances.
* An `Administrator` registers and manages Marketplace-owned `Warehouse` instances.

---

# Supervisor

## Description

Represents a read-only, operational-monitoring role with no transactional authority.

## Inherits From

`User`

## Business Rules

* A `Supervisor` may consult operational information but must not create, modify, or delete business data.

---

# Warehouse

## Description

Represents a physical location where product inventory is managed.

Warehouses are classified by ownership: Marketplace-owned or Seller-owned.

## Attributes

| Attribute   | Type            | Description                                            |
| ------------ | --------------- | --------------------------------------------------------- |
| identifier   | String          | Unique identifier of the warehouse.                        |
| name         | String          | Descriptive name of the warehouse.                          |
| address      | String          | Physical location of the warehouse.                         |
| ownerType    | WarehouseOwnerType | Classification of ownership (Marketplace or Seller).     |
| owner        | Seller          | Owning seller. Present only when `ownerType` is `SELLER`.  |

## Relationships

* A warehouse stores zero or more `InventoryItem` instances.
* A Seller-owned warehouse references exactly one `Seller` as its owner.

---

# Product

## Description

Represents a good, physical or digital, offered for sale in the catalog.

Physical products require inventory tracking and shipping; digital products are delivered immediately after payment confirmation.

## Attributes

| Attribute     | Type            | Description                                            |
| -------------- | --------------- | ---------------------------------------------------------- |
| identifier     | String          | Unique identifier of the product.                            |
| name           | String          | Commercial name of the product.                              |
| productType    | ProductType     | Physical or Digital.                                          |
| variants       | List\<String\>  | Variations such as color, size, or model. May be empty.       |
| status         | ProductStatus   | Published, Suspended, or Discontinued.                        |
| seller         | Seller          | Seller who owns and publishes the product.                    |
| price          | BigDecimal      | Current selling price per unit.                               |

> **Addition:** `price` was not part of the original attribute list. It was added because `OrderLine` must freeze the unit price at the moment an order is confirmed (see `unitPrice` under `OrderLine` below), and there was no other source for that value.

## Relationships

* A product is published by exactly one `Seller`.
* A product is stocked through zero or more `InventoryItem` records (one per warehouse holding it), required only when `productType` is `PHYSICAL`.

---

# InventoryItem

## Description

Represents the distributed stock of a specific product held in a specific warehouse. Every inventory record must be linked to exactly one product and one warehouse.

## Attributes

| Attribute        | Type          | Description                                                  |
| ------------------ | ------------- | ------------------------------------------------------------------ |
| product             | Product       | Product this inventory record belongs to.                          |
| warehouse           | Warehouse     | Warehouse holding the stock.                                        |
| availableQuantity   | Integer       | Quantity available for sale. Must never be negative.                |
| reservedQuantity    | Integer       | Quantity reserved by open orders. Must never be negative.           |

## Relationships

* An `InventoryItem` references exactly one `Product` and exactly one `Warehouse`.

## Business Rules

* Negative stock is not permitted under any circumstance.
* Inventory that is nonexistent or marked as damaged cannot be reserved.
* Recognized movement types are: Inbound (`Ingreso`), Reservation (`Reserva`), Sale Outbound (`Salida por venta`), Adjustment (`Ajuste`), and Return (`Devolución`). Each movement must be traceable to the `InventoryItem` it affects.

---

# ShoppingCart

## Description

Represents a buyer's provisional selection of products prior to confirming an order. A cart has no binding commercial commitment.

## Attributes

| Attribute   | Type              | Description                                     |
| ------------ | ----------------- | -------------------------------------------------- |
| buyer        | Buyer             | Owner of the cart.                                   |
| lines        | List\<CartLine\>  | Products and quantities currently selected.          |

## Relationships

* A `ShoppingCart` belongs to exactly one `Buyer`.
* A `ShoppingCart` is converted into an `Order` when the buyer confirms the purchase; it does not persist after conversion.

---

# CartLine

## Description

Represents a single product selection, with quantity, inside a shopping cart.

## Attributes

| Attribute  | Type     | Description                        |
| ----------- | -------- | -------------------------------------- |
| product     | Product  | Selected product.                        |
| quantity    | Integer  | Quantity selected. Must be greater than zero. |

---

# Order

## Description

Represents the formal commercial commitment between a buyer and one or more sellers. Its lifecycle is the central process of the system.

## Attributes

| Attribute     | Type              | Description                                                |
| -------------- | ----------------- | ---------------------------------------------------------------- |
| identifier     | String            | Unique identifier of the order.                                    |
| buyer          | Buyer             | Buyer who placed the order.                                        |
| lines          | List\<OrderLine\> | Products, quantities, and unit prices confirmed for this order.     |
| status         | OrderStatus       | Current state of the order lifecycle.                              |
| createdAt      | LocalDateTime     | Date and time the order was created.                               |

## Relationships

* An `Order` belongs to exactly one `Buyer`.
* An `Order` contains one or more `OrderLine` instances.
* Each `OrderLine` consumes or reserves stock from the corresponding `InventoryItem`.

## Business Rules

* An order follows the lifecycle: `CART` → `PENDING_PAYMENT` → `PAID` → `SHIPPED` → `DELIVERED`.
* A finalized (`DELIVERED`) order cannot be modified under any circumstance.

---

# OrderLine

## Description

Represents a single confirmed product line within an order, fixing the quantity and unit price at the time of purchase.

## Attributes

| Attribute   | Type     | Description                                   |
| ------------ | -------- | -------------------------------------------------- |
| product      | Product  | Purchased product.                                   |
| quantity     | Integer  | Quantity purchased. Must be greater than zero.        |
| unitPrice    | Decimal  | Price per unit at the time the order was confirmed.    |
| warehouseId  | String   | Identifier of the `Warehouse` the stock for this line was reserved from. |

> **Addition:** `warehouseId` was not part of the original attribute list. It was added because releasing an inventory reservation (e.g. when an order is cancelled) requires knowing exactly which `Warehouse` the stock was reserved from — `InventoryItem` is keyed by `product` + `warehouse`, so the product alone is not enough to identify the reservation to release.

---

# Domain Design Rules

## Users and Roles

* `Buyer`, `Seller`, `LogisticsOperator`, `Administrator`, and `Supervisor` all inherit identity and role information from `User`.
* Every user has exactly one role, and that role is defined at the `User` level, not duplicated in specializations.
* A user must not manage information or data outside the scope of their role.

## Sellers and Warehouses

* Sellers cannot self-register; only an `Administrator` may register a new seller.
* Warehouses are explicitly classified as Marketplace-owned or Seller-owned; a Seller-owned warehouse must reference its owning `Seller`.

## Catalog and Inventory

* A product is either Physical or Digital; only physical products require inventory tracking.
* Every `InventoryItem` must be linked to exactly one `Product` and exactly one `Warehouse`.
* Negative stock is forbidden under any circumstance.
* Damaged or nonexistent inventory cannot be reserved.

## Carts and Orders

* A `ShoppingCart` is provisional and carries no commercial commitment; it becomes an `Order` only upon buyer confirmation.
* Every authenticated operation must be traceable to the `User` who performed it.
* An `Order` follows a strict, forward-only lifecycle and can never be modified once `DELIVERED`.

## Value Objects

* Value Objects are immutable.
* Equality is determined by value, not identity.
* Business entities reference Value Objects instead of primitive strings for controlled business concepts (status, type, role).
* Fixed technical values without business metadata are represented as primitive enumerations instead of Value Objects.