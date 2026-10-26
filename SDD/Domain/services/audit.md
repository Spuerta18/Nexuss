# Audit and Notifications — Domain Services — NexusMarket

## Introduction

Services that record and consult the operation history and the audit trail, and notify users.

Every Domain Service that changes the state of the platform records what it did as an `Operation` and an `AuditLog` (see [Domain Model](../Domain%20Model.md)). Both are append-only: they are added, never updated nor deleted. The operation types and affected entity types are the catalogs `OperationType` and `AffectedEntityType` (see [Domain Value Objects](../Domain%20Value%20Objects.md)).

Part of the [Domain Services](../Domain%20Services.md) catalog, which holds the design conventions, the complete service tables and the collaboration diagram.

---

# Recording

---

# RegisterOperationAndAuditService

## Description

Internal service, not exposed through any role port. Every state-changing Domain Service calls it once, **after** saving its change. It persists the `Operation` through `RegisterOperationService`, then builds the `AuditLog` from the saved operation and persists it through `RegisterAuditLogService`.

## Parameters

`execute(Operation operation, Map<String, Object> details)`

| Parameter | Type | Description |
| --------- | ---- | ----------- |
| operation | Operation | Usually built with `Operation.of(OperationType, User performedBy, AffectedEntityType, String affectedEntityId)`, which generates a UUID identifier and sets `executionDate` to now. |
| details   | Map\<String, Object\> | Facts worth tracing, such as `previousStatus` and `newStatus`. Services build it with the package-private helper `AuditDetails.of(key, value, ...)`, which keeps the given order and leaves out `null` values. May be empty. |

**Returns:** `AuditLog`

## Flow

1. Save the `Operation` with `RegisterOperationService`.
2. Build an `AuditLog` with a new UUID `auditId`, the operation's `operationType`, `performedBy`, `affectedEntityType` and `affectedEntityId`, `operationDate` = now, `userRole` = the performer's current role (`null` if there is no performer), and the given `details`.
3. Save it with `RegisterAuditLogService` and return it.

## Business Rules

* Only state-changing operations are recorded; reads never are.
* The operation is recorded after the change is saved, so a failed operation (one whose service throws) records nothing. A failed login is therefore not audited; a successful one is (`USER_LOGIN`).
* The internal stock services (`ReserveInventoryService`, `ReleaseInventoryReservationService`, `ConfirmInventoryOutboundService`) are not audited on their own: the operation that calls them (order confirmation, cancellation, shipment dispatch) is.
* **No credentials in details.** Details never hold passwords, password hashes, tokens or secrets: `AuditLog` rejects a detail key containing `password`, `hash`, `token` or `secret` (case-insensitive) with `IllegalArgumentException`, a programming guard.

---

# RegisterOperationService

## Description

Internal service. Appends an `Operation` to the operation history through `OperationRepositoryPort.save`. Services do not call it directly: they use `RegisterOperationAndAuditService`.

## Operation

`Operation execute(Operation operation)`

---

# RegisterAuditLogService

## Description

Internal service. Appends an `AuditLog` to the audit trail through `AuditLogRepositoryPort.save`. Services do not call it directly: they use `RegisterOperationAndAuditService`.

## Operation

`AuditLog execute(AuditLog auditLog)`

---

# Consultation

---

# ConsultOperationsService

## Description

Returns the whole operation history, most recent first, with no pagination. Exposed as `consultOperations(User)` on `AdministratorPort` and `SupervisorPort`.

## Parameters

`execute(User actor)`

| Parameter | Type | Description |
| --------- | ---- | ----------- |
| actor     | User | Administrator or supervisor consulting the history. |

**Returns:** `List<Operation>` (`OperationRepositoryPort.findAll`)

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `ADMINISTRATOR` or `SUPERVISOR` (`ValidateRoleAuthorizationService`). |

---

# ConsultAuditLogsService

## Description

Returns the whole audit trail, most recent first, with no pagination. Exposed as `consultAuditLogs(User)` on `AdministratorPort` and `SupervisorPort`.

## Parameters

`execute(User actor)`

| Parameter | Type | Description |
| --------- | ---- | ----------- |
| actor     | User | Administrator or supervisor consulting the trail. |

**Returns:** `List<AuditLog>` (`AuditLogRepositoryPort.findAll`)

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `ADMINISTRATOR` or `SUPERVISOR` (`ValidateRoleAuthorizationService`). |

---

# Recorded Operations

One row per state-changing service. Every detail key is listed in the order the service gives it.

