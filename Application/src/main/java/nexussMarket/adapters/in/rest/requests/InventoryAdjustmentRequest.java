package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** A manual correction of the available stock, positive or negative. */
public record InventoryAdjustmentRequest(
        @NotBlank String productId,
        @NotBlank String warehouseId,
        @NotNull Integer quantityDelta) {
}
