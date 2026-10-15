# CLAUDE.md

Guía para trabajar en este repositorio. El código vive en `Application/` (Spring Boot 4, Java 17, MongoDB, JWT).

## Comandos

Ejecutar desde `Application/`:

- Compilar: `mvn -o clean compile`
- Tests (incluye carga del contexto de Spring): `mvn -o clean test`

`ApplicationTests.contextLoads` arranca el contexto completo sin necesitar MongoDB levantado: el driver registra
`MongoSocketOpenException` en el hilo monitor, pero el contexto carga igual. El secreto JWT de test está en
`src/test/resources/application.properties`.

## Arquitectura (hexagonal, alineada con la del profesor)

- `domain/models`, `domain/valueobjects`, `domain/exceptions`: modelo de dominio.
- `domain/services`: Domain Services, clases concretas `@Service` que reciben modelos de dominio y contienen
  toda la lógica de negocio y las validaciones de autorización.
- `domain/services/authorization`: servicios de autorización (`ValidateRoleAuthorizationService`,
  `AuthorizeCartAccessService`, `AuthorizeOrderAccessService`, ...). Se cablean como beans en
  `infrastructure/config/AuthorizationBeanConfiguration`.
- `domain/ports/in`: **un puerto de entrada por rol**: `PublicAccessPort`, `BuyerPort`, `SellerPort`,
  `AdministratorPort`, `LogisticsOperatorPort`, `SupervisorPort`.
- `adapters/useCases`: una clase `*UseCaseImpl` por puerto (`@Service` + `@RequiredArgsConstructor`). Cada método
  **solo delega** al Domain Service correspondiente: sin `if`, sin validaciones, sin lógica.
- `domain/ports/out` + `adapters/out/persistence`: persistencia Mongo.
- `adapters/in/rest`: controladores, mappers y DTOs REST.
- `infrastructure/config`, `infrastructure/security`: configuración de beans y seguridad JWT.

## Estado del refactor de `domain/ports/in`

- **Lote 1 — hecho.** Los 6 puertos por rol existen en `domain/ports/in`.
- **Lote 2 — hecho.** Los servicios son clases concretas `@Service` que reciben modelos de dominio; lista de cambios
  de lógica aprobada.
- **Lote 3 — hecho.** `adapters/useCases` con `package-info.java` y los 6 `*UseCaseImpl`. Verificado: compila y el
  contexto de Spring crea los 6 beans (uno por puerto) sin dependencias circulares ni beans faltantes.
- **Lote 4 — hecho.** Javadoc de `package-info` de `domain/ports/in`, `domain/services` y
  `adapters/in/rest/controllers` actualizado a la arquitectura por roles.
- **Lote 5 — hecho.** Borradas las 41 interfaces antiguas `*UseCase` (una por operación, con su record `Command`)
  de `domain/ports/in`, tras confirmar con grep que no quedaba ninguna referencia en `src/main` ni `src/test`.
  Compila y el contexto de Spring carga.
- **Lote 6 — pendiente.** No empezar sin confirmación explícita del usuario.

Los documentos `SDD/Domain/Domain Services.md` y `SDD/Software Architecture/Software Architecture.md` todavía
mencionan las interfaces `*UseCase` borradas.

## Decisiones tomadas

- **Carritos, solo `BUYER`:** `AddCartLineService`, `UpdateCartLineQuantityService`, `RemoveCartLineService` y
  `ConsultCartService` validan `ValidateRoleAuthorizationService` con `SystemRole.BUYER` antes de
  `AuthorizeCartAccessService`. El administrador **no** modifica ni consulta líneas de carrito.
  `ConfirmOrderService` sigue permitiendo `BUYER` y `ADMINISTRATOR`.
- **`CreateShoppingCartService` sin cambios:** ya exige que el actor sea un `Buyer`.
- **Operador logístico consulta pedidos:** `AuthorizeOrderAccessService` permite `LogisticsOperator`. Es una
  excepción puntual a la regla 7, solo para esto; no tocar ninguna otra regla de ese servicio. `CancelOrderService`
  y `RequestReturnService` validan rol antes, así que en la práctica solo afecta a `ConsultOrderService`.
- **`LogisticsOperatorPort.consultOrder(User, Order)`** añadido; `LogisticsOperatorUseCaseImpl` delega a
  `ConsultOrderService`.
- **Almacenes:** `AdministratorPort.registerMarketplaceWarehouse` y `SellerPort.registerWarehouse` delegan ambos en
  `RegisterWarehouseService`, que decide el tipo de almacén según el rol del actor.

## Reglas de trabajo

- Trabajar por lotes; compilar con `mvn -o clean compile` al terminar cada paso y no avanzar al siguiente lote sin
  confirmación del usuario.
- No inventar métodos en los puertos: los `*UseCaseImpl` implementan exactamente lo que declara su puerto.
- `passwordHash` nunca aparece en DTOs; `/me` lee el principal autenticado, no `ConsultUserUseCase`.
