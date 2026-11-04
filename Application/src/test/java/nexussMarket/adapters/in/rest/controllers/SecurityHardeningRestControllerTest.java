package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.InvalidCredentialsException;

/**
 * The REST side of the hardening of {@code stress-tests/REPORT.md}: login
 * throttling (F4), duplicate keys as {@code 409} (F5), the body size limit
 * (F6), authorization before validation (F7), and concurrency conflicts as
 * {@code 409}.
 */
class SecurityHardeningRestControllerTest extends RestControllerTestSupport {

    private static final String PASSWORD = "Wrong-Pass-123";

    @Test
    void tenFailedLoginsBlockTheEleventhWith429AndRetryAfter() throws Exception {
        when(publicAccessPort.authenticate(any(), any())).thenThrow(new InvalidCredentialsException("bad"));
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(login("nobody@example.com")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("nobody@example.com")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.message").value("Too many failed login attempts; try again later"));
    }

    @Test
    void aBlockedClientGetsTheSameAnswerForExistingAndUnknownEmails() throws Exception {
        when(publicAccessPort.authenticate(any(), any())).thenThrow(new InvalidCredentialsException("bad"));
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(login("user" + i + "@example.com")));
        }

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("buyer@example.com")))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("never-registered@example.com")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void aBodyLargerThanOneMegabyteGets413() throws Exception {
        String huge = "{\"identifier\":\"B-9\",\"fullName\":\"" + "x".repeat(1_100_000) + "\"}";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(huge))
                .andExpect(status().is(413));
        verify(publicAccessPort, never()).registerBuyer(any(), any());
    }

    @Test
    void anUnmappedDuplicateKeyIs409NotA500() throws Exception {
        when(publicAccessPort.registerBuyer(any(), any())).thenThrow(new DuplicateKeyException("E11000 users.email"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(register()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The resource already exists"));
    }

    @Test
    void concurrencyConflictsAre409() throws Exception {
        when(publicAccessPort.registerBuyer(any(), any()))
                .thenThrow(new ConcurrentUpdateException("User B-1 was changed by another request"))
                .thenThrow(new OptimisticLockingFailureException("stale"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(register()))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(register()))
                .andExpect(status().isConflict());
    }

    @Test
    void aWrongRoleGets403EvenWithAnInvalidBody() throws Exception {
        mockMvc.perform(post("/api/buyer/cart/items").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/sellers").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void routesOutsideTheRolePrefixesAreDenied() throws Exception {
        mockMvc.perform(get("/api/unlisted").header(authorization(), bearer(administrator())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/unlisted"))
                .andExpect(status().isUnauthorized());
    }

    private static String login(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
    }

    private static String register() {
        return "{\"identifier\":\"B-1\",\"fullName\":\"Ana Buyer\",\"email\":\"buyer@example.com\","
                + "\"primaryAddress\":\"1 Main Street\",\"password\":\"" + PASSWORD + "\"}";
    }
}
