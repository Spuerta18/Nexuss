package nexussMarket.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * The two persistence guarantees, checked on the in-memory adapters that the
 * {@code memory} profile runs (the MongoDB adapters are checked against a real
 * server in {@code MongoVersioningIntegrationTest}):
 * <ul>
 *   <li>optimistic locking: a copy saved after someone else saved the same
 *       entity fails with {@link ConcurrentUpdateException} and stores
 *       nothing, for each of the 7 versioned entities;</li>
 *   <li>insert-only creation: creating anything with an identifier already in
 *       use fails with {@link DuplicateResourceException} and leaves the
 *       stored entity as it was.</li>
 * </ul>
 */
class PersistenceGuaranteesTest extends MemoryScenarioSupport {

    @Autowired
    private ProductRepositoryPort products;
    @Autowired
    private WarehouseRepositoryPort warehouses;

    // ---- optimistic locking ------------------------------------------------------------------------------------

    @Test
    void staleUserIsNotSaved() {
        Buyer buyer = buyer();
        User first = users.findById(buyer.getIdentifier()).orElseThrow();
        User stale = users.findById(buyer.getIdentifier()).orElseThrow();
        first.setStatus(UserStatus.BLOCKED);
        users.save(first);

        stale.setFullName("Overwritten");
        assertThrows(ConcurrentUpdateException.class, () -> users.save(stale));

        User stored = users.findById(buyer.getIdentifier()).orElseThrow();
        assertEquals(UserStatus.BLOCKED, stored.getStatus());
        assertEquals(buyer.getFullName(), stored.getFullName());
        assertEquals(1, stored.getVersion());
    }

    @Test
    void staleInventoryItemIsNotSaved() {
        Seller seller = seller();
        Warehouse warehouse = warehouseOf(seller);
        Product product = stockedProduct(seller, warehouse, 10);
        InventoryItem first = stockOf(product, warehouse);
        InventoryItem stale = stockOf(product, warehouse);
        first.reserve(7);
        inventory.save(first);

        stale.reserve(7);
        assertThrows(ConcurrentUpdateException.class, () -> inventory.save(stale));

        assertEquals(3, stockOf(product, warehouse).getAvailableQuantity());
    }

    @Test
    void staleCartIsNeitherSavedNorDeleted() {
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 10);
        Buyer buyer = buyer();
        buyerPort.addActiveCartLine(buyer, reference(product), 1);
        ShoppingCart first = activeCartOf(buyer);
        ShoppingCart stale = activeCartOf(buyer);
        first.updateLineQuantity(product, 4);
        carts.save(first);

        stale.updateLineQuantity(product, 9);
        assertThrows(ConcurrentUpdateException.class, () -> carts.save(stale));
        assertThrows(ConcurrentUpdateException.class, () -> carts.delete(stale));

