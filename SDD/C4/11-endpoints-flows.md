# Endpoint Flows

## Purpose

The complete list of REST endpoints, each traced to the port method and Domain Service it reaches, and one sequence diagram per role showing a representative flow end to end. The matrix was generated from the controllers and the `*UseCaseImpl` classes.

## Diagram

### Endpoint matrix (49 endpoints)

| Method | Path | Role | Port method | Domain service | Request | Response | Success |
| ------ | ---- | ---- | ----------- | -------------- | ------- | -------- | ------- |
| POST | `/api/auth/login` | public | `PublicAccessPort.authenticate` | `AuthenticateUserService` | `LoginRequest` | `AuthResponse` | 200 |
| POST | `/api/auth/register` | public | `PublicAccessPort.registerBuyer` | `RegisterBuyerService` | `RegisterBuyerRequest` | `UserResponse` | 201 |
| GET | `/api/me` | authenticated | — | — (reads the principal) | — | `UserResponse` | 200 |
| POST | `/api/buyer/carts` | BUYER | `BuyerPort.createShoppingCart` | `CreateShoppingCartService` | `CreateCartRequest` | `CartResponse` | 201 |
| GET | `/api/buyer/carts/{cartId}` | BUYER | `BuyerPort.consultCart` | `ConsultCartService` | — | `CartResponse` | 200 |
| POST | `/api/buyer/carts/{cartId}/lines` | BUYER | `BuyerPort.addCartLine` | `AddCartLineService` | `AddCartLineRequest` | `CartResponse` | 200 |
| PUT | `/api/buyer/carts/{cartId}/lines/{productId}` | BUYER | `BuyerPort.updateCartLineQuantity` | `UpdateCartLineQuantityService` | `QuantityRequest` | `CartResponse` | 200 |
| DELETE | `/api/buyer/carts/{cartId}/lines/{productId}` | BUYER | `BuyerPort.removeCartLine` | `RemoveCartLineService` | — | `CartResponse` | 200 |
| POST | `/api/buyer/carts/{cartId}/orders` | BUYER | `BuyerPort.confirmOrder` | `ConfirmOrderService` | `ConfirmOrderRequest` | `OrderResponse` | 201 |
| GET | `/api/buyer/orders` | BUYER | `BuyerPort.listBuyerOrders` | `ListBuyerOrdersService` | — | `OrderResponse[]` | 200 |
| GET | `/api/buyer/orders/{orderId}` | BUYER | `BuyerPort.consultOrder` | `ConsultOrderService` | — | `OrderResponse` | 200 |
| POST | `/api/buyer/orders/{orderId}/cancel` | BUYER | `BuyerPort.cancelOrder` | `CancelOrderService` | — | `OrderResponse` | 200 |
| POST | `/api/buyer/orders/{orderId}/returns` | BUYER | `BuyerPort.requestReturn` | `RequestReturnService` | `RequestReturnRequest` | `ReturnRequestResponse` | 201 |
| GET | `/api/buyer/products` | BUYER | `BuyerPort.listPublishedProducts` | `ListPublishedProductsService` | — | `ProductResponse[]` | 200 |
| GET | `/api/buyer/products/{productId}` | BUYER | `BuyerPort.consultProduct` | `ConsultProductService` | — | `ProductResponse` | 200 |
| POST | `/api/seller/products` | SELLER | `SellerPort.publishProduct` | `PublishProductService` | `PublishProductRequest` | `ProductResponse` | 201 |
| PATCH | `/api/seller/products/{productId}/status` | SELLER | `SellerPort.updateProductStatus` | `UpdateProductStatusService` | `ProductStatusRequest` | `ProductResponse` | 200 |
| PUT | `/api/seller/products/{productId}/variants` | SELLER | `SellerPort.updateProductVariants` | `UpdateProductVariantsService` | `VariantsRequest` | `ProductResponse` | 200 |
| GET | `/api/seller/products` | SELLER | `SellerPort.listSellerProducts` | `ListSellerProductsService` | — | `ProductResponse[]` | 200 |
| GET | `/api/seller/products/{productId}` | SELLER | `SellerPort.consultProduct` | `ConsultProductService` | — | `ProductResponse` | 200 |
| GET | `/api/seller/products/{productId}/inventory` | SELLER | `SellerPort.consultInventory` | `ConsultInventoryService` | — | `InventoryItemResponse[]` | 200 |
| POST | `/api/seller/warehouses` | SELLER | `SellerPort.registerWarehouse` | `RegisterWarehouseService` | `RegisterWarehouseRequest` | `WarehouseResponse` | 201 |
| POST | `/api/seller/inventory/inbound` | SELLER | `SellerPort.registerInventoryInbound` | `RegisterInventoryInboundService` | `InventoryMovementRequest` | `InventoryItemResponse` | 200 |
| POST | `/api/seller/inventory/adjustments` | SELLER | `SellerPort.adjustInventory` | `AdjustInventoryService` | `InventoryAdjustmentRequest` | `InventoryItemResponse` | 200 |
| POST | `/api/admin/sellers` | ADMINISTRATOR | `AdministratorPort.registerSeller` | `RegisterSellerService` | `RegisterStaffRequest` | `UserResponse` | 201 |
| POST | `/api/admin/supervisors` | ADMINISTRATOR | `AdministratorPort.registerSupervisor` | `RegisterSupervisorService` | `RegisterStaffRequest` | `UserResponse` | 201 |
| POST | `/api/admin/logistics-operators` | ADMINISTRATOR | `AdministratorPort.registerLogisticsOperator` | `RegisterLogisticsOperatorService` | `RegisterStaffRequest` | `UserResponse` | 201 |
| PATCH | `/api/admin/sellers/{sellerId}/status` | ADMINISTRATOR | `AdministratorPort.updateSellerStatus` | `UpdateSellerStatusService` | `SellerStatusRequest` | `UserResponse` | 200 |
| PATCH | `/api/admin/users/{userId}/status` | ADMINISTRATOR | `AdministratorPort.updateUserStatus` | `UpdateUserStatusService` | `UserStatusRequest` | `UserResponse` | 200 |
| GET | `/api/admin/users/{userId}?role={role}` | ADMINISTRATOR | `AdministratorPort.consultUser` | `ConsultUserService` | — | `UserResponse` | 200 |
| POST | `/api/admin/warehouses` | ADMINISTRATOR | `AdministratorPort.registerMarketplaceWarehouse` | `RegisterWarehouseService` | `RegisterWarehouseRequest` | `WarehouseResponse` | 201 |
| POST | `/api/admin/warehouses/{warehouseId}/deactivate` | ADMINISTRATOR | `AdministratorPort.deactivateWarehouse` | `DeactivateWarehouseService` | — | `WarehouseResponse` | 200 |
| POST | `/api/admin/returns/{returnRequestId}/approve` | ADMINISTRATOR | `AdministratorPort.approveReturn` | `ApproveReturnService` | — | `ReturnRequestResponse` | 200 |
| POST | `/api/admin/returns/{returnRequestId}/reject` | ADMINISTRATOR | `AdministratorPort.rejectReturn` | `RejectReturnService` | — | `ReturnRequestResponse` | 200 |
| POST | `/api/admin/refunds/{refundId}/process` | ADMINISTRATOR | `AdministratorPort.processRefund` | `ProcessRefundService` | — | `RefundResponse` | 200 |
| GET | `/api/logistics/orders/{orderId}` | LOGISTICS_OPERATOR | `LogisticsOperatorPort.consultOrder` | `ConsultOrderService` | — | `OrderResponse` | 200 |
| POST | `/api/logistics/orders/{orderId}/status` | LOGISTICS_OPERATOR | `LogisticsOperatorPort.advanceOrderStatus` | `AdvanceOrderStatusService` | `AdvanceOrderStatusRequest` | `OrderResponse` | 200 |
| POST | `/api/logistics/shipments` | LOGISTICS_OPERATOR | `LogisticsOperatorPort.createShipment` | `CreateShipmentService` | `CreateShipmentRequest` | `ShipmentResponse` | 201 |
| POST | `/api/logistics/shipments/{shipmentId}/pack` | LOGISTICS_OPERATOR | `LogisticsOperatorPort.packShipment` | `PackShipmentService` | — | `ShipmentResponse` | 200 |
| POST | `/api/logistics/shipments/{shipmentId}/dispatch` | LOGISTICS_OPERATOR | `LogisticsOperatorPort.dispatchShipment` | `DispatchShipmentService` | — | `ShipmentResponse` | 200 |
| POST | `/api/logistics/shipments/{shipmentId}/deliver` | LOGISTICS_OPERATOR | `LogisticsOperatorPort.confirmDelivery` | `ConfirmDeliveryService` | — | `ShipmentResponse` | 200 |
| POST | `/api/logistics/inventory/damaged` | LOGISTICS_OPERATOR | `LogisticsOperatorPort.reportDamagedInventory` | `ReportDamagedInventoryService` | `InventoryMovementRequest` | `InventoryItemResponse` | 200 |
| POST | `/api/logistics/inventory/returns` | LOGISTICS_OPERATOR | `LogisticsOperatorPort.registerInventoryReturn` | `RegisterInventoryReturnService` | `InventoryMovementRequest` | `InventoryItemResponse` | 200 |
| GET | `/api/logistics/products/{productId}/inventory` | LOGISTICS_OPERATOR | `LogisticsOperatorPort.consultInventory` | `ConsultInventoryService` | — | `InventoryItemResponse[]` | 200 |
| GET | `/api/supervisor/orders/{orderId}` | SUPERVISOR | `SupervisorPort.consultOrder` | `ConsultOrderService` | — | `OrderResponse` | 200 |
| GET | `/api/supervisor/products` | SUPERVISOR | `SupervisorPort.listPublishedProducts` | `ListPublishedProductsService` | — | `ProductResponse[]` | 200 |
| GET | `/api/supervisor/products/{productId}` | SUPERVISOR | `SupervisorPort.consultProduct` | `ConsultProductService` | — | `ProductResponse` | 200 |
| GET | `/api/supervisor/products/{productId}/inventory` | SUPERVISOR | `SupervisorPort.consultInventory` | `ConsultInventoryService` | — | `InventoryItemResponse[]` | 200 |
| GET | `/api/supervisor/users/{userId}?role={role}` | SUPERVISOR | `SupervisorPort.consultUser` | `ConsultUserService` | — | `UserResponse` | 200 |

