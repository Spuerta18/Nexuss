package nexussMarket.domain.services;

import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.ports.out.AuditLogRepositoryPort;

/**
 * Internal service, not exposed as a use case. Appends an {@link AuditLog} to
 * the audit trail. Services do not call it directly: they use
 * {@link RegisterOperationAndAuditService}.
 */
public class RegisterAuditLogService {

    private final AuditLogRepositoryPort auditLogRepositoryPort;

    public RegisterAuditLogService(AuditLogRepositoryPort auditLogRepositoryPort) {
        this.auditLogRepositoryPort = auditLogRepositoryPort;
    }

    public AuditLog execute(AuditLog auditLog) {
        return auditLogRepositoryPort.save(auditLog);
    }
}
