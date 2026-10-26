# Layer — REST Adapters

## Purpose

The REST input adapter (`nexussMarket.adapters.in.rest`) turns HTTP requests into calls on the role input ports and domain results into JSON. It holds no business rules.

## Diagram

```mermaid
flowchart LR
    subgraph rest["adapters.in.rest"]
        direction TB
        subgraph controllers["controllers"]
            auth["AuthRestController<br/>/api/auth, /api/me"]
            buyer["BuyerRestController<br/>/api/buyer — BUYER"]
            seller["SellerRestController<br/>/api/seller — SELLER"]
            admin["AdministratorRestController<br/>/api/admin — ADMINISTRATOR"]
            logistics["LogisticsOperatorRestController<br/>/api/logistics — LOGISTICS_OPERATOR"]
            supervisor["SupervisorRestController<br/>/api/supervisor — SUPERVISOR"]
        end
        requests["requests<br/>21 records incl. ChangePasswordRequest + Jakarta Validation"]
        responses["responses<br/>17 records incl. OperationResponse, AuditLogResponse, ErrorResponse"]
        mappers["mappers<br/>DomainReferences, UserRestMapper, CatalogRestMapper,<br/>CartRestMapper, OrderRestMapper, WarehouseRestMapper,<br/>InventoryRestMapper, ShipmentRestMapper, ReturnRestMapper,<br/>AuditRestMapper, RestCodes"]
        exception["exception<br/>GlobalExceptionHandler, RestErrors,<br/>InvalidRequestException"]
    end

    subgraph ports["domain.ports.in"]
        pa["PublicAccessPort"]
        bp["BuyerPort"]
        sp["SellerPort"]
        ap["AdministratorPort"]
        lp["LogisticsOperatorPort"]
        vp["SupervisorPort"]
    end

    auth --> pa
    buyer --> bp
    seller --> sp
    admin --> ap
    logistics --> lp
    supervisor --> vp
    controllers --> requests
    controllers --> mappers
    mappers --> responses
    exception --> responses
```

## Notes

| Controller | Port | `@PreAuthorize` | Endpoints |
| ---------- | ---- | --------------- | --------- |
| `AuthRestController` | `PublicAccessPort` | none (login and register are public; `/api/me` needs any token) | 3 |
| `BuyerRestController` | `BuyerPort` | `hasRole('BUYER')` | 14 |
| `SellerRestController` | `SellerPort` | `hasRole('SELLER')` | 11 |
| `AdministratorRestController` | `AdministratorPort` | `hasRole('ADMINISTRATOR')` | 15 |
| `LogisticsOperatorRestController` | `LogisticsOperatorPort` | `hasRole('LOGISTICS_OPERATOR')` | 11 |
| `SupervisorRestController` | `SupervisorPort` | `hasRole('SUPERVISOR')` | 9 |

* **Each controller injects only its port** (`@RequiredArgsConstructor`, one `private final *Port` field) and gets the actor with `@AuthenticationPrincipal(expression = "user") User actor`.
* **Session endpoints.** Every role controller exposes `POST /logout` and `POST /password` (`ChangePasswordRequest`), both answering `204` with no body; after either one, every earlier token of the caller gets `401`. `SecurityConfig.SESSION_PATHS` (`/api/*/logout`, `/api/*/password`) only requires a valid token; the controller's `@PreAuthorize` still checks the role.
* **Audit consultation.** `AdministratorRestController` and `SupervisorRestController` expose `GET /operations` and `GET /audit-logs` (`OperationResponse[]`, `AuditLogResponse[]`, most recent first, no pagination), mapped by `AuditRestMapper`.
* **Mappers** are `final` classes with static methods. `DomainReferences` builds references from identifiers; `DomainReferences.user(id, role)` builds the `User` subtype from the role sent by the client (abstract `User` targets).
* **Codes** (status, type, role, currency, variant attribute) are resolved in the mappers; an unknown code throws `InvalidRequestException` → `400`.
* **Errors** always use `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`, `requestId`, `fieldErrors`). The security chain writes the same body through `RestErrors.write`.
* Details: [Rest-adapters](../Adapters/Rest-adapters.md), [Rest-validation](../Adapters/Rest-validation.md), [Global-exception-handler](../Adapters/Global-exception-handler.md), and the [endpoint flows](11-endpoints-flows.md).
