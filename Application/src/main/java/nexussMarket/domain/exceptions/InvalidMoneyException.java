package nexussMarket.domain.exceptions;

/**
 * A {@code Money} value without amount or currency, or with a negative amount.
 */
public class InvalidMoneyException extends DomainException {

    public InvalidMoneyException(String message) {
        super(message);
    }
}
