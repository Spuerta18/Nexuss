package nexussMarket.domain.exceptions;

/**
 * Base class of every business exception, so input adapters can handle any
 * domain error, present or future, in a single place.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
