package nexussMarket.domain.exceptions;

public class InvalidShipmentStatusTransitionException extends DomainException {

    public InvalidShipmentStatusTransitionException(String message) {
        super(message);
    }
}
