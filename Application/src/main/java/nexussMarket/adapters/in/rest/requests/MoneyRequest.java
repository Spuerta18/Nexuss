package nexussMarket.adapters.in.rest.requests;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** An amount greater than zero and its ISO 4217 currency code (e.g. {@code COP}). */
public record MoneyRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency) {
}
