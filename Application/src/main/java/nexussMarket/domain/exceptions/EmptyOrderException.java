package nexussMarket.domain.exceptions;

/**
 * An order without lines was totalled, so it cannot be invoiced or refunded.
 */
public class EmptyOrderException extends DomainException {

    public EmptyOrderException(String message) {
        super(message);
    }
}
