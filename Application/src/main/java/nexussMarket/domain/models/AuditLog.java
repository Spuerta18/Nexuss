package nexussMarket.domain.models;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Audit trail entry of an {@link Operation}: who performed it, with which role
 * at that moment, on which entity, and the relevant {@code details} (for
 * example {@code previousStatus} and {@code newStatus}).
 *
 * <p>Immutable and append-only. {@code details} never holds credentials: a key
 * that names a password, a hash, a token or a secret is rejected.</p>
 */
public class AuditLog {

    /** Fragments of detail keys that would reveal credentials. */
    private static final List<String> FORBIDDEN_DETAIL_KEYS = List.of("password", "hash", "token", "secret");

    private final String auditId;
    private final OperationType operationType;
    private final LocalDateTime operationDate;
    private final User performedBy;
    private final SystemRole userRole;
    private final AffectedEntityType affectedEntityType;
    private final String affectedEntityId;
    private final Map<String, Object> details;

    public AuditLog(String auditId, OperationType operationType, LocalDateTime operationDate, User performedBy,
                    SystemRole userRole, AffectedEntityType affectedEntityType, String affectedEntityId,
                    Map<String, Object> details) {
        this.auditId = Objects.requireNonNull(auditId, "auditId must not be null");
        this.operationType = Objects.requireNonNull(operationType, "operationType must not be null");
        this.operationDate = Objects.requireNonNull(operationDate, "operationDate must not be null");
        this.performedBy = performedBy;
        this.userRole = userRole;
        this.affectedEntityType = Objects.requireNonNull(affectedEntityType, "affectedEntityType must not be null");
        this.affectedEntityId = Objects.requireNonNull(affectedEntityId, "affectedEntityId must not be null");
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(requireNoCredentials(details)));
    }

    private static Map<String, Object> requireNoCredentials(Map<String, Object> details) {
        if (details == null) {
            return Map.of();
        }
        for (String key : details.keySet()) {
            String normalized = key.toLowerCase(Locale.ROOT);
            if (FORBIDDEN_DETAIL_KEYS.stream().anyMatch(normalized::contains)) {
                throw new IllegalArgumentException("Audit details must not hold credentials: '" + key + "'");
            }
        }
        return details;
    }

    /** Unique identifier of the audit entry. */
    public String getAuditId() {
        return auditId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    /** When the audit entry was recorded. */
    public LocalDateTime getOperationDate() {
        return operationDate;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    /** Role of {@code performedBy} when the operation was performed, or {@code null} if unknown. */
    public SystemRole getUserRole() {
        return userRole;
    }

    public AffectedEntityType getAffectedEntityType() {
        return affectedEntityType;
    }

    public String getAffectedEntityId() {
        return affectedEntityId;
    }

    /** Read-only details of the operation; never {@code null}. */
    public Map<String, Object> getDetails() {
        return details;
    }
}
