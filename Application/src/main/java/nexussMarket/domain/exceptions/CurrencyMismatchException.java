package nexussMarket.domain.exceptions;

/**
 * Amounts in different currencies were combined, e.g. a cart that would hold products priced in two
 * currencies, or a sum of {@code Money} values with different currencies.
 */
public class CurrencyMismatchException extends DomainException {

    public CurrencyMismatchException(String message) {
        super(message);
    }
}
