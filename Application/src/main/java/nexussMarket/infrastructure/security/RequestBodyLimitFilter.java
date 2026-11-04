package nexussMarket.infrastructure.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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

/**
 * Rejects request bodies larger than {@code nexus.http.max-request-body-size}
 * (1 MB by default) with {@code 413}. A declared {@code Content-Length} above
 * the limit is rejected before anything is read; a body without one is
 * counted while it is read and fails with {@link RequestBodyTooLargeException},
 * which {@code GlobalExceptionHandler} also answers with {@code 413}.
 */
public class RequestBodyLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;

    public RequestBodyLimitFilter(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maxBytes) {
            RestErrors.write(response, HttpStatus.CONTENT_TOO_LARGE,
                    "Request body is larger than " + maxBytes + " bytes", request);
            return;
        }
        chain.doFilter(new LimitedBodyRequest(request, maxBytes), response);
    }

    /** Fails the read as soon as more than {@code maxBytes} bytes have been consumed. */
    private static final class LimitedBodyRequest extends HttpServletRequestWrapper {

        private final long maxBytes;
        private ServletInputStream limited;

        LimitedBodyRequest(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (limited == null) {
                limited = new LimitedInputStream(super.getInputStream(), maxBytes);
            }
            return limited;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8));
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long maxBytes;
        private long consumed;

        LimitedInputStream(ServletInputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = delegate.read(buffer, offset, length);
            if (read > 0) {
                count(read);
            }
            return read;
        }

        private void count(int bytes) throws RequestBodyTooLargeException {
            consumed += bytes;
            if (consumed > maxBytes) {
                throw new RequestBodyTooLargeException(maxBytes);
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }
    }
}
