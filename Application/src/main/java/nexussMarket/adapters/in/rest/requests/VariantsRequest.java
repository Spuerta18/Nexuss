package nexussMarket.adapters.in.rest.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** The complete new list of variants of a product; an empty list removes them all. */
public record VariantsRequest(
        @NotNull List<@Valid VariantRequest> variants) {
}
