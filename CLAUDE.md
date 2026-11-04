# CLAUDE.md

Guide for working in this repository. The code lives in `Application/` (Spring Boot 4, Java 17, MongoDB, JWT).

## Resume here

Branch `feature/parity-hardening` (from `feature/cart-order-flow` @ `ffbb6fb`), one commit + push per block. Plan:
Block 0 state, 1 defects (stress-tests/REPORT.md F1-F7), 2 missing features, 3 `memory` profile + demo seed + E2E
tests, 4 JaCoCo / guard tests / SETUP.md / Postman, 5 Docker, 6 closing campaign, docs, merge to `main`.

- **Block 0 — done.** `feature/cart-order-flow` pushed to origin (269 tests green, 0 skipped).
  `nexusmarket_demo` copied to `nexusmarket_demo_backup` (13 collections, counts match; nothing deleted).
- **Block 1 — core done, 332 tests green.** Optimistic locking: `long version` on `InventoryItem`, `Order`,
  `Shipment`, `ReturnRequest`, `Refund`, `ShoppingCart`, `User` (`getVersion` / `restoreVersion`); Mongo documents
  carry `@Version Long version`, adapters translate `OptimisticLockingFailureException` into
  `ConcurrentUpdateException` (409) via `MongoVersioning`. `VersionFieldMigration` (startup, idempotent,
  `nexus.mongo.migrations.enabled`) adds `version: 0` and normalizes emails; run on `nexusmarket_migration_check`
  (copy of the demo backup): 22 documents versioned, 0 left. `OptimisticRetry` (10 retries, random 1-20 ms) on every
  inventory write. Guard-first order in Cancel / Advance / ApproveReturn / ProcessRefund / Dispatch / ConfirmDelivery /
  ConfirmOrder (versioned cart delete, cart restored on failure); `OrderFollowUp` for the order after a shipment step.
  Insert-only `insert` on users, inventory, invoices. Login: dummy BCrypt, email normalized (`EmailAddress`),
  `LoginThrottleFilter` + `LoginAttemptLimiter` (429 + Retry-After), `RequestBodyLimitFilter` (413), DuplicateKey and
  OptimisticLocking → 409, path-prefix role rules + `denyAll`. Admin cannot change own status (403). `UserFactory`
  registry (+ `SystemRole.extension`) replaces role if-chains. In-memory adapters (`adapters/out/persistence/memory`,
  profile `memory`) with insert-only and versioning; `DocumentReferenceResolver` shared with Mongo.
  Tests: `ConcurrencyScenariosTest` (60 buyers / 10 units → 10), `PersistenceGuaranteesTest`,
  `MongoVersioningIntegrationTest`, `SecurityHardeningRestControllerTest`, `RequestLimitsTest`, `UserFactoryTest`.
- **Block 1 — pending (deferred by the user to the end):** restart the app on `nexusmarket_migration_check` once more
  and confirm no migration log line (idempotence on real data; already covered by the integration test); SDD updates
  for Block 1 (Domain Model, Output-ports, Persistence, Rest-security-cors, services); verify the demo flow on the
  migrated `nexusmarket_demo`.
- **Block 3 — done, 341 tests green.** Profile `memory`: `application-memory.properties` excludes every Mongo
  auto-configuration; the in-memory adapters serve every output port; `nexus.security.jwt.generate-if-missing=true`
  signs with a random key when `NEXUS_JWT_SECRET` is unset. One command: `./mvnw spring-boot:run
  -Dspring-boot.run.profiles=memory` (the plugin artifacts are now in `~/.m2`). `DemoDataSeeder`
  (`nexus.seed.demo.enabled`, on by default only in `memory`): `admin|seller|buyer|logistics|supervisor@demo.nexus`,
  warehouse `WH-DEMO-1`, `PRD-DEMO-1..3` physical with stock 50/30/10, `PRD-DEMO-4` digital; password from
  `NEXUS_DEMO_PASSWORD`, otherwise generated and logged once. Tests: `EndToEndScenariosTest` (real HTTP, random port:
  all roles, product lifecycle, sale to delivery + invoice, return + refund, 403 matrix, duplicate id 409,
  logout / password change, 60 buyers over HTTP for 10 units → 10), `DemoDataSeederTest`. The rejected-payment
  scenario comes with Block 2.
