package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EmptyOrderException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.valueobjects.Money;

/**
 * Amount-related calculations over an {@link Order}, shared by the services
 * that invoice and refund orders.
 */
final class OrderTotals {

    private OrderTotals() {
    }

    /** The order total ({@link Order#getTotal()}); an order without lines cannot be totalled. */
    static Money totalOf(Order order) {
        return order.getTotal()
                .orElseThrow(() -> new EmptyOrderException(
                        "Order " + order.getIdentifier() + " has no lines to total"));
    }
}