Every endpoint of a role controller is restricted with `@PreAuthorize("hasRole('<ROLE>')")` at class level. All routes need `Authorization: Bearer <token>` except the two public ones.

### Public — login

```mermaid
sequenceDiagram
    autonumber
    actor V as Public visitor
    participant C as AuthRestController
    participant P as PublicAccessPort
    participant S as AuthenticateUserService
    participant U as UserRepositoryPort
    participant H as PasswordHasherPort (BCrypt)
    participant T as TokenServicePort (JWT)

    V->>C: POST /api/auth/login {email, password}
    C->>P: authenticate(email, password)
    P->>S: execute(email, password)
    S->>U: findByEmail(email)
    S->>H: matches(password, passwordHash)
    alt unknown email, wrong password or user not ACTIVE
        S-->>C: InvalidCredentialsException
        C-->>V: 401 "Invalid email or password"
    else valid and ACTIVE
        S->>T: generateToken(user)
        S-->>C: AuthenticationResult(user, token)
        C-->>V: 200 AuthResponse {accessToken, tokenType, user}
    end
```

### Buyer — confirm an order

```mermaid
sequenceDiagram
    autonumber
    actor B as Buyer
    participant C as BuyerRestController
    participant S as ConfirmOrderService
    participant CR as CartRepositoryPort
    participant RI as ReserveInventoryService
    participant RL as ReleaseInventoryReservationService
    participant OR as OrderRepositoryPort

    B->>C: POST /api/buyer/carts/{cartId}/orders {orderId}
    C->>S: BuyerPort.confirmOrder → execute(actor, cartRef, newOrder)
    S->>S: role BUYER or ADMINISTRATOR
    S->>CR: findById(cartId)
    S->>S: AuthorizeCartAccessService, buyer ACTIVE and ENABLED, cart not empty, products PUBLISHED
    loop every cart line
        S->>RI: reserve(productId, quantity)
    end
    alt a reservation fails
        S->>RL: release the lines already reserved
        S-->>C: InsufficientStockException
        C-->>B: 409
    else all reserved
        S->>OR: save(new Order PENDING_PAYMENT with immutable lines)
        S->>CR: deleteById(cartId)
        C-->>B: 201 OrderResponse
    end
```

