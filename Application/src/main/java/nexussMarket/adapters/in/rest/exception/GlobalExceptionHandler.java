package nexussMarket.adapters.in.rest.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import nexussMarket.adapters.in.rest.responses.ErrorResponse;
import nexussMarket.adapters.in.rest.responses.ErrorResponse.FieldViolation;
import nexussMarket.domain.exceptions.BusinessRuleViolationException;
import nexussMarket.domain.exceptions.DomainException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InsufficientStockException;
import nexussMarket.domain.exceptions.InvalidCredentialsException;
import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.exceptions.InvalidShipmentStatusTransitionException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.exceptions.SellerNotAuthorizedException;

/**
 * Translates exceptions into HTTP error responses with the uniform
 * {@link ErrorResponse} body (see {@link RestErrors}).
 *
 * <p>Domain exceptions carry business messages meant for the client. Failed
 * logins always get the same generic message, so the response never reveals
 * whether the email or the password was wrong. Unexpected errors are logged
 * with their request id and answered with a generic message, so internal
 * details never leak. Request bodies, passwords and tokens are never logged
 * nor echoed back.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Same message for an unknown email and a wrong password. */
    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex,
                                                                  HttpServletRequest request) {
        return RestErrors.response(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE, request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return RestErrors.response(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** The request is valid, but the current state of the data does not allow it. */
    @ExceptionHandler({DuplicateResourceException.class, InsufficientStockException.class,
            InvalidOrderStatusTransitionException.class, InvalidShipmentStatusTransitionException.class,
            BusinessRuleViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(DomainException ex, HttpServletRequest request) {
        return RestErrors.response(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** The domain authorization services rejected the authenticated actor. */
    @ExceptionHandler({SellerNotAuthorizedException.class, OperationNotAllowedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(DomainException ex, HttpServletRequest request) {
        return RestErrors.response(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    /** Any other domain exception, present or future (e.g. an invalid {@code Money}). */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex, HttpServletRequest request) {
        return RestErrors.response(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** A request value the REST mappers cannot translate, such as an unknown status code. */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return RestErrors.response(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Invalid values rejected by the entities themselves, e.g. a quantity that is not greater than zero. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        return RestErrors.response(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Role checks declared with {@code @PreAuthorize} on the controllers. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return RestErrors.response(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    /** Jakarta Validation failures on a {@code @Valid} request body. Rejected values are never echoed back. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<FieldViolation> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return RestErrors.response(HttpStatus.BAD_REQUEST, "Validation failed", request,
                RestErrors.requestId(request), fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                              HttpServletRequest request) {
        return RestErrors.response(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Spring MVC's own exceptions (unknown route, wrong method, missing parameter...) keep their status.
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            String detail = springError.getBody().getDetail();
            return RestErrors.response(springError.getStatusCode(), detail != null ? detail : ex.getMessage(),
                    request);
        }
        String requestId = RestErrors.requestId(request);
        log.error("Unhandled error [requestId={}] on {} {}", requestId, request.getMethod(), request.getRequestURI(),
                ex);
        return RestErrors.response(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, requestId,
                List.of());
    }
}
