package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration of a seller, supervisor or logistics operator by an
 * administrator. The password must have 8 to 72 characters: BCrypt ignores
 * anything beyond 72 bytes.
 */
public record RegisterStaffRequest(
        @NotBlank String identifier,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password) {

    /** Never prints the password, e.g. if the request ends up in a log. */
    @Override
    public String toString() {
        return "RegisterStaffRequest[identifier=" + identifier + ", email=" + email + ", password=***]";
    }
}