### Seller — publish a product

```mermaid
sequenceDiagram
    autonumber
    actor Sl as Seller
    participant C as SellerRestController
    participant M as CatalogRestMapper
    participant S as PublishProductService
    participant U as UserRepositoryPort
    participant PR as ProductRepositoryPort

    Sl->>C: POST /api/seller/products {identifier, name, productType, price, variants}
    C->>C: @Valid PublishProductRequest
    C->>M: newProduct(request) — codes, Money, variants
    C->>S: SellerPort.publishProduct → execute(actor, product)
    S->>U: findById(actor) — must be a Seller
    S->>S: sellerStatus ACTIVE, price not null and greater than zero
    S->>PR: save(Product PUBLISHED, seller = actor)
    C-->>Sl: 201 ProductResponse
```

### Administrator — approve a return

```mermaid
sequenceDiagram
    autonumber
    actor A as Administrator
    participant C as AdministratorRestController
    participant S as ApproveReturnService
    participant RR as ReturnRequestRepositoryPort
    participant IR as RegisterInventoryReturnService
    participant RF as RefundRepositoryPort

    A->>C: POST /api/admin/returns/{returnRequestId}/approve
    C->>S: AdministratorPort.approveReturn → execute(actor, returnRequestRef)
    S->>S: role ADMINISTRATOR
    S->>RR: findById(returnRequestId)
    S->>S: returnRequest.approve(actor) — fails if already decided
    loop every order line
        S->>IR: execute(actor, product, warehouse, quantity) — RETURN movement
    end
    S->>RR: save(returnRequest)
    S->>RF: save(Refund "REF-" + id, PENDING, order total)
    C-->>A: 200 ReturnRequestResponse
```

