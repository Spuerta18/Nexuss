package nexussMarket.domain.exceptions;

/**
 * A return request that was already approved or rejected was decided again.
 */
public class ReturnAlreadyDecidedException extends DomainException {

    public ReturnAlreadyDecidedException(String message) {
        super(message);
    }
}
