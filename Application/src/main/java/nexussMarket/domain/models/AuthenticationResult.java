package nexussMarket.domain.models;

/**
 * Outcome of a successful authentication: the authenticated {@link User} and
 * the access token issued for it.
 */
public class AuthenticationResult {

    private final User user;
    private final String token;

    public AuthenticationResult(User user, String token) {
        this.user = user;
        this.token = token;
    }

    /** The authenticated user. */
    public User getUser() {
        return user;
    }

    /** Access token the user presents on later requests. */
    public String getToken() {
        return token;
    }
}
