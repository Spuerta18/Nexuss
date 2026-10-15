package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;

/** Creates an empty cart for the authenticated buyer. */
public record CreateCartRequest(
        @NotBlank String cartId) {
}
