package nexussMarket.domain.exceptions;

/**
 * A return was requested for an order that is not {@code DELIVERED}.
 */
public class ReturnNotEligibleException extends DomainException {

    public ReturnNotEligibleException(String message) {
        super(message);
    }
}
