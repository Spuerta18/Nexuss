package nexussMarket.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

class OrderTest {

    private final Buyer buyer = new Buyer("B-1", "Ana Smith", "ana@example.com", UserStatus.ACTIVE,
            CustomerStatus.ENABLED);
    private final Product product = new Product("P-1", "Shirt", ProductType.PHYSICAL, ProductStatus.PUBLISHED);
    private final Warehouse warehouse = new Warehouse("WH-1", "North Warehouse", "1 Main Street",
            WarehouseOwnerType.MARKETPLACE);

    @Test
    void advancesOneStepAtATimeUntilDelivered() {
        Order order = order(OrderStatus.PENDING_PAYMENT);

        order.advanceTo(OrderStatus.PAID);
        order.advanceTo(OrderStatus.SHIPPED);
        order.advanceTo(OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void cannotSkipAStep() {
        Order order = order(OrderStatus.PENDING_PAYMENT);

        assertThrows(InvalidOrderStatusTransitionException.class, () -> order.advanceTo(OrderStatus.SHIPPED));
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }

    @Test
    void cannotMoveBackwards() {
        Order order = order(OrderStatus.PAID);

        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> order.advanceTo(OrderStatus.PENDING_PAYMENT));
    }

    @Test
    void cannotBeCancelledByAdvancing() {
        Order order = order(OrderStatus.PENDING_PAYMENT);

        assertThrows(InvalidOrderStatusTransitionException.class, () -> order.advanceTo(OrderStatus.CANCELLED));
    }

    @Test
    void cancelsOnlyBeforePayment() {
        Order pending = order(OrderStatus.PENDING_PAYMENT);
        pending.cancel();
        assertEquals(OrderStatus.CANCELLED, pending.getStatus());

        Order paid = order(OrderStatus.PAID);
        assertThrows(InvalidOrderStatusTransitionException.class, paid::cancel);
    }

    @Test
    void cancelledOrderCannotReenterTheLifecycle() {
        Order order = order(OrderStatus.CANCELLED);

        assertThrows(InvalidOrderStatusTransitionException.class, () -> order.advanceTo(OrderStatus.PAID));
    }

    @Test
    void deliveredOrderCannotBeModifiedByAnyRoute() {
        Order order = order(OrderStatus.DELIVERED);

        for (OrderStatus target : OrderStatus.values()) {
            assertThrows(InvalidOrderStatusTransitionException.class, () -> order.advanceTo(target));
        }
        assertThrows(InvalidOrderStatusTransitionException.class, order::cancel);
        assertThrows(UnsupportedOperationException.class, () -> order.getLines().add(line()));
        assertThrows(UnsupportedOperationException.class, () -> order.getLines().clear());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(1, order.getLines().size());
    }

    @Test
    void linesAreFixedAtConstruction() {
        List<OrderLine> source = new ArrayList<>(List.of(line()));
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        Order order = new Order("O-1", buyer, OrderStatus.PENDING_PAYMENT, source, createdAt);

        source.add(line());

        assertEquals(1, order.getLines().size());
        assertEquals(createdAt, order.getCreatedAt());
    }

    private Order order(OrderStatus status) {
        return new Order("O-1", buyer, status, List.of(line()), LocalDateTime.now());
    }

    private OrderLine line() {
        return new OrderLine(product, "Shirt", 2, Money.of(new BigDecimal("50000"), Currency.COP), warehouse);
    }
}
