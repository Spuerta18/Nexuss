package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;

/**
 * {@code POST /api/<role>/logout} and {@code POST /api/<role>/password} for
 * each of the five roles, and revocation of the previous token.
 */
class SessionRestControllerTest extends RestControllerTestSupport {

    private static final String CHANGE_PASSWORD = """
            {"currentPassword": "Current-Pass-1", "newPassword": "New-Pass-123"}""";

    static Stream<Arguments> roles() {
        return Stream.of(
                Arguments.of("buyer", (Supplier<User>) RestControllerTestSupport::buyer),
                Arguments.of("seller", (Supplier<User>) RestControllerTestSupport::seller),
                Arguments.of("admin", (Supplier<User>) RestControllerTestSupport::administrator),
                Arguments.of("logistics", (Supplier<User>) RestControllerTestSupport::logisticsOperator),
                Arguments.of("supervisor", (Supplier<User>) RestControllerTestSupport::supervisor));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roles")
    void logoutAnswers204AndDelegatesToTheRolePort(String path, Supplier<User> userSupplier) throws Exception {
        User user = userSupplier.get();

        mockMvc.perform(post("/api/" + path + "/logout").header(authorization(), bearer(user)))
                .andExpect(status().isNoContent());

        switch (path) {
            case "buyer" -> verify(buyerPort).logout(user);
            case "seller" -> verify(sellerPort).logout(user);
            case "admin" -> verify(administratorPort).logout(user);
            case "logistics" -> verify(logisticsOperatorPort).logout(user);
            default -> verify(supervisorPort).logout(user);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roles")
    void passwordChangeAnswers204AndDelegatesToTheRolePort(String path, Supplier<User> userSupplier)
            throws Exception {
        User user = userSupplier.get();

        mockMvc.perform(post("/api/" + path + "/password").header(authorization(), bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(CHANGE_PASSWORD))
                .andExpect(status().isNoContent());

        switch (path) {
            case "buyer" -> verify(buyerPort).changePassword(user, "Current-Pass-1", "New-Pass-123");
            case "seller" -> verify(sellerPort).changePassword(user, "Current-Pass-1", "New-Pass-123");
            case "admin" -> verify(administratorPort).changePassword(user, "Current-Pass-1", "New-Pass-123");
            case "logistics" -> verify(logisticsOperatorPort).changePassword(user, "Current-Pass-1", "New-Pass-123");
            default -> verify(supervisorPort).changePassword(user, "Current-Pass-1", "New-Pass-123");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roles")
    void shortNewPasswordIsRejectedWithoutEchoingIt(String path, Supplier<User> userSupplier) throws Exception {
        mockMvc.perform(post("/api/" + path + "/password").header(authorization(), bearer(userSupplier.get()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"Current-Pass-1\", \"newPassword\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));
    }

    @Test
    void sessionEndpointsNeedATokenOfTheirRole() throws Exception {
        mockMvc.perform(post("/api/seller/logout")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/seller/logout").header(authorization(), bearer(buyer())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/password").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON).content(CHANGE_PASSWORD))
                .andExpect(status().isForbidden());
        verifyNoInteractions(sellerPort, administratorPort);
    }

    @Test
    void theTokenUsedToLogOutIsRejectedAfterwards() throws Exception {
        Buyer buyer = buyer();
        String token = bearer(buyer);
        // The real LogoutService raises the stored user's version; the stored user here is `buyer` itself.
        doAnswer(invocation -> {
            buyer.bumpAuthTokenVersion();
            return null;
        }).when(buyerPort).logout(any());

        mockMvc.perform(get("/api/me").header(authorization(), token)).andExpect(status().isOk());
        mockMvc.perform(post("/api/buyer/logout").header(authorization(), token)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").header(authorization(), token)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/buyer/logout").header(authorization(), token)).andExpect(status().isUnauthorized());
    }

    @Test
    void aPasswordChangeRevokesThePreviousTokenButANewOneWorks() throws Exception {
        Seller seller = seller();
        String token = bearer(seller);
        doAnswer(invocation -> {
            seller.bumpAuthTokenVersion();
            return null;
        }).when(sellerPort).changePassword(any(), anyString(), anyString());

        mockMvc.perform(post("/api/seller/password").header(authorization(), token)
                        .contentType(MediaType.APPLICATION_JSON).content(CHANGE_PASSWORD))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").header(authorization(), token)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me").header(authorization(), bearer(seller))).andExpect(status().isOk());
    }
}
