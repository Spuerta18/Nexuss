package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import nexussMarket.domain.exceptions.InvalidShipmentStatusTransitionException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.valueobjects.OrderStatus;

class LogisticsOperatorRestControllerTest extends RestControllerTestSupport {

    @Test
    void consultOrderIsAllowedToLogisticsOperators() throws Exception {
        when(logisticsOperatorPort.consultOrder(any(), any()))
                .thenReturn(new Order("O-1", buyer(), OrderStatus.PAID, List.of(), LocalDateTime.now()));

        mockMvc.perform(get("/api/logistics/orders/O-1").header(authorization(), bearer(logisticsOperator())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("O-1"))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void advanceOrderStatusResolvesTheTargetStatus() throws Exception {
        when(logisticsOperatorPort.advanceOrderStatus(any(), any(), eq(OrderStatus.PAID)))
                .thenReturn(new Order("O-1", buyer(), OrderStatus.PAID, List.of(), LocalDateTime.now()));

        mockMvc.perform(post("/api/logistics/orders/O-1/status").header(authorization(), bearer(logisticsOperator()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetStatus\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void createShipmentAnswers201() throws Exception {
        when(logisticsOperatorPort.createShipment(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        mockMvc.perform(post("/api/logistics/shipments").header(authorization(), bearer(logisticsOperator()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"shipmentId\":\"SH-1\",\"orderId\":\"O-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value("SH-1"))
                .andExpect(jsonPath("$.orderId").value("O-1"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void invalidShipmentTransitionAnswers409() throws Exception {
        when(logisticsOperatorPort.dispatchShipment(any(), any()))
                .thenThrow(new InvalidShipmentStatusTransitionException("Shipment SH-1 is not PACKED"));

        mockMvc.perform(post("/api/logistics/shipments/SH-1/dispatch")
                        .header(authorization(), bearer(logisticsOperator())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Shipment SH-1 is not PACKED"));
    }

    @Test
    void withoutTokenAnswers401() throws Exception {
        mockMvc.perform(post("/api/logistics/shipments/SH-1/pack"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(logisticsOperatorPort);
    }

    @Test
    void anotherRoleAnswers403() throws Exception {
        mockMvc.perform(post("/api/logistics/shipments/SH-1/pack").header(authorization(), bearer(buyer())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(logisticsOperatorPort);
    }

    @Test
    void invalidMovementAnswers400() throws Exception {
        mockMvc.perform(post("/api/logistics/inventory/damaged").header(authorization(), bearer(logisticsOperator()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P-1\",\"warehouseId\":\"WH-1\",\"quantity\":-3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
        verifyNoInteractions(logisticsOperatorPort);
    }

    @Test
    void unknownTargetStatusAnswers400() throws Exception {
        mockMvc.perform(post("/api/logistics/orders/O-1/status").header(authorization(), bearer(logisticsOperator()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetStatus\":\"LOST\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(logisticsOperatorPort);
    }
}
