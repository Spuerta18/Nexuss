package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Credentials exchanged for an access token. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {

    /** Never prints the password, e.g. if the request ends up in a log. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