        assertEquals(4, activeCartOf(buyer).getLines().get(0).getQuantity());
    }

    @Test
    void staleOrderIsNotSaved() {
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 10);
        Order order = pendingOrder(buyer(), product, 1);
        Order first = orders.findById(order.getIdentifier()).orElseThrow();
        Order stale = orders.findById(order.getIdentifier()).orElseThrow();
        first.cancel();
        orders.save(first);

        stale.advanceTo(OrderStatus.PAID);
        assertThrows(ConcurrentUpdateException.class, () -> orders.save(stale));

        assertEquals(OrderStatus.CANCELLED, orders.findById(order.getIdentifier()).orElseThrow().getStatus());
    }

    @Test
    void staleShipmentReturnRequestAndRefundAreNotSaved() {
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 10);
        Administrator administrator = administrator();
        LogisticsOperator operator = logisticsOperator();

        Shipment shipment = packedShipment(buyer(), product, 1, administrator, operator);
        Shipment firstShipment = shipments.findById(shipment.getIdentifier()).orElseThrow();
        Shipment staleShipment = shipments.findById(shipment.getIdentifier()).orElseThrow();
        firstShipment.dispatch();
        shipments.save(firstShipment);
        staleShipment.dispatch();
        assertThrows(ConcurrentUpdateException.class, () -> shipments.save(staleShipment));

        ReturnRequest request = requestedReturn(buyer(), product, 1, administrator, operator);
        ReturnRequest firstRequest = returnRequests.findById(request.getIdentifier()).orElseThrow();
        ReturnRequest staleRequest = returnRequests.findById(request.getIdentifier()).orElseThrow();
        firstRequest.reject(administrator);
        returnRequests.save(firstRequest);
        staleRequest.approve(administrator);
        assertThrows(ConcurrentUpdateException.class, () -> returnRequests.save(staleRequest));

        Refund refund = pendingRefund(buyer(), product, 1, administrator, operator);
        Refund firstRefund = refunds.findById(refund.getIdentifier()).orElseThrow();
        Refund staleRefund = refunds.findById(refund.getIdentifier()).orElseThrow();
        firstRefund.process();
        refunds.save(firstRefund);
        staleRefund.process();
        assertThrows(ConcurrentUpdateException.class, () -> refunds.save(staleRefund));
        assertEquals(1, refunds.findById(refund.getIdentifier()).orElseThrow().getVersion());
    }

    // ---- insert-only creation ----------------------------------------------------------------------------------

    @Test
    void registeringATakenUserIdentifierChangesNothing() {
        Administrator administrator = administrator();
        Seller existing = seller();

        assertThrows(DuplicateResourceException.class, () -> administratorPort.registerSeller(administrator,
                new Seller(existing.getIdentifier(), "Impostor", "impostor@test.local", null, null), "password-123"));
        assertThrows(DuplicateResourceException.class, () -> users.insert(
                new Seller(existing.getIdentifier(), "Impostor", "impostor@test.local", UserStatus.ACTIVE, null)));

        assertEquals(existing.getFullName(), users.findById(existing.getIdentifier()).orElseThrow().getFullName());
        assertEquals(existing.getEmail(), users.findById(existing.getIdentifier()).orElseThrow().getEmail());
    }

    @Test
    void creatingWithAnIdentifierOfAnotherOwnerChangesNothing() {
        Seller owner = seller();
        Seller intruder = seller();
        Warehouse warehouse = warehouseOf(owner);
        Product product = stockedProduct(owner, warehouse, 10);

        assertThrows(DuplicateResourceException.class, () -> sellerPort.publishProduct(intruder,
                new Product(product.getIdentifier(), "Stolen", ProductType.PHYSICAL, ProductStatus.PUBLISHED,
                        intruder, cop(5), List.of())));
        assertThrows(DuplicateResourceException.class, () -> sellerPort.registerWarehouse(intruder,
                new Warehouse(warehouse.getIdentifier(), "Stolen", "x", WarehouseOwnerType.SELLER, intruder, true)));
        assertEquals(owner.getIdentifier(), products.findById(product.getIdentifier()).orElseThrow()
                .getSeller().getIdentifier());
        assertEquals("Keyboard", products.findById(product.getIdentifier()).orElseThrow().getName());
        assertEquals(owner.getIdentifier(), warehouses.findById(warehouse.getIdentifier()).orElseThrow()
                .getOwner().getIdentifier());

        Buyer firstBuyer = buyer();
        Buyer secondBuyer = buyer();
        ShoppingCart cart = buyerPort.createShoppingCart(firstBuyer, new ShoppingCart(unique("CART"), firstBuyer));
        assertThrows(DuplicateResourceException.class, () -> buyerPort.createShoppingCart(secondBuyer,
                new ShoppingCart(cart.getIdentifier(), secondBuyer)));
        assertEquals(firstBuyer.getIdentifier(),
                carts.findById(cart.getIdentifier()).orElseThrow().getBuyer().getIdentifier());

        Order order = pendingOrder(buyer(), product, 1);
        buyerPort.addActiveCartLine(secondBuyer, reference(product), 2);
        assertThrows(DuplicateResourceException.class,
                () -> buyerPort.confirmActiveCart(secondBuyer, new Order(order.getIdentifier(), null, null)));
        assertEquals(1, orders.findById(order.getIdentifier()).orElseThrow().getLines().get(0).getQuantity());
        assertEquals(1, stockOf(product, warehouse).getReservedQuantity(), "no stock reserved for the rejected order");
    }

    @Test
    void shipmentsReturnsAndRefundsWithTakenIdentifiersChangeNothing() {
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 10);
        Administrator administrator = administrator();
        LogisticsOperator operator = logisticsOperator();

        Shipment shipment = packedShipment(buyer(), product, 1, administrator, operator);
        Order otherPaid = paidOrder(buyer(), product, 1, administrator);
        assertThrows(DuplicateResourceException.class, () -> logisticsOperatorPort.createShipment(operator,
                new Shipment(shipment.getIdentifier(), orderReference(otherPaid))));
        assertEquals(shipment.getOrder().getIdentifier(),
                shipments.findById(shipment.getIdentifier()).orElseThrow().getOrder().getIdentifier());

        Buyer buyer = buyer();
        ReturnRequest request = requestedReturn(buyer, product, 1, administrator, operator);
        Order otherDelivered = deliveredOrder(buyer, product, 1, administrator, operator);
        assertThrows(DuplicateResourceException.class, () -> buyerPort.requestReturn(buyer,
                new ReturnRequest(request.getIdentifier(), orderReference(otherDelivered), null, "again")));
        assertEquals(request.getOrder().getIdentifier(),
                returnRequests.findById(request.getIdentifier()).orElseThrow().getOrder().getIdentifier());

        Refund refund = pendingRefund(buyer(), product, 1, administrator, operator);
        assertThrows(DuplicateResourceException.class,
                () -> refunds.insert(new Refund(refund.getIdentifier(), request, cop(1))));
        assertEquals(refund.getAmount(), refunds.findById(refund.getIdentifier()).orElseThrow().getAmount());
    }
}
