# REST Security and CORS — NexusMarket

## Introduction

The API is protected with stateless JWT authentication (`infrastructure/security`) and role-based authorization on the controllers. There is no HTTP session, no form login, no HTTP Basic and no CSRF protection: the token travels in the `Authorization` header, not in a cookie, so CSRF does not apply.

| Piece | Responsibility |
| ----- | -------------- |
| `SecurityConfig` | Filter chain, public routes, `401` / `403` handlers, CORS configuration. |
| `JwtProvider` | Issues and validates HMAC-signed tokens. |
| `JwtAuthenticationFilter` | Validates the token, reloads the user and puts it in the security context. |
| `NexusUserDetailsService` | Loads users through `UserRepositoryPort`. |
| `AuthenticatedUserPrincipal` | Wraps the domain `User`; authority `ROLE_<role code>`. |
| `JwtTokenServiceAdapter` | Implements `TokenServicePort` for `AuthenticateUserService`. |

---

# Login and Token

`POST /api/auth/login` calls `PublicAccessPort.authenticate`. On success it returns `AuthResponse` with the token (`tokenType: "Bearer"`) and the user. A wrong email, a wrong password and a correct password for a user who is not `ACTIVE` (e.g. `BLOCKED`) all answer `401` with the same message, `"Invalid email or password"`: a failed login never reveals whether the account exists, whether the password was right, or whether the account is blocked. The rule lives in `AuthenticateUserService`, which throws the same `InvalidCredentialsException` in every case.

The token is signed with HMAC-SHA using `NEXUS_JWT_SECRET` (Base64, at least 32 bytes). The algorithm follows the key length (jjwt's `Keys.hmacShaKeyFor`): a 32-byte key gives HS256, a 48-byte key HS384 and a 64-byte key HS512. The token expires after `nexus.security.jwt.expiration` (`1h` by default). Its claims are enough to rebuild the right `User` subtype:

| Claim | Content |
| ----- | ------- |
| `sub` | User identifier. |
| `role` | `SystemRole` code (`BUYER`, `SELLER`, …). |
| `email` | User email. |
| `iat` / `exp` | Issue and expiration timestamps. |

---

# Request Authentication

For every request with `Authorization: Bearer <token>`, `JwtAuthenticationFilter`:

1. Validates the signature and the expiration. An invalid token leaves the request unauthenticated.
2. **Reloads the user from the database** by the `sub` identifier. The loaded user is already the concrete subtype.
3. Rejects the request if the user no longer exists, is not `ACTIVE` (e.g. `BLOCKED`), or **its stored role differs from the `role` claim**.
4. Puts an `AuthenticatedUserPrincipal` in the security context and records the request through `AuditOperationService` as `"<METHOD> <path>"` (without the query string).

A rejected or missing token on a protected route answers `401` with the uniform error body.

## Why the user is reloaded instead of rebuilt from the token

The token carries enough to rebuild a `User`, but the filter prefers **fresh state** from the database:

* **Blocking takes effect immediately.** A token stays valid until it expires. If the user were rebuilt from the token, a blocked or deactivated user would keep full access for up to an hour. Reloading stops them at the next request.
* **Role changes cannot be replayed.** If a user's stored role no longer matches the token, the token is rejected instead of granting the old role.
* **The domain sees the real actor.** The authorization services check the actor's current `status` (`ValidateUserAuthorizationStatusService`) and its type (`instanceof Buyer`, …); some services also read the actor's data. A token snapshot would make those checks run against stale values.
* **Authorities never come from the token alone.** The granted role is the one stored in the database; the token's role is only used to detect a mismatch.

The cost is one lookup by identifier per request.

---

# Authorization

* Each role controller declares `@PreAuthorize("hasRole('<ROLE>')")` at **class level**, so every endpoint of the controller is restricted to its role. Method security is enabled with `@EnableMethodSecurity`.
* An authenticated user with another role gets `403` (`"Access denied"`) before the port is called.
* **Order of checks.** Request body validation (`@Valid`) runs while Spring MVC resolves the controller arguments, before the controller method and its `@PreAuthorize` proxy are invoked. A user with the wrong role who sends an *invalid* body therefore gets `400` instead of `403`. The port is never called in either case; the only thing revealed is the shape of the expected body, which is public in the API documentation.
* The Domain Services still authorize the actor on each operation (ownership, status); a domain refusal also answers `403` (see [Global Exception Handler](Global-exception-handler.md)). The `@PreAuthorize` role check is the first barrier, not the only one.
* Public routes: `POST /api/auth/login`, `POST /api/auth/register` and `/error`. `GET /api/me` needs any valid token.

---

# CORS

Browsers may call `/api/**` from the origins listed in `nexus.cors.allowed-origins`, bound to `NEXUS_CORS_ORIGINS` (comma-separated). The default is `http://localhost:3000,http://localhost:5173`.

| Setting | Value |
| ------- | ----- |
| Allowed methods | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| Allowed headers | `Authorization`, `Content-Type`, `X-Request-Id` |
| Exposed headers | `X-Request-Id` |
| Credentials | Not allowed: the token is sent in a header, not in a cookie |
| Preflight cache | 3600 s |

A preflight from an origin that is not listed answers `403`.

---

# Secrets in Logs and Responses

* Passwords are never logged nor returned: request records mask them in `toString`, validation errors list the field name and message but never the rejected value, and `UserResponse` has no password or hash field.
* Tokens are never logged: the filter does not log them, `AuditOperationService` records only the method and path, and `AuthResponse.toString` masks the token.
* Unexpected errors answer a generic `"Unexpected error"`; details only go to the server log with the request id.
