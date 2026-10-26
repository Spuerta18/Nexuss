package nexussMarket.domain.models;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;

/**
 * A state-changing business operation: what was done ({@code operationType}),
 * when, by whom, and on which entity. Every Domain Service that changes state
 * records one, together with its {@link AuditLog}, after saving the change.
 *
 * <p>Immutable: operations are an append-only history and are never
 * modified.</p>
 */
public class Operation {

    private final String identifier;
    private final OperationType operationType;
    private final LocalDateTime executionDate;
    private final User performedBy;
    private final AffectedEntityType affectedEntityType;
    private final String affectedEntityId;

    public Operation(String identifier, OperationType operationType, LocalDateTime executionDate, User performedBy,
                     AffectedEntityType affectedEntityType, String affectedEntityId) {
        this.identifier = Objects.requireNonNull(identifier, "identifier must not be null");
        this.operationType = Objects.requireNonNull(operationType, "operationType must not be null");
        this.executionDate = Objects.requireNonNull(executionDate, "executionDate must not be null");
        this.performedBy = performedBy;
        this.affectedEntityType = Objects.requireNonNull(affectedEntityType, "affectedEntityType must not be null");
        this.affectedEntityId = Objects.requireNonNull(affectedEntityId, "affectedEntityId must not be null");
    }

    /** A new operation executed now, with a generated identifier. */
    public static Operation of(OperationType operationType, User performedBy, AffectedEntityType affectedEntityType,
                               String affectedEntityId) {
        return new Operation(UUID.randomUUID().toString(), operationType, LocalDateTime.now(), performedBy,
                affectedEntityType, affectedEntityId);
    }

    /** Unique identifier of the operation. */
    public String getIdentifier() {
        return identifier;
    }

    /** What was done. */
    public OperationType getOperationType() {
        return operationType;
    }

    /** When it was done. */
    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    /** The user who performed the operation. */
    public User getPerformedBy() {
        return performedBy;
    }

    /** Kind of entity the operation changed. */
    public AffectedEntityType getAffectedEntityType() {
        return affectedEntityType;
    }

    /** Identifier of the entity the operation changed. */
    public String getAffectedEntityId() {
        return affectedEntityId;
    }
}
