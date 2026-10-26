package nexussMarket.domain.exceptions;

/**
 * A quantity breaks its rule: a cart line, order line or stock movement needs more than zero units,
 * and stock quantities are never negative.
 */
public class InvalidQuantityException extends DomainException {

    public InvalidQuantityException(String message) {
        super(message);
    }
}
