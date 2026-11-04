package nexussMarket.domain.valueobjects;

import java.util.Locale;

/**
 * Canonical form of an email address: surrounding blanks removed and every
 * letter in lower case. Every email the domain stores or looks up goes
 * through {@link #normalize(String)}, so {@code " Ana@Example.COM "} and
 * {@code "ana@example.com"} are the same account.
 */
public final class EmailAddress {

    private EmailAddress() {
    }

    /** The canonical form of {@code email}, or {@code null} when it is {@code null}. */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
