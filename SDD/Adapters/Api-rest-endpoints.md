# REST API Endpoints — NexusMarket

## Introduction

The REST API exposes the [role input ports](../Domain/Input-ports.md) over HTTP. There is one controller per port; each controller only depends on its own port and is restricted to its role with `@PreAuthorize` at class level (see [REST Security and CORS](Rest-security-cors.md)).

* Base path: `/api`. Requests and responses are JSON.
* Every endpoint except login and registration needs `Authorization: Bearer <token>`. The **actor** passed to the port is always the authenticated user, never a value from the request.
* Identifiers in the URL (`{id}`) are entity identifiers. Status and type values travel as Value Object codes (e.g. `"SUSPENDED"`, `"PHYSICAL"`, `"COP"`); an unknown code answers `400`.
* Request bodies are validated with Jakarta Validation (see [REST Validation](Rest-validation.md)). Errors use the uniform body described in [Global Exception Handler](Global-exception-handler.md).
* Responses never expose domain models, persistence documents or password hashes; related entities appear by identifier only.

| Controller | Port | Role | Base path |
| ---------- | ---- | ---- | --------- |
| `AuthRestController` | `PublicAccessPort` | public (+ any authenticated user for `/me`) | `/api` |
| `BuyerRestController` | `BuyerPort` | `BUYER` | `/api/buyer` |
| `SellerRestController` | `SellerPort` | `SELLER` | `/api/seller` |
| `AdministratorRestController` | `AdministratorPort` | `ADMINISTRATOR` | `/api/admin` |
| `LogisticsOperatorRestController` | `LogisticsOperatorPort` | `LOGISTICS_OPERATOR` | `/api/logistics` |
| `SupervisorRestController` | `SupervisorPort` | `SUPERVISOR` | `/api/supervisor` |

---

# Public — `AuthRestController`

| Method | Path | Port method | Request | Response | Success |
| ------ | ---- | ----------- | ------- | -------- | ------- |
| POST | `/api/auth/login` | `authenticate` | `LoginRequest` | `AuthResponse` | 200 |
| POST | `/api/auth/register` | `registerBuyer` | `RegisterBuyerRequest` | `UserResponse` | 201 |
| GET | `/api/me` | — (reads the authenticated principal) | — | `UserResponse` | 200 |

A failed login answers `401` with the same message (`"Invalid email or password"`) whether the email or the password was wrong. `/api/me` needs a token but no specific role; the user's own profile is read from the principal, not through a use case.

---

# Buyer — `BuyerRestController` (`BUYER`)

| Method | Path | Port method | Request | Response | Success |
| ------ | ---- | ----------- | ------- | -------- | ------- |
| POST | `/api/buyer/carts` | `createShoppingCart` | `CreateCartRequest` | `CartResponse` | 201 |
| GET | `/api/buyer/carts/{cartId}` | `consultCart` | — | `CartResponse` | 200 |
| POST | `/api/buyer/carts/{cartId}/lines` | `addCartLine` | `AddCartLineRequest` | `CartResponse` | 200 |
| PUT | `/api/buyer/carts/{cartId}/lines/{productId}` | `updateCartLineQuantity` | `QuantityRequest` | `CartResponse` | 200 |
| DELETE | `/api/buyer/carts/{cartId}/lines/{productId}` | `removeCartLine` | — | `CartResponse` | 200 |
| POST | `/api/buyer/carts/{cartId}/orders` | `confirmOrder` | `ConfirmOrderRequest` | `OrderResponse` | 201 |
| GET | `/api/buyer/orders` | `listBuyerOrders` (the buyer is the actor) | — | `OrderResponse[]` | 200 |
| GET | `/api/buyer/orders/{orderId}` | `consultOrder` | — | `OrderResponse` | 200 |
| POST | `/api/buyer/orders/{orderId}/cancel` | `cancelOrder` | — | `OrderResponse` | 200 |
| POST | `/api/buyer/orders/{orderId}/returns` | `requestReturn` | `RequestReturnRequest` | `ReturnRequestResponse` | 201 |
| GET | `/api/buyer/products` | `listPublishedProducts` | — | `ProductResponse[]` | 200 |
| GET | `/api/buyer/products/{productId}` | `consultProduct` | — | `ProductResponse` | 200 |

---

# Seller — `SellerRestController` (`SELLER`)

| Method | Path | Port method | Request | Response | Success |
| ------ | ---- | ----------- | ------- | -------- | ------- |
| POST | `/api/seller/products` | `publishProduct` | `PublishProductRequest` | `ProductResponse` | 201 |
| PATCH | `/api/seller/products/{productId}/status` | `updateProductStatus` | `ProductStatusRequest` | `ProductResponse` | 200 |
| PUT | `/api/seller/products/{productId}/variants` | `updateProductVariants` | `VariantsRequest` | `ProductResponse` | 200 |
| GET | `/api/seller/products` | `listSellerProducts` (the seller is the actor) | — | `ProductResponse[]` | 200 |
| GET | `/api/seller/products/{productId}` | `consultProduct` | — | `ProductResponse` | 200 |
| GET | `/api/seller/products/{productId}/inventory` | `consultInventory` | — | `InventoryItemResponse[]` | 200 |
| POST | `/api/seller/warehouses` | `registerWarehouse` (owner type `SELLER`) | `RegisterWarehouseRequest` | `WarehouseResponse` | 201 |
| POST | `/api/seller/inventory/inbound` | `registerInventoryInbound` | `InventoryMovementRequest` | `InventoryItemResponse` | 200 |
| POST | `/api/seller/inventory/adjustments` | `adjustInventory` | `InventoryAdjustmentRequest` | `InventoryItemResponse` | 200 |

