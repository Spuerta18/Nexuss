# Catalog — Domain Services — NexusMarket

## Introduction

Publishing products and changing their status and variants.

Part of the [Domain Services](../Domain%20Services.md) catalog, which holds the design conventions, the complete service tables and the collaboration diagram.

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

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `PRODUCT_PUBLICATION` | `PRODUCT` — the new product | `newStatus`, `price` (plain amount), `currency` |

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | The actor's identifier does not belong to a `Seller`. |
| `SellerNotAuthorizedException` | The seller's `sellerStatus` is not `ACTIVE`. |
| `InvalidProductPriceException` | The product has no price, or its price is zero. |

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

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `PRODUCT_STATUS_CHANGE` | `PRODUCT` — the product | `previousStatus`, `newStatus` |

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

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `PRODUCT_VARIANTS_UPDATE` | `PRODUCT` — the product | `previousVariantCount`, `newVariantCount` |

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No product exists with the referenced identifier. |
| `OperationNotAllowedException` | The actor is neither an active `Administrator` nor the seller who owns the product. |
