# CLAUDE.md

Guide for working in this repository. The code lives in `Application/` (Spring Boot 4, Java 17, MongoDB, JWT).

## Language Rule (permanent)

**Every artifact is written in English, with no exceptions.** This covers code, names, Javadoc, comments, exception
and log messages, commit messages, every `.md` file in `SDD/`, and this `CLAUDE.md`. Do not generate Spanish versions
or translations of any artifact.

## Commands

Run from `Application/`:

- Compile: `mvn -o clean compile`
- Tests (including the Spring context load): `mvn -o clean test`

`ApplicationTests.contextLoads` starts the full context without a running MongoDB: the driver logs
`MongoSocketOpenException` in its monitor thread, but the context still loads. The test JWT secret is in
`src/test/resources/application.properties`, which also sets `spring.data.mongodb.auto-index-creation=false`
(index creation at startup needs a MongoDB server). That file shadows the main `application.properties` in tests.

## Environment Variables

| Variable | Required | Purpose |
| -------- | -------- | ------- |
| `NEXUS_JWT_SECRET` | Yes | Base64 HMAC key, at least 32 bytes. Without it the application does not start. |
| `NEXUS_MONGO_URI` | No | MongoDB URI; defaults to `mongodb://localhost:27017/nexusmarket`. |
| `NEXUS_ADMIN_EMAIL`, `NEXUS_ADMIN_PASSWORD` | No | Initial administrator, created at startup only when no administrator exists. |
| `NEXUS_CORS_ORIGINS` | No | Comma-separated origins allowed by CORS; defaults to `http://localhost:3000,http://localhost:5173`. |

The application creates the MongoDB indexes at startup (`spring.data.mongodb.auto-index-creation=true`), so it needs
a reachable MongoDB server to start.

## Architecture (hexagonal, aligned with the professor's architecture)

- `domain/models`, `domain/valueobjects`, `domain/exceptions`: domain model.
- `domain/services`: Domain Services, concrete `@Service` classes that receive domain models and hold all the
  business logic and authorization checks.
- `domain/services/authorization`: authorization services (`ValidateRoleAuthorizationService`,
  `AuthorizeCartAccessService`, `AuthorizeOrderAccessService`, ...). They are wired as beans in
  `infrastructure/config/AuthorizationBeanConfiguration`.
- `domain/ports/in`: **one input port per role**: `PublicAccessPort`, `BuyerPort`, `SellerPort`,
  `AdministratorPort`, `LogisticsOperatorPort`, `SupervisorPort`.
- `adapters/useCases`: one `*UseCaseImpl` class per port (`@Service` + `@RequiredArgsConstructor`). Each method
  **only delegates** to the corresponding Domain Service: no `if`, no validations, no logic.
- `domain/ports/out` + `adapters/out/persistence`: MongoDB persistence.
- `adapters/in/rest`: REST layer (see `SDD/Adapters/Rest-adapters.md`): one controller per role port, each injecting
  only its port, `@PreAuthorize` by role at class level; request/response records with Jakarta Validation; static
  mappers (`DomainReferences` builds references from identifiers); `GlobalExceptionHandler` with the uniform
  `ErrorResponse`.
- `infrastructure/config`, `infrastructure/security`: bean configuration and JWT security.

## Status of the `domain/ports/in` Refactor

- **Batch 1 — done.** The 6 role ports exist in `domain/ports/in`.
- **Batch 2 — done.** Services are concrete `@Service` classes that receive domain models; the list of logic changes
  was approved.
- **Batch 3 — done.** `adapters/useCases` with `package-info.java` and the 6 `*UseCaseImpl` classes. Verified: it
  compiles and the Spring context creates the 6 beans (one per port) with no circular dependencies or missing beans.
- **Batch 4 — done.** `package-info` Javadoc of `domain/ports/in`, `domain/services` and
  `adapters/in/rest/controllers` updated to the role-based architecture.
- **Batch 5 — done.** Deleted the 41 old `*UseCase` interfaces (one per operation, each with its `Command` record)
  from `domain/ports/in`, after confirming with grep that no reference remained in `src/main` or `src/test`.
  It compiles and the Spring context loads.
- **Batch 6 — done.** SDD documentation aligned with the role-based architecture:
  - New: `SDD/Domain/Input-ports.md` (Role Isolation and Domain Model Parameters principles, definition of the 6
    ports, note on the abstract `User`) and `SDD/Adapters/Use-cases-adapters.md` (role of the layer, flow,
    port → impl → services table, example).
  - Updated: `Software Architecture.md` (package tree, dependency flow, constraints 11 and 12, config without
    per-use-case `*BeanConfiguration` classes, ports table), `Domain Services.md` (concrete services receiving domain
    models, "Parameters" sections instead of "Command", authorization changes of the refactor) and one line of
    `Persistence.md`.
  - Verified with grep: no reference to the `*UseCase` interfaces remains in `SDD/`.

## Status of the Domain Model Hardening

- **Batch A — done.** Removed `User.setRole` (the role is fixed by each subclass constructor) and the quantity
  setters of `InventoryItem` (stock only changes through its behavior methods).
- **Batch B — done.** `Money` value object (`amount` + `Currency`, never negative, equality ignoring scale) and
  `Currency` catalog (`COP`, `USD`). Used by `Product.price`, `OrderLine.unitPrice`, `Invoice.totalAmount` and
  `Refund.amount`; persisted as an embedded `{ amount: Decimal128, currency }` subdocument (`MoneyDocument`,
  `MoneyMapper`).