---

# Administrator — `AdministratorRestController` (`ADMINISTRATOR`)

| Method | Path | Port method | Request | Response | Success |
| ------ | ---- | ----------- | ------- | -------- | ------- |
| POST | `/api/admin/sellers` | `registerSeller` | `RegisterStaffRequest` | `UserResponse` | 201 |
| POST | `/api/admin/supervisors` | `registerSupervisor` | `RegisterStaffRequest` | `UserResponse` | 201 |
| POST | `/api/admin/logistics-operators` | `registerLogisticsOperator` | `RegisterStaffRequest` | `UserResponse` | 201 |
| PATCH | `/api/admin/sellers/{sellerId}/status` | `updateSellerStatus` | `SellerStatusRequest` | `UserResponse` | 200 |
| PATCH | `/api/admin/users/{userId}/status` | `updateUserStatus` | `UserStatusRequest` (with `role`) | `UserResponse` | 200 |
| GET | `/api/admin/users/{userId}?role={role}` | `consultUser` | — | `UserResponse` | 200 |
| POST | `/api/admin/warehouses` | `registerMarketplaceWarehouse` (owner type `MARKETPLACE`) | `RegisterWarehouseRequest` | `WarehouseResponse` | 201 |
| POST | `/api/admin/warehouses/{warehouseId}/deactivate` | `deactivateWarehouse` | — | `WarehouseResponse` | 200 |
| POST | `/api/admin/returns/{returnRequestId}/approve` | `approveReturn` | — | `ReturnRequestResponse` | 200 |
| POST | `/api/admin/returns/{returnRequestId}/reject` | `rejectReturn` | — | `ReturnRequestResponse` | 200 |
| POST | `/api/admin/refunds/{refundId}/process` | `processRefund` | — | `RefundResponse` | 200 |

The `role` of the target user is required by the two `/users/{userId}` endpoints; see [REST Adapters](Rest-adapters.md#abstract-user-references).

---

# Logistics Operator — `LogisticsOperatorRestController` (`LOGISTICS_OPERATOR`)

| Method | Path | Port method | Request | Response | Success |
| ------ | ---- | ----------- | ------- | -------- | ------- |
| GET | `/api/logistics/orders/{orderId}` | `consultOrder` | — | `OrderResponse` | 200 |
| POST | `/api/logistics/orders/{orderId}/status` | `advanceOrderStatus` | `AdvanceOrderStatusRequest` | `OrderResponse` | 200 |
| POST | `/api/logistics/shipments` | `createShipment` | `CreateShipmentRequest` | `ShipmentResponse` | 201 |
| POST | `/api/logistics/shipments/{shipmentId}/pack` | `packShipment` | — | `ShipmentResponse` | 200 |
| POST | `/api/logistics/shipments/{shipmentId}/dispatch` | `dispatchShipment` | — | `ShipmentResponse` | 200 |
| POST | `/api/logistics/shipments/{shipmentId}/deliver` | `confirmDelivery` | — | `ShipmentResponse` | 200 |
| POST | `/api/logistics/inventory/damaged` | `reportDamagedInventory` | `InventoryMovementRequest` | `InventoryItemResponse` | 200 |
| POST | `/api/logistics/inventory/returns` | `registerInventoryReturn` | `InventoryMovementRequest` | `InventoryItemResponse` | 200 |
| GET | `/api/logistics/products/{productId}/inventory` | `consultInventory` | — | `InventoryItemResponse[]` | 200 |

---

# Supervisor — `SupervisorRestController` (`SUPERVISOR`, read-only)

| Method | Path | Port method | Response | Success |
| ------ | ---- | ----------- | -------- | ------- |
| GET | `/api/supervisor/orders/{orderId}` | `consultOrder` | `OrderResponse` | 200 |
| GET | `/api/supervisor/products` | `listPublishedProducts` | `ProductResponse[]` | 200 |
| GET | `/api/supervisor/products/{productId}` | `consultProduct` | `ProductResponse` | 200 |
| GET | `/api/supervisor/products/{productId}/inventory` | `consultInventory` | `InventoryItemResponse[]` | 200 |
| GET | `/api/supervisor/users/{userId}?role={role}` | `consultUser` | `UserResponse` | 200 |

---

# Example

```text
POST /api/auth/login
{ "email": "admin@nexus.com", "password": "********" }

200 OK
{ "accessToken": "eyJ...", "tokenType": "Bearer",
  "user": { "identifier": "INITIAL-ADMIN", "fullName": "Initial Administrator", "email": "admin@nexus.com",
            "role": "ADMINISTRATOR", "status": "ACTIVE", "commercialStatus": null, "primaryAddress": null,
            "sellerStatus": null } }

GET /api/admin/users/INITIAL-ADMIN?role=ADMINISTRATOR
Authorization: Bearer eyJ...
```