- **Block 2 — in progress (user order: payment → PUT/DELETE product → PUT /api/me → lists and reports; commit +
  push after every step; verify on port 8081 only, never 8080, which the user uses for manual demos).**
  - **Step 1 — buyer payment, done.** `PaymentGatewayPort` + `SimulatedPaymentGatewayAdapter` (approves, reference
    `SIM-<orderId>`; the test token `DECLINED` is always declined). `PayOrderService` (BUYER + ownership through
    `AuthorizeOrderAccessService`; only `PENDING_PAYMENT`, else 409; declined → `PaymentDeclinedException` 402, order
    unchanged, `PAYMENT_DECLINED` audited; approved → versioned save to `PAID`, invoice insert, `ORDER_PAYMENT`
    audited). `POST /api/buyer/orders/{orderId}/payment` (`PayOrderRequest.paymentToken`, never echoed). Digital
    products: no stock reserved (`OrderLine.holdsStock()` false, no warehouse), and `FulfillDigitalOrderService`
    moves a digital-only order PAID → SHIPPED → DELIVERED after either payment path (`DIGITAL_ORDER_FULFILLMENT`).
    Before this, confirming a digital product failed with insufficient stock. Tests: `BuyerPaymentScenariosTest`,
    `BuyerPaymentRestControllerTest`, two new `EndToEndScenariosTest` cases. 362 tests green; checked on 8081.
  - **Next:** step 2, `PUT /api/seller/products/{id}` and `DELETE` (logical, `DISCONTINUED`).
- **After Block 2:** Block 4 (SETUP.md and Postman first; JaCoCo and guard tests last) → remaining Block 1, JaCoCo,
  Block 5, Block 6. Move the `demo-stable` tag to every green block commit.
- Verify with `./mvnw -o clean test` from `Application/` (Maven is not on the PATH; use the wrapper). Scratchpad
  launchers: `run-app.sh <db> <port> <log>` (Mongo) and `run-memory-8081.sh` (memory profile on 8081).
- **Local demo launcher (not committed):** `demo-local/iniciar-demo-memory.bat` (memory profile on 8080, copies
  classes and jars to `demo-local/runtime`, demo password only in that file).

## Language Rule (permanent)

**Every artifact is written in English, with no exceptions.** This covers code, names, Javadoc, comments, exception
and log messages, commit messages, every `.md` file in `SDD/`, and this `CLAUDE.md`. Do not generate Spanish versions
or translations of any artifact.

## Commands

Run from `Application/`:

- Compile: `./mvnw -o clean compile` (Maven is not on the PATH; the wrapper uses `~/.m2/wrapper`)
- Tests (including the Spring context load): `./mvnw -o clean test`
- Run without a database: `./mvnw spring-boot:run -Dspring-boot.run.profiles=memory` (demo data, password in the log
  or from `NEXUS_DEMO_PASSWORD`)

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
- **Swagger UI — done.** springdoc-openapi `springdoc-openapi-starter-webmvc-ui` 3.1.1 (built on Spring Boot 4.1.0;
  checked on Maven Central before adding it; 3.2.0 targets 4.1.2 but was only two days old). UI at `/swagger-ui.html`,
  document at `/v3/api-docs`. `SecurityConfig.API_DOCUMENTATION_PATHS` permits only `GET` on `/swagger-ui.html` (the
  redirect into the UI), `/swagger-ui/**` and `/v3/api-docs/**`. `infrastructure/config/OpenApiConfiguration` declares
  the `bearerAuth` scheme for the Authorize button and removes it from login and registration. Documented in
  `SDD/Adapters/Rest-adapters.md`; tested by `ApiDocumentationTest`.
- **Tests** (no MongoDB needed): one `*RestControllerTest` per controller, all extending `RestControllerTestSupport`
  (full context, real security, every role port mocked plus `UserRepositoryPort`, so a single cached context serves
  all of them), `ApiDocumentationTest` and `AuditRestControllerTest` on the same context, and `RestMappersTest`.

## Status of the Alignment with the Professor's Business Model

Run as one execution in 3 blocks (each compiled, tested, recorded here and committed locally; pushed at the end).

