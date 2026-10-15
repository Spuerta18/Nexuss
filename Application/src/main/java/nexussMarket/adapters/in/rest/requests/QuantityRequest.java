package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** New quantity of a cart line. */
public record QuantityRequest(
        @NotNull @Positive Integer quantity) {
}
