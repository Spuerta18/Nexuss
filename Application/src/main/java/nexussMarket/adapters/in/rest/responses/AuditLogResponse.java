package nexussMarket.adapters.in.rest.responses;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * An audit log entry. The performer appears by identifier and the role they
 * had at that moment; {@code details} never holds credentials.
 */
public record AuditLogResponse(
        String auditId,
        String operationType,
        LocalDateTime operationDate,
        String performedBy,
        String userRole,
        String affectedEntityType,
        String affectedEntityId,
        Map<String, Object> details) {
}
