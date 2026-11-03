package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * A warehouse to register. The ownership comes from the endpoint: a seller
 * registers Seller-owned warehouses, an administrator Marketplace-owned ones.
 * {@code identifier} is optional: without it the server generates one.
 */
public record RegisterWarehouseRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") String identifier,
        @NotBlank String name,
        @NotBlank String address) {
}
