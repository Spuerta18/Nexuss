package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;

/** Registers the shipment of a paid order. */
public record CreateShipmentRequest(
        @NotBlank String shipmentId,
        @NotBlank String orderId) {
}