- **Batch C — done.** Behavior moved into the entities: `Product.changeStatus` / `replaceVariants`,
  `Warehouse.deactivate`, `ShoppingCart.addLine` / `updateLineQuantity` / `removeLine`; `OrderLine` is immutable.
  `Order` has no setters: identifier, buyer, lines and `createdAt` are fixed by the constructor, lines are read-only.
  Unused setters were removed and mappers rebuild entities through constructors.
- **Batch D — done.** `Domain Model.md`, `Domain Value Objects.md`, `Domain Services.md` and `Persistence.md`
  updated (Money, Currency, entity behavior, `AuthenticationResult`).
- **Batch E — done.**
  - Removed `User.setIdentifier` and `User.setEmail`: identifier, email and role are fixed by the constructor.
  - Indexes are created at startup with `spring.data.mongodb.auto-index-creation=true`. Verified in the
    `spring-boot-data-mongodb-4.1.1` metadata: unlike the connection URI (`spring.mongodb.*`), this property keeps
    the `spring.data.mongodb` prefix.
  - `infrastructure/seed/InitialAdministratorSeeder` creates the first administrator from `NEXUS_ADMIN_EMAIL` /
    `NEXUS_ADMIN_PASSWORD` (identifier `INITIAL-ADMIN`), idempotently; without the variables it only logs.
    `UserRepositoryPort.existsByRole` was added for it.
- **Unit tests** (no MongoDB needed): `UserMapperTest`, `InventoryItemMapperTest`, `MoneyMapperTest`, `MoneyTest`,
  `ShoppingCartTest`, `OrderTest`, `UserTest`, `InitialAdministratorSeederTest`.

## Status of the REST Layer

- **Done.** Six controllers (`AuthRestController` + one per role) covering every port method, plus `GET /api/me`.
  Endpoints in `SDD/Adapters/Api-rest-endpoints.md`; security in `Rest-security-cors.md`; validation in
  `Rest-validation.md`; errors in `Global-exception-handler.md`.
- **Tests** (no MongoDB needed): one `*RestControllerTest` per controller, all extending `RestControllerTestSupport`
  (full context, real security, every role port mocked plus `UserRepositoryPort` and `AuditLogPort`, so a single
  cached context serves all of them), and `RestMappersTest`.

## Decisions

- **Carts, `BUYER` only:** `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService` and
  `ConsultCartService` call `ValidateRoleAuthorizationService` with `SystemRole.BUYER` before
  `AuthorizeCartAccessService`. Administrators do **not** modify or read cart lines.
  `ConfirmOrderService` still allows `BUYER` and `ADMINISTRATOR`.
- **`CreateShoppingCartService` unchanged:** it already requires the actor to be a `Buyer`.
- **Logistics operators consult orders:** `AuthorizeOrderAccessService` allows `LogisticsOperator`. This is a scoped
  exception to rule 7, for this purpose only; do not change any other rule of that service. `CancelOrderService` and
  `RequestReturnService` check the role first, so in practice it only affects `ConsultOrderService`.
- **`LogisticsOperatorPort.consultOrder(User, Order)`** added; `LogisticsOperatorUseCaseImpl` delegates to
  `ConsultOrderService`.
- **Warehouses:** `AdministratorPort.registerMarketplaceWarehouse` and `SellerPort.registerWarehouse` both delegate to
  `RegisterWarehouseService`, which decides the kind of warehouse from the actor's role.
- **Staff registration:** `RegisterSellerService`, `RegisterSupervisorService` and `RegisterLogisticsOperatorService`
  all authorize with `ValidateRoleAuthorizationService` (`ADMINISTRATOR`), so the actor must be an active
  administrator.
- **Single currency per cart:** `ShoppingCart.addLine` rejects a product priced in another currency than the lines
  already in the cart, or without a price. Orders and invoices therefore never mix currencies.
- **Price required:** `PublishProductService` requires a price that is not null and greater than zero.
- **No data migration** for the new money format: documents stored before Batch B cannot be read.
- **Remaining setters** in `User`, `Buyer` and `Seller` are kept for now; do not remove them without the user's
  confirmation. Reviewed: `Buyer.setCarts` / `setOrders` and `Seller.setWarehouses` / `setProducts` can attach
  entities owned by someone else; the other setters break no business rule. (`User.setIdentifier` and
  `User.setEmail`, which could bypass the uniqueness rule, were removed in Batch E.)

- **JWT and fresh state:** the token carries `sub`, `role` and `email`, but `JwtAuthenticationFilter` reloads the user
  from the database on every request and rejects it if it is not `ACTIVE` or its stored role differs from the token.
- **Abstract `User` targets in REST:** the client sends the target user's role (body of `PATCH /users/{id}/status`,
  `?role=` on `GET /users/{id}`) and the mapper builds that subtype. The services only read the identifier.
- **Login errors:** a wrong email and a wrong password both answer `401` with `"Invalid email or password"`.
- **Passwords:** 8 to 72 characters in `RegisterBuyerRequest` and `RegisterStaffRequest`. Passwords and tokens never
  appear in logs or in `ErrorResponse` (records mask them in `toString`; validation errors omit rejected values).

## Working Rules

- Work in batches; compile with `mvn -o clean compile` after each step and do not move to the next batch without
  the user's confirmation.
- Do not invent port methods: each `*UseCaseImpl` implements exactly what its port declares.
- `passwordHash` never appears in DTOs; `/me` reads the authenticated principal, not `ConsultUserService`.
