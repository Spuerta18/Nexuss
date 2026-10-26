# Output Ports — NexusMarket

## Introduction

Output ports are the contracts the domain uses to reach anything outside it: persistence, password hashing, token issuing, audit and notifications. They live in `domain/ports/out` and are implemented by adapters in `adapters/out/persistence/mongodb/adapters` (MongoDB, notifications) and `infrastructure/security` (BCrypt, JWT). The Domain Services only know the interfaces (see [Domain Services](Domain%20Services.md) and [Persistence](../Adapters/Persistence.md)).

```text
domain/ports/out/
├── UserRepositoryPort.java               users
├── WarehouseRepositoryPort.java          warehouses
├── ProductRepositoryPort.java            products
├── InventoryRepositoryPort.java          inventory_items
├── InventoryMovementRepositoryPort.java  inventory_movements
├── CartRepositoryPort.java               shopping_carts
├── OrderRepositoryPort.java              orders
├── ShipmentRepositoryPort.java           shipments
├── InvoiceRepositoryPort.java            invoices
├── ReturnRequestRepositoryPort.java      return_requests
├── RefundRepositoryPort.java             refunds
├── OperationRepositoryPort.java          operations (append-only)
├── AuditLogRepositoryPort.java           audit_logs (append-only)
├── PasswordHasherPort.java               BCrypt
├── TokenServicePort.java                 JWT
└── NotificationPort.java                 log (no real channel yet)
```

---

# Conventions

* **Domain types only.** Ports receive and return Domain Models, never documents or framework types. Lookups return `Optional` instead of `null`.
* **`save` returns the domain object** that was stored. Adapters map it to a document and back.
* **Identifiers are plain `String`s** in finder parameters (`findByOrderId(String orderId)`), matching the references stored by ID in MongoDB.
* **One adapter per port.** Every port has exactly one production implementation, found by component scan.
* **Audit ports are append-only.** `OperationRepositoryPort` and `AuditLogRepositoryPort` only add entries and read them; they declare no update or delete. Their Spring Data repositories extend the bare `Repository` and declare only `insert` and reads, so an existing identifier is never overwritten.

---

# Port Catalog

| Port | Implemented by | Used by |
| ---- | -------------- | ------- |
| `UserRepositoryPort` | `MongoUserRepositoryAdapter` | `AuthenticateUserService`, `LogoutService`, `ChangeUserPasswordService`, `RegisterBuyerService`, `RegisterSellerService`, `RegisterSupervisorService`, `RegisterLogisticsOperatorService`, `UpdateUserStatusService`, `UpdateSellerStatusService`, `ConsultUserService`, `CreateShoppingCartService`, `PublishProductService`, `RegisterWarehouseService`; `NexusUserDetailsService` (security); `InitialAdministratorSeeder` (seed) |
| `WarehouseRepositoryPort` | `MongoWarehouseRepositoryAdapter` | `RegisterWarehouseService`, `DeactivateWarehouseService`, `RegisterInventoryInboundService` |
| `ProductRepositoryPort` | `MongoProductRepositoryAdapter` | `PublishProductService`, `UpdateProductStatusService`, `UpdateProductVariantsService`, `ConsultProductService`, `ListPublishedProductsService`, `ListSellerProductsService`, `ConsultInventoryService`, `RegisterInventoryInboundService`, `AddCartLineService` |
| `InventoryRepositoryPort` | `MongoInventoryRepositoryAdapter` | `RegisterInventoryInboundService`, `RegisterInventoryReturnService`, `AdjustInventoryService`, `ReportDamagedInventoryService`, `ReserveInventoryService`, `ReleaseInventoryReservationService`, `ConfirmInventoryOutboundService`, `ConsultInventoryService` |
| `InventoryMovementRepositoryPort` | `MongoInventoryMovementRepositoryAdapter` | Every inventory service that changes stock (the seven above except `ConsultInventoryService`) |
| `CartRepositoryPort` | `MongoCartRepositoryAdapter` | `CreateShoppingCartService`, `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService`, `ConsultCartService`, `ConfirmOrderService` |
| `OrderRepositoryPort` | `MongoOrderRepositoryAdapter` | `ConfirmOrderService`, `AdvanceOrderStatusService`, `CancelOrderService`, `ConsultOrderService`, `ListBuyerOrdersService`, `CreateShipmentService`, `DispatchShipmentService`, `ConfirmDeliveryService`, `RequestReturnService` |
| `ShipmentRepositoryPort` | `MongoShipmentRepositoryAdapter` | `CreateShipmentService`, `PackShipmentService`, `DispatchShipmentService`, `ConfirmDeliveryService` |
| `InvoiceRepositoryPort` | `MongoInvoiceRepositoryAdapter` | `AdvanceOrderStatusService` |
| `ReturnRequestRepositoryPort` | `MongoReturnRequestRepositoryAdapter` | `RequestReturnService`, `ApproveReturnService`, `RejectReturnService` |
| `RefundRepositoryPort` | `MongoRefundRepositoryAdapter` | `ApproveReturnService`, `ProcessRefundService` |
| `OperationRepositoryPort` | `MongoOperationRepositoryAdapter` | `RegisterOperationService`, `ConsultOperationsService` |
| `AuditLogRepositoryPort` | `MongoAuditLogRepositoryAdapter` | `RegisterAuditLogService`, `ConsultAuditLogsService` |
| `PasswordHasherPort` | `BCryptPasswordHasherAdapter` (`infrastructure/security`) | `AuthenticateUserService`, `ChangeUserPasswordService`, the four registration services; `InitialAdministratorSeeder` (seed) |
| `TokenServicePort` | `JwtTokenServiceAdapter` (`infrastructure/security`) | `AuthenticateUserService` |
| `NotificationPort` | `LoggingNotificationAdapter` (writes to the log) | `NotifyUserService` (not used by any service yet) |

