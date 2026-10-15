package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Units of a product moved in or out of a warehouse (inbound, return, damaged). */
public record InventoryMovementRequest(
        @NotBlank String productId,
        @NotBlank String warehouseId,
        @NotNull @Positive Integer quantity) {
}
