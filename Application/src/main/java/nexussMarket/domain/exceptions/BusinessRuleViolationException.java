package nexussMarket.domain.exceptions;

/**
 * An operation the actor is authorized to perform, but that the current state
 * of the business data does not allow (e.g. confirming an empty cart, or
 * stocking an inactive warehouse). Authorization failures use
 * {@link OperationNotAllowedException} instead.
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
