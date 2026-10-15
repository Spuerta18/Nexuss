package nexussMarket.adapters.in.rest.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import nexussMarket.domain.exceptions.InvalidCredentialsException;
import nexussMarket.domain.models.AuthenticationResult;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.valueobjects.UserStatus;

class AuthRestControllerTest extends RestControllerTestSupport {

    private static final String SECRET_PASSWORD = "Sup3r-Secret-Pass";

    @Test
    void loginReturnsTheTokenAndTheUserWithoutPasswordHash() throws Exception {
        Buyer buyer = buyer();
        buyer.setPasswordHash("$2a$10$hash");
        when(publicAccessPort.authenticate("buyer@example.com", SECRET_PASSWORD))
                .thenReturn(new AuthenticationResult(buyer, "signed.jwt.token"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("buyer@example.com", SECRET_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("BUYER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$10$hash"))));
    }

    @Test
    void failedLoginAnswersTheSameGeneric401() throws Exception {
        when(publicAccessPort.authenticate(any(), any()))
                .thenThrow(new InvalidCredentialsException("No user with email unknown@example.com"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("unknown@example.com", SECRET_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(content().string(not(containsString(SECRET_PASSWORD))))
                .andExpect(content().string(not(containsString("unknown@example.com"))));
    }

    @Test
    void invalidLoginBodyAnswers400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("not-an-email", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
    }

    @Test
    void registerBuyerAnswers201() throws Exception {
        when(publicAccessPort.registerBuyer(any(), eq(SECRET_PASSWORD))).thenReturn(buyer());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(register(SECRET_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value("B-1"))
                .andExpect(jsonPath("$.role").value("BUYER"))
                .andExpect(jsonPath("$.commercialStatus").value("ENABLED"));
    }

    @Test
    void shortPasswordAnswers400WithoutEchoingIt() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(register("short7!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(content().string(not(containsString("short7!"))));
    }

    @Test
    void meReturnsTheAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/me").header(authorization(), bearer(seller())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("S-1"))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.sellerStatus").value("ACTIVE"));
    }

    @Test
    void meWithoutTokenAnswers401WithTheUniformBody() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/me"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void tokenWhoseRoleNoLongerMatchesTheStoredUserIsRejected() throws Exception {
        // The token says BUYER, but the stored user with that identifier is now a SELLER.
        String token = rawBearer(buyer());
        when(userRepositoryPort.findById("B-1")).thenReturn(Optional.of(
                new Seller("B-1", "Ana Buyer", "buyer@example.com", UserStatus.ACTIVE, null)));

        mockMvc.perform(get("/api/me").header(authorization(), token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blockedUserIsRejectedEvenWithAValidToken() throws Exception {
        Buyer blocked = new Buyer("B-1", "Ana Buyer", "buyer@example.com", UserStatus.BLOCKED, null);
        String token = bearer(blocked);

        mockMvc.perform(get("/api/me").header(authorization(), token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightAllowsAConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsPreflightRejectsAnUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/me")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    private static String login(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String register(String password) {
        return "{\"identifier\":\"B-1\",\"fullName\":\"Ana Buyer\",\"email\":\"buyer@example.com\","
                + "\"primaryAddress\":\"1 Main Street\",\"password\":\"" + password + "\"}";
    }
}
