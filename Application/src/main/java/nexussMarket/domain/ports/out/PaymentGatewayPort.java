package nexussMarket.domain.ports.out;

import nexussMarket.domain.models.PaymentResult;
import nexussMarket.domain.valueobjects.Money;

/** Charges a buyer's payment through an external payment gateway. */
public interface PaymentGatewayPort {

    /**
     * Charges {@code amount} for order {@code orderId} with the payment method
     * identified by {@code paymentToken} (issued to the client by the gateway;
     * never stored nor logged). Never throws for a declined payment: the result
     * says whether it was approved.
     */
    PaymentResult charge(String orderId, Money amount, String paymentToken);
}
