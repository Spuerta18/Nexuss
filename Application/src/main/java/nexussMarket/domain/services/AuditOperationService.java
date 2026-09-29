package nexussMarket.domain.services;

import java.time.LocalDateTime;

import nexussMarket.domain.ports.out.AuditLogPort;

/**
 * Internal service, not exposed as a use case. Records which authenticated
 * user performed which operation. It is invoked for every authenticated
 * request by the security layer, so traceability does not depend on each use
 * case remembering to call it.
 */
public class AuditOperationService {

    private final AuditLogPort auditLogPort;

    public AuditOperationService(AuditLogPort auditLogPort) {
        this.auditLogPort = auditLogPort;
    }

    public void record(String userId, String operation) {
        auditLogPort.record(userId, operation, LocalDateTime.now());
    }
}
