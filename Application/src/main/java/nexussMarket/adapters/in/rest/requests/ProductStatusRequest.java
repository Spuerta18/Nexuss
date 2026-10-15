package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;

/** New product status: {@code PUBLISHED}, {@code SUSPENDED} or {@code DISCONTINUED}. */
public record ProductStatusRequest(
        @NotBlank String status) {
}
