# Domain Services — NexusMarket

## Introduction

Domain Services contain the business logic of NexusMarket that does not naturally belong to a single entity: they load entities through output ports, coordinate them, enforce cross-entity rules, and persist the result.

Operation services are concrete Spring `@Service` classes that implement no interface. Their dependencies (output ports, and other services when needed) are received through the constructor and found by component scan. The authorization services and the internal services `ReserveInventoryService`, `RegisterOperationService`, `RegisterAuditLogService`, `RegisterOperationAndAuditService` and `NotifyUserService` are plain classes registered as beans in `infrastructure/config` (see [Software Architecture](../Software%20Architecture/Software%20Architecture.md)).

There are two kinds of services:

* **Operation services** — one per operation. Each exposes a single `execute(...)` method that receives domain models, with the authenticated `User actor` first when the operation must be authorized. They are reached from the [role input ports](Input-ports.md) through the [use case adapters](../Adapters/Use-cases-adapters.md), which only delegate to them.
* **Internal services** — not reached from any role port; they are injected into other services. The authorization services in `domain/services/authorization/` are internal services, and so are `ReleaseInventoryReservationService`, `ConfirmInventoryOutboundService` and the audit services `RegisterOperationService`, `RegisterAuditLogService` and `RegisterOperationAndAuditService`.

---

# Service Documents

The detailed description of each service (parameters, flow, business rules, exceptions) is split by area:

| Area | Document | Services |
| ---- | -------- | -------- |
| Users and Authentication | [users-and-authentication.md](services/users-and-authentication.md) | `AuthenticateUserService`, `LogoutService`, `ChangeUserPasswordService`, `RegisterBuyerService`, `RegisterSellerService`, `RegisterSupervisorService`, `RegisterLogisticsOperatorService`, `UpdateUserStatusService`, `UpdateSellerStatusService` |
| Catalog | [catalog.md](services/catalog.md) | `PublishProductService`, `UpdateProductStatusService`, `UpdateProductVariantsService` |
| Inventory and Warehouses | [inventory.md](services/inventory.md) | `RegisterWarehouseService`, `DeactivateWarehouseService`, `RegisterInventoryInboundService`, `RegisterInventoryReturnService`, `AdjustInventoryService`, `ReportDamagedInventoryService`, `ReleaseInventoryReservationService`, `ConfirmInventoryOutboundService`, `ReserveInventoryService` |
| Carts and Orders | [carts-and-orders.md](services/carts-and-orders.md) | `CreateShoppingCartService`, `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService`, `ConfirmOrderService`, `AdvanceOrderStatusService`, `CancelOrderService` |
| Fulfillment | [fulfillment.md](services/fulfillment.md) | `CreateShipmentService`, `PackShipmentService`, `DispatchShipmentService`, `ConfirmDeliveryService` |
| Returns and Refunds | [returns-and-refunds.md](services/returns-and-refunds.md) | `RequestReturnService`, `ApproveReturnService`, `RejectReturnService`, `ProcessRefundService` |
| Audit and Notifications | [audit.md](services/audit.md) | `RegisterOperationService`, `RegisterAuditLogService`, `RegisterOperationAndAuditService`, `ConsultOperationsService`, `ConsultAuditLogsService`, `NotifyUserService` |
| Authorization | [authorization.md](services/authorization.md) | `ValidateUserAuthorizationStatusService`, `ValidateRoleAuthorizationService`, `AuthorizeOrderAccessService`, `AuthorizeProductOwnershipService`, `AuthorizeWarehouseOperationService`, `AuthorizeCartAccessService` |

