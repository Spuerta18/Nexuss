package nexussMarket.domain.services;

import java.math.BigDecimal;

import nexussMarket.domain.exceptions.EmptyOrderException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.valueobjects.Money;

/**
 * Amount-related calculations over an {@link Order}, shared by the services
 * that invoice and refund orders.
 */
final class OrderTotals {

    private OrderTotals() {
    }

    /**
     * Sum of {@code quantity × unitPrice} over the order lines, using the prices frozen at confirmation. Every line
     * must be in the same currency; {@link Money#add} rejects a mix.
     */
    static Money totalOf(Order order) {
        return order.getLines().stream()
                .map(OrderTotals::lineTotal)
                .reduce(Money::add)
                .orElseThrow(() -> new EmptyOrderException(
                        "Order " + order.getIdentifier() + " has no lines to total"));
    }

    private static Money lineTotal(OrderLine line) {
        Money unitPrice = line.getUnitPrice();
        return Money.of(unitPrice.getAmount().multiply(BigDecimal.valueOf(line.getQuantity())),
                unitPrice.getCurrency());
    }
}
