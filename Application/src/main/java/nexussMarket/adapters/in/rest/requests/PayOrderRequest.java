package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A buyer's payment of an order. {@code paymentToken} identifies the payment
 * method at the gateway; the simulated gateway approves any token except
 * {@code DECLINED}. Never printed, e.g. if the request ends up in a log.
 */
public record PayOrderRequest(@NotBlank @Size(max = 200) String paymentToken) {

    @Override
    public String toString() {
        return "PayOrderRequest[paymentToken=***]";
    }
}