The internal services `ReserveInventoryService`, `RegisterOperationService`, `RegisterAuditLogService` and `NotifyUserService` receive their ports in `infrastructure/config/InternalServiceBeanConfiguration`; every other user receives them by constructor injection.

---

# Port Definitions

## Persistence

```java
public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(String identifier);
    Optional<User> findByEmail(String email);
    boolean existsById(String identifier);
    boolean existsByEmail(String email);
    /** Whether at least one user has {@code role}. */
    boolean existsByRole(SystemRole role);
}

public interface WarehouseRepositoryPort {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findById(String identifier);
}

public interface ProductRepositoryPort {
    Product save(Product product);
    Optional<Product> findById(String identifier);
    List<Product> findBySellerId(String sellerId);
    List<Product> findAllPublished();
}

public interface InventoryRepositoryPort {
    InventoryItem save(InventoryItem item);
    Optional<InventoryItem> findByProductIdAndWarehouseId(String productId, String warehouseId);
    List<InventoryItem> findAllByProductId(String productId);
}

public interface InventoryMovementRepositoryPort {
    InventoryMovement save(InventoryMovement movement);
    List<InventoryMovement> findAllByProductIdAndWarehouseId(String productId, String warehouseId);
}

public interface CartRepositoryPort {
    ShoppingCart save(ShoppingCart cart);
    Optional<ShoppingCart> findById(String identifier);
    List<ShoppingCart> findByBuyerId(String buyerId);
    void deleteById(String identifier);
}

public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(String identifier);
    List<Order> findAllByBuyerId(String buyerId);
}

public interface ShipmentRepositoryPort {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(String identifier);
    Optional<Shipment> findByOrderId(String orderId);
}

public interface InvoiceRepositoryPort {
    Invoice save(Invoice invoice);
    Optional<Invoice> findByOrderId(String orderId);
}

public interface ReturnRequestRepositoryPort {
    ReturnRequest save(ReturnRequest returnRequest);
    Optional<ReturnRequest> findById(String identifier);
    Optional<ReturnRequest> findByOrderId(String orderId);
}

public interface RefundRepositoryPort {
    Refund save(Refund refund);
    Optional<Refund> findById(String identifier);
    Optional<Refund> findByReturnRequestId(String returnRequestId);
}
```

## Audit

```java
/** Append-only store of {@link Operation}s: operations are added, never updated nor deleted. */
public interface OperationRepositoryPort {
    /** Appends {@code operation}; an existing identifier is never overwritten. */
    Operation save(Operation operation);
    /** Every operation, most recent first. */
    List<Operation> findAll();
}

/** Append-only store of {@link AuditLog}s: entries are added, never updated nor deleted. */
public interface AuditLogRepositoryPort {
    /** Appends {@code auditLog}; an existing identifier is never overwritten. */
    AuditLog save(AuditLog auditLog);
    /** Every audit log entry, most recent first. */
    List<AuditLog> findAll();
}
```

`findAll` sorts by `executionDate` / `operationDate` descending and has no pagination. `MongoAuditLogRepositoryAdapter` skips legacy `audit_logs` entries without `operationType` (written by the former per-request auditing of the security layer).

## Security and Notifications

```java
public interface PasswordHasherPort {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String passwordHash);
}

public interface TokenServicePort {
    String generateToken(User user);
}

public interface NotificationPort {
    void send(String userId, NotificationChannel channel, String message);
}
```

---

# Notes

* **Methods without callers.** `CartRepositoryPort.findByBuyerId`, `InvoiceRepositoryPort.findByOrderId`, `RefundRepositoryPort.findByReturnRequestId` and `InventoryMovementRepositoryPort.findAllByProductIdAndWarehouseId` are implemented by their adapters but not called by any service yet; they support queries that no input port exposes today (a buyer's carts, an order's invoice, a return's refund, a stock history).
* **Uniqueness backed by indexes.** The services check duplicates before saving (`existsById`, `existsByEmail`, `findByOrderId`); the unique indexes created at startup (`users.email`, `shipments.orderId`, `invoices.orderId`, `return_requests.orderId`, `refunds.returnRequestId`, `inventory_items(productId, warehouseId)`) are the second barrier against concurrent requests (see [Persistence](../Adapters/Persistence.md#index-creation)).
* **No transactions.** Ports have no transaction boundary: services that write several documents do so as separate writes (see the Known Gaps in [Persistence](../Adapters/Persistence.md#known-gaps)).
