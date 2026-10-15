package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * The next status of an order. In practice {@code PAID}: {@code SHIPPED} and
 * {@code DELIVERED} are reached through the shipment endpoints.
 */
public record AdvanceOrderStatusRequest(
        @NotBlank String targetStatus) {
}
