package nexussMarket.adapters.out.persistence.mongodb.mappers;

import java.time.LocalDateTime;

import nexussMarket.adapters.out.persistence.mongodb.documents.AuditLogDocument;

/**
 * Builds {@link AuditLogDocument} entries from the values received by
 * {@code AuditLogPort}. There is no {@code toDomain}: the audit log has no
 * Domain Model and no port reads it back.
 */
public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogDocument toDocument(String userId, String operation, LocalDateTime occurredAt) {
        AuditLogDocument document = new AuditLogDocument();
        document.setUserId(userId);
        document.setOperation(operation);
        document.setOccurredAt(occurredAt);
        return document;
    }
}
