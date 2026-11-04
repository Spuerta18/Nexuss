package nexussMarket.domain.exceptions;

/** The product cannot be discontinued while an open order still holds it ({@code 409}). */
public class ProductInOpenOrderException extends DomainException {

    public ProductInOpenOrderException(String message) {
        super(message);
    }
}
