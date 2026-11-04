package nexussMarket.infrastructure.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import nexussMarket.adapters.in.rest.exception.RequestBodyTooLargeException;
import nexussMarket.adapters.in.rest.exception.RestErrors;
import nexussMarket.domain.valueobjects.EmailAddress;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Throttles {@code POST /api/auth/login} with a {@link LoginAttemptLimiter}:
 * every {@code 401} counts as a failure for the client address and for the
 * attempted email, and a blocked address or email gets {@code 429} with a
 * {@code Retry-After} header (seconds) before the credentials are even
 * checked. A successful login clears the email's count. The response is the
 * same whether the email exists or not.
 *
 * <p>The client address is the connection's remote address;
 * {@code X-Forwarded-For} is not trusted, since any client can set it.</p>
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

    static final String TOO_MANY_ATTEMPTS = "Too many failed login attempts; try again later";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final LoginAttemptLimiter limiter;

    public LoginThrottleFilter(LoginAttemptLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && SecurityConfig.LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] body;
        try {
            body = request.getInputStream().readAllBytes();
        } catch (RequestBodyTooLargeException e) {
            RestErrors.write(response, HttpStatus.CONTENT_TOO_LARGE, e.getMessage(), request);
            return;
        }
        String emailKey = emailOf(body).map(email -> "email:" + email).orElse(null);
        List<String> keys = new ArrayList<>();
        keys.add("ip:" + request.getRemoteAddr());
        if (emailKey != null) {
            keys.add(emailKey);
        }
        Optional<Duration> blocked = limiter.blockedFor(keys);
        if (blocked.isPresent()) {
            long seconds = Math.max(1, (blocked.get().toMillis() + 999) / 1000);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
            RestErrors.write(response, HttpStatus.TOO_MANY_REQUESTS, TOO_MANY_ATTEMPTS, request);
            return;
        }
        chain.doFilter(new CachedBodyRequest(request, body), response);
        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            limiter.recordFailure(keys);
        } else if (response.getStatus() == HttpStatus.OK.value() && emailKey != null) {
            limiter.recordSuccess(emailKey);
        }
    }

    /** The normalized {@code email} of a JSON login body; empty when the body is not such an object. */
    private static Optional<String> emailOf(byte[] body) {
        try {
            JsonNode email = JSON.readTree(body).get("email");
            if (email != null && email.isString() && !email.stringValue().isBlank()) {
                return Optional.of(EmailAddress.normalize(email.stringValue()));
            }
        } catch (RuntimeException malformed) {
            // A malformed body is rejected later by the controller; it is only counted by address.
        }
        return Optional.empty();
    }

    /** The request with its body already read, so the controller can read it again. */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    return input.read(buffer, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Asynchronous reads are not supported");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
