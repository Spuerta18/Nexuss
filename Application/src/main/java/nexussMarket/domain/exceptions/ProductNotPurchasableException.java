package nexussMarket.domain.exceptions;

/**
 * A product cannot be bought: it is not {@code PUBLISHED}, or it has no price.
 */
public class ProductNotPurchasableException extends DomainException {

    public ProductNotPurchasableException(String message) {
        super(message);
    }
}
