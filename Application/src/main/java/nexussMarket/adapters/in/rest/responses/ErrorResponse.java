package nexussMarket.adapters.in.rest.responses;

import java.util.List;

/**
 * Uniform body of every error answered by the API, whether it comes from a
 * controller, the validation layer or the security filter chain.
 *
 * <p>{@code fieldErrors} lists the invalid request fields with their message
 * only; the rejected values are never echoed back, so a password or a token
 * can never appear in an error body.</p>
 *
 * @param timestamp   when the error happened (ISO-8601 instant)
 * @param status      HTTP status code
 * @param error       HTTP reason phrase (e.g. {@code "Not Found"})
 * @param message     human-readable explanation, safe to show to the client
 * @param path        request path, without the query string
 * @param requestId   correlation id, also returned in the {@code X-Request-Id} header
 * @param fieldErrors invalid fields; empty unless the request failed validation
 */
public record ErrorResponse(String timestamp, int status, String error, String message, String path,
                            String requestId, List<FieldViolation> fieldErrors) {

    /** One invalid request field. */
    public record FieldViolation(String field, String message) {
    }
}
