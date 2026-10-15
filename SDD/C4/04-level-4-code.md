# C4 Level 4 — Code

## Purpose

Shows the key classes behind one representative operation, confirming an order, from the REST controller to the MongoDB document, and the classes that every request shares (security and errors).

## Diagram

### Confirming an order (`POST /api/buyer/carts/{cartId}/orders`)

```mermaid
classDiagram
    direction LR

    class BuyerRestController {
        -BuyerPort buyerPort
        +confirmOrder(User actor, String cartId, ConfirmOrderRequest request) OrderResponse
    }
    class BuyerPort {
        <<interface>>
        +confirmOrder(User user, ShoppingCart cart, Order newOrder) Order
    }
    class BuyerUseCaseImpl {
        -ConfirmOrderService confirmOrderService
        +confirmOrder(User, ShoppingCart, Order) Order
    }
    class ConfirmOrderService {
        -CartRepositoryPort cartRepositoryPort
        -OrderRepositoryPort orderRepositoryPort
        -ReserveInventoryService reserveInventoryService
        -ReleaseInventoryReservationService releaseInventoryReservationService
        -ValidateRoleAuthorizationService validateRoleAuthorizationService
        -AuthorizeCartAccessService authorizeCartAccessService
        +execute(User actor, ShoppingCart cartReference, Order newOrder) Order
    }
    class ShoppingCart {
        -String identifier
        -Buyer buyer
        -List~CartLine~ lines
        +addLine(Product, int)
        +updateLineQuantity(Product, int)
        +removeLine(Product)
    }
    class Order {
        -String identifier
        -Buyer buyer
        -List~OrderLine~ lines
        -OrderStatus status
        -LocalDateTime createdAt
        +advanceTo(OrderStatus)
        +cancel()
    }
    class OrderLine {
        -Product product
        -String productName
        -Integer quantity
        -Money unitPrice
        -Warehouse warehouse
    }
    class Money {
        -BigDecimal amount
        -Currency currency
        +add(Money) Money
        +subtract(Money) Money
    }
    class OrderRepositoryPort {
        <<interface>>
        +save(Order) Order
        +findById(String) Optional~Order~
        +findAllByBuyerId(String) List~Order~
    }
    class MongoOrderRepositoryAdapter {
        -OrderMongoRepository orderRepository
        -MongoReferenceResolver referenceResolver
    }
    class OrderMongoRepository {
        <<interface>>
    }
    class OrderMapper {
        +toDocument(Order)$ OrderDocument
        +toDomain(OrderDocument, Buyer, Map, Map)$ Order
    }
    class OrderDocument {
        -String id
        -String buyerId
        -String status
        -List~OrderLineDocument~ lines
    }

    BuyerRestController --> BuyerPort : calls
    BuyerUseCaseImpl ..|> BuyerPort
    BuyerUseCaseImpl --> ConfirmOrderService : delegates
    ConfirmOrderService --> ShoppingCart : reads lines
    ConfirmOrderService --> Order : creates
    Order *-- OrderLine
    OrderLine --> Money
    ConfirmOrderService --> OrderRepositoryPort : save
    MongoOrderRepositoryAdapter ..|> OrderRepositoryPort
    MongoOrderRepositoryAdapter --> OrderMongoRepository
    MongoOrderRepositoryAdapter --> OrderMapper
    OrderMapper --> OrderDocument
```

### Shared by every request

```mermaid
classDiagram
    direction LR

    class SecurityConfig {
        +securityFilterChain(...) SecurityFilterChain
        +corsConfigurationSource(CorsProperties) CorsConfigurationSource
    }
    class JwtAuthenticationFilter {
        -JwtProvider jwtProvider
        -NexusUserDetailsService userDetailsService
        -AuditOperationService auditOperationService
        #doFilterInternal(...)
    }
    class JwtProvider {
        +generateToken(User) String
        +validate(String) Optional~JwtClaims~
    }
    class NexusUserDetailsService {
        -UserRepositoryPort userRepositoryPort
        +loadUserById(String) Optional~AuthenticatedUserPrincipal~
    }
    class AuthenticatedUserPrincipal {
        -User user
        +getUser() User
        +getAuthorities()
    }
    class GlobalExceptionHandler {
        +handleNotFound(...) ResponseEntity~ErrorResponse~
        +handleConflict(...) ResponseEntity~ErrorResponse~
        +handleForbidden(...) ResponseEntity~ErrorResponse~
    }
    class RestErrors {
        +response(...)$ ResponseEntity~ErrorResponse~
        +write(...)$
    }
    class ErrorResponse {
        <<record>>
        timestamp, status, error, message, path, requestId, fieldErrors
    }

    SecurityConfig --> JwtAuthenticationFilter : registers
    SecurityConfig --> RestErrors : 401 and 403
    JwtAuthenticationFilter --> JwtProvider : validate
    JwtAuthenticationFilter --> NexusUserDetailsService : reload user
    NexusUserDetailsService --> AuthenticatedUserPrincipal : creates
    GlobalExceptionHandler --> RestErrors
    RestErrors --> ErrorResponse
```

## Notes

* **`ConfirmOrderService.execute`**: checks the role (`BUYER` or `ADMINISTRATOR`), loads the cart, checks cart access, requires the buyer to be `ACTIVE` and `ENABLED` and the cart to be non-empty with `PUBLISHED` products, reserves stock per line with `ReserveInventoryService` (releasing what was already reserved if one line fails), builds the immutable `Order` with its lines, saves it and deletes the cart.
* **Controllers receive the actor** through `@AuthenticationPrincipal(expression = "user") User actor`, i.e. `AuthenticatedUserPrincipal.getUser()` — the user reloaded by `JwtAuthenticationFilter`.
* **`MongoReferenceResolver`** turns the IDs stored in documents (`buyerId`, `productId`, `warehouseId`) back into domain objects, with one query per collection.
* **`ReserveInventoryService`, `AuditOperationService` and the authorization services** are plain classes registered in `infrastructure/config`; the rest of the classes above are component-scanned.
