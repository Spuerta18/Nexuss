# Layer — Domain

## Purpose

`nexussMarket.domain` holds the business: entities with behavior, value objects, the domain services that implement each operation, the authorization services, and the input and output ports. It depends on no adapter or infrastructure class.

## Diagram

```mermaid
flowchart TB
    subgraph in["ports.in — one port per role"]
        IP["PublicAccessPort · BuyerPort · SellerPort ·<br/>AdministratorPort · LogisticsOperatorPort · SupervisorPort"]
    end
    subgraph services["services — 44 services + OrderTotals"]
        OPS["Operation services (one per operation)<br/>e.g. ConfirmOrderService, PublishProductService"]
        INT["Internal services<br/>ReserveInventoryService, ReleaseInventoryReservationService,<br/>ConfirmInventoryOutboundService, AuditOperationService, NotifyUserService"]
        AUTH["services.authorization<br/>ValidateUserAuthorizationStatusService, ValidateRoleAuthorizationService,<br/>AuthorizeOrderAccessService, AuthorizeProductOwnershipService,<br/>AuthorizeWarehouseOperationService, AuthorizeCartAccessService"]
    end
    subgraph model["models · valueobjects · enums · exceptions"]
        ENT["19 models<br/>User (abstract) + 5 roles, Warehouse, Product, InventoryItem,<br/>InventoryMovement, ShoppingCart, CartLine, Order, OrderLine,<br/>Shipment, Invoice, ReturnRequest, Refund, AuthenticationResult"]
        VO["valueobjects<br/>DomainCatalog + 13 catalogs (incl. Currency), Money, ProductVariant"]
        EX["exceptions<br/>DomainException + 9 subclasses"]
    end
    subgraph out["ports.out"]
        OP["11 repository ports · AuditLogPort ·<br/>PasswordHasherPort · TokenServicePort · NotificationPort"]
    end

    IP -. "implemented by adapters.useCases, which call" .-> OPS
    OPS --> AUTH
    OPS --> INT
    OPS --> ENT
    OPS --> OP
    INT --> OP
    ENT --> VO
    OPS -. throws .-> EX
```

## Notes

* **Entities protect their invariants.** State changes go through behavior methods: `InventoryItem.receive/reserve/releaseReservation/confirmOutbound/adjust/markDamaged`, `Order.advanceTo/cancel`, `ShoppingCart.addLine/updateLineQuantity/removeLine`, `Product.changeStatus/replaceVariants`, `Warehouse.deactivate`, `Shipment.pack/dispatch/confirmDelivery`, `ReturnRequest.approve/reject`, `Refund.process`. `Order`, `OrderLine` and `Invoice` are immutable except for the order status; `User` has no setter for identifier, email or role.
* **Money.** `Money` (amount + `Currency`, never negative, equality ignoring scale) is used for every price and total; a cart accepts a single currency.
* **Services.** Operation services are `@Service` classes with one `execute(...)` method that receives the actor and domain models; authorization and three internal services are plain classes registered in `infrastructure/config`.
* **Exceptions to HTTP.** `EntityNotFoundException` → 404; `OperationNotAllowedException`, `SellerNotAuthorizedException` → 403; `DuplicateResourceException`, `InsufficientStockException`, `InvalidOrderStatusTransitionException`, `InvalidShipmentStatusTransitionException`, `BusinessRuleViolationException` → 409; `InvalidCredentialsException` → 401; other `DomainException` → 400.
* Details: [Domain Model](../Domain/Domain%20Model.md), [Value Objects](../Domain/Domain%20Value%20Objects.md), [Domain Services](../Domain/Domain%20Services.md), [Input Ports](../Domain/Input-ports.md), [Output Ports](../Domain/Output-ports.md).
