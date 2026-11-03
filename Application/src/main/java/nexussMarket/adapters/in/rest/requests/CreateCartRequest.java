package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.Pattern;

/**
 * Creates an empty cart for the authenticated buyer. {@code cartId} is
 * optional: without it (or without a body) the server generates one.
 */
public record CreateCartRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") String cartId) {
}
