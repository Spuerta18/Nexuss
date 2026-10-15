package nexussMarket.adapters.in.rest.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * New operational status of a user ({@code ACTIVE}, {@code BLOCKED} or
 * {@code INACTIVE}). {@code role} is the role of the user
 * being changed: {@code User} is abstract, so the REST mapper needs it to
 * build a reference of the right subtype from the identifier in the URL.
 */
public record UserStatusRequest(
        @NotBlank String role,
        @NotBlank String status) {
}