- **Block 1 — domain auditing, done.**
  - Models `Operation` (identifier, `OperationType`, executionDate, performedBy, `AffectedEntityType` + affected
    entity id) and `AuditLog` (auditId, operationType, operationDate, performedBy, userRole, affected entity,
    `details`). Value Objects `OperationType` (33 codes, one per state-changing operation) and `AffectedEntityType`.
    `AuditLog` rejects detail keys naming a password, hash, token or secret. `InventoryItem.getIdentifier()` is
    `productId:warehouseId`.
  - Internal services (wired in `InternalServiceBeanConfiguration`): `RegisterOperationService`,
    `RegisterAuditLogService`, `RegisterOperationAndAuditService.execute(Operation, Map details)`; package-private
    helper `AuditDetails.of(...)` keeps detail order and drops null values. Every state-changing service (31) calls
    it after saving, with `previousStatus` / `newStatus` where they apply. Successful logins are audited, failed
    ones are not. The internal stock services (`ReserveInventoryService`, `ReleaseInventoryReservationService`,
    `ConfirmInventoryOutboundService`) are covered by their calling operation; `ApproveReturnService` also produces
    one `INVENTORY_RETURN` per line through `RegisterInventoryReturnService`.
  - `ConsultOperationsService` / `ConsultAuditLogsService` (`ADMINISTRATOR`, `SUPERVISOR`), on `AdministratorPort` and
    `SupervisorPort`: `GET /api/admin|supervisor/operations` and `/audit-logs`, most recent first, no pagination.
  - Output ports `OperationRepositoryPort` and `AuditLogRepositoryPort` replace `AuditLogPort`. Mongo collections
    `operations` and `audit_logs` are append-only: the Spring Data repositories extend the bare `Repository` and
    declare only `insert` and reads. Indexes on type, date, performer + date and affected entity. Legacy
    `audit_logs` entries (no `operationType`) are skipped when reading.
  - Removed: per-request auditing in `JwtAuthenticationFilter`, `AuditOperationService`, `AuditLogPort`,
    `MongoAuditLogAdapter`.
  - Tests: one audit assertion per state-changing service (`*AuditTest` classes on `AuditedServiceTestSupport`),
    `AuditServicesTest`, `AuditMappersTest`, `AuditRestControllerTest`. 156 tests green.
- **Block 2 — logout and password change, done.**
  - `User.authTokenVersion` (int, starts at 0) with `bumpAuthTokenVersion()`; `restoreAuthTokenVersion(int)` is only
    for mappers rebuilding a stored user. Stored as `users.authTokenVersion`; documents without it read as 0.
  - The JWT carries the claim `ver`; `JwtAuthenticationFilter` rejects (401) a token whose `ver` differs from the
    stored user's version, and `JwtProvider` rejects tokens without `ver`.
  - `LogoutService` (loads the stored user, bumps, saves, audits `USER_LOGOUT`; idempotent, no status check) and
    `ChangeUserPasswordService` (stored user must be `ACTIVE`, current password must match →
    `IncorrectCurrentPasswordException`, new password ≥ 8 characters → `InvalidPasswordException`, BCrypt hash, bump,
    audits `PASSWORD_CHANGE`). Neither audits a password.
  - `logout(User)` and `changePassword(User, String, String)` on the 5 role ports (not `PublicAccessPort`), delegated
    by each `*UseCaseImpl`; `POST /api/{buyer|seller|admin|logistics|supervisor}/logout` and `/password`
    (`ChangePasswordRequest`, new password 8-72), both `204`. After either call every earlier token answers `401`.
  - `SecurityConfig`: Spring's session logout stays disabled (there is no session, and mapped to these paths it would
    answer before the controllers); `SESSION_PATHS` require any valid token (redundant with `anyRequest()`, kept as
    explicit intent), the controllers then restrict them to their role.
  - Tests: `SessionServicesTest`, `SessionRestControllerTest` (5 roles, revoked token → 401), `UserTest`,
    `UserMapperTest`. 183 tests green.
