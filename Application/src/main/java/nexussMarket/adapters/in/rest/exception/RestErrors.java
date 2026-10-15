package nexussMarket.adapters.in.rest.exception;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nexussMarket.adapters.in.rest.responses.ErrorResponse;
import nexussMarket.adapters.in.rest.responses.ErrorResponse.FieldViolation;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds the uniform {@link ErrorResponse}, both for
 * {@link GlobalExceptionHandler} and for the security filter chain, which
 * answers {@code 401} / {@code 403} before any controller runs.
 *
 * <p>The request id comes from the {@code X-Request-Id} header, or is
 * generated when missing, and is echoed back in that header.</p>
 */
public final class RestErrors {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private RestErrors() {
    }

    public static ResponseEntity<ErrorResponse> response(HttpStatusCode status, String message,
                                                         HttpServletRequest request) {
        return response(status, message, request, requestId(request), List.of());
    }

    public static ResponseEntity<ErrorResponse> response(HttpStatusCode status, String message,
                                                         HttpServletRequest request, String requestId,
                                                         List<FieldViolation> fieldErrors) {
        return ResponseEntity.status(status)
                .header(REQUEST_ID_HEADER, requestId)
                .body(body(status, message, request, requestId, fieldErrors));
    }

    /** Writes the error directly to the servlet response; used where no controller is involved. */
    public static void write(HttpServletResponse response, HttpStatus status, String message,
                             HttpServletRequest request) throws IOException {
        String requestId = requestId(request);
        response.setStatus(status.value());
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.writeValueAsString(body(status, message, request, requestId, List.of())));
    }

    public static String requestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    private static ErrorResponse body(HttpStatusCode status, String message, HttpServletRequest request,
                                      String requestId, List<FieldViolation> fieldErrors) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String error = resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
        return new ErrorResponse(Instant.now().toString(), status.value(), error, message,
                request.getRequestURI(), requestId, fieldErrors);
    }
}
