package nexussMarket.adapters.out.persistence.mongodb.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import nexussMarket.adapters.out.persistence.mongodb.documents.AuditLogDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.OperationDocument;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

class AuditMappersTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 10, 26, 10, 0);
    private final Administrator admin = new Administrator("A-1", "Alex Admin", "admin@example.com", UserStatus.ACTIVE);

    @Test
    void operationRoundTripStoresCodesAndPerformerId() {
        Operation operation = new Operation("OP-1", OperationType.WAREHOUSE_DEACTIVATION, WHEN, admin,
                AffectedEntityType.WAREHOUSE, "W-1");

        OperationDocument document = OperationMapper.toDocument(operation);
        Operation rebuilt = OperationMapper.toDomain(document, admin);

        assertEquals("WAREHOUSE_DEACTIVATION", document.getOperationType());
        assertEquals("WAREHOUSE", document.getAffectedEntityType());
        assertEquals("A-1", document.getPerformedById());
        assertSame(OperationType.WAREHOUSE_DEACTIVATION, rebuilt.getOperationType());
        assertSame(AffectedEntityType.WAREHOUSE, rebuilt.getAffectedEntityType());
        assertEquals("W-1", rebuilt.getAffectedEntityId());
        assertEquals(WHEN, rebuilt.getExecutionDate());
    }

    @Test
    void auditLogRoundTripKeepsRoleAndDetails() {
        AuditLog auditLog = new AuditLog("AU-1", OperationType.USER_STATUS_CHANGE, WHEN, admin,
                SystemRole.ADMINISTRATOR, AffectedEntityType.USER, "B-1",
                Map.of("previousStatus", "ACTIVE", "newStatus", "BLOCKED"));

        AuditLogDocument document = AuditLogMapper.toDocument(auditLog);
        AuditLog rebuilt = AuditLogMapper.toDomain(document, admin);

        assertEquals("ADMINISTRATOR", document.getUserRole());
        assertSame(SystemRole.ADMINISTRATOR, rebuilt.getUserRole());
        assertEquals(Map.of("previousStatus", "ACTIVE", "newStatus", "BLOCKED"), rebuilt.getDetails());
    }

    @Test
    void auditLogWithoutPerformerHasNoRole() {
        AuditLog auditLog = new AuditLog("AU-2", OperationType.CART_CREATION, WHEN, null, null,
                AffectedEntityType.SHOPPING_CART, "C-1", null);

        AuditLogDocument document = AuditLogMapper.toDocument(auditLog);
        AuditLog rebuilt = AuditLogMapper.toDomain(document, null);

        assertNull(document.getPerformedById());
        assertNull(document.getUserRole());
        assertNull(rebuilt.getUserRole());
        assertEquals(Map.of(), rebuilt.getDetails());
    }
}
