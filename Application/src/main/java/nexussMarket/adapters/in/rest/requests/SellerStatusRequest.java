package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;

/** New commercial status of a seller: {@code ACTIVE} or {@code SUSPENDED}. */
public record SellerStatusRequest(
        @NotBlank String status) {
}
