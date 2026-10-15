# Input Ports — NexusMarket

## Introduction

Input ports are the contracts through which the outside world (the REST controllers, see [REST API Endpoints](../Adapters/Api-rest-endpoints.md)) asks the domain to do something. NexusMarket defines **one input port per role**: each port lists every operation that role can execute, and nothing else.

The ports live in `domain/ports/in`. Each one is implemented by a single class in `adapters/useCases` that only delegates to the Domain Services (see [Use Case Adapters](../Adapters/Use-cases-adapters.md)), where the business rules and authorization checks live (see [Domain Services](Domain%20Services.md)).

```text
domain/ports/in/
├── PublicAccessPort.java        No authenticated user: login and buyer self-registration
├── BuyerPort.java               Carts, orders, catalog browsing, returns
├── SellerPort.java              Own catalog, own warehouses, inventory
├── AdministratorPort.java       Staff registration, user/seller status, warehouses, returns, refunds
├── LogisticsOperatorPort.java   Order payment, shipments, warehouse stock
└── SupervisorPort.java          Read-only consultation
```

---

# Principles

## Role Isolation

* **One port per role.** A port groups the operations of one role, so the operations a role can reach are listed in a single interface. A controller serving a role depends only on that role's port, never on another role's port or on a Domain Service.
* **Shared operations are repeated, not shared.** When several roles can perform the same operation, each port declares it (e.g. `consultProduct` is in `BuyerPort`, `SellerPort` and `SupervisorPort`). Every declaration delegates to the same Domain Service, so the rule is still written once.
* **Ports define reach, services define permission.** A port decides which operations a role can *reach*; it does not authorize anything. The Domain Service always authorizes the actor itself, so calling an operation through the wrong port still fails. A service may allow more roles than the ports currently expose: e.g. shipment services accept `ADMINISTRATOR`, but `AdministratorPort` does not declare shipment operations.

## Domain Model Parameters

* **Ports receive domain models.** Parameters are Domain Models (`Product`, `Order`, `ShoppingCart`, …), Value Objects (`OrderStatus`, `SellerStatus`, …) or plain values (`int quantity`, `String password`). There are no `Command` records and no DTOs: converting transport objects into domain models is the input adapter's job (its mappers).
* **A model is either a reference or a new model.**
  * A **reference** carries only the identifier of an existing entity (e.g. the `order` in `cancelOrder`). The service loads the real entity through its output port and ignores every other field.
  * A **new model** carries the data of the entity to create (e.g. the `product` in `publishProduct`). The service builds the stored entity itself, so fields the caller must not decide (initial status, owner, timestamps) are always set by the domain.
* **The actor comes first.** Every operation except those of `PublicAccessPort` receives the authenticated user as its first parameter, `User user`. It is the user the security layer authenticated, never one taken from the request body. Domain Services call this parameter `actor`.
* **Secrets are separate parameters.** A plain password travels as its own `String password` parameter, never inside the model, and is hashed by the service.

---

# Port Catalog

| Port | Role | Implemented by | Operations |
| ---- | ---- | -------------- | ---------- |
| `PublicAccessPort` | No authenticated user | `PublicAccessUseCaseImpl` | 2 |
| `BuyerPort` | `BUYER` | `BuyerUseCaseImpl` | 12 |
| `SellerPort` | `SELLER` | `SellerUseCaseImpl` | 9 |
| `AdministratorPort` | `ADMINISTRATOR` | `AdministratorUseCaseImpl` | 11 |
| `LogisticsOperatorPort` | `LOGISTICS_OPERATOR` | `LogisticsOperatorUseCaseImpl` | 9 |
| `SupervisorPort` | `SUPERVISOR` | `SupervisorUseCaseImpl` | 5 |

---

# Port Definitions

## PublicAccessPort

```java
/** Operations available without an authenticated user. */
public interface PublicAccessPort {

    AuthenticationResult authenticate(String email, String password);

    Buyer registerBuyer(Buyer buyer, String password);
}
```

## BuyerPort

```java
/**
 * Operations a {@link Buyer} can execute. The buyer's own profile is read from
 * the authenticated principal, not through a use case.
 */
public interface BuyerPort {

    ShoppingCart createShoppingCart(User user, ShoppingCart cart);

    ShoppingCart addCartLine(User user, ShoppingCart cart, Product product, int quantity);

    ShoppingCart updateCartLineQuantity(User user, ShoppingCart cart, Product product, int quantity);

    ShoppingCart removeCartLine(User user, ShoppingCart cart, Product product);

    ShoppingCart consultCart(User user, ShoppingCart cart);

    /** {@code newOrder} carries the identifier of the order to create. */
    Order confirmOrder(User user, ShoppingCart cart, Order newOrder);

    Order cancelOrder(User user, Order order);

    Order consultOrder(User user, Order order);

    List<Order> listBuyerOrders(User user, Buyer buyer);

    List<Product> listPublishedProducts(User user);

    Product consultProduct(User user, Product product);

    ReturnRequest requestReturn(User user, ReturnRequest returnRequest);
}
```