### Logistics operator — dispatch a shipment

```mermaid
sequenceDiagram
    autonumber
    actor L as Logistics operator
    participant C as LogisticsOperatorRestController
    participant S as DispatchShipmentService
    participant SR as ShipmentRepositoryPort
    participant CO as ConfirmInventoryOutboundService
    participant OR as OrderRepositoryPort

    L->>C: POST /api/logistics/shipments/{shipmentId}/dispatch
    C->>S: LogisticsOperatorPort.dispatchShipment → execute(actor, shipmentRef)
    S->>S: role LOGISTICS_OPERATOR or ADMINISTRATOR
    S->>SR: findById(shipmentId)
    S->>S: shipment.dispatch(), order.advanceTo(SHIPPED)
    loop every order line
        S->>CO: execute(product, warehouse, quantity) — SALE_OUTBOUND movement
    end
    S->>OR: save(order)
    S->>SR: save(shipment)
    C-->>L: 200 ShipmentResponse
```

### Supervisor — consult inventory

```mermaid
sequenceDiagram
    autonumber
    actor V as Supervisor
    participant C as SupervisorRestController
    participant S as ConsultInventoryService
    participant PR as ProductRepositoryPort
    participant IR as InventoryRepositoryPort

    V->>C: GET /api/supervisor/products/{productId}/inventory
    C->>S: SupervisorPort.consultInventory → execute(actor, productRef)
    S->>PR: findById(productId)
    S->>S: Supervisor or LogisticsOperator: active status only
    S->>IR: findAllByProductId(productId)
    C-->>V: 200 InventoryItemResponse[]
```

### Rejected requests

```mermaid
sequenceDiagram
    autonumber
    actor X as Client
    participant F as Security filter chain
    participant C as Role controller
    participant H as GlobalExceptionHandler

    X->>F: request without token
    F-->>X: 401 ErrorResponse "Authentication required"
    X->>F: request with a valid token of another role
    F->>C: authenticated
    C-->>X: 403 ErrorResponse "Access denied" (@PreAuthorize)
    X->>F: valid token, invalid body
    F->>C: authenticated
    C->>H: MethodArgumentNotValidException
    H-->>X: 400 ErrorResponse with fieldErrors
```

## Notes

* **Status codes.** `201` for creations, `200` otherwise; errors use the uniform `ErrorResponse` (see [Global Exception Handler](../Adapters/Global-exception-handler.md)).
* **Abstract `User` targets.** `/api/admin/users/{userId}` and `/api/supervisor/users/{userId}` need the target's `role` (body of the `PATCH`, `role` query parameter of the `GET`).
* **Shared services.** Several endpoints of different roles reach the same service (e.g. `ConsultOrderService` from the buyer, logistics and supervisor controllers); the service applies the rule for the actor's role.
* Endpoint details and DTO fields: [REST API Endpoints](../Adapters/Api-rest-endpoints.md), [REST Validation](../Adapters/Rest-validation.md).
