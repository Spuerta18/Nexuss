package nexussMarket.domain.exceptions;

/** A password change was requested with a current password that does not match the stored one. */
public class IncorrectCurrentPasswordException extends DomainException {

    public IncorrectCurrentPasswordException(String message) {
        super(message);
    }
}
