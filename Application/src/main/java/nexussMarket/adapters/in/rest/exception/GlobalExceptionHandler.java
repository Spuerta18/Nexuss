package nexussMarket.adapters.in.rest.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import nexussMarket.domain.exceptions.BusinessRuleViolationException;
import nexussMarket.domain.exceptions.DomainException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InsufficientStockException;
import nexussMarket.domain.exceptions.InvalidCredentialsException;
import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.exceptions.SellerNotAuthorizedException;

/**
 * Translates exceptions into HTTP error responses. Every error body is a map
 * with {@code timestamp}, {@code status}, {@code message} and
 * {@code requestId}; the request id comes from the {@code X-Request-Id}
 * header, or is generated when missing, and is echoed back in that header.
 *
 * <p>Domain exceptions carry business messages meant for the client.
 * Unexpected errors are logged with their request id and answered with a
 * generic message, so internal details never leak.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex,
                                                                        HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({DuplicateResourceException.class, InsufficientStockException.class,
            InvalidOrderStatusTransitionException.class, BusinessRuleViolationException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(DomainException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({SellerNotAuthorizedException.class, OperationNotAllowedException.class})
    public ResponseEntity<Map<String, Object>> handleForbidden(DomainException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    /** Any other domain exception, present or future. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomain(DomainException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Invalid values rejected by the entities themselves, e.g. a quantity that is not greater than zero. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex,
                                                                     HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Role checks declared with {@code @PreAuthorize} on controllers. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
                                                                  HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Validation failed" : message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                    HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Spring MVC's own exceptions (unknown route, wrong method, missing parameter...) keep their status.
        if (ex instanceof ErrorResponse errorResponse) {
            return error(errorResponse.getStatusCode(), ex.getMessage(), request);
        }
        String requestId = requestId(request);
        log.error("Unhandled error [requestId={}] on {} {}", requestId, request.getMethod(), request.getRequestURI(),
                ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", requestId);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatusCode status, String message,
                                                      HttpServletRequest request) {
        return error(status, message, requestId(request));
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatusCode status, String message, String requestId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        body.put("requestId", requestId);
        return ResponseEntity.status(status).header(REQUEST_ID_HEADER, requestId).body(body);
    }

    private static String requestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }
}
