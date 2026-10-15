package nexussMarket.adapters.in.rest.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A product to publish. {@code productType} is {@code PHYSICAL} or
 * {@code DIGITAL}; {@code variants} may be omitted. The seller is always the
 * authenticated user.
 */
public record PublishProductRequest(
        @NotBlank String identifier,
        @NotBlank String name,
        @NotBlank String productType,
        @NotNull @Valid MoneyRequest price,
        List<@Valid VariantRequest> variants) {
}
