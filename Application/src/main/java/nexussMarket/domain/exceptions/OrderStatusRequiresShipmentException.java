package nexussMarket.domain.exceptions;

/**
 * An order was asked to move directly to {@code SHIPPED} or {@code DELIVERED}; those statuses are only
 * reached through the shipment lifecycle.
 */
public class OrderStatusRequiresShipmentException extends DomainException {

    public OrderStatusRequiresShipmentException(String message) {
        super(message);
    }
}
