package nexussMarket.domain.services;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.function.Predicate;

/**
 * Identifiers the server generates when a client creates a cart, order,
 * product or warehouse without one: the entity's prefix and eight random hex
 * digits, e.g. {@code ORD-ab12cd34}. A candidate already in use is skipped;
 * the insert that stores the entity still rejects a collision that happens
 * after the check, so a stored entity is never replaced.
 */
final class GeneratedIdentifiers {

    static final String CART = "CART";
    static final String ORDER = "ORD";
    static final String PRODUCT = "PRD";
    static final String WAREHOUSE = "WH";

    private static final int ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneratedIdentifiers() {
    }

    /** {@code requested} when the client sent one; otherwise a new identifier for which {@code taken} is false. */
    static String requestedOrGenerated(String requested, String prefix, Predicate<String> taken) {
        if (requested != null) {
            return requested;
        }
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            String candidate = prefix + "-" + randomHex();
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a free " + prefix + " identifier");
    }

    private static String randomHex() {
        byte[] bytes = new byte[4];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