- **Block 3 — one exception per business rule, done.** `DomainException` stays the base;
  `BusinessRuleViolationException` was replaced everywhere and deleted. New exceptions and their status in
  `GlobalExceptionHandler` (each one listed explicitly):
  - `409` (the state of the data does not allow it): `EmptyCartException`, `ProductNotPurchasableException` (not
    `PUBLISHED` or no price), `CurrencyMismatchException` (cart or `Money` currencies), `OrderNotShippableException`,
    `EmptyOrderException`, `ReturnNotEligibleException`, `ReturnAlreadyDecidedException`,
    `RefundAlreadyProcessedException`, `InactiveWarehouseException`, `InventoryNotTrackedException` (DIGITAL), plus
    the existing duplicate, stock and transition exceptions.
  - `403`: `BuyerNotEnabledException` (buyer not `ACTIVE` or suspended), with `OperationNotAllowedException` and
    `SellerNotAuthorizedException`.
  - `400` (a client value breaks a rule): `InvalidQuantityException` (replaces the `IllegalArgumentException`s of
    `CartLine`, `OrderLine`, `InventoryItem`, `ShoppingCart`), `InvalidProductPriceException`, `InvalidMoneyException`
    (replaces plain `DomainException` in `Money`), `InvalidPasswordException`, `IncorrectCurrentPasswordException`,
    `OrderStatusRequiresShipmentException`. Any other `DomainException` → `400`.
  - The remaining `IllegalArgumentException`s are programming guards (null product status, audit detail keys,
    negative token version) and still answer `400`.
  - Tests: `DomainExceptionMappingTest` (every exception → status), `BusinessRuleExceptionsTest`, and the updated
    `ShoppingCartTest`, `MoneyTest`, `InventoryItemMapperTest`, `BuyerRestControllerTest`. 219 tests green.
- **Documentation and real verification — done.** Updated `Domain Model.md`, `Domain Value Objects.md`,
  `Domain Services.md`, every file in `SDD/Domain/services/` (with an "Audit" section per state-changing service and a
  "Recorded Operations" table in `audit.md`), `Input-ports.md`, `Output-ports.md`, `Software Architecture.md`, every
  adapter doc (`Api-rest-endpoints.md`, `Rest-security-cors.md`, `Global-exception-handler.md`, `Persistence.md`,
  `Rest-adapters.md`, `Use-cases-adapters.md`), all of `SDD/C4/` (63 endpoints) and `README.md`. Verified with the
  app running on `nexusmarket_demo`: logins, registration, publication and status change wrote matching
  `operations` / `audit_logs` entries (checked with mongosh, no credentials in details); logout and password change
  made the previous token answer `401`.
  - Known leftovers: databases created before Block 1 keep the old `audit_logs` index `user_occurred` and the
    legacy entries (skipped on read); audit inserts are not transactional with the business write; the initial
    administrator seeder records no operation; `USER_LOGIN` is recorded after issuing the token (nothing is saved).

## Status of the `feature/cart-order-flow` Branch (sales flow)

Additive changes (existing routes keep working), one local commit per block, merge to `main` only when all three
blocks are green. Plan:

- **Block 1 — insert-only creation, done (`183d939`).** `insert(...)` on the 7 output ports (cart, order, product,
  warehouse, return request, shipment, refund); the Mongo adapters run a real `insert` and `MongoInsertion` turns
  `DuplicateKeyException` into `DuplicateResourceException` (409). `ConfirmOrderService` rejects a taken order id
  before reserving stock and releases every reservation if the insert loses a race. Tests: `IdentifierCollisionTest`,
  `MongoInsertOnlyIntegrationTest` (real local MongoDB, throwaway database, skipped without a server).
- **Block 2 — automatic ids and active cart, done (`86b1b7d`).** Optional identifier (blank → 400) for carts, orders,
  products and warehouses; `GeneratedIdentifiers` (package-private) generates `CART-` / `ORD-` / `PRD-` / `WH-` + 8 hex
  digits, skipping ids in use. At most one cart per buyer: `CreateShoppingCartService` answers 409 for a second one, and
  `shopping_carts.buyerId` is unique through `ActiveCartIndexInitializer` (not `@Indexed`: it drops the legacy
  non-unique `buyerId` index first; logs an error instead of failing if data already has several carts per buyer, as
  `nexusmarket_stress` does). Routes `GET /api/buyer/cart`, `POST /api/buyer/cart/items`,
  `PUT|DELETE /api/buyer/cart/items/{productId}`, `POST /api/buyer/orders` (body optional) on 5 new `BuyerPort`
  methods and services (`ConsultActiveCartService`, `AddActiveCartLineService`, `UpdateActiveCartLineQuantityService`,
  `RemoveActiveCartLineService`, `ConfirmActiveCartService`) that delegate to the existing ones. `Order.getTotal()` /
  `OrderLine.getSubtotal()`; `OrderResponse.total`. Tests: `ActiveCartAndGeneratedIdentifierTest`,
  `ActiveCartRestControllerTest`, one more case in `MongoInsertOnlyIntegrationTest`.
