# C4 Level 1 — System Context

## Purpose

Shows NexusMarket as a single system, the people who use it and the systems around it. NexusMarket is a marketplace that mediates between buyers and sellers: it manages catalogs, warehouses and stock, carts, orders, shipments, returns and refunds.

## Diagram

```mermaid
C4Context
    title NexusMarket — System Context

    Person(public, "Public visitor", "Not authenticated. Can log in and self-register as a buyer.")
    Person(buyer, "Buyer", "Browses the catalog, manages carts, places and cancels orders, requests returns.")
    Person(seller, "Seller", "Publishes products, manages own warehouses and stock.")
    Person(admin, "Administrator", "Registers staff, manages user and seller status, warehouses, returns and refunds.")
    Person(logistics, "Logistics Operator", "Confirms payment, runs the shipment lifecycle, reports damaged and returned stock.")
    Person(supervisor, "Supervisor", "Read-only consultation of orders, products, inventory and users.")

    System(nexus, "NexusMarket", "Marketplace backend. REST/JSON API secured with JWT.")

    System_Ext(client, "Client application", "Web or mobile front end, or any HTTP client. Not part of this repository.")
    System_Ext(notifications, "Notification channel", "E-mail, SMS or push. Not implemented: notifications are only logged.")

    Rel(public, client, "Uses")
    Rel(buyer, client, "Uses")
    Rel(seller, client, "Uses")
    Rel(admin, client, "Uses")
    Rel(logistics, client, "Uses")
    Rel(supervisor, client, "Uses")
    Rel(client, nexus, "Calls", "HTTPS, JSON, Bearer JWT")
    Rel(nexus, notifications, "Would send notifications", "NotificationPort")
```

## Notes

* **Actors map to roles.** The five authenticated actors are the `SystemRole` values `BUYER`, `SELLER`, `ADMINISTRATOR`, `LOGISTICS_OPERATOR` and `SUPERVISOR`; each has its own input port (`BuyerPort`, …) and REST controller. The public visitor uses `PublicAccessPort` (login and buyer registration).
* **No real external integrations yet.** There is no payment gateway, carrier or e-mail provider. Payment is confirmed by a logistics operator (`advanceOrderStatus` to `PAID`), and `NotificationPort` is implemented by `LoggingNotificationAdapter`, which only writes to the log; no service calls `NotifyUserService` today.
* **The client is outside the repository.** CORS allows browser clients from the origins in `NEXUS_CORS_ORIGINS` (default `http://localhost:3000` and `http://localhost:5173`).
* MongoDB is part of the system, not an external system; it appears at [level 2](02-level-2-containers.md).
