package nexussMarket.domain.exceptions;

public class InvalidOrderStatusTransitionException extends DomainException {

    public InvalidOrderStatusTransitionException(String message) {
        super(message);
    }
}
