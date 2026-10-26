package nexussMarket.adapters.out.persistence.mongodb.mappers;

import java.util.LinkedHashMap;

import nexussMarket.adapters.out.persistence.mongodb.documents.AuditLogDocument;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;

/** Converts {@link AuditLog} to and from {@link AuditLogDocument}. */
public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogDocument toDocument(AuditLog auditLog) {
        AuditLogDocument document = new AuditLogDocument();
        document.setId(auditLog.getAuditId());
        document.setOperationType(auditLog.getOperationType().getCode());
        document.setOperationDate(auditLog.getOperationDate());
        document.setPerformedById(auditLog.getPerformedBy() != null ? auditLog.getPerformedBy().getIdentifier() : null);
        document.setUserRole(auditLog.getUserRole() != null ? auditLog.getUserRole().getCode() : null);
        document.setAffectedEntityType(auditLog.getAffectedEntityType().getCode());
        document.setAffectedEntityId(auditLog.getAffectedEntityId());
        document.setDetails(new LinkedHashMap<>(auditLog.getDetails()));
        return document;
    }

    /** {@code performedBy} is the user stored as {@code performedById}, already resolved; may be {@code null}. */
    public static AuditLog toDomain(AuditLogDocument document, User performedBy) {
        SystemRole userRole = document.getUserRole() != null
                ? CatalogCodes.resolve(SystemRole::fromCode, document.getUserRole(), "SystemRole")
                : null;
        return new AuditLog(document.getId(),
                CatalogCodes.resolve(OperationType::fromCode, document.getOperationType(), "OperationType"),
                document.getOperationDate(), performedBy, userRole,
                CatalogCodes.resolve(AffectedEntityType::fromCode, document.getAffectedEntityType(),
                        "AffectedEntityType"),
                document.getAffectedEntityId(), document.getDetails());
    }
}
