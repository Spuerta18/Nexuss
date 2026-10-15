package nexussMarket.adapters.in.rest.exception;

/**
 * A request value the REST mappers cannot translate into the domain, such as
 * an unknown status or currency code. Answered with {@code 400 Bad Request}.
 * It belongs to the REST adapter, not to the domain.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
