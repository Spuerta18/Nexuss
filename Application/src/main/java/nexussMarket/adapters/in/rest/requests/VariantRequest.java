package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;

/** One product variant: {@code attribute} is {@code COLOR}, {@code SIZE} or {@code MODEL}. */
public record VariantRequest(
        @NotBlank String attribute,
        @NotBlank String value) {
}
