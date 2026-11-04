package nexussMarket.adapters.out.payment;

import java.util.Locale;

import org.springframework.stereotype.Component;

import nexussMarket.domain.models.PaymentResult;
import nexussMarket.domain.ports.out.PaymentGatewayPort;
import nexussMarket.domain.valueobjects.Money;

/**
 * Deterministic stand-in for a real payment gateway, so payments can be
 * demonstrated and tested without one. Every charge is approved, with
 * reference {@code SIM-<orderId>}, except with the documented test token
 * {@value #DECLINED_TEST_TOKEN} (any case), which is always declined. Nothing is
 * charged and the token is not kept.
 */
@Component
public class SimulatedPaymentGatewayAdapter implements PaymentGatewayPort {

    /** Payment token that the simulated gateway always declines. */
    public static final String DECLINED_TEST_TOKEN = "DECLINED";

    @Override
    public PaymentResult charge(String orderId, Money amount, String paymentToken) {
        if (paymentToken != null && DECLINED_TEST_TOKEN.equals(paymentToken.trim().toUpperCase(Locale.ROOT))) {
            return PaymentResult.declinedBecause("the card was declined (simulated gateway test token)");
        }
        return PaymentResult.approvedWith("SIM-" + orderId);
    }
}
