# C4 Level 3 — Components of the API

## Purpose

Shows the main components inside the NexusMarket API container, grouped by hexagonal layer, and follows one request through them from the HTTP filter chain to MongoDB and back.

## Diagram

### Components

```mermaid
C4Component
    title NexusMarket API — Components

    System_Ext(client, "Client application")
    ContainerDb(mongo, "MongoDB", "nexusmarket")

    Container_Boundary(api, "NexusMarket API") {
        Component(filter, "Security filter chain", "SecurityConfig, JwtAuthenticationFilter", "CORS, JWT validation, user reload, token version check, 401/403")
        Component(controllers, "REST controllers", "adapters.in.rest.controllers", "One per role port, @PreAuthorize by role")
        Component(restmappers, "REST mappers and DTOs", "adapters.in.rest.mappers / requests / responses", "Jakarta Validation, DTO to domain and back")
        Component(handler, "GlobalExceptionHandler", "adapters.in.rest.exception", "Exceptions to HTTP status and ErrorResponse")
        Component(ports, "Role input ports", "domain.ports.in", "PublicAccessPort, BuyerPort, SellerPort, AdministratorPort, LogisticsOperatorPort, SupervisorPort")
        Component(usecases, "Use case adapters", "adapters.useCases", "*UseCaseImpl: delegation only")
        Component(services, "Domain services", "domain.services", "One service per operation, business rules, operation and audit recording")
        Component(authz, "Authorization services", "domain.services.authorization", "Role, status and ownership checks")
        Component(model, "Domain model", "domain.models / valueobjects", "Entities with behavior, Operation, AuditLog, Money, catalogs")
        Component(outports, "Output ports", "domain.ports.out", "Repositories incl. OperationRepositoryPort and AuditLogRepositoryPort, PasswordHasherPort, TokenServicePort, NotificationPort")
        Component(mongoadapters, "MongoDB adapters", "adapters.out.persistence.mongodb", "Mongo*Adapter, documents, Spring Data repositories, mappers")
        Component(secadapters, "Security adapters", "infrastructure.security", "BCryptPasswordHasherAdapter, JwtTokenServiceAdapter")
        Component(seed, "Seed", "infrastructure.seed", "InitialAdministratorSeeder (ApplicationRunner)")
    }

    Rel(client, filter, "HTTP request")
    Rel(filter, controllers, "Authenticated request")
    Rel(controllers, restmappers, "Maps with")
    Rel(controllers, ports, "Calls")
    Rel(usecases, services, "Delegates to")
    Rel(services, authz, "Authorizes with")
    Rel(services, model, "Uses")
    Rel(services, outports, "Calls")
    Rel(mongoadapters, mongo, "Reads and writes")
    Rel(secadapters, outports, "Implements")
    Rel(seed, outports, "Uses")
```

`*UseCaseImpl` classes implement the role ports, and the MongoDB adapters implement the output ports; Mermaid C4 draws no separate "implements" arrow, so those two links are described here.

### Lifecycle of a request

`POST /api/buyer/carts/C-1/lines` with a valid buyer token:

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant F as JwtAuthenticationFilter
    participant U as NexusUserDetailsService
    participant Ctl as BuyerRestController
    participant P as BuyerPort (BuyerUseCaseImpl)
    participant S as AddCartLineService
    participant Cart as ShoppingCart
    participant R as CartRepositoryPort (MongoCartRepositoryAdapter)
    participant A as RegisterOperationAndAuditService
    participant DB as MongoDB

    C->>F: POST /api/buyer/carts/C-1/lines + Bearer JWT
    F->>F: JwtProvider.validate(token)
    F->>U: loadUserById(sub)
    U->>DB: users.findById
    F->>F: reject unless ACTIVE, stored role == token role and authTokenVersion == ver
    F->>Ctl: request with AuthenticatedUserPrincipal
    Ctl->>Ctl: @Valid AddCartLineRequest (argument resolution)
    Ctl->>Ctl: @PreAuthorize hasRole('BUYER') (method proxy)
    Ctl->>P: addCartLine(actor, cartRef, productRef, quantity)
    P->>S: execute(actor, cartRef, productRef, quantity)
    S->>S: ValidateRoleAuthorizationService(BUYER)
    S->>R: findById("C-1")
    R->>DB: shopping_carts.findById + referenced documents
    S->>S: AuthorizeCartAccessService(actor, cart)
    S->>Cart: addLine(product, quantity)
    S->>R: save(cart)
    R->>DB: shopping_carts.save
    S->>A: execute(Operation CART_LINE_ADDITION, details productId and quantity)
    A->>DB: operations.insert, audit_logs.insert
    S-->>Ctl: ShoppingCart
    Ctl-->>C: 200 CartResponse (JSON)
```

## Notes

* **Where a request can stop.** Missing, invalid or revoked token (its `ver` claim differs from the stored `authTokenVersion`) → `401` from the filter chain. Invalid body → `400` from validation. Wrong role → `403` from `@PreAuthorize`. Body validation runs while Spring MVC resolves the controller arguments, before the method (and its `@PreAuthorize` proxy) is invoked, so a user with the wrong role who sends an invalid body gets `400`, not `403`; the port is never called in either case. Unknown code → `400` from a REST mapper. Domain refusal → `403`/`404`/`409` from `GlobalExceptionHandler`. All of them use the same `ErrorResponse` body.
* **Dependency direction.** Every arrow points toward the domain: controllers and use case adapters depend on `domain.ports.in`, services depend on `domain.ports.out`, and the adapters implement those ports. The domain does not import any adapter or infrastructure class.
* **Audit.** The filter does not audit. Every state-changing Domain Service, after saving, calls `RegisterOperationAndAuditService`, which inserts one `Operation` (collection `operations`) and one `AuditLog` (collection `audit_logs`, with the performer's role and the operation details). Successful logins are recorded as `USER_LOGIN`; read-only requests and failed logins are not recorded. Both collections are append-only.
