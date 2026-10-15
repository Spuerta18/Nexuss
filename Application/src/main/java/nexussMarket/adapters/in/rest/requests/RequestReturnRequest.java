package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Asks to return a delivered order. */
public record RequestReturnRequest(
        @NotBlank String returnRequestId,
        @NotBlank @Size(max = 500) String reason) {
}
