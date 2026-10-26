package nexussMarket.domain.exceptions;

/** A new password does not meet the password rules (at least 8 characters). */
public class InvalidPasswordException extends DomainException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
