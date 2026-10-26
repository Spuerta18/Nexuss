package nexussMarket.domain.exceptions;

/**
 * An order was requested from a shopping cart that has no lines.
 */
public class EmptyCartException extends DomainException {

    public EmptyCartException(String message) {
        super(message);
    }
}
