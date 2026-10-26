# Authorization — Domain Services — NexusMarket

## Introduction

The authorization services and the authorization changes of the role-based refactor.

Part of the [Domain Services](../Domain%20Services.md) catalog, which holds the design conventions, the complete service tables and the collaboration diagram.

---

# Authorization Services

## Description

`domain/services/authorization/` holds internal services with a single purpose each: deciding whether the authenticated `User` (the *actor*) may perform an operation. Unlike the operation services, they are plain classes with a manual constructor and no framework annotations; they are registered as beans in `infrastructure/config/AuthorizationBeanConfiguration`.

Each one exposes an `execute(...)` method that returns normally when the actor is authorized and throws `OperationNotAllowedException` otherwise. Authentication (who the actor is) happens in the security layer; these services only decide what an authenticated actor may do, so the rules live in the domain and are testable without Spring. They change no state, so they record no operation and no audit log; the operation service that calls them records its own once its change is saved (see [Audit](audit.md)).

## Services

| Service | Operation | Allows | Used by |
| ------- | --------- | ------ | ------- |
| `ValidateUserAuthorizationStatusService` | `execute(User actor)` | A present (non-`null`) actor whose `status` is `ACTIVE`. | Every other authorization service; `ListPublishedProductsService`, `ConsultProductService`, `ListSellerProductsService`, `ListBuyerOrdersService`, `ConsultInventoryService`, `ChangeUserPasswordService` (on the stored actor). |
| `ValidateRoleAuthorizationService` | `execute(User actor, SystemRole... allowedRoles)` | An active actor whose role is one of `allowedRoles`. | `RegisterSellerService`, `RegisterLogisticsOperatorService`, `RegisterSupervisorService`, `UpdateUserStatusService`, `UpdateSellerStatusService` (`ADMINISTRATOR`); `RegisterWarehouseService` (`SELLER`, `ADMINISTRATOR`); `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService`, `ConsultCartService` (`BUYER`); `AdvanceOrderStatusService`, `CreateShipmentService`, `PackShipmentService`, `DispatchShipmentService`, `ConfirmDeliveryService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`); `ConfirmOrderService`, `CancelOrderService` (`BUYER`, `ADMINISTRATOR`); `ConsultUserService`, `ConsultOperationsService`, `ConsultAuditLogsService` (`ADMINISTRATOR`, `SUPERVISOR`); `RequestReturnService` (`BUYER`); `ApproveReturnService`, `RejectReturnService`, `ProcessRefundService` (`ADMINISTRATOR`). |
| `AuthorizeOrderAccessService` | `execute(User actor, Order order)` | An active `Administrator`, `Supervisor` or `LogisticsOperator`, or the `Buyer` who placed the order. | `CancelOrderService`, `ConsultOrderService`, `RequestReturnService`. |
| `AuthorizeProductOwnershipService` | `execute(User actor, Product product)` | An active `Administrator`, or the `Seller` who owns the product. | `UpdateProductStatusService`, `UpdateProductVariantsService`, `ConsultProductService` (non-published products only), `ConsultInventoryService`. |
| `AuthorizeWarehouseOperationService` | `execute(User actor, Warehouse warehouse)` | An active `Administrator` or `LogisticsOperator` on any warehouse, or the `Seller` who owns a Seller-owned warehouse. | `DeactivateWarehouseService`, `RegisterInventoryInboundService`, `RegisterInventoryReturnService`, `AdjustInventoryService`, `ReportDamagedInventoryService`. |
| `AuthorizeCartAccessService` | `execute(User actor, ShoppingCart cart)` | An active `Administrator` or `Supervisor`, or the `Buyer` who owns the cart. | `ConfirmOrderService`, `ConsultCartService`, `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService`. |

## Business Rules

* **The status check comes first.** Every authorization service calls `ValidateUserAuthorizationStatusService` before its own rule, so a missing, `BLOCKED` or `INACTIVE` actor is never authorized.
* **When authorization happens.** A service that authorizes by role alone does it before loading anything. A service that authorizes on an entity (order, cart, product, warehouse) loads that entity first — the rule needs it — and authorizes before changing anything. Inventory services authorize on the warehouse of the affected `InventoryItem`.
* **Ownership is compared by identifier** (`order.getBuyer()`, `cart.getBuyer()`, `product.getSeller()`, `warehouse.getOwner()` against the actor). A missing owner (e.g. a Marketplace-owned warehouse, which has no owning seller) is never matched, so only the roles allowed regardless of ownership get through.
* **Restrict the role first, then check access.** The access services allow `Supervisor` (and, for orders, `LogisticsOperator`) so that read operations can reuse them. Services that need a narrower rule first restrict the role with `ValidateRoleAuthorizationService` and then apply the access service, which at that point only lets through the roles left: `CancelOrderService` and `ConfirmOrderService` (`BUYER`, `ADMINISTRATOR`: administrators and the owning buyer), `RequestReturnService` and every cart service (`BUYER`: only the owning buyer).
* **Carts belong to their buyer.** Every cart operation except confirming an order — `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService` and `ConsultCartService` — is restricted to `BUYER`, so neither administrators nor supervisors can read or change a cart. `CreateShoppingCartService` already requires the actor to be a `Buyer`. `ConfirmOrderService` keeps allowing `ADMINISTRATOR`.
* **A buyer cannot advance its own order.** `AdvanceOrderStatusService` is restricted to `LOGISTICS_OPERATOR` and `ADMINISTRATOR`.
* **Logistics operators can consult orders.** `AuthorizeOrderAccessService` allows `LogisticsOperator` so that `ConsultOrderService` can serve `LogisticsOperatorPort.consultOrder`. This is a deliberate, scoped exception: no other rule of that service changed, and since `CancelOrderService` and `RequestReturnService` restrict the role before calling it, in practice it only affects `ConsultOrderService`.

## Authorization Changes in the Role-Based Refactor

Moving to one input port per role (see [Input Ports](../Input-ports.md)) also tightened or adjusted these rules:

| Service | Before | After |
| ------- | ------ | ----- |
| `RegisterSellerService` | `administratorId` in the command had to reference an `Administrator`; the administrator's `status` was not checked. | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`): the actor must be an active administrator. |
| `RegisterSupervisorService`, `RegisterLogisticsOperatorService` | No authorization. | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`). |
| `RegisterWarehouseService` | `ownerType`, `sellerId` and `administratorId` in the command decided the owner. | `ValidateRoleAuthorizationService` (`SELLER`, `ADMINISTRATOR`), then by role: administrators register Marketplace-owned warehouses; sellers register Seller-owned warehouses owned by themselves. |
| `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService` | No authorization. | `ValidateRoleAuthorizationService` (`BUYER`), then `AuthorizeCartAccessService`. |
| `ConsultCartService` | `AuthorizeCartAccessService` (administrators, supervisors, owning buyer). | `ValidateRoleAuthorizationService` (`BUYER`), then `AuthorizeCartAccessService`: only the owning buyer. |
| `AuthorizeOrderAccessService` | Administrators, supervisors, owning buyer. | Also `LogisticsOperator`, so `ConsultOrderService` serves `LogisticsOperatorPort`. Scoped exception; no other rule changed. |
| `PublishProductService`, `CreateShoppingCartService` | `sellerId` / `buyerId` in the command named the owner. | The actor is the owner: it must be a `Seller` / `Buyer`. |

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is missing or not `ACTIVE`, its role is not allowed, or it does not own the order, cart, product or warehouse. |
