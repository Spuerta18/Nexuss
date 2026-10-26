package nexussMarket.domain.exceptions;

/**
 * A refund that was already paid was processed again.
 */
public class RefundAlreadyProcessedException extends DomainException {

    public RefundAlreadyProcessedException(String message) {
        super(message);
    }
}
