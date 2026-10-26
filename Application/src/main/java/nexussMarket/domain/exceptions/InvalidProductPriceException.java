package nexussMarket.domain.exceptions;

/**
 * A product is published without a price, or with a price that is not greater than zero.
 */
public class InvalidProductPriceException extends DomainException {

    public InvalidProductPriceException(String message) {
        super(message);
    }
}
