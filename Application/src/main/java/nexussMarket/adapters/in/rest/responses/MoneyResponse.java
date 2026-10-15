package nexussMarket.adapters.in.rest.responses;

import java.math.BigDecimal;

/** An amount with its currency code. */
public record MoneyResponse(BigDecimal amount, String currency) {
}
