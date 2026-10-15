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
        requests["requests<br/>20 records + Jakarta Validation"]
        responses["responses<br/>15 records incl. ErrorResponse"]
        mappers["mappers<br/>DomainReferences, UserRestMapper, CatalogRestMapper,<br/>CartRestMapper, OrderRestMapper, WarehouseRestMapper,<br/>InventoryRestMapper, ShipmentRestMapper, ReturnRestMapper, RestCodes"]
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
| `BuyerRestController` | `BuyerPort` | `hasRole('BUYER')` | 12 |
| `SellerRestController` | `SellerPort` | `hasRole('SELLER')` | 9 |
| `AdministratorRestController` | `AdministratorPort` | `hasRole('ADMINISTRATOR')` | 11 |
| `LogisticsOperatorRestController` | `LogisticsOperatorPort` | `hasRole('LOGISTICS_OPERATOR')` | 9 |
| `SupervisorRestController` | `SupervisorPort` | `hasRole('SUPERVISOR')` | 5 |

* **Each controller injects only its port** (`@RequiredArgsConstructor`, one `private final *Port` field) and gets the actor with `@AuthenticationPrincipal(expression = "user") User actor`.
* **Mappers** are `final` classes with static methods. `DomainReferences` builds references from identifiers; `DomainReferences.user(id, role)` builds the `User` subtype from the role sent by the client (abstract `User` targets).
* **Codes** (status, type, role, currency, variant attribute) are resolved in the mappers; an unknown code throws `InvalidRequestException` → `400`.
* **Errors** always use `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`, `requestId`, `fieldErrors`). The security chain writes the same body through `RestErrors.write`.
* Details: [Rest-adapters](../Adapters/Rest-adapters.md), [Rest-validation](../Adapters/Rest-validation.md), [Global-exception-handler](../Adapters/Global-exception-handler.md), and the [endpoint flows](11-endpoints-flows.md).
