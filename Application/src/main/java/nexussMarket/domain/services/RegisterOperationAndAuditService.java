package nexussMarket.domain.services;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Internal service, not exposed as a use case. Every Domain Service that
 * changes state calls it once, after saving the change: it persists the
 * {@link Operation} and then an {@link AuditLog} built from it, dated now, with
 * the performer's current role and the given {@code details}.
 *
 * <p>{@code details} carries the facts worth tracing, such as
 * {@code previousStatus} and {@code newStatus}; never passwords, password
 * hashes or tokens ({@link AuditLog} rejects such keys).</p>
 */
public class RegisterOperationAndAuditService {

    private final RegisterOperationService registerOperationService;
    private final RegisterAuditLogService registerAuditLogService;

    public RegisterOperationAndAuditService(RegisterOperationService registerOperationService,
                                            RegisterAuditLogService registerAuditLogService) {
        this.registerOperationService = registerOperationService;
        this.registerAuditLogService = registerAuditLogService;
    }

    public AuditLog execute(Operation operation, Map<String, Object> details) {
        Operation saved = registerOperationService.execute(operation);
        User performedBy = saved.getPerformedBy();
        SystemRole userRole = performedBy != null ? performedBy.getRole() : null;
        AuditLog auditLog = new AuditLog(UUID.randomUUID().toString(), saved.getOperationType(), LocalDateTime.now(),
                performedBy, userRole, saved.getAffectedEntityType(), saved.getAffectedEntityId(), details);
        return registerAuditLogService.execute(auditLog);
    }
}
