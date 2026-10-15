# Layer — Security Adapters

## Purpose

`nexussMarket.infrastructure.security` authenticates requests with JWT, enforces roles together with `@PreAuthorize`, configures CORS, and implements the two security output ports: password hashing and token issuing.

## Diagram

```mermaid
flowchart TB
    req(["HTTP request"]) --> cors["CORS<br/>CorsConfigurationSource (CorsProperties)"]
    cors --> jwtf["JwtAuthenticationFilter"]
    jwtf -->|"validate(token)"| prov["JwtProvider<br/>HMAC-SHA, claims sub, role, email"]
    jwtf -->|"loadUserById(sub)"| uds["NexusUserDetailsService"]
    uds -->|"findById"| urp[["UserRepositoryPort"]]
    jwtf -->|"ACTIVE and role matches"| principal["AuthenticatedUserPrincipal<br/>authority ROLE_ + role code"]
    jwtf -->|"record(userId, METHOD path)"| audit["AuditOperationService"]
    principal --> chain{"SecurityConfig<br/>authorizeHttpRequests"}
    chain -->|"public: POST /api/auth/login, /api/auth/register, /error"| ctrl["Controllers"]
    chain -->|"authenticated"| ctrl
    chain -->|"no valid token"| e401["401 ErrorResponse<br/>(RestErrors.write)"]
    ctrl -->|"@PreAuthorize hasRole fails"| e403["403 ErrorResponse"]

    subgraph ports["Security output ports"]
        bcrypt["BCryptPasswordHasherAdapter<br/>implements PasswordHasherPort"]
        jwtadapter["JwtTokenServiceAdapter<br/>implements TokenServicePort"]
    end
    jwtadapter --> prov
```

## Notes

| Class | Role |
| ----- | ---- |
| `SecurityConfig` | `@EnableWebSecurity`, `@EnableMethodSecurity`; stateless; CSRF, form login, HTTP Basic and logout disabled; public routes; `401` / `403` written with `RestErrors`; CORS source for `/api/**`. Enables `JwtProperties` and `CorsProperties`. |
| `JwtAuthenticationFilter` | Not a bean on purpose (otherwise it would also run outside the security chain). Validates the token, reloads the user, rejects it if not `ACTIVE` or if its stored role differs from the token, audits the request. |
| `JwtProvider` | `generateToken(User)` and `validate(String)`. The algorithm follows the key length (HS256 / HS384 / HS512). |
| `JwtProperties` | `nexus.security.jwt.secret` (`NEXUS_JWT_SECRET`, required) and `expiration` (`1h`). |
| `CorsProperties` | `nexus.cors.allowed-origins` (`NEXUS_CORS_ORIGINS`). |
| `NexusUserDetailsService` | Loads users through `UserRepositoryPort`. |
| `AuthenticatedUserPrincipal` | `UserDetails` wrapping the domain `User`; controllers read it with `@AuthenticationPrincipal(expression = "user")`. |
| `BCryptPasswordHasherAdapter` | `PasswordHasherPort` with `BCryptPasswordEncoder`. |
| `JwtTokenServiceAdapter` | `TokenServicePort`, used by `AuthenticateUserService`. |

* **Fresh state over token state.** The token carries enough to rebuild the user, but the filter reloads it so that blocking a user, or changing its role, takes effect at the next request. See [REST Security and CORS](../Adapters/Rest-security-cors.md).
* **Login.** A wrong email, a wrong password and a user who is not `ACTIVE` all get `401` `"Invalid email or password"` (`AuthenticateUserService`).
* **No secrets in logs.** The filter never logs tokens; the audit records only method and path.
