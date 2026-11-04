package nexussMarket.adapters.in.rest.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.exceptions.PaymentDeclinedException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.valueobjects.OrderStatus;

/** {@code POST /api/buyer/orders/{orderId}/payment}: status codes and what the response carries. */
class BuyerPaymentRestControllerTest extends RestControllerTestSupport {

    private static final String BODY = "{\"paymentToken\":\"tok_test_visa\"}";

    @Test
    void anApprovedPaymentAnswersTheOrder() throws Exception {
        when(buyerPort.payOrder(any(), any(), eq("tok_test_visa")))
                .thenReturn(new Order("O-1", buyer(), OrderStatus.PAID));

        mockMvc.perform(post("/api/buyer/orders/O-1/payment").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("O-1"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(content().string(not(containsString("tok_test_visa"))));
    }

    @Test
    void aDeclinedPaymentIs402() throws Exception {
        when(buyerPort.payOrder(any(), any(), any()))
                .thenThrow(new PaymentDeclinedException("The payment of order O-1 was declined: the card was declined"));

        mockMvc.perform(post("/api/buyer/orders/O-1/payment").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentToken\":\"DECLINED\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402));
    }

    @Test
    void payingTwiceIs409AndAForeignOrderIs403() throws Exception {
        when(buyerPort.payOrder(any(), any(), any()))
                .thenThrow(new InvalidOrderStatusTransitionException("Order O-1 is PAID"))
                .thenThrow(new OperationNotAllowedException("Not your order"));

        mockMvc.perform(post("/api/buyer/orders/O-1/payment").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/buyer/orders/O-1/payment").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void aMissingTokenIs400AndOtherRolesOrAnonymousCannotPay() throws Exception {
        mockMvc.perform(post("/api/buyer/orders/O-1/payment").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentToken\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/buyer/orders/O-1/payment").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/buyer/orders/O-1/payment")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(buyerPort, never()).payOrder(any(), any(), any());
    }
}
