package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SystemRole;

/** The paginated list routes: page shape, parameters, limits and who may call them. */
class ListsRestControllerTest extends RestControllerTestSupport {

    @Test
    void administratorsListUsersByRoleWithPageInformation() throws Exception {
        when(administratorPort.listUsers(any(), eq(SystemRole.SELLER), eq(new PageQuery(1, 2))))
                .thenReturn(new ResultPage<User>(List.of(seller()), 1, 2, 3));

        mockMvc.perform(get("/api/admin/users?role=SELLER&page=1&size=2").header(authorization(),
                        bearer(administrator())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].role").value("SELLER"))
                .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void theDefaultPageIsTheFirstOfTwenty() throws Exception {
        when(logisticsOperatorPort.listOrders(any(), eq(new PageQuery(0, 20))))
                .thenReturn(new ResultPage<>(List.of(new Order("O-1", buyer(), OrderStatus.PAID)), 0, 20, 1));

        mockMvc.perform(get("/api/logistics/orders").header(authorization(), bearer(logisticsOperator())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].identifier").value("O-1"))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void sellersAndSupervisorsHaveTheirLists() throws Exception {
        when(sellerPort.listInventory(any(), any())).thenReturn(new ResultPage<>(List.of(), 0, 20, 0));
        when(sellerPort.listWarehouses(any(), any())).thenReturn(new ResultPage<>(List.of(), 0, 20, 0));
        when(sellerPort.listOrders(any(), any())).thenReturn(new ResultPage<>(List.of(), 0, 20, 0));
        when(supervisorPort.listOrders(any(), any())).thenReturn(new ResultPage<>(List.of(), 0, 20, 0));

        for (String route : List.of("/api/seller/inventory", "/api/seller/warehouses", "/api/seller/orders")) {
            mockMvc.perform(get(route).header(authorization(), bearer(seller())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPages").value(0));
        }
        mockMvc.perform(get("/api/supervisor/orders").header(authorization(), bearer(supervisor())))
                .andExpect(status().isOk());
    }

    @Test
    void invalidPagesOrRolesAre400() throws Exception {
        String admin = bearer(administrator());
        mockMvc.perform(get("/api/admin/orders?size=101").header(authorization(), admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/orders?page=-1").header(authorization(), admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/orders?page=abc").header(authorization(), admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/users?role=WIZARD").header(authorization(), admin))
                .andExpect(status().isBadRequest());
        verify(administratorPort, never()).listOrders(any(), any());
    }

    @Test
    void otherRolesAreForbiddenAndAnonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users").header(authorization(), bearer(buyer())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/seller/orders").header(authorization(), bearer(logisticsOperator())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/supervisor/inventory"))
                .andExpect(status().isUnauthorized());
    }
}
