# C4 Level 2 — Containers

## Purpose

Shows the deployable pieces of NexusMarket and how they communicate: the client, the Spring Boot API and the MongoDB database.

## Diagram

```mermaid
C4Container
    title NexusMarket — Containers

    Person(user, "User", "Public visitor or one of the five roles")

    System_Ext(client, "Client application", "Not in this repository")

    System_Boundary(nexus, "NexusMarket") {
        Container(api, "NexusMarket API", "Java 17, Spring Boot 4.1.1, Spring Security, Spring Data MongoDB", "REST/JSON on port 8080. Hexagonal architecture: REST adapters, role ports, domain services, MongoDB adapters.")
        ContainerDb(mongo, "MongoDB", "MongoDB (9.0.2 local service, mongo:7 in docker-compose)", "Database nexusmarket: users, warehouses, products, inventory, carts, orders, shipments, invoices, returns, refunds, operations, audit logs.")
    }

    Rel(user, client, "Uses")
    Rel(client, api, "Calls /api/**", "HTTPS, JSON, Authorization: Bearer JWT")
    Rel(api, mongo, "Reads and writes documents, creates indexes at startup", "MongoDB wire protocol, driver 5.8.1")
```

## Notes

| Container | Technology | Configuration |
| --------- | ---------- | ------------- |
| NexusMarket API | Java 17, Spring Boot 4.1.1 (`spring-boot-starter-webmvc`, `-security`, `-validation`, `-data-mongodb`), jjwt 0.13.0, Lombok | Port 8080 (Spring default). `NEXUS_JWT_SECRET` (required), `NEXUS_MONGO_URI`, `NEXUS_CORS_ORIGINS`, `NEXUS_ADMIN_EMAIL` / `NEXUS_ADMIN_PASSWORD` |
| MongoDB | MongoDB server; `mongodb-driver-sync` 5.8.1 | URI `spring.mongodb.uri=${NEXUS_MONGO_URI:mongodb://localhost:27017/nexusmarket}` |

* **Stateless API.** No HTTP session: every request carries the JWT. Logout and password change revoke tokens through `users.authTokenVersion` (JWT claim `ver`), stored in the database, so any number of API instances can share the same database.
* **Startup needs MongoDB.** `spring.data.mongodb.auto-index-creation=true` creates the declared indexes at startup, so the API does not start without a reachable server.
* **Single database, thirteen collections**, one per aggregate plus the append-only `operations` and `audit_logs` (see the [persistence layer view](07-layer-adapters-persistence.md)).
* **Containers in this repository.** `Application/Dockerfile` builds the API image (multi-stage) and `Application/docker-compose.yml` runs it with `mongo:7`.
