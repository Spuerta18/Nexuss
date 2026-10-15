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
`src/test/resources/application.properties`.

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
- `adapters/in/rest`: REST controllers, mappers and DTOs.
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

## Working Rules

- Work in batches; compile with `mvn -o clean compile` after each step and do not move to the next batch without
  the user's confirmation.
- Do not invent port methods: each `*UseCaseImpl` implements exactly what its port declares.
- `passwordHash` never appears in DTOs; `/me` reads the authenticated principal, not `ConsultUserService`.
