package nexussMarket.domain.exceptions;

/** The product was discontinued (logically deleted) and cannot be changed any more ({@code 409}). */
public class ProductDiscontinuedException extends DomainException {

    public ProductDiscontinuedException(String message) {
        super(message);
    }
}
