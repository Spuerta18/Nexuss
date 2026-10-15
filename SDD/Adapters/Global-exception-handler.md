# Global Exception Handler — NexusMarket

## Introduction

`GlobalExceptionHandler` (`adapters/in/rest/exception`, a `@RestControllerAdvice`) translates every exception thrown while handling a request into an HTTP status and the uniform `ErrorResponse` body. The security filter chain answers `401` and `403` with the same body through `RestErrors`, so a client always receives one error format.

---

# Error Body

```json
{
  "timestamp": "2026-10-15T15:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No order found with id O-9",
  "path": "/api/buyer/orders/O-9",
  "requestId": "3f0c…",
  "fieldErrors": []
}
```

| Field | Content |
| ----- | ------- |
| `timestamp` | ISO-8601 instant of the error. |
| `status` / `error` | HTTP status code and reason phrase. |
| `message` | Safe, human-readable explanation. Domain messages are shown as they are; unexpected errors get a generic message. |
| `path` | Request path, without the query string. |
| `requestId` | Taken from the `X-Request-Id` request header, or generated; also returned in the `X-Request-Id` response header. |
| `fieldErrors` | `{ field, message }` per invalid field; empty unless validation failed. Rejected values are never included. |

---

# Mapping

| Exception | Status | Message |
| --------- | ------ | ------- |
| `MethodArgumentNotValidException` (`@Valid` body) | 400 | `"Validation failed"` + `fieldErrors` |
| `HttpMessageNotReadableException` | 400 | `"Malformed or unreadable request body"` |
| `InvalidRequestException` (unknown code in a request) | 400 | e.g. `"Unknown role 'KING'"` |
| `IllegalArgumentException` (value rejected by an entity, e.g. a quantity) | 400 | Entity message |
| Other `DomainException` (e.g. an invalid `Money`) | 400 | Domain message |
| `InvalidCredentialsException` | 401 | Always `"Invalid email or password"` |
| No or invalid token on a protected route (security chain) | 401 | `"Authentication required"` |
| `AccessDeniedException` (`@PreAuthorize`) / wrong role (security chain) | 403 | `"Access denied"` |
| `OperationNotAllowedException`, `SellerNotAuthorizedException` | 403 | Domain message |
| `EntityNotFoundException` | 404 | Domain message |
| `DuplicateResourceException`, `InsufficientStockException`, `InvalidOrderStatusTransitionException`, `InvalidShipmentStatusTransitionException`, `BusinessRuleViolationException` | 409 | Domain message |
| Spring MVC errors (unknown route `404`, wrong method `405`, missing parameter `400`, …) | Their own status | Spring's detail message |
| Any other exception | 500 | `"Unexpected error"` |

`409 Conflict` means the request is valid but the current state of the data does not allow it (duplicate, not enough stock, wrong status for the transition).

---

# Logging and Secrets

* Only unexpected errors (`500`) are logged, with the request id, method and path, and the stack trace. Request bodies are never logged.
* Failed logins never reveal which credential was wrong.
* Passwords and tokens never appear in an error body nor in a log line produced by the handler: validation errors omit rejected values, and the `401` login message is fixed.
