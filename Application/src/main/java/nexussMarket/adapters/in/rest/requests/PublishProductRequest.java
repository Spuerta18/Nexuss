package nexussMarket.adapters.in.rest.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * A product to publish. {@code productType} is {@code PHYSICAL} or
 * {@code DIGITAL}; {@code variants} may be omitted. The seller is always the
 * authenticated user. {@code identifier} is optional: without it the server
 * generates one.
 */
public record PublishProductRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") String identifier,
        @NotBlank String name,
        @NotBlank String productType,
        @NotNull @Valid MoneyRequest price,
        List<@Valid VariantRequest> variants) {
}
