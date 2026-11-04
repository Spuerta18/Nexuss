package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** New name and price of a product; the identifier comes from the URL. */
public record UpdateProductRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @Valid MoneyRequest price) {
}
