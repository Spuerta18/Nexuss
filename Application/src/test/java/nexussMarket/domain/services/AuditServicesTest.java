package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;

/**
 * {@link RegisterOperationAndAuditService} builds the audit log from the
 * operation, and the consult services are restricted to administrators and
 * supervisors.
 */
class AuditServicesTest extends AuditedServiceTestSupport {

    @Test
    void operationIsStoredBeforeItsAuditLog() {
        Operation operation = Operation.of(OperationType.CART_CREATION, buyer(), AffectedEntityType.SHOPPING_CART, "C-1");

        AuditLog entry = audit.execute(operation, Map.of("buyerId", "B-1"));

        InOrder order = Mockito.inOrder(operationRepositoryPort, auditLogRepositoryPort);
        order.verify(operationRepositoryPort).save(operation);
        order.verify(auditLogRepositoryPort).save(entry);
        assertEquals(Map.of("buyerId", "B-1"), entry.getDetails());
    }

    @Test
    void roleIsNullWhenThereIsNoPerformer() {
        Operation operation = Operation.of(OperationType.CART_CREATION, null, AffectedEntityType.SHOPPING_CART, "C-1");

        AuditLog entry = audit.execute(operation, null);

        assertNull(entry.getPerformedBy());
        assertNull(entry.getUserRole());
        assertEquals(Map.of(), entry.getDetails());
    }

    @Test
    void detailsNamingCredentialsAreRejected() {
        Operation operation = Operation.of(OperationType.PASSWORD_CHANGE, buyer(), AffectedEntityType.USER, "B-1");

        for (String key : List.of("password", "newPasswordHash", "accessToken", "jwtSecret")) {
            assertThrows(IllegalArgumentException.class, () -> audit.execute(operation, Map.of(key, "x")), key);
        }
    }

    @Test
    void administratorsAndSupervisorsConsultTheHistory() {
        Operation operation = Operation.of(OperationType.CART_CREATION, buyer(), AffectedEntityType.SHOPPING_CART, "C-1");
        when(operationRepositoryPort.findAll()).thenReturn(List.of(operation));
        ConsultOperationsService operations = new ConsultOperationsService(operationRepositoryPort, validateRole);
        ConsultAuditLogsService auditLogs = new ConsultAuditLogsService(auditLogRepositoryPort, validateRole);
        Supervisor supervisor = supervisor();

        assertSame(operation, operations.execute(administrator()).get(0));
        assertSame(operation, operations.execute(supervisor).get(0));
        auditLogs.execute(supervisor);
        verify(auditLogRepositoryPort).findAll();
    }

    @Test
    void otherRolesCannotConsultTheHistory() {
        ConsultOperationsService operations = new ConsultOperationsService(operationRepositoryPort, validateRole);
        ConsultAuditLogsService auditLogs = new ConsultAuditLogsService(auditLogRepositoryPort, validateRole);

        assertThrows(OperationNotAllowedException.class, () -> operations.execute(buyer()));
        assertThrows(OperationNotAllowedException.class, () -> operations.execute(seller()));
        assertThrows(OperationNotAllowedException.class, () -> auditLogs.execute(logisticsOperator()));
        assertNothingAudited();
    }
}
