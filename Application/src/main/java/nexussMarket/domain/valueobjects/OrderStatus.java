package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Current stage of an {@link nexussMarket.domain.models.Order} in its
 * lifecycle.
 */
public final class OrderStatus extends DomainCatalog {

    public static final OrderStatus CART =
            new OrderStatus("CART", "Cart", "Provisional selection, not yet a binding commitment.");
    public static final OrderStatus PENDING_PAYMENT =
            new OrderStatus("PENDING_PAYMENT", "Pending Payment", "Awaiting financial confirmation.");
    public static final OrderStatus PAID =
            new OrderStatus("PAID", "Paid", "Payment confirmed; fulfillment process begins.");
    public static final OrderStatus SHIPPED =
            new OrderStatus("SHIPPED", "Shipped", "The order has left the warehouse.");
    public static final OrderStatus DELIVERED =
            new OrderStatus("DELIVERED", "Delivered", "The order has been successfully completed. Cannot be modified.");
    public static final OrderStatus CANCELLED =
            new OrderStatus("CANCELLED", "Cancelled", "The order was cancelled before payment was confirmed.");

    private OrderStatus(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code OrderStatus} value list. */
    public static List<OrderStatus> values() {
        return List.of(CART, PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED);
    }
}