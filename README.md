# NexusMarket

NexusMarket is the backend of a digital marketplace that mediates between buyers and sellers. It manages catalogs, Marketplace-owned and Seller-owned warehouses, distributed stock, shopping carts, the order lifecycle, shipments, invoices, returns and refunds, through a REST/JSON API secured with JWT.

## Architecture

* **Hexagonal (ports and adapters) with DDD**: the domain (`domain/`) holds entities with behavior, value objects (`Money`, catalogs) and one Domain Service per operation; it depends on no framework adapter.
* **One input port per role** (`PublicAccessPort`, `BuyerPort`, `SellerPort`, `AdministratorPort`, `LogisticsOperatorPort`, `SupervisorPort`), implemented by `adapters/useCases/*UseCaseImpl` classes that only delegate to the services.
* **REST adapter** (`adapters/in/rest`): one controller per role port, `@PreAuthorize` by role, request/response records with Jakarta Validation, a uniform error body.
* **MongoDB adapter** (`adapters/out/persistence/mongodb`) implements the output ports with Spring Data; indexes are created at startup.
* **Security** (`infrastructure/security`): stateless JWT; the filter reloads the user on every request, so blocking a user takes effect immediately.

Full design: [SDD](SDD/) — start with the [C4 model](SDD/C4/README.md).

## Requirements

| Tool | Version |
| ---- | ------- |
| JDK | 17 or later (the code compiles with `release 17`) |
| Maven | 3.9 or later (or the wrapper `Application/mvnw`) |
| MongoDB | 7 or later, reachable at startup (tested with 9.0.2) |
| Docker | Optional, for `Application/docker-compose.yml` |

Stack: Spring Boot 4.1.1, Spring Security, Spring Data MongoDB (driver 5.8.1), jjwt 0.13.0, Lombok.

## Environment Variables

| Variable | Required | Purpose |
| -------- | -------- | ------- |
| `NEXUS_JWT_SECRET` | Yes | Base64 HMAC key of at least 32 bytes (e.g. `openssl rand -base64 48`). The application does not start without it. Never commit it. |
| `NEXUS_MONGO_URI` | No | MongoDB URI, including the database name. Default: `mongodb://localhost:27017/nexusmarket`. |
| `NEXUS_CORS_ORIGINS` | No | Comma-separated browser origins allowed by CORS. Default: `http://localhost:3000,http://localhost:5173`. |
| `NEXUS_ADMIN_EMAIL`, `NEXUS_ADMIN_PASSWORD` | No | Initial administrator, created at startup only if no administrator exists. Without them nothing is created. |

The API listens on port **8080**.

## Build and Test

All commands run from `Application/`:

```bash
mvn clean compile        # compile
mvn clean test           # unit tests + MockMvc tests (no MongoDB needed)
```

The tests do not need a MongoDB server: they mock the persistence ports and disable index creation. Add `-o` to work offline once the dependencies are in the local Maven repository.

## Run

MongoDB must be running first: the application creates its indexes at startup.

**With Maven (online)** — the Spring Boot plugin needs some artifacts the first time:

```bash
export NEXUS_JWT_SECRET="<base64 key>"          # PowerShell: $env:NEXUS_JWT_SECRET = "<base64 key>"
mvn spring-boot:run
```

**Offline, without the Spring Boot plugin** — build the runtime classpath once and start the main class with `java`:

```bash
mvn -o dependency:build-classpath -Dmdep.outputFile=target/classpath.txt -Dmdep.includeScope=runtime
mvn -o compile
java -cp "target/classes:$(cat target/classpath.txt)" nexussMarket.Application        # Linux/macOS
```

```powershell
# Windows PowerShell (the classpath is longer than the 8191-character limit of a CMD command line)
$env:NEXUS_JWT_SECRET = "<base64 key>"
java -cp ("target\classes;" + (Get-Content target\classpath.txt -Raw).Trim()) nexussMarket.Application
```

**With Docker** (not part of the tested setup):

```bash
cd Application
NEXUS_JWT_SECRET="<base64 key>" docker compose up --build
```

Then log in with `POST /api/auth/login` and send the returned token as `Authorization: Bearer <token>`.

The Swagger UI is at `http://localhost:8080/swagger-ui.html` (no token needed to open it; use **Authorize** with the `accessToken` to call protected endpoints). The OpenAPI document is at `/v3/api-docs`.

## Documentation

| Area | Documents |
| ---- | --------- |
| Architecture | [Software Architecture](SDD/Software%20Architecture/Software%20Architecture.md), [C4 model](SDD/C4/README.md) |
| Domain | [Domain Model](SDD/Domain/Domain%20Model.md), [Value Objects](SDD/Domain/Domain%20Value%20Objects.md), [Domain Services](SDD/Domain/Domain%20Services.md), [Input Ports](SDD/Domain/Input-ports.md), [Output Ports](SDD/Domain/Output-ports.md) |
| REST API | [Endpoints](SDD/Adapters/Api-rest-endpoints.md), [REST Adapters](SDD/Adapters/Rest-adapters.md), [Security and CORS](SDD/Adapters/Rest-security-cors.md), [Validation](SDD/Adapters/Rest-validation.md), [Error handling](SDD/Adapters/Global-exception-handler.md) |
| Other adapters | [Use Case Adapters](SDD/Adapters/Use-cases-adapters.md), [Persistence (MongoDB)](SDD/Adapters/Persistence.md) |
| Contributors | [CLAUDE.md](CLAUDE.md) — project rules, decisions and status |
