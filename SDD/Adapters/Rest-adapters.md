# REST Adapters — NexusMarket

## Introduction

The REST input adapter translates HTTP requests into calls on the [role input ports](../Domain/Input-ports.md) and domain results into JSON responses. It contains no business rules: every decision is taken by the Domain Services behind the ports.

```text
adapters/in/rest/
├── controllers/   One controller per role port; @PreAuthorize at class level
├── requests/      Request DTOs (records) with Jakarta Validation annotations
├── responses/     Response DTOs (records), including ErrorResponse
├── mappers/       DTO → domain model and domain model → DTO conversion
└── exception/     GlobalExceptionHandler, RestErrors, InvalidRequestException
```

See [REST API Endpoints](Api-rest-endpoints.md) for the full list.

---

# Design Conventions

* **One controller, one port.** Each controller injects only its role port (`BuyerRestController` → `BuyerPort`, …) and never a Domain Service, a repository or another controller's port. A method only maps the request, calls one port method and maps the result.
* **The actor comes from the token.** Controllers receive the authenticated user with `@AuthenticationPrincipal(expression = "user") User actor`. The JWT filter reloads it from the database on every request, so it is always the concrete subtype (`Buyer`, `Seller`, …) with its current state (see [REST Security and CORS](Rest-security-cors.md)). Request bodies never carry the actor.
* **DTOs only at the boundary.** Requests and responses are Java records in `requests/` and `responses/`. Domain models and persistence documents are never serialized. Related entities appear by identifier (`buyerId`, `sellerId`, `warehouseId`); money as `{ "amount", "currency" }`.
* **No password or token in `toString`.** Request records carrying a password (including both fields of `ChangePasswordRequest`), and `AuthResponse`, override `toString` to mask it, so they never leak into a log by accident. `UserResponse` has no password field at all.
* **Status codes.** `201 Created` for endpoints that create something (registrations, carts, orders, products, warehouses, shipments, return requests); `204 No Content` for logout and password change, which return nothing; `200 OK` otherwise, including `DELETE` (it returns the updated cart).

---

# Mappers

Mappers are `final` classes with static methods, like the persistence mappers.

| Mapper | Converts |
| ------ | -------- |
| `DomainReferences` | Identifiers into domain **references** (models carrying only the identifier): product, cart, order, warehouse, shipment, return request, refund, seller, buyer, and any `User` by role. |
| `UserRestMapper` | Registration requests into new users; `User` into `UserResponse`; `AuthenticationResult` into `AuthResponse`; role and status codes. |
| `CatalogRestMapper` | `PublishProductRequest` into a new `Product`; `MoneyRequest` into `Money`; variants; `Product` / `Money` into responses. |
| `CartRestMapper` | `CreateCartRequest` into a new cart; `ShoppingCart` into `CartResponse`. |
| `OrderRestMapper` | `ConfirmOrderRequest` into the new order's identifier; order status codes; `Order` into `OrderResponse`. |
| `WarehouseRestMapper` | `RegisterWarehouseRequest` into a new `Warehouse`, with the owner type fixed by the endpoint; `Warehouse` into `WarehouseResponse`. |
| `InventoryRestMapper` | `InventoryItem` into `InventoryItemResponse`. |
| `ShipmentRestMapper` | `CreateShipmentRequest` into a new `Shipment`; `Shipment` into `ShipmentResponse`. |
| `ReturnRestMapper` | `RequestReturnRequest` into a new `ReturnRequest`; return requests and refunds into responses. |
| `AuditRestMapper` | `Operation` into `OperationResponse` and `AuditLog` into `AuditLogResponse`: catalog values as codes, the performer by identifier. |
| `RestCodes` | Catalog codes into Value Objects (package-private helper). |

