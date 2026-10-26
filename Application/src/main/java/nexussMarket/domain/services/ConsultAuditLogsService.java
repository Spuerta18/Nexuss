package nexussMarket.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.AuditLogRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/** Returns the audit trail, most recent first. Only administrators and supervisors may consult it. */
@Service
public class ConsultAuditLogsService {

    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultAuditLogsService(AuditLogRepositoryPort auditLogRepositoryPort,
                                   ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.auditLogRepositoryPort = auditLogRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public List<AuditLog> execute(User actor) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        return auditLogRepositoryPort.findAll();
    }
}
