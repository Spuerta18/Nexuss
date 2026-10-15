# Use Case Adapters — NexusMarket

## Introduction

The use case adapters connect the [role input ports](../Domain/Input-ports.md) with the [Domain Services](../Domain/Domain%20Services.md). There is one class per port, in `adapters/useCases`, named after the port's role:

```text
adapters/useCases/
├── package-info.java
├── PublicAccessUseCaseImpl.java        implements PublicAccessPort
├── BuyerUseCaseImpl.java               implements BuyerPort
├── SellerUseCaseImpl.java              implements SellerPort
├── AdministratorUseCaseImpl.java       implements AdministratorPort
├── LogisticsOperatorUseCaseImpl.java   implements LogisticsOperatorPort
└── SupervisorUseCaseImpl.java          implements SupervisorPort
```

---

# Role of the Layer

* **Delegation only.** Every method calls exactly one Domain Service and returns its result unchanged. A use case adapter contains no business rules, no validations, no authorization checks and no branching (`if`, `switch`, `try`). Everything that decides something lives in the Domain Service, so it is written once even when several ports expose the same operation.
* **Implements exactly its port.** Each class implements every method its port declares, and nothing else: no public methods outside the port.
* **Passes the parameters through.** The domain models the port receives are handed to the service as they are; the adapter does not build, complete or load models. Converting transport objects into domain models belongs to the REST mappers, and loading entities belongs to the services.
* **Spring wiring.** Each class is annotated `@Service` and `@RequiredArgsConstructor` (Lombok): its dependencies are `private final` fields holding the concrete Domain Services it delegates to, injected through the generated constructor. The classes are found by component scan; no configuration class registers them.
* **The service's own names are not exposed.** Port methods are named after the operation (`cancelOrder`); every service exposes a single `execute(...)` method. The adapter is where one name maps to the other.

---

# Request Flow

```text
REST Controller            adapters/in/rest/controllers
   │  Request DTO → domain models (mappers), authenticated User from the security layer
   ▼
Role Input Port            domain/ports/in          e.g. BuyerPort.cancelOrder(user, order)
   │
   ▼
UseCaseImpl                adapters/useCases        e.g. BuyerUseCaseImpl → cancelOrderService.execute(user, order)
   │
   ▼
Domain Service             domain/services          authorizes the actor, loads entities, applies the rules
   │
   ▼
Output Port                domain/ports/out         e.g. OrderRepositoryPort.save(order)
   │
   ▼
Output Adapter             adapters/out/persistence/mongodb (MongoDB), infrastructure/security
```

The controller depends only on the role port, never on the `UseCaseImpl` or on a Domain Service. Swapping the implementation of a port does not affect the controller.

---

# Port → Implementation → Injected Services

| Port | Implementation | Injected Domain Services |
| ---- | -------------- | ------------------------ |
| `PublicAccessPort` | `PublicAccessUseCaseImpl` | `AuthenticateUserService`, `RegisterBuyerService` |
| `BuyerPort` | `BuyerUseCaseImpl` | `CreateShoppingCartService`, `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService`, `ConsultCartService`, `ConfirmOrderService`, `CancelOrderService`, `ConsultOrderService`, `ListBuyerOrdersService`, `ListPublishedProductsService`, `ConsultProductService`, `RequestReturnService` |
| `SellerPort` | `SellerUseCaseImpl` | `PublishProductService`, `UpdateProductStatusService`, `UpdateProductVariantsService`, `ListSellerProductsService`, `ConsultProductService`, `RegisterWarehouseService`, `RegisterInventoryInboundService`, `AdjustInventoryService`, `ConsultInventoryService` |
| `AdministratorPort` | `AdministratorUseCaseImpl` | `RegisterSellerService`, `RegisterSupervisorService`, `RegisterLogisticsOperatorService`, `UpdateSellerStatusService`, `UpdateUserStatusService`, `RegisterWarehouseService`, `DeactivateWarehouseService`, `ApproveReturnService`, `RejectReturnService`, `ProcessRefundService`, `ConsultUserService` |
| `LogisticsOperatorPort` | `LogisticsOperatorUseCaseImpl` | `ConsultOrderService`, `AdvanceOrderStatusService`, `CreateShipmentService`, `PackShipmentService`, `DispatchShipmentService`, `ConfirmDeliveryService`, `ReportDamagedInventoryService`, `RegisterInventoryReturnService`, `ConsultInventoryService` |
| `SupervisorPort` | `SupervisorUseCaseImpl` | `ConsultOrderService`, `ConsultInventoryService`, `ConsultProductService`, `ConsultUserService`, `ListPublishedProductsService` |

Notes:

* `AdministratorPort.registerMarketplaceWarehouse` and `SellerPort.registerWarehouse` both delegate to `RegisterWarehouseService`, which decides the kind of warehouse from the actor's role (see [RegisterWarehouseService](../Domain/services/inventory.md#registerwarehouseservice)).
* Services used only by other services (`ReserveInventoryService`, `ReleaseInventoryReservationService`, `ConfirmInventoryOutboundService`), the security layer (`AuditOperationService`) or nobody yet (`NotifyUserService`) are not injected into any use case adapter.

---

# Example

`SellerUseCaseImpl` (excerpt):

```java
/** Implements {@link SellerPort} by delegating each operation to its domain service. */
@Service
@RequiredArgsConstructor
public class SellerUseCaseImpl implements SellerPort {

    private final PublishProductService publishProductService;
    private final RegisterWarehouseService registerWarehouseService;
    private final ConsultInventoryService consultInventoryService;
    // … one field per Domain Service the port needs

    @Override
    public Product publishProduct(User user, Product product) {
        return publishProductService.execute(user, product);
    }

    @Override
    public Warehouse registerWarehouse(User user, Warehouse warehouse) {
        return registerWarehouseService.execute(user, warehouse);
    }

    @Override
    public List<InventoryItem> consultInventory(User user, Product product) {
        return consultInventoryService.execute(user, product);
    }
}
```

What does **not** belong here, because it is a rule and therefore lives in the Domain Service:

```java
// Wrong: role check and branching in the adapter.
@Override
public Warehouse registerWarehouse(User user, Warehouse warehouse) {
    if (user.getRole() != SystemRole.SELLER) {
        throw new OperationNotAllowedException("…");
    }
    return registerWarehouseService.execute(user, warehouse);
}
```
