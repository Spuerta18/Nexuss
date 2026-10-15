package nexussMarket.adapters.in.rest.responses;

/**
 * A user as exposed by the API. Never carries the password hash.
 *
 * @param commercialStatus buyers only; {@code null} for other roles
 * @param primaryAddress   buyers only; {@code null} for other roles
 * @param sellerStatus     sellers only; {@code null} for other roles
 */
public record UserResponse(String identifier, String fullName, String email, String role, String status,
                           String commercialStatus, String primaryAddress, String sellerStatus) {
}
