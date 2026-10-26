package nexussMarket.domain.services;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the {@code details} of an audit log entry, keeping the order in which
 * the facts are given. Entries with a {@code null} value are left out.
 */
final class AuditDetails {

    private AuditDetails() {
    }

    /** {@code keyValues} alternates keys (strings) and values: {@code of("previousStatus", "A", "newStatus", "B")}. */
    static Map<String, Object> of(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Audit details need a value for every key");
        }
        Map<String, Object> details = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) {
                details.put((String) keyValues[i], keyValues[i + 1]);
            }
        }
        return details;
    }
}
