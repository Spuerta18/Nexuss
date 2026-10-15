package nexussMarket.adapters.in.rest.responses;

/**
 * Result of a successful login. The client sends {@code accessToken} back in
 * the {@code Authorization: Bearer <token>} header.
 */
public record AuthResponse(String accessToken, String tokenType, UserResponse user) {

    /** Never prints the token, e.g. if the response ends up in a log. */
    @Override
    public String toString() {
        return "AuthResponse[tokenType=" + tokenType + ", accessToken=***, user=" + user + "]";
    }
}
