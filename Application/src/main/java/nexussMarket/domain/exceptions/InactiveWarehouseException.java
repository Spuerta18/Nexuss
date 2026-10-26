package nexussMarket.domain.exceptions;

/**
 * Stock was sent to a warehouse that is no longer active.
 */
public class InactiveWarehouseException extends DomainException {

    public InactiveWarehouseException(String message) {
        super(message);
    }
}
