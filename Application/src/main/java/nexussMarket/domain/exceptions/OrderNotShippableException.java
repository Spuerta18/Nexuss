package nexussMarket.domain.exceptions;

/**
 * A shipment was requested for an order that is not {@code PAID}.
 */
public class OrderNotShippableException extends DomainException {

    public OrderNotShippableException(String message) {
        super(message);
    }
}
