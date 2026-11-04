package nexussMarket.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;
import nexussMarket.adapters.in.rest.exception.RequestBodyTooLargeException;

/** Unit tests of {@link LoginAttemptLimiter} and of the streaming path of {@link RequestBodyLimitFilter}. */
class RequestLimitsTest {

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter =
            new LoginAttemptLimiter(10, Duration.ofMinutes(5), Duration.ofMinutes(1), clock);

    @Test
    void tenFailuresWithinTheWindowBlockForOneMinute() {
        List<String> keys = List.of("ip:10.0.0.1", "email:ana@example.com");
        for (int i = 0; i < 9; i++) {
            limiter.recordFailure(keys);
        }
        assertTrue(limiter.blockedFor(keys).isEmpty(), "nine failures do not block");

        limiter.recordFailure(keys);
        Optional<Duration> blocked = limiter.blockedFor(List.of("email:ana@example.com"));
        assertEquals(Duration.ofMinutes(1), blocked.orElseThrow());
        assertTrue(limiter.blockedFor(List.of("ip:10.0.0.1")).isPresent(), "the address is blocked too");

        clock.advance(Duration.ofSeconds(61));
        assertTrue(limiter.blockedFor(keys).isEmpty(), "the block ends after one minute");
    }

    @Test
    void failuresOlderThanTheWindowAreForgotten() {
        List<String> keys = List.of("email:ana@example.com");
        for (int i = 0; i < 9; i++) {
            limiter.recordFailure(keys);
        }
        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        limiter.recordFailure(keys);

        assertTrue(limiter.blockedFor(keys).isEmpty());
    }

    @Test
    void aSuccessfulLoginClearsTheEmailCount() {
        List<String> keys = List.of("email:ana@example.com");
        for (int i = 0; i < 9; i++) {
            limiter.recordFailure(keys);
        }
        limiter.recordSuccess("email:ana@example.com");
        limiter.recordFailure(keys);

        assertTrue(limiter.blockedFor(keys).isEmpty());
    }

    @Test
    void aBodyWithoutContentLengthIsCutAtTheLimit() throws Exception {
        RequestBodyLimitFilter filter = new RequestBodyLimitFilter(16);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }

            @Override
            public int getContentLength() {
                return -1;
            }
        };
        request.setContent(new byte[64]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] reachedTheLimit = {false};

        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest wrapped, jakarta.servlet.ServletResponse ignored) {
                assertThrows(RequestBodyTooLargeException.class,
                        () -> ((HttpServletRequest) wrapped).getInputStream().readAllBytes());
                reachedTheLimit[0] = true;
            }
        });

        assertTrue(reachedTheLimit[0]);
    }

    @Test
    void aDeclaredLengthAboveTheLimitIsRejectedBeforeReading() throws Exception {
        RequestBodyLimitFilter filter = new RequestBodyLimitFilter(16);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.setContent(new byte[64]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(413, response.getStatus());
        assertFalse(chain.getRequest() != null, "the request never reaches the application");
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-11-04T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
