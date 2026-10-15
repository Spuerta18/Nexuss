package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Adds units of a product to a cart; merged with an existing line of the same product. */
public record AddCartLineRequest(
        @NotBlank String productId,
        @NotNull @Positive Integer quantity) {
}