- **Block 3 — payment and invoice, done (`f3b3dec`).** `RegisterOrderPaymentService` (`ADMINISTRATOR`, delegates to
  `AdvanceOrderStatusService` with `PAID`: invoice + `ORDER_STATUS_ADVANCE` audit) on
  `AdministratorPort.registerOrderPayment` → `POST /api/admin/orders/{orderId}/payment`. `ConsultOrderInvoiceService`
  (`BUYER` / `ADMINISTRATOR` / `SUPERVISOR` + `AuthorizeOrderAccessService`; 404 "has no invoice yet") on the three
  ports → `GET /api/{buyer|admin|supervisor}/orders/{orderId}/invoice` (`InvoiceResponse`, `InvoiceRestMapper`).
  `ConsultOrderShipmentService` (`BUYER` + ownership) → `GET /api/buyer/orders/{orderId}/shipment`. Tests:
  `PaymentInvoiceShipmentServicesTest`, `PaymentInvoiceRestControllerTest`.
- **Then:** tests for every new service and endpoint (happy path, forbidden role, foreign ownership); a curl run on
  `nexusmarket_demo` with automatic ids (login, add product, confirm, pay, ship to delivery, invoice) plus a duplicate
  id showing the 409 and the unchanged data; update `Api-rest-endpoints.md`, `Input-ports.md`, `Persistence.md`, the
  affected C4 views, the service files, `README.md` and this file; merge to `main`.
- **Last verification:** 2026-11-03, after the Block 3 commit: `mvn -o clean test`, 269 tests green, including
  the 6 integration tests run against the real MongoDB (not skipped). Real run on `nexusmarket_demo` (app on 8080,
  driver script in the session scratchpad, run id `1f48d4`): generated `PRD-` / `WH-` / `CART-` / `ORD-` ids, second
  cart 409, order total 45000 COP, payment 200 then 409, shipment to `DELIVERED`, invoice for owner and admin, foreign
  buyer 403, existing order id 409 with the order unchanged and no stock reserved, `operations` == `audit_logs`. On
  startup `ActiveCartIndexInitializer` replaced the demo's non-unique `buyerId` index. **Next:** docs (SDD, C4,
  README), then merge to `main`. Not merged, not pushed.
- **Caution:** the VS Code Java extension compiles into the same `target/`; if a test fails with
  "Unresolved compilation problems" (Eclipse compiler output), rerun with `clean`. It can also lock `target/` briefly.

## Documentation and Delivery

- `README.md` (repo root): overview, requirements, environment variables, build/test/run (including the offline
  `java -cp` route, since `mvn -o spring-boot:run` lacks plugin artifacts), links to the SDD.
- `SDD/C4/`: C4 model in Mermaid (levels 1-4, one view per layer, endpoint matrix and sequence diagrams per role),
  built from the code. Keep it in sync when routes, ports or classes change.
- `SDD/Domain/Output-ports.md`: every output port with its signature, users and adapter.
- `SDD/Domain/Domain Services.md` is an index (conventions, catalog tables, collaboration diagram); the detailed
  service descriptions live in `SDD/Domain/services/` (one file per area).
- `Application/Dockerfile` (multi-stage) and `Application/docker-compose.yml` (API + `mongo:7`): written but never
  run, because Docker is not installed on the development machine. `.env` is git-ignored.

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

- **JWT and fresh state:** the token carries `sub`, `role`, `email` and `ver`, but `JwtAuthenticationFilter` reloads the
  user from the database on every request and rejects it if it is not `ACTIVE`, its stored role differs from the
  token, or its `authTokenVersion` differs from `ver` (logout and password change revoke tokens this way).
- **Abstract `User` targets in REST:** the client sends the target user's role (body of `PATCH /users/{id}/status`,
  `?role=` on `GET /users/{id}`) and the mapper builds that subtype. The services only read the identifier.
- **Login errors:** a wrong email, a wrong password and a user who is not `ACTIVE` all answer `401` with
  `"Invalid email or password"` (decided in `AuthenticateUserService`). `expiresAt` is not part of the login response.
- **Passwords:** 8 to 72 characters in `RegisterBuyerRequest` and `RegisterStaffRequest`. Passwords and tokens never
  appear in logs or in `ErrorResponse` (records mask them in `toString`; validation errors omit rejected values).

## Working Rules

- Work in batches; compile with `mvn -o clean compile` after each step and do not move to the next batch without
  the user's confirmation.
- Do not invent port methods: each `*UseCaseImpl` implements exactly what its port declares.
- `passwordHash` never appears in DTOs; `/me` reads the authenticated principal, not `ConsultUserService`.
