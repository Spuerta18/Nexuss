package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.Pattern;

/**
 * Converts a cart into an order. {@code orderId} is optional: without it (or
 * without a body) the server generates one.
 */
public record ConfirmOrderRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") String orderId) {
}
