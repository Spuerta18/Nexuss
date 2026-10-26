package nexussMarket.domain.exceptions;

/**
 * The buyer of a cart may not purchase: the buyer is not {@code ACTIVE}, or is suspended from purchasing.
 */
public class BuyerNotEnabledException extends DomainException {

    public BuyerNotEnabledException(String message) {
        super(message);
    }
}