## SellerPort

```java
/** Operations a {@link Seller} can execute on their own catalog, warehouses and inventory. */
public interface SellerPort {

    Product publishProduct(User user, Product product);

    Product updateProductStatus(User user, Product product, ProductStatus status);

    Product updateProductVariants(User user, Product product, List<ProductVariant> variants);

    List<Product> listSellerProducts(User user, Seller seller);

    Product consultProduct(User user, Product product);

    /** Registers a Seller-owned warehouse, owned by {@code user}. */
    Warehouse registerWarehouse(User user, Warehouse warehouse);

    InventoryItem registerInventoryInbound(User user, Product product, Warehouse warehouse, int quantity);

    InventoryItem adjustInventory(User user, Product product, Warehouse warehouse, int quantityDelta);

    List<InventoryItem> consultInventory(User user, Product product);
}
```

## AdministratorPort

```java
/**
 * Operations an {@link Administrator} can execute: staff registration, user
 * and seller status, Marketplace-owned warehouses, returns and refunds.
 */
public interface AdministratorPort {

    Seller registerSeller(User user, Seller seller, String password);

    Supervisor registerSupervisor(User user, Supervisor supervisor, String password);

    LogisticsOperator registerLogisticsOperator(User user, LogisticsOperator logisticsOperator, String password);

    Seller updateSellerStatus(User user, Seller seller, SellerStatus status);

    User updateUserStatus(User user, User target, UserStatus status);

    /** Registers a Marketplace-owned warehouse. */
    Warehouse registerMarketplaceWarehouse(User user, Warehouse warehouse);

    Warehouse deactivateWarehouse(User user, Warehouse warehouse);

    ReturnRequest approveReturn(User user, ReturnRequest returnRequest);

    ReturnRequest rejectReturn(User user, ReturnRequest returnRequest);

    Refund processRefund(User user, Refund refund);

    User consultUser(User user, User target);
}
```

## LogisticsOperatorPort

```java
/** Operations a {@link LogisticsOperator} can execute on orders, shipments and warehouse stock. */
public interface LogisticsOperatorPort {

    Order consultOrder(User user, Order order);

    Order advanceOrderStatus(User user, Order order, OrderStatus targetStatus);

    /** {@code shipment} carries the identifier of the shipment to create and the order it delivers. */
    Shipment createShipment(User user, Shipment shipment);

    Shipment packShipment(User user, Shipment shipment);

    Shipment dispatchShipment(User user, Shipment shipment);

    Shipment confirmDelivery(User user, Shipment shipment);

    InventoryItem reportDamagedInventory(User user, Product product, Warehouse warehouse, int quantity);

    InventoryItem registerInventoryReturn(User user, Product product, Warehouse warehouse, int quantity);

    List<InventoryItem> consultInventory(User user, Product product);
}
```

## SupervisorPort

```java
/** Read-only operations a {@link Supervisor} can execute. */
public interface SupervisorPort {

    Order consultOrder(User user, Order order);

    List<InventoryItem> consultInventory(User user, Product product);

    Product consultProduct(User user, Product product);

    User consultUser(User user, User target);

    List<Product> listPublishedProducts(User user);
}
```

---

# Abstract `User` Parameters

`AdministratorPort.updateUserStatus`, `AdministratorPort.consultUser` and `SupervisorPort.consultUser` receive the affected user as `User target`, and `User` is **abstract**: its role is fixed by the concrete subclass (`Buyer`, `Seller`, `Administrator`, `LogisticsOperator`, `Supervisor`).

These operations work on any role, so the port cannot narrow the type. The service only reads `target.getIdentifier()` and loads the real user, so any subclass works as a reference. But a REST request usually carries only the user identifier, and **the mapper of a future controller cannot build a `User` from an identifier alone without knowing which subclass to instantiate**.

The REST adapter solves it without changing the ports: the client sends the role of the target user (in the request body, or as a `role` query parameter for `GET`), and the mapper builds a reference of that subtype. The services only read the identifier, so a wrong role still acts on the right user. See [REST Adapters](../Adapters/Rest-adapters.md#abstract-user-references) for this limitation.

The other operations do not have this problem: when the parameter is a concrete role (`Seller seller`, `Buyer buyer`), the mapper knows which class to build.
