package nexussMarket.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.DomainException;
import nexussMarket.domain.exceptions.InsufficientStockException;
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
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.RefundStatus;
import nexussMarket.domain.valueobjects.ReturnStatus;

/**
 * Concurrency defects of {@code stress-tests/REPORT.md} (F1 overselling, F2
 * double state transitions), reproduced on the real services with the
 * in-memory adapters, which enforce the same optimistic locking as MongoDB.
 * Every losing call must fail with a {@link DomainException} (a 4xx, never a
 * 500) and leave no side effect behind.
 */
class ConcurrencyScenariosTest extends MemoryScenarioSupport {

    @Test
    void sixtyBuyersRacingForTenUnitsGetExactlyTenOrders() throws Exception {
        Seller seller = seller();
        Warehouse warehouse = warehouseOf(seller);
        Product product = stockedProduct(seller, warehouse, 10);
        List<Buyer> buyers = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            Buyer buyer = buyer();
            buyerPort.addActiveCartLine(buyer, reference(product), 1);
            buyers.add(buyer);
        }

        List<Outcome<Order>> outcomes = race(60,
                i -> buyerPort.confirmActiveCart(buyers.get(i), new Order(null, null, null)));

        assertEquals(10, successes(outcomes));
        outcomes.stream().filter(outcome -> !outcome.succeeded())
                .forEach(outcome -> assertInstanceOf(InsufficientStockException.class, outcome.failure()));
        InventoryItem stock = stockOf(product, warehouse);
        assertEquals(0, stock.getAvailableQuantity());
        assertEquals(10, stock.getReservedQuantity());
        // Every buyer who got no order keeps the cart they tried to confirm.
        for (int i = 0; i < 60; i++) {
            if (!outcomes.get(i).succeeded()) {
                assertNotNull(activeCartOf(buyers.get(i)), "the losing buyer's cart is restored");
            }
        }
    }

    @RepeatedTest(5)
    void theSameCartConfirmedTwiceAtOnceBecomesOneOrder() throws Exception {
        Seller seller = seller();
        Warehouse warehouse = warehouseOf(seller);
        Product product = stockedProduct(seller, warehouse, 10);
        Buyer buyer = buyer();
        buyerPort.addActiveCartLine(buyer, reference(product), 2);

        List<Outcome<Order>> outcomes = race(2, i -> buyerPort.confirmActiveCart(buyer, new Order(null, null, null)));

        assertOneWinner(outcomes);
        assertEquals(2, stockOf(product, warehouse).getReservedQuantity());
        assertEquals(1, orders.findAllByBuyerId(buyer.getIdentifier()).size());
    }

    @RepeatedTest(5)
    void anOrderCancelledTwiceAtOnceReleasesItsStockOnce() throws Exception {
        Seller seller = seller();
        Warehouse warehouse = warehouseOf(seller);
        Product product = stockedProduct(seller, warehouse, 10);
        Buyer buyer = buyer();
        Order order = pendingOrder(buyer, product, 3);

        List<Outcome<Order>> outcomes = race(2, i -> buyerPort.cancelOrder(buyer, orderReference(order)));

        assertOneWinner(outcomes);
        InventoryItem stock = stockOf(product, warehouse);
        assertEquals(10, stock.getAvailableQuantity());
        assertEquals(0, stock.getReservedQuantity());
        assertEquals(OrderStatus.CANCELLED, orders.findById(order.getIdentifier()).orElseThrow().getStatus());
    }

    @RepeatedTest(5)
    void anOrderPaidTwiceAtOnceIsPaidAndInvoicedOnce() throws Exception {
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 10);
        Buyer buyer = buyer();
        Administrator administrator = administrator();
        Order order = pendingOrder(buyer, product, 1);

        List<Outcome<Order>> outcomes = race(2,
                i -> administratorPort.registerOrderPayment(administrator, orderReference(order)));

        assertOneWinner(outcomes);
        assertEquals(OrderStatus.PAID, orders.findById(order.getIdentifier()).orElseThrow().getStatus());
        assertTrue(invoices.findByOrderId(order.getIdentifier()).isPresent());
    }

    @RepeatedTest(5)
    void aShipmentDispatchedTwiceAtOnceTakesTheStockOutOnce() throws Exception {
        Seller seller = seller();
        Warehouse warehouse = warehouseOf(seller);
        Product product = stockedProduct(seller, warehouse, 10);
        LogisticsOperator operator = logisticsOperator();
        Shipment shipment = packedShipment(buyer(), product, 4, administrator(), operator);

        List<Outcome<Shipment>> outcomes = race(2, i -> logisticsOperatorPort.dispatchShipment(operator,
                new Shipment(shipment.getIdentifier(), null)));

        assertOneWinner(outcomes);
        InventoryItem stock = stockOf(product, warehouse);
        assertEquals(6, stock.getAvailableQuantity());
        assertEquals(0, stock.getReservedQuantity());
        assertEquals(OrderStatus.SHIPPED,
                orders.findById(shipment.getOrder().getIdentifier()).orElseThrow().getStatus());
    }

    @RepeatedTest(5)
    void aReturnApprovedTwiceAtOnceReturnsTheStockOnce() throws Exception {
        Seller seller = seller();
        Warehouse warehouse = warehouseOf(seller);
        Product product = stockedProduct(seller, warehouse, 10);
        Administrator administrator = administrator();
        ReturnRequest request = requestedReturn(buyer(), product, 2, administrator, logisticsOperator());
        assertEquals(8, stockOf(product, warehouse).getAvailableQuantity());

        List<Outcome<ReturnRequest>> outcomes = race(2, i -> administratorPort.approveReturn(administrator,
                new ReturnRequest(request.getIdentifier(), null, null, null)));

        assertOneWinner(outcomes);
        assertEquals(10, stockOf(product, warehouse).getAvailableQuantity());
        assertEquals(ReturnStatus.APPROVED,
                returnRequests.findById(request.getIdentifier()).orElseThrow().getStatus());
        assertTrue(refunds.findByReturnRequestId(request.getIdentifier()).isPresent());
    }

    @RepeatedTest(5)
    void aRefundProcessedTwiceAtOnceIsPaidOnce() throws Exception {
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 10);
        Administrator administrator = administrator();
        Refund refund = pendingRefund(buyer(), product, 1, administrator, logisticsOperator());

        List<Outcome<Refund>> outcomes = race(2, i -> administratorPort.processRefund(administrator,
                new Refund(refund.getIdentifier(), null, null)));

        assertOneWinner(outcomes);
        assertEquals(RefundStatus.PROCESSED, refunds.findById(refund.getIdentifier()).orElseThrow().getStatus());
    }

    /** Exactly one call won; the other failed with a business error (409 family), never an unexpected one. */
    private static <T> void assertOneWinner(List<Outcome<T>> outcomes) {
        assertEquals(1, successes(outcomes), () -> "outcomes: " + outcomes);
        outcomes.stream().filter(outcome -> !outcome.succeeded())
                .forEach(outcome -> assertInstanceOf(DomainException.class, outcome.failure()));
    }
}