* **Codes.** Status, type, role and currency codes are resolved with each catalog's `fromCode`. An unknown code throws `InvalidRequestException`, answered with `400` and a message such as `Unknown currency 'XYZ'`.
* **New models carry placeholders** for the fields the service decides (initial status, owner): the Domain Service builds the stored entity itself and ignores them (see [Input Ports](../Domain/Input-ports.md#domain-model-parameters)).
* **Ownership from the endpoint.** `POST /api/seller/warehouses` always builds a `SELLER` warehouse and `POST /api/admin/warehouses` a `MARKETPLACE` one; the client cannot choose.

---

# Abstract User References

`AdministratorPort.updateUserStatus`, `AdministratorPort.consultUser` and `SupervisorPort.consultUser` receive the target user as the abstract `User`. An identifier alone does not tell which subtype to build, so **the client sends the role of the target user**:

* in the body of `PATCH /api/admin/users/{userId}/status` (`UserStatusRequest.role`);
* as the `role` query parameter of `GET /api/admin/users/{userId}` and `GET /api/supervisor/users/{userId}`, since a `GET` request has no body.

`DomainReferences.user(id, role)` then builds a reference of that subtype.

**Limitation.** The role is chosen by the client and is not checked against the stored user. This is harmless today: the services only read the reference's identifier and load the real user, so a wrong role still acts on the right user. But a future service that relies on the reference's subtype would trust a client-supplied value; such a service must load the user instead. A missing or unknown role answers `400`.

---

# API Documentation (Swagger UI)

The OpenAPI document and the Swagger UI are generated by springdoc-openapi (`springdoc-openapi-starter-webmvc-ui` 3.1.1, built on Spring Boot 4.1.0; the version is in the `springdoc.version` property of `pom.xml`). No annotation is added to the controllers: springdoc reads the routes, the request records and their Jakarta Validation constraints.

| URL | Content |
| --- | ------- |
| `/swagger-ui.html` | Entry point; redirects to `/swagger-ui/index.html`. |
| `/swagger-ui/**` | The Swagger UI. |
| `/v3/api-docs` | The OpenAPI 3 document (JSON). |

* **Public, read-only.** `SecurityConfig.API_DOCUMENTATION_PATHS` allows `GET` on these three paths without a token; any other method on them still answers `401`. The documentation only describes the API: calling an endpoint from the UI goes through the same security as any other client.
* **Authorize button.** `infrastructure/config/OpenApiConfiguration` declares the `bearerAuth` scheme (HTTP, `bearer`, format `JWT`) and requires it on every operation. Log in with `POST /api/auth/login`, paste the `accessToken` (without the `Bearer ` prefix) into Authorize, and the UI sends `Authorization: Bearer <token>`. An `OpenApiCustomizer` removes the requirement from `POST /api/auth/login` and `POST /api/auth/register`, which are public.
* Role restrictions (`@PreAuthorize`) are not shown in the document; see [REST API Endpoints](Api-rest-endpoints.md) for the role of each route.

---

# Testing

* **Controllers** — one MockMvc test class per controller (`*RestControllerTest`), sharing `RestControllerTestSupport`: the full application context with the real security, controllers, mappers and exception handler, and every role port mocked. `UserRepositoryPort` is mocked so the JWT filter reloads the test users; nothing else is needed, since auditing happens in the Domain Services behind the mocked ports. Each class covers the happy path, `401` without a token, `403` with another role and `400` with an invalid body. No MongoDB server is needed.
* **Audit endpoints** — `AuditRestControllerTest`: administrators and supervisors read operations and audit logs (with `details`); other roles get `403` and anonymous callers `401`.
* **Session endpoints** — `SessionRestControllerTest`: logout and password change answer `204` and delegate to the role port for each of the five roles; a short new password answers `400` without echoing it; each path needs a token of its role; the token used to log out, and the token from before a password change, are rejected with `401` afterwards while a new one works.
* **Exception mapping** — `DomainExceptionMappingTest`: `GlobalExceptionHandler` answers each domain exception with its HTTP status (`400`, `403`, `404`, `409`) and its own message.
* **API documentation** — `ApiDocumentationTest` (same shared context): `/v3/api-docs` and the Swagger UI answer without a token, the document declares the `bearerAuth` scheme, login and registration carry no security requirement, and `POST` on a documentation path still answers `401`.
* **Mappers** — `RestMappersTest`: subtype by role, unknown codes, money and variants, ownership from the endpoint, and no password in responses or in `toString`.
