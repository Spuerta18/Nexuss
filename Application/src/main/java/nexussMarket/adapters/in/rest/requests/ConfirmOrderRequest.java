package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;

/** Converts a cart into an order with the given identifier. */
public record ConfirmOrderRequest(
        @NotBlank String orderId) {
}
