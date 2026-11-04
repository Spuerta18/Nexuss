package nexussMarket.domain.models;

/**
 * Answer of the payment gateway to one charge: approved with the gateway's
 * {@code reference}, or declined with a {@code reason}.
 */
public record PaymentResult(boolean approved, String reference, String reason) {

    public static PaymentResult approvedWith(String reference) {
        return new PaymentResult(true, reference, null);
    }

    public static PaymentResult declinedBecause(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
