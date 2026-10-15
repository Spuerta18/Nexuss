# REST Validation — NexusMarket

## Introduction

Request bodies are validated **structurally** at the REST boundary with Jakarta Validation (Hibernate Validator), before any port is called. Business validation (stock, ownership, status transitions, uniqueness) stays in the domain.

| Layer | Validates | Failure |
| ----- | --------- | ------- |
| Request DTO (`@Valid`) | Presence, format, length, sign | `400`, `"Validation failed"` + `fieldErrors` |
| REST mapper | Catalog codes (status, type, role, currency, variant attribute) | `400`, e.g. `"Unknown currency 'XYZ'"` |
| Domain (entities, services) | Business rules | `400` / `403` / `404` / `409` (see [Global Exception Handler](Global-exception-handler.md)) |

---

# Rules per DTO

| Request | Constraints |
| ------- | ----------- |
| `LoginRequest` | `email`: `@NotBlank @Email`; `password`: `@NotBlank` (no length rule, so a wrong password and a short one get the same answer) |
| `RegisterBuyerRequest` | `identifier`, `fullName`, `primaryAddress`: `@NotBlank`; `email`: `@NotBlank @Email`; `password`: `@NotBlank @Size(min = 8, max = 72)` |
| `RegisterStaffRequest` | `identifier`, `fullName`: `@NotBlank`; `email`: `@NotBlank @Email`; `password`: `@NotBlank @Size(min = 8, max = 72)` |
| `CreateCartRequest` | `cartId`: `@NotBlank` |
| `AddCartLineRequest` | `productId`: `@NotBlank`; `quantity`: `@NotNull @Positive` |
| `QuantityRequest` | `quantity`: `@NotNull @Positive` |
| `ConfirmOrderRequest` | `orderId`: `@NotBlank` |
| `RequestReturnRequest` | `returnRequestId`: `@NotBlank`; `reason`: `@NotBlank @Size(max = 500)` |
| `PublishProductRequest` | `identifier`, `name`, `productType`: `@NotBlank`; `price`: `@NotNull @Valid`; `variants`: each `@Valid` (may be omitted) |
| `MoneyRequest` | `amount`: `@NotNull @Positive`; `currency`: `@NotBlank` |
| `VariantRequest` | `attribute`, `value`: `@NotBlank` |
| `ProductStatusRequest` | `status`: `@NotBlank` |
| `VariantsRequest` | `variants`: `@NotNull`, each `@Valid` (an empty list removes all variants) |
| `RegisterWarehouseRequest` | `identifier`, `name`, `address`: `@NotBlank` |
| `InventoryMovementRequest` | `productId`, `warehouseId`: `@NotBlank`; `quantity`: `@NotNull @Positive` |
| `InventoryAdjustmentRequest` | `productId`, `warehouseId`: `@NotBlank`; `quantityDelta`: `@NotNull` (positive or negative) |
| `SellerStatusRequest` | `status`: `@NotBlank` |
| `UserStatusRequest` | `role`, `status`: `@NotBlank` |
| `AdvanceOrderStatusRequest` | `targetStatus`: `@NotBlank` |
| `CreateShipmentRequest` | `shipmentId`, `orderId`: `@NotBlank` |

**Passwords** must have 8 to 72 characters: 8 is the minimum length required by the project, and 72 is the BCrypt limit (longer passwords would be silently truncated).

---

# Error Body

A validation failure answers `400` with the uniform body and one entry per invalid field. Nested fields use a dotted path:

```json
{
  "timestamp": "2026-10-15T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/seller/products",
  "requestId": "3f0c…",
  "fieldErrors": [
    { "field": "name", "message": "must not be blank" },
    { "field": "price.amount", "message": "must be greater than 0" }
  ]
}
```

The rejected values are never echoed back, so a password that fails validation never appears in the response.

Other malformed input also answers `400`: an unreadable JSON body (`"Malformed or unreadable request body"`), a missing required query parameter (e.g. `role`), or an unknown catalog code.
