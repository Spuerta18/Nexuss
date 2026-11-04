package nexussMarket.adapters.in.rest.requests;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Changes to the caller's own profile. Email, role and status cannot be sent:
 * unknown fields are ignored. The addresses are only accepted from buyers;
 * {@code null} keeps the current value, and an empty
 * {@code additionalAddresses} removes them all.
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 200) String fullName,
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 300) String primaryAddress,
        @Size(max = 5) List<@NotBlank @Size(max = 300) String> additionalAddresses) {
}