The read-only services (queries) are summarized in [Queries](#queries) below; their rules are documented there.

---

# Design Conventions

* **One operation, one service.** Each operation has one concrete service class with a single `execute(...)` method. When several role ports expose the same operation (e.g. `consultProduct`), they all delegate to the same service, so the rule is written once.
* **Services receive domain models.** A parameter is either a **reference** — a model carrying only the identifier of an existing entity, which the service loads through its output port (`Order orderReference`) — or a **new model** carrying the data of the entity to create (`Product product`). When creating, the service builds the stored entity itself, so the initial status, owner and timestamps are always set by the domain. Plain values (`int quantity`, `OrderStatus targetStatus`, `String password`) are separate parameters.
* **Authorized operations receive the actor.** Operations restricted to some users take the authenticated `User actor` as first parameter. The service authorizes the actor through an authorization service before it changes anything (see [Authorization Services](services/authorization.md#authorization-services)). Only `AuthenticateUserService`, `RegisterBuyerService` and the internal services have no actor. `LogoutService` and `ChangeUserPasswordService` receive the actor but only act on the actor's own stored account.
* **Entities protect their own invariants.** Services never change entity state through setters: they call the entity methods (`InventoryItem.receive`, `reserve`, `releaseReservation`, `confirmOutbound`, `adjust`, `markDamaged`; `Order.advanceTo`, `cancel`; `ShoppingCart.addLine`, `updateLineQuantity`, `removeLine`; `Product.changeStatus`, `replaceVariants`; `Warehouse.deactivate`). The rules of a single entity live in that entity (see [Domain Model](Domain%20Model.md)); services add authorization, loading and the rules that need more than one entity (e.g. the warehouse must be active, the seller must be `ACTIVE`).
* **Services inject other services directly.** When a service needs another one, it receives the concrete class (e.g. `CancelOrderService` → `ReleaseInventoryReservationService`). Services never depend on input ports: ports are the entry point from outside, not a way for the domain to call itself.
* **Every stock change records an `InventoryMovement`.** Each inventory service saves the `InventoryItem` and then an `InventoryMovement` through `InventoryMovementRepositoryPort`.
* **Every state change is audited.** Each state-changing service calls `RegisterOperationAndAuditService.execute(Operation, Map<String, Object> details)` once, **after** saving the change, with `Operation.of(OperationType, performedBy, AffectedEntityType, affectedEntityId)` and details built with the package-private helper `AuditDetails.of(key, value, ...)` (pairs kept in order; `null` values are left out). Status changes carry `previousStatus` and `newStatus`; other facts (identifiers, quantities, amounts) are added where they help to trace the operation. Details never hold passwords, hashes or tokens: `AuditLog` rejects such keys. Reads are not audited, and nothing is recorded when the service throws. The internal stock services (`ReserveInventoryService`, `ReleaseInventoryReservationService`, `ConfirmInventoryOutboundService`) are not audited on their own: the operation that calls them is. Each service card states the operation it records; see [Audit](services/audit.md).
* **Lookups fail with `EntityNotFoundException`.** When an identifier refers to a user of the wrong role (e.g. a `Seller` reference whose identifier belongs to a `Buyer`), the service also throws `EntityNotFoundException`, so the role of other accounts is not revealed.
* **One exception per business rule.** Every broken rule throws its own subclass of `DomainException` (`domain/exceptions`), named after the rule — for example `EmptyCartException`, `ProductNotPurchasableException`, `CurrencyMismatchException`, `BuyerNotEnabledException`, `ReturnNotEligibleException`, `ReturnAlreadyDecidedException`, `RefundAlreadyProcessedException`, `OrderNotShippableException`, `EmptyOrderException`, `OrderStatusRequiresShipmentException`, `InactiveWarehouseException`, `InventoryNotTrackedException`, `InvalidProductPriceException`, `InvalidMoneyException`, `InvalidPasswordException` and `IncorrectCurrentPasswordException` — besides `EntityNotFoundException`, `DuplicateResourceException`, `InsufficientStockException`, `InvalidOrderStatusTransitionException`, `InvalidShipmentStatusTransitionException`, `OperationNotAllowedException`, `SellerNotAuthorizedException` and `InvalidCredentialsException`. The REST layer maps each one to its HTTP status (see [Global Exception Handler](../Adapters/Global-exception-handler.md)). The remaining `IllegalArgumentException`s are programming guards (e.g. a `null` status), not business rules.
* **Invalid quantities** are rejected by the entities (`CartLine`, `OrderLine`, `InventoryItem`, `ShoppingCart`) with `InvalidQuantityException`. `ShoppingCart` throws it with the cart and product in the message.
* **Money is a `Money` value.** Prices and totals carry their `Currency`; amounts of different currencies are never added together (see [Money](Domain%20Value%20Objects.md#money)).

---

# Service Catalog

`Role ports` lists the [role input ports](Input-ports.md) that expose the operation. Every service of this table outside the `Internal` and `Authorization` rows changes state and therefore also depends on `RegisterOperationAndAuditService`; it is left out of the dependencies column for readability. A service's authorization may accept more roles than the ports currently expose (e.g. `ADMINISTRATOR` in the shipment services); the port decides what a role can reach, the service decides what it is allowed to do.

| Area       | Service | Role ports | Output ports / dependencies | Authorization |
| ---------- | ------- | ---------- | ---------------------------- | ------------- |
| Users      | `AuthenticateUserService` | `PublicAccessPort` | `UserRepositoryPort`, `PasswordHasherPort`, `TokenServicePort` | — |
| Users      | `LogoutService` | `BuyerPort`, `SellerPort`, `AdministratorPort`, `LogisticsOperatorPort`, `SupervisorPort` | `UserRepositoryPort` | — (acts only on the actor's own account) |
| Users      | `ChangeUserPasswordService` | `BuyerPort`, `SellerPort`, `AdministratorPort`, `LogisticsOperatorPort`, `SupervisorPort` | `UserRepositoryPort`, `PasswordHasherPort` | `ValidateUserAuthorizationStatusService` (the stored actor must be `ACTIVE`) |
| Users      | `RegisterBuyerService` | `PublicAccessPort` | `UserRepositoryPort`, `PasswordHasherPort` | — |
| Users      | `RegisterSellerService` | `AdministratorPort` | `UserRepositoryPort`, `PasswordHasherPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Users      | `RegisterLogisticsOperatorService` | `AdministratorPort` | `UserRepositoryPort`, `PasswordHasherPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Users      | `RegisterSupervisorService` | `AdministratorPort` | `UserRepositoryPort`, `PasswordHasherPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Users      | `UpdateUserStatusService` | `AdministratorPort` | `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Users      | `UpdateSellerStatusService` | `AdministratorPort` | `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Warehouses | `RegisterWarehouseService` | `AdministratorPort`, `SellerPort` | `WarehouseRepositoryPort`, `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`SELLER`, `ADMINISTRATOR`) + own rule by role |
| Warehouses | `DeactivateWarehouseService` | `AdministratorPort` | `WarehouseRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Catalog    | `PublishProductService` | `SellerPort` | `ProductRepositoryPort`, `UserRepositoryPort` | Own rule: the actor must be a `Seller` (it becomes the product's seller) |
| Catalog    | `UpdateProductStatusService` | `SellerPort` | `ProductRepositoryPort` | `AuthorizeProductOwnershipService` |
| Catalog    | `UpdateProductVariantsService` | `SellerPort` | `ProductRepositoryPort` | `AuthorizeProductOwnershipService` |
| Inventory  | `RegisterInventoryInboundService` | `SellerPort` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort`, `ProductRepositoryPort`, `WarehouseRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Inventory  | `RegisterInventoryReturnService` | `LogisticsOperatorPort` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Inventory  | `AdjustInventoryService` | `SellerPort` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Inventory  | `ReportDamagedInventoryService` | `LogisticsOperatorPort` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | `AuthorizeWarehouseOperationService` |
| Carts      | `CreateShoppingCartService` | `BuyerPort` | `CartRepositoryPort`, `UserRepositoryPort` | Own rule: the actor must be a `Buyer` (it becomes the cart's owner) |
| Carts      | `AddCartLineService` | `BuyerPort` | `CartRepositoryPort`, `ProductRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeCartAccessService` |
| Carts      | `UpdateCartLineQuantityService` | `BuyerPort` | `CartRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeCartAccessService` |
| Carts      | `RemoveCartLineService` | `BuyerPort` | `CartRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeCartAccessService` |
| Orders     | `ConfirmOrderService` | `BuyerPort` | `CartRepositoryPort`, `OrderRepositoryPort`, `ReserveInventoryService`, `ReleaseInventoryReservationService` | `ValidateRoleAuthorizationService` (`BUYER`, `ADMINISTRATOR`), `AuthorizeCartAccessService` |
| Orders     | `AdvanceOrderStatusService` | `LogisticsOperatorPort` | `OrderRepositoryPort`, `InvoiceRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Orders     | `CancelOrderService` | `BuyerPort` | `OrderRepositoryPort`, `ReleaseInventoryReservationService` | `ValidateRoleAuthorizationService` (`BUYER`, `ADMINISTRATOR`), `AuthorizeOrderAccessService` |
| Shipments  | `CreateShipmentService` | `LogisticsOperatorPort` | `ShipmentRepositoryPort`, `OrderRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Shipments  | `PackShipmentService` | `LogisticsOperatorPort` | `ShipmentRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Shipments  | `DispatchShipmentService` | `LogisticsOperatorPort` | `ShipmentRepositoryPort`, `OrderRepositoryPort`, `ConfirmInventoryOutboundService` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Shipments  | `ConfirmDeliveryService` | `LogisticsOperatorPort` | `ShipmentRepositoryPort`, `OrderRepositoryPort` | `ValidateRoleAuthorizationService` (`LOGISTICS_OPERATOR`, `ADMINISTRATOR`) |
| Returns    | `RequestReturnService` | `BuyerPort` | `ReturnRequestRepositoryPort`, `OrderRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeOrderAccessService` |
| Returns    | `ApproveReturnService` | `AdministratorPort` | `ReturnRequestRepositoryPort`, `RefundRepositoryPort`, `RegisterInventoryReturnService` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Returns    | `RejectReturnService` | `AdministratorPort` | `ReturnRequestRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Returns    | `ProcessRefundService` | `AdministratorPort` | `RefundRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`) |
| Internal   | `ReserveInventoryService` | — | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | — |
| Internal   | `ReleaseInventoryReservationService` | — | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | — (no actor) |
| Internal   | `ConfirmInventoryOutboundService` | — | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort` | — (no actor) |
| Internal   | `RegisterOperationService` | — | `OperationRepositoryPort` | — |
| Internal   | `RegisterAuditLogService` | — | `AuditLogRepositoryPort` | — |
| Internal   | `RegisterOperationAndAuditService` | — | `RegisterOperationService`, `RegisterAuditLogService` | — |
| Internal   | `NotifyUserService` | — | `NotificationPort` | — |
| Authorization | `ValidateUserAuthorizationStatusService` | — | — | — |
| Authorization | `ValidateRoleAuthorizationService` | — | `ValidateUserAuthorizationStatusService` | — |
| Authorization | `AuthorizeOrderAccessService` | — | `ValidateUserAuthorizationStatusService` | — |
| Authorization | `AuthorizeProductOwnershipService` | — | `ValidateUserAuthorizationStatusService` | — |
| Authorization | `AuthorizeWarehouseOperationService` | — | `ValidateUserAuthorizationStatusService` | — |
| Authorization | `AuthorizeCartAccessService` | — | `ValidateUserAuthorizationStatusService` | — |

---

# Queries

Read-only operations. They follow the same conventions as every other service — `execute(User actor, …)` with domain model references — and never modify data. Supervisors, a read-only role, are allowed wherever the rule below says so.

| Area       | Service | Role ports | Output ports / dependencies | Authorization |
| ---------- | ------- | ---------- | ---------------------------- | ------------- |
| Catalog    | `ListPublishedProductsService` | `BuyerPort`, `SupervisorPort` | `ProductRepositoryPort` | `ValidateUserAuthorizationStatusService` (any active user) |
| Catalog    | `ConsultProductService` | `BuyerPort`, `SellerPort`, `SupervisorPort` | `ProductRepositoryPort` | `ValidateUserAuthorizationStatusService` (any active user); if not `PUBLISHED`, also `AuthorizeProductOwnershipService` |
| Catalog    | `ListSellerProductsService` | `SellerPort` | `ProductRepositoryPort` | `ValidateUserAuthorizationStatusService` + own rule: `Administrator`, `Supervisor`, or the `Seller` itself |
| Carts      | `ConsultCartService` | `BuyerPort` | `CartRepositoryPort` | `ValidateRoleAuthorizationService` (`BUYER`), `AuthorizeCartAccessService` |
| Orders     | `ConsultOrderService` | `BuyerPort`, `LogisticsOperatorPort`, `SupervisorPort` | `OrderRepositoryPort` | `AuthorizeOrderAccessService` (administrators, supervisors, logistics operators, or the owning buyer) |
| Orders     | `ListBuyerOrdersService` | `BuyerPort` | `OrderRepositoryPort` | `ValidateUserAuthorizationStatusService` + own rule: `Administrator`, `Supervisor`, or the `Buyer` itself |
| Inventory  | `ConsultInventoryService` | `SellerPort`, `LogisticsOperatorPort`, `SupervisorPort` | `ProductRepositoryPort`, `InventoryRepositoryPort` | `Supervisor` / `LogisticsOperator`: `ValidateUserAuthorizationStatusService`; anyone else: `AuthorizeProductOwnershipService` (`Administrator` or owning `Seller`) |
| Users      | `ConsultUserService` | `AdministratorPort`, `SupervisorPort` | `UserRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`, `SUPERVISOR`) |
| Audit      | `ConsultOperationsService` | `AdministratorPort`, `SupervisorPort` | `OperationRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`, `SUPERVISOR`) |
| Audit      | `ConsultAuditLogsService` | `AdministratorPort`, `SupervisorPort` | `AuditLogRepositoryPort` | `ValidateRoleAuthorizationService` (`ADMINISTRATOR`, `SUPERVISOR`) |

| Service | Parameters | Returns | Notes |
| ------- | ---------- | ------- | ----- |
| `ListPublishedProductsService` | `actor` | `List<Product>` | Only `PUBLISHED` products. |
| `ConsultProductService` | `actor`, `Product productReference` | `Product` | A `PUBLISHED` product is visible to any active user; a `SUSPENDED` or `DISCONTINUED` one only to an `Administrator` or its owning `Seller`. `EntityNotFoundException` if it does not exist. |
| `ListSellerProductsService` | `actor`, `Seller seller` (reference) | `List<Product>` | Every status. Empty if the seller has no products. |
| `ConsultCartService` | `actor`, `ShoppingCart cartReference` | `ShoppingCart` | The role is checked first, then the cart is loaded and authorized: only its owning buyer can read it. |
| `ConsultOrderService` | `actor`, `Order orderReference` | `Order` | The order is loaded first, then authorized. |
| `ListBuyerOrdersService` | `actor`, `Buyer buyer` (reference) | `List<Order>` | Most recent first. |
| `ConsultInventoryService` | `actor`, `Product productReference` | `List<InventoryItem>` | One item per warehouse holding the product. The product is loaded first, then authorized. |
| `ConsultUserService` | `actor`, `User target` (reference) | `User` | Authorized by role before loading. `target` is the abstract `User`; see [Input Ports](Input-ports.md#abstract-user-parameters). |
| `ConsultOperationsService` | `actor` | `List<Operation>` | The whole operation history, most recent first; no pagination. |
| `ConsultAuditLogsService` | `actor` | `List<AuditLog>` | The whole audit trail, most recent first; no pagination. |

The "own rule" of `ListSellerProductsService` and `ListBuyerOrdersService` compares the identifier of the referenced seller or buyer with the actor, not a loaded entity, so no existing authorization service fits; it is a private method of each service until another service needs it.

---

# Service Collaboration

```text
ConfirmOrderService
   ├── ReserveInventoryService ──────────────> reserve stock for each line
   └── ReleaseInventoryReservationService ───> undo partial reservations on failure

AdvanceOrderStatusService
   └── InvoiceRepositoryPort ────────────────> issue the invoice when the order becomes PAID

DispatchShipmentService
   └── ConfirmInventoryOutboundService ──────> every line, when the order becomes SHIPPED

ApproveReturnService
   ├── RegisterInventoryReturnService ───────> every line back to its warehouse, one INVENTORY_RETURN each
   └── RefundRepositoryPort ─────────────────> create the PENDING refund

CancelOrderService
   └── ReleaseInventoryReservationService ───> release every line

Every state-changing service
   └── RegisterOperationAndAuditService ─────> after saving: the Operation, then its AuditLog
          ├── RegisterOperationService ──────> OperationRepositoryPort
          └── RegisterAuditLogService ───────> AuditLogRepositoryPort
```

## Stock Lifecycle of an Order Line

```text
available ──reserve──> reserved ──confirmOutbound──> (leaves the warehouse)
    ▲                     │
    └─releaseReservation──┘
```

| Order event | Service | Stock effect |
| ----------- | ------- | ------------ |
| Order confirmed | `ConfirmOrderService` → `ReserveInventoryService` | available → reserved |
| Order cancelled | `CancelOrderService` → `ReleaseInventoryReservationService` | reserved → available |
| Order shipped | `DispatchShipmentService` → `ConfirmInventoryOutboundService` | reserved → out |
