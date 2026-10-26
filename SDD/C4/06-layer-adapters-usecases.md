# Layer — Use Case Adapters

## Purpose

`nexussMarket.adapters.useCases` holds one class per role input port. Each class implements its port and only delegates every method to one Domain Service — no rules, validations or branching.

## Diagram

```mermaid
flowchart LR
    subgraph ports["domain.ports.in"]
        PA["PublicAccessPort"]
        BP["BuyerPort"]
        SP["SellerPort"]
        AP["AdministratorPort"]
        LP["LogisticsOperatorPort"]
        VP["SupervisorPort"]
    end
    subgraph impls["adapters.useCases"]
        PAI["PublicAccessUseCaseImpl"]
        BPI["BuyerUseCaseImpl"]
        SPI["SellerUseCaseImpl"]
        API["AdministratorUseCaseImpl"]
        LPI["LogisticsOperatorUseCaseImpl"]
        VPI["SupervisorUseCaseImpl"]
    end
    subgraph services["domain.services"]
        S1["AuthenticateUserService<br/>RegisterBuyerService"]
        S2["CreateShoppingCart, AddCartLine, UpdateCartLineQuantity,<br/>RemoveCartLine, ConsultCart, ConfirmOrder, CancelOrder,<br/>ConsultOrder, ListBuyerOrders, ListPublishedProducts,<br/>ConsultProduct, RequestReturn,<br/>Logout, ChangeUserPassword"]
        S3["PublishProduct, UpdateProductStatus, UpdateProductVariants,<br/>ListSellerProducts, ConsultProduct, RegisterWarehouse,<br/>RegisterInventoryInbound, AdjustInventory, ConsultInventory,<br/>Logout, ChangeUserPassword"]
        S4["RegisterSeller, RegisterSupervisor, RegisterLogisticsOperator,<br/>UpdateSellerStatus, UpdateUserStatus, RegisterWarehouse,<br/>DeactivateWarehouse, ApproveReturn, RejectReturn,<br/>ProcessRefund, ConsultUser, ConsultOperations,<br/>ConsultAuditLogs, Logout, ChangeUserPassword"]
        S5["ConsultOrder, AdvanceOrderStatus, CreateShipment,<br/>PackShipment, DispatchShipment, ConfirmDelivery,<br/>ReportDamagedInventory, RegisterInventoryReturn, ConsultInventory,<br/>Logout, ChangeUserPassword"]
        S6["ConsultOrder, ConsultInventory, ConsultProduct,<br/>ConsultUser, ListPublishedProducts, ConsultOperations,<br/>ConsultAuditLogs, Logout, ChangeUserPassword"]
    end

    PAI -. implements .-> PA
    BPI -. implements .-> BP
    SPI -. implements .-> SP
    API -. implements .-> AP
    LPI -. implements .-> LP
    VPI -. implements .-> VP
    PAI --> S1
    BPI --> S2
    SPI --> S3
    API --> S4
    LPI --> S5
    VPI --> S6
```

## Notes

| Implementation | Port methods | Delegation (port method → service) |
| -------------- | ------------ | ---------------------------------- |
| `PublicAccessUseCaseImpl` | 2 | `authenticate` → `AuthenticateUserService`, `registerBuyer` → `RegisterBuyerService` |
| `BuyerUseCaseImpl` | 14 | one service per method, named after it (e.g. `confirmOrder` → `ConfirmOrderService`) |
| `SellerUseCaseImpl` | 11 | idem; `registerWarehouse` → `RegisterWarehouseService` |
| `AdministratorUseCaseImpl` | 15 | idem; `registerMarketplaceWarehouse` → `RegisterWarehouseService`, `consultOperations` → `ConsultOperationsService`, `consultAuditLogs` → `ConsultAuditLogsService` |
| `LogisticsOperatorUseCaseImpl` | 11 | idem |
| `SupervisorUseCaseImpl` | 9 | idem; `consultOperations` → `ConsultOperationsService`, `consultAuditLogs` → `ConsultAuditLogsService` |

* All six classes are `@Service` + `@RequiredArgsConstructor` (Lombok) and are found by component scan.
* Shared services: `ConsultProductService`, `ConsultInventoryService`, `ConsultOrderService`, `ConsultUserService`, `ListPublishedProductsService`, `RegisterWarehouseService`, `ConsultOperationsService`, `ConsultAuditLogsService`, `LogoutService` and `ChangeUserPasswordService` are injected into several implementations; each service is still written once.
* Session methods: the five authenticated role ports (not `PublicAccessPort`) declare `logout(User)` → `LogoutService` and `changePassword(User, currentPassword, newPassword)` → `ChangeUserPasswordService`.
* The use case adapters never call `RegisterOperationAndAuditService`: each state-changing Domain Service records its own operation and audit log.
* `RegisterWarehouseService` serves both `SellerPort.registerWarehouse` and `AdministratorPort.registerMarketplaceWarehouse`; it decides the kind of warehouse from the actor's role.
* Details: [Use Case Adapters](../Adapters/Use-cases-adapters.md).
