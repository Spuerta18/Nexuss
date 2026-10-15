package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * A warehouse to register. The ownership comes from the endpoint: a seller
 * registers Seller-owned warehouses, an administrator Marketplace-owned ones.
 */
public record RegisterWarehouseRequest(
        @NotBlank String identifier,
        @NotBlank String name,
        @NotBlank String address) {
}
