package nexussMarket.adapters.in.rest.mappers;

import java.util.List;

import nexussMarket.adapters.in.rest.responses.AuditLogResponse;
import nexussMarket.adapters.in.rest.responses.OperationResponse;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;

/** Converts {@link Operation}s and {@link AuditLog}s into their responses. */
public final class AuditRestMapper {

    private AuditRestMapper() {
    }

    public static OperationResponse toResponse(Operation operation) {
        return new OperationResponse(operation.getIdentifier(), RestCodes.code(operation.getOperationType()),
                operation.getExecutionDate(), identifierOf(operation.getPerformedBy()),
                RestCodes.code(operation.getAffectedEntityType()), operation.getAffectedEntityId());
    }

    public static List<OperationResponse> toOperationResponses(List<Operation> operations) {
        return operations.stream().map(AuditRestMapper::toResponse).toList();
    }

    public static AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(auditLog.getAuditId(), RestCodes.code(auditLog.getOperationType()),
                auditLog.getOperationDate(), identifierOf(auditLog.getPerformedBy()),
                RestCodes.code(auditLog.getUserRole()), RestCodes.code(auditLog.getAffectedEntityType()),
                auditLog.getAffectedEntityId(), auditLog.getDetails());
    }

    public static List<AuditLogResponse> toAuditLogResponses(List<AuditLog> auditLogs) {
        return auditLogs.stream().map(AuditRestMapper::toResponse).toList();
    }

    private static String identifierOf(User user) {
        return user != null ? user.getIdentifier() : null;
    }
}
