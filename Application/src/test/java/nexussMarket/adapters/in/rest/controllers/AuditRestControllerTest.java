package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;

/** {@code GET /operations} and {@code GET /audit-logs} for administrators and supervisors. */
class AuditRestControllerTest extends RestControllerTestSupport {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 10, 26, 10, 0);

    @Test
    void administratorConsultsOperations() throws Exception {
        Administrator admin = administrator();
        when(administratorPort.consultOperations(any())).thenReturn(List.of(
                new Operation("OP-1", OperationType.SELLER_REGISTRATION, WHEN, admin, AffectedEntityType.USER, "S-1")));

        mockMvc.perform(get("/api/admin/operations").header(authorization(), bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].identifier").value("OP-1"))
                .andExpect(jsonPath("$[0].operationType").value("SELLER_REGISTRATION"))
                .andExpect(jsonPath("$[0].performedBy").value("A-1"))
                .andExpect(jsonPath("$[0].affectedEntityType").value("USER"))
                .andExpect(jsonPath("$[0].affectedEntityId").value("S-1"));
    }

    @Test
    void administratorConsultsAuditLogsWithDetails() throws Exception {
        Administrator admin = administrator();
        when(administratorPort.consultAuditLogs(any())).thenReturn(List.of(new AuditLog("AU-1",
                OperationType.USER_STATUS_CHANGE, WHEN, admin, SystemRole.ADMINISTRATOR, AffectedEntityType.USER,
                "B-1", Map.of("previousStatus", "ACTIVE", "newStatus", "BLOCKED"))));

        mockMvc.perform(get("/api/admin/audit-logs").header(authorization(), bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].auditId").value("AU-1"))
                .andExpect(jsonPath("$[0].userRole").value("ADMINISTRATOR"))
                .andExpect(jsonPath("$[0].details.previousStatus").value("ACTIVE"))
                .andExpect(jsonPath("$[0].details.newStatus").value("BLOCKED"));
    }

    @Test
    void supervisorConsultsOperationsAndAuditLogs() throws Exception {
        when(supervisorPort.consultOperations(any())).thenReturn(List.of());
        when(supervisorPort.consultAuditLogs(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/supervisor/operations").header(authorization(), bearer(supervisor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/supervisor/audit-logs").header(authorization(), bearer(supervisor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void otherRolesAndAnonymousCallersAreRejected() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs").header(authorization(), bearer(buyer())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/supervisor/operations").header(authorization(), bearer(seller())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/operations"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(administratorPort, supervisorPort);
    }
}
