# Layer — Infrastructure

## Purpose

`nexussMarket.infrastructure` holds technical configuration with no business logic: the beans that component scan does not create, the database settings (connection and index creation), the startup seed, and security (described in its own [view](08-layer-adapters-security.md)).

## Diagram

```mermaid
flowchart LR
    subgraph app["Application (@SpringBootApplication)"]
        scan["Component scan of nexussMarket.*<br/>@Service, @Component, @Repository, @RestController"]
    end
    subgraph config["infrastructure.config"]
        ABC["AuthorizationBeanConfiguration<br/>6 authorization services"]
        ISC["InternalServiceBeanConfiguration<br/>ReserveInventoryService, AuditOperationService,<br/>NotifyUserService"]
        OAC["OpenApiConfiguration<br/>OpenAPI document, bearerAuth scheme"]
    end
    subgraph database["infrastructure.database"]
        DBP["application.properties<br/>spring.mongodb.uri = NEXUS_MONGO_URI<br/>spring.data.mongodb.auto-index-creation = true"]
    end
    subgraph seed["infrastructure.seed"]
        SC["SeedConfiguration<br/>nexus.seed.admin.email / password"]
        IAS["InitialAdministratorSeeder<br/>(ApplicationRunner)"]
    end
    MONGO[("MongoDB")]

    scan --> config
    scan --> seed
    DBP -->|"creates @Indexed / @CompoundIndex at startup"| MONGO
    SC --> IAS
    IAS -->|"existsByRole, existsByEmail, existsById, save"| URP[["UserRepositoryPort"]]
    IAS -->|"hash"| PHP[["PasswordHasherPort"]]
```

## Notes

### Indexes created at startup

| Collection | Indexes (besides `_id`) |
| ---------- | ----------------------- |
| `users` | `email` (unique) |
| `products` | `status`, `sellerId` |
| `inventory_items` | `product_warehouse` (`productId`, `warehouseId`, unique), `productId` |
| `inventory_movements` | `product_warehouse_occurred` (`productId`, `warehouseId`, `occurredAt`) |
| `shopping_carts` | `buyerId` |
| `orders` | `buyerId` |
| `shipments` | `orderId` (unique) |
| `invoices` | `orderId` (unique), `buyerId` |
| `return_requests` | `orderId` (unique), `buyerId` |
| `refunds` | `returnRequestId` (unique) |
| `audit_logs` | `user_occurred` (`userId`, `occurredAt`) |
| `warehouses` | none |

* **Index creation** is enabled with `spring.data.mongodb.auto-index-creation=true`; in Spring Boot 4.1.1 the property stays under `spring.data.mongodb`, while the connection moved to `spring.mongodb`. The tests set it to `false` because they run without MongoDB. `infrastructure.database` has no class: auto-configuration covers it.
* **Seed.** `InitialAdministratorSeeder` creates the first administrator (identifier `INITIAL-ADMIN`) when `NEXUS_ADMIN_EMAIL` and `NEXUS_ADMIN_PASSWORD` are set and no administrator exists. It is idempotent, checks the variables before touching the database, hashes the password with BCrypt and never logs it. It uses the output ports directly because every registration service needs an administrator actor.
* **Configuration classes.** Only the authorization services and three internal services are plain classes; everything else is component-scanned, so there are no per-use-case configuration classes.
* **Environment.** `NEXUS_JWT_SECRET` (required), `NEXUS_MONGO_URI`, `NEXUS_CORS_ORIGINS`, `NEXUS_ADMIN_EMAIL`, `NEXUS_ADMIN_PASSWORD`.
