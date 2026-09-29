package nexussMarket.domain.services;

import java.math.BigDecimal;

import nexussMarket.domain.models.Order;

/**
 * Amount-related calculations over an {@link Order}, shared by the services
 * that invoice and refund orders.
 */
final class OrderTotals {

    private OrderTotals() {
    }

    /** Sum of {@code quantity × unitPrice} over the order lines, using the prices frozen at confirmation. */
    static BigDecimal totalOf(Order order) {
        return order.getLines().stream()
                .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
