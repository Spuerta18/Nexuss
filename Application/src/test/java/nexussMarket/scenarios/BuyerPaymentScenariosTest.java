package nexussMarket.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import nexussMarket.domain.exceptions.DomainException;
import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.exceptions.PaymentDeclinedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.OperationRepositoryPort;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;

/**
 * The buyer's payment through the simulated gateway ({@code DECLINED} is its
 * test token for a declined card), and the automatic delivery of orders that
 * hold only digital products, on the real services and in-memory adapters.
 */
class BuyerPaymentScenariosTest extends MemoryScenarioSupport {

    private static final String CARD = "tok_test_visa";

    @Autowired
    private OperationRepositoryPort operations;

    @Test
    void anApprovedPaymentLeavesTheOrderPaidInvoicedAndAudited() {
        Seller seller = seller();
        Buyer buyer = buyer();
        Order order = pendingOrder(buyer, stockedProduct(seller, warehouseOf(seller), 5), 2);

        Order paid = buyerPort.payOrder(buyer, orderReference(order), CARD);

        assertEquals(OrderStatus.PAID, paid.getStatus());
        assertTrue(invoices.findByOrderId(order.getIdentifier()).isPresent());
        assertTrue(recorded(OperationType.ORDER_PAYMENT, order));
    }

    @Test
    void aDeclinedPaymentKeepsTheOrderPendingAndIsAudited() {
        Seller seller = seller();
        Buyer buyer = buyer();
        Order order = pendingOrder(buyer, stockedProduct(seller, warehouseOf(seller), 5), 1);

        assertThrows(PaymentDeclinedException.class, () -> buyerPort.payOrder(buyer, orderReference(order), "DECLINED"));

        assertEquals(OrderStatus.PENDING_PAYMENT, orders.findById(order.getIdentifier()).orElseThrow().getStatus());
        assertFalse(invoices.findByOrderId(order.getIdentifier()).isPresent());
        assertTrue(recorded(OperationType.PAYMENT_DECLINED, order));
        // The buyer can still pay with another card.
        assertEquals(OrderStatus.PAID, buyerPort.payOrder(buyer, orderReference(order), CARD).getStatus());
    }

    @Test
    void payingTwiceIsAConflict() {
        Seller seller = seller();
        Buyer buyer = buyer();
        Order order = pendingOrder(buyer, stockedProduct(seller, warehouseOf(seller), 5), 1);
        buyerPort.payOrder(buyer, orderReference(order), CARD);

        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> buyerPort.payOrder(buyer, orderReference(order), CARD));
    }

    @Test
    void twoSimultaneousPaymentsPayOnce() throws Exception {
        Seller seller = seller();
        Buyer buyer = buyer();
        Order order = pendingOrder(buyer, stockedProduct(seller, warehouseOf(seller), 5), 1);

        List<Outcome<Order>> outcomes = race(2, i -> buyerPort.payOrder(buyer, orderReference(order), CARD));

        assertEquals(1, successes(outcomes));
        outcomes.stream().filter(outcome -> !outcome.succeeded())
                .forEach(outcome -> assertInstanceOf(DomainException.class, outcome.failure()));
        assertEquals(1, operations.findAll().stream().filter(operation ->
                operation.getOperationType() == OperationType.ORDER_PAYMENT
                        && order.getIdentifier().equals(operation.getAffectedEntityId())).count());
    }

    @Test
    void onlyTheOwningBuyerMayPay() {
        Seller seller = seller();
        Buyer owner = buyer();
        Order order = pendingOrder(owner, stockedProduct(seller, warehouseOf(seller), 5), 1);

        assertThrows(OperationNotAllowedException.class, () -> buyerPort.payOrder(buyer(), orderReference(order), CARD));
        Administrator administrator = administrator();
        assertThrows(OperationNotAllowedException.class,
                () -> buyerPort.payOrder(administrator, orderReference(order), CARD));
        assertEquals(OrderStatus.PENDING_PAYMENT, orders.findById(order.getIdentifier()).orElseThrow().getStatus());
    }

    @Test
    void aDigitalOnlyOrderIsDeliveredAsSoonAsItIsPaid() {
        Seller seller = seller();
        Buyer buyer = buyer();
        Product ebook = digitalProduct(seller);
        buyerPort.addActiveCartLine(buyer, reference(ebook), 1);
        Order order = buyerPort.confirmActiveCart(buyer, new Order(null, null, null));

        Order paid = buyerPort.payOrder(buyer, orderReference(order), CARD);

        assertEquals(OrderStatus.DELIVERED, paid.getStatus());
        assertEquals(OrderStatus.DELIVERED, orders.findById(order.getIdentifier()).orElseThrow().getStatus());
        assertTrue(shipments.findByOrderId(order.getIdentifier()).isEmpty(), "no shipment for digital goods");
        assertTrue(recorded(OperationType.DIGITAL_ORDER_FULFILLMENT, order));
    }

    @Test
    void anAdministratorPaymentAlsoDeliversADigitalOnlyOrder() {
        Seller seller = seller();
        Buyer buyer = buyer();
        buyerPort.addActiveCartLine(buyer, reference(digitalProduct(seller)), 2);
        Order order = buyerPort.confirmActiveCart(buyer, new Order(null, null, null));

        Order paid = administratorPort.registerOrderPayment(administrator(), orderReference(order));

        assertEquals(OrderStatus.DELIVERED, paid.getStatus());
    }

    @Test
    void aMixedOrderOnlyReservesAndReleasesThePhysicalStock() {
        Seller seller = seller();
        Warehouse warehouse = warehouseOf(seller);
        Product keyboard = stockedProduct(seller, warehouse, 5);
        Buyer buyer = buyer();
        buyerPort.addActiveCartLine(buyer, reference(keyboard), 2);
        buyerPort.addActiveCartLine(buyer, reference(digitalProduct(seller)), 1);
        Order order = buyerPort.confirmActiveCart(buyer, new Order(null, null, null));
        assertEquals(2, stockOf(keyboard, warehouse).getReservedQuantity());

        Order paid = buyerPort.payOrder(buyer, orderReference(order), CARD);
        assertEquals(OrderStatus.PAID, paid.getStatus(), "a physical line still needs a shipment");

        Buyer other = buyer();
        buyerPort.addActiveCartLine(other, reference(keyboard), 1);
        buyerPort.addActiveCartLine(other, reference(digitalProduct(seller)), 1);
        Order cancelled = buyerPort.cancelOrder(other, orderReference(
                buyerPort.confirmActiveCart(other, new Order(null, null, null))));
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(2, stockOf(keyboard, warehouse).getReservedQuantity());
    }

    private Product digitalProduct(Seller seller) {
        return sellerPort.publishProduct(seller, new Product(null, "E-book", ProductType.DIGITAL,
                ProductStatus.PUBLISHED, seller, cop(30000), List.of()));
    }

    private boolean recorded(OperationType type, Order order) {
        return operations.findAll().stream().map(Operation.class::cast).anyMatch(operation ->
                operation.getOperationType() == type && order.getIdentifier().equals(operation.getAffectedEntityId()));
    }
}
