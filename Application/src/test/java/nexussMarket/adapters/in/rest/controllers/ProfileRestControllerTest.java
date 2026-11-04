package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.valueobjects.UserStatus;

/** {@code PUT /api/me} for every role, and {@code DELETE /api/buyer/cart/items}. */
class ProfileRestControllerTest extends RestControllerTestSupport {

    @Test
    void aBuyerUpdatesNameAndAddressesAndSentEmailOrRoleAreIgnored() throws Exception {
        Buyer updated = buyer();
        updated.setFullName("Ana Renamed");
        when(buyerPort.updateProfile(any(), argThat(changes -> changes instanceof Buyer buyer
                && "Ana Renamed".equals(buyer.getFullName()) && buyer.getEmail() == null
                && "Calle 99".equals(buyer.getPrimaryAddress())
                && List.of("Office").equals(buyer.getAdditionalAddresses())))).thenReturn(updated);

        mockMvc.perform(put("/api/me").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ana Renamed\",\"primaryAddress\":\"Calle 99\","
                                + "\"additionalAddresses\":[\"Office\"],\"email\":\"hacker@evil.test\","
                                + "\"role\":\"ADMINISTRATOR\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Renamed"))
                .andExpect(jsonPath("$.email").value("buyer@example.com"))
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    void eachRoleReachesItsOwnPort() throws Exception {
        Supervisor renamed = new Supervisor("V-1", "Val Renamed", "supervisor@example.com", UserStatus.ACTIVE);
        when(supervisorPort.updateProfile(any(), any())).thenReturn(renamed);

        mockMvc.perform(put("/api/me").header(authorization(), bearer(supervisor()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Val Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Val Renamed"));
        verify(buyerPort, never()).updateProfile(any(), any());
    }

    @Test
    void onlyBuyersSendAddressesAndTheNameIsRequired() throws Exception {
        mockMvc.perform(put("/api/me").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Sam\",\"primaryAddress\":\"Calle 1\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/me").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/me").contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Anon\"}"))
                .andExpect(status().isUnauthorized());
        verify(sellerPort, never()).updateProfile(any(), any());
        verify(buyerPort, never()).updateProfile(any(), any());
    }

    @Test
    void theBuyerEmptiesTheActiveCartAndOtherRolesCannot() throws Exception {
        when(buyerPort.clearActiveCart(any())).thenReturn(new ShoppingCart("C-1", buyer()));

        mockMvc.perform(delete("/api/buyer/cart/items").header(authorization(), bearer(buyer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines").isEmpty());
        mockMvc.perform(delete("/api/buyer/cart/items").header(authorization(), bearer(seller())))
                .andExpect(status().isForbidden());
    }
}
