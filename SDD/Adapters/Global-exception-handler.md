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

Every business rule has its own exception in `domain/exceptions`, all extending `DomainException`; there is no generic "business rule violated" exception. The handler groups them by status:

| Exception | Status | Message |
| --------- | ------ | ------- |
| `MethodArgumentNotValidException` (`@Valid` body) | 400 | `"Validation failed"` + `fieldErrors` |
| `HttpMessageNotReadableException` | 400 | `"Malformed or unreadable request body"` |
| `InvalidRequestException` (unknown code in a request) | 400 | e.g. `"Unknown role 'KING'"` |
| `InvalidQuantityException` (cart line, order line, inventory or cart quantity out of range), `InvalidProductPriceException`, `InvalidMoneyException`, `InvalidPasswordException` (new password too short), `IncorrectCurrentPasswordException`, `OrderStatusRequiresShipmentException` | 400 | Domain message |
| Other `DomainException` (present or future) | 400 | Domain message |
| `IllegalArgumentException` (programming guard of an entity, e.g. a `null` status) | 400 | Entity message |
| `InvalidCredentialsException` | 401 | Always `"Invalid email or password"` |
| No, invalid or revoked token on a protected route (security chain) | 401 | `"Authentication required"` |
| `AccessDeniedException` (`@PreAuthorize`) / wrong role (security chain) | 403 | `"Access denied"` |
| `OperationNotAllowedException`, `SellerNotAuthorizedException`, `BuyerNotEnabledException` | 403 | Domain message |
| `EntityNotFoundException` | 404 | Domain message |
| `DuplicateResourceException`, `InsufficientStockException`, `InvalidOrderStatusTransitionException`, `InvalidShipmentStatusTransitionException`, `EmptyCartException`, `ProductNotPurchasableException`, `CurrencyMismatchException`, `OrderNotShippableException`, `EmptyOrderException`, `ReturnNotEligibleException`, `ReturnAlreadyDecidedException`, `RefundAlreadyProcessedException`, `InactiveWarehouseException`, `InventoryNotTrackedException` | 409 | Domain message |
| Spring MVC errors (unknown route `404`, wrong method `405`, missing parameter `400`, …) | Their own status | Spring's detail message |
| Any other exception | 500 | `"Unexpected error"` |

* `400` (value rules) means a value sent by the client breaks a rule whatever the state of the data: a quantity, price or amount out of range, a new password that is too short, a wrong current password, or an order status that only the shipment lifecycle may set.
* `403` means the actor may not perform the operation: the domain authorization services rejected them, or their current state does not allow it (a suspended seller, a buyer not enabled to purchase).
* `409 Conflict` means the request is valid but the current state of the data does not allow it (duplicate, not enough stock, wrong status for the transition, empty cart or order, product not purchasable, mixed currencies, inactive warehouse, return or refund already decided).
* **`IllegalArgumentException` is only for programming guards.** Rules that a client can break have their own `DomainException`; the `IllegalArgumentException`s left in the entities protect against values no business rule covers (e.g. a `null` argument) and still answer `400` with their message.

---

# Logging and Secrets

* Only unexpected errors (`500`) are logged, with the request id, method and path, and the stack trace. Request bodies are never logged.
* Failed logins never reveal which credential was wrong.
* Passwords and tokens never appear in an error body nor in a log line produced by the handler: validation errors omit rejected values, and the `401` login message is fixed.