| Service | Operation type | Affected entity (identifier) | Performed by | Details |
| ------- | -------------- | ---------------------------- | ------------ | ------- |
| `RegisterBuyerService` | `BUYER_REGISTRATION` | `USER` (new buyer) | The new buyer | `newStatus` |
| `RegisterSellerService` | `SELLER_REGISTRATION` | `USER` (new seller) | Actor | `newStatus` |
| `RegisterSupervisorService` | `SUPERVISOR_REGISTRATION` | `USER` (new supervisor) | Actor | `newStatus` |
| `RegisterLogisticsOperatorService` | `LOGISTICS_OPERATOR_REGISTRATION` | `USER` (new operator) | Actor | `newStatus` |
| `AuthenticateUserService` | `USER_LOGIN` | `USER` (the user) | The authenticated user | — |
| `LogoutService` | `USER_LOGOUT` | `USER` (the user) | The stored user | — |
| `ChangeUserPasswordService` | `PASSWORD_CHANGE` | `USER` (the user) | The stored user | — |
| `UpdateUserStatusService` | `USER_STATUS_CHANGE` | `USER` (target) | Actor | `previousStatus`, `newStatus`, `role` |
| `UpdateSellerStatusService` | `SELLER_STATUS_CHANGE` | `USER` (seller) | Actor | `previousStatus`, `newStatus` (seller status) |
| `PublishProductService` | `PRODUCT_PUBLICATION` | `PRODUCT` (new product) | Actor | `newStatus`, `price`, `currency` |
| `UpdateProductStatusService` | `PRODUCT_STATUS_CHANGE` | `PRODUCT` | Actor | `previousStatus`, `newStatus` |
| `UpdateProductVariantsService` | `PRODUCT_VARIANTS_UPDATE` | `PRODUCT` | Actor | `previousVariantCount`, `newVariantCount` |
| `RegisterWarehouseService` | `WAREHOUSE_REGISTRATION` | `WAREHOUSE` (new warehouse) | Actor | `ownerType`, `ownerId` (seller warehouses only) |
| `DeactivateWarehouseService` | `WAREHOUSE_DEACTIVATION` | `WAREHOUSE` | Actor | `previousActive`, `newActive` |
| `RegisterInventoryInboundService` | `INVENTORY_INBOUND` | `INVENTORY_ITEM` (`productId:warehouseId`) | Actor | `productId`, `warehouseId`, `quantity`, `previousAvailableQuantity`, `newAvailableQuantity` |
| `AdjustInventoryService` | `INVENTORY_ADJUSTMENT` | `INVENTORY_ITEM` | Actor | `productId`, `warehouseId`, `quantityDelta`, `previousAvailableQuantity`, `newAvailableQuantity` |
| `ReportDamagedInventoryService` | `INVENTORY_DAMAGE_REPORT` | `INVENTORY_ITEM` | Actor | `productId`, `warehouseId`, `quantity`, `previousDamagedQuantity`, `newDamagedQuantity` |
| `RegisterInventoryReturnService` | `INVENTORY_RETURN` | `INVENTORY_ITEM` | Actor | `productId`, `warehouseId`, `quantity`, `previousAvailableQuantity`, `newAvailableQuantity` |
| `CreateShoppingCartService` | `CART_CREATION` | `SHOPPING_CART` (new cart) | Actor | `buyerId` |
| `AddCartLineService` | `CART_LINE_ADDITION` | `SHOPPING_CART` | Actor | `productId`, `quantity` |
| `UpdateCartLineQuantityService` | `CART_LINE_QUANTITY_UPDATE` | `SHOPPING_CART` | Actor | `productId`, `newQuantity` |
| `RemoveCartLineService` | `CART_LINE_REMOVAL` | `SHOPPING_CART` | Actor | `productId` |
| `ConfirmOrderService` | `ORDER_CONFIRMATION` | `ORDER` (new order) | Actor | `cartId`, `newStatus`, `lineCount` |
| `CancelOrderService` | `ORDER_CANCELLATION` | `ORDER` | Actor | `previousStatus`, `newStatus` |
| `AdvanceOrderStatusService` | `ORDER_STATUS_ADVANCE` | `ORDER` | Actor | `previousStatus`, `newStatus`, `invoiceId` (only when an invoice is issued) |
| `CreateShipmentService` | `SHIPMENT_CREATION` | `SHIPMENT` (new shipment) | Actor | `orderId`, `newStatus` |
| `PackShipmentService` | `SHIPMENT_PACKING` | `SHIPMENT` | Actor | `previousStatus`, `newStatus` |
| `DispatchShipmentService` | `SHIPMENT_DISPATCH` | `SHIPMENT` | Actor | `previousStatus`, `newStatus`, `orderId`, `newOrderStatus` |
| `ConfirmDeliveryService` | `SHIPMENT_DELIVERY` | `SHIPMENT` | Actor | `previousStatus`, `newStatus`, `orderId`, `newOrderStatus` |
| `RequestReturnService` | `RETURN_REQUEST` | `RETURN_REQUEST` (new request) | Actor | `orderId`, `newStatus` |
| `ApproveReturnService` | `RETURN_APPROVAL` | `RETURN_REQUEST` | Actor | `previousStatus`, `newStatus`, `orderId`, `refundId` |
| `RejectReturnService` | `RETURN_REJECTION` | `RETURN_REQUEST` | Actor | `previousStatus`, `newStatus` |
| `ProcessRefundService` | `REFUND_PROCESSING` | `REFUND` | Actor | `previousStatus`, `newStatus`, `amount`, `currency` |

`ApproveReturnService` also yields one `INVENTORY_RETURN` per order line, recorded by the `RegisterInventoryReturnService` it calls.

---

# Notifications

---

# NotifyUserService

## Description

Sends a message to a user through a `NotificationChannel`, via `NotificationPort`. It is meant to be injected into services that need to notify a user of an outcome.

## Operation

`void notify(String userId, NotificationChannel channel, String message)`

> **Note:** no service uses it yet. `NotificationPort` is backed by `LoggingNotificationAdapter`, which only logs the message (see [Persistence](../../Adapters/Persistence.md)).
