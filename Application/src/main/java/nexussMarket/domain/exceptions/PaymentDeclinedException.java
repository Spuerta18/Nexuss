package nexussMarket.domain.exceptions;

/** The payment gateway declined the charge; the order is left unchanged ({@code 402}). */
public class PaymentDeclinedException extends DomainException {

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
