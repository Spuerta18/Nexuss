package nexussMarket.adapters.in.rest.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.UserStatus;

class AdministratorRestControllerTest extends RestControllerTestSupport {

    private static final String STAFF = "{\"identifier\":\"S-9\",\"fullName\":\"New Seller\","
            + "\"email\":\"new.seller@example.com\",\"password\":\"Strong-Pass-1\"}";

    @Test
    void registerSellerAnswers201WithoutPasswordHash() throws Exception {
        when(administratorPort.registerSeller(any(), any(), eq("Strong-Pass-1"))).thenAnswer(invocation -> {
            Seller seller = invocation.getArgument(1);
            seller.setPasswordHash("$2a$10$hash");
            return seller;
        });

        mockMvc.perform(post("/api/admin/sellers").header(authorization(), bearer(administrator()))
                        .contentType(MediaType.APPLICATION_JSON).content(STAFF))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value("S-9"))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(content().string(not(containsString("$2a$10$hash"))))
                .andExpect(content().string(not(containsString("Strong-Pass-1"))));
    }

    @Test
    void updateUserStatusBuildsTheReferenceFromTheRoleInTheBody() throws Exception {
        when(administratorPort.updateUserStatus(any(), any(), eq(UserStatus.BLOCKED))).thenReturn(seller());

        mockMvc.perform(patch("/api/admin/users/S-1/status").header(authorization(), bearer(administrator()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SELLER\",\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<User> target = ArgumentCaptor.forClass(User.class);
        verify(administratorPort).updateUserStatus(any(), target.capture(), eq(UserStatus.BLOCKED));
        assertInstanceOf(Seller.class, target.getValue());
        assertEquals("S-1", target.getValue().getIdentifier());
    }

    @Test
    void consultUserNeedsTheRole() throws Exception {
        String token = bearer(administrator());
        when(administratorPort.consultUser(any(), any())).thenReturn(buyer());

        mockMvc.perform(get("/api/admin/users/B-1").param("role", "BUYER").header(authorization(), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("B-1"));
        mockMvc.perform(get("/api/admin/users/B-1").header(authorization(), token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/users/B-1").param("role", "KING").header(authorization(), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown role 'KING'"));
    }

    @Test
    void processRefundReturnsTheRefund() throws Exception {
        when(administratorPort.processRefund(any(), any()))
                .thenReturn(new Refund("REF-1", null, Money.of(new BigDecimal("99900.50"), Currency.COP)));

        mockMvc.perform(post("/api/admin/refunds/REF-1/process").header(authorization(), bearer(administrator())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("REF-1"))
                .andExpect(jsonPath("$.amount.amount").value(99900.50))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void withoutTokenAnswers401() throws Exception {
        mockMvc.perform(post("/api/admin/sellers").contentType(MediaType.APPLICATION_JSON).content(STAFF))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(administratorPort);
    }

    @Test
    void anotherRoleAnswers403() throws Exception {
        mockMvc.perform(post("/api/admin/sellers").header(authorization(), bearer(supervisor()))
                        .contentType(MediaType.APPLICATION_JSON).content(STAFF))
                .andExpect(status().isForbidden());
        verifyNoInteractions(administratorPort);
    }

    @Test
    void shortStaffPasswordAnswers400WithoutEchoingIt() throws Exception {
        mockMvc.perform(post("/api/admin/supervisors").header(authorization(), bearer(administrator()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STAFF.replace("Strong-Pass-1", "abc1234")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(content().string(not(containsString("abc1234"))));
        verifyNoInteractions(administratorPort);
    }

    @Test
    void duplicateUserAnswers409() throws Exception {
        when(administratorPort.registerLogisticsOperator(any(), any(), any()))
                .thenThrow(new DuplicateResourceException("A user with email new.seller@example.com already exists"));

        mockMvc.perform(post("/api/admin/logistics-operators").header(authorization(), bearer(administrator()))
                        .contentType(MediaType.APPLICATION_JSON).content(STAFF))
                .andExpect(status().isConflict());
    }
}
