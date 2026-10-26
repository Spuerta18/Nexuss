# Users and Authentication — Domain Services — NexusMarket

## Introduction

Authentication, logout and password change, and the registration and status of users.

Part of the [Domain Services](../Domain%20Services.md) catalog, which holds the design conventions, the complete service tables and the collaboration diagram.

---

# Users

---

# AuthenticateUserService

## Description

Verifies a user's credentials and returns the authenticated `User` together with an access token issued for it.

## Parameters

`execute(String email, String password)`

| Parameter | Type   | Description |
| --------- | ------ | ----------- |
| email     | String | Email the user registered with. |
| password  | String | Plain password, checked with `PasswordHasherPort.matches`. It is never stored. |

**Returns:** `AuthenticationResult` (`user`, `token`)

## Flow

1. Find the user by `email`.
2. Check the password against `User.passwordHash`.
3. Check that the user's `status` is `ACTIVE`.
4. If any of the steps above fails, throw `InvalidCredentialsException` with the same message, `"Invalid email or password"`.
5. Issue a token for the user through `TokenServicePort.generateToken` and return it with the user. The domain never knows the token format (JWT, in `infrastructure/security`); the token carries the user's current `authTokenVersion`.
6. Record the login (`USER_LOGIN`).

## Business Rules

* Only users whose `status` is `ACTIVE` can authenticate.
* An unknown email, a wrong password and a user who is not `ACTIVE` all produce the same error, so a failed login never reveals whether an account exists, whether the password was right, or whether the account is blocked.
* No token is issued for a failed login, and a failed login is not audited.

## Audit

Recorded through `RegisterOperationAndAuditService` once the token is issued (a login saves nothing else).

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `USER_LOGIN` | `USER` — the authenticated user (also `performedBy`) | — (none) |

## Exceptions

| Exception | When |
| --------- | ---- |
| `InvalidCredentialsException` | Unknown email, missing password hash, wrong password, or a user who is not `ACTIVE` (always `"Invalid email or password"`). |

---

# LogoutService

## Description

Logs the actor out by revoking every access token issued to them: it raises the stored user's `authTokenVersion`, so the token used for the call and any other earlier token stop being accepted (`JwtAuthenticationFilter` rejects a token whose `ver` claim differs from the stored version). Exposed as `logout(User)` on the five role ports (`BuyerPort`, `SellerPort`, `AdministratorPort`, `LogisticsOperatorPort`, `SupervisorPort`).

## Parameters

`execute(User actor)`

| Parameter | Type | Description |
| --------- | ---- | ----------- |
| actor     | User | The authenticated user logging out. Only its identifier is used. |

**Returns:** nothing

## Flow

1. Load the stored user by the actor's identifier (its version is the one tokens are checked against, not the reference received).
2. Call `User.bumpAuthTokenVersion()` and save the user.
3. Record the logout (`USER_LOGOUT`).

## Business Rules

* Available to every role, and only on the actor's own account.
* Idempotent: logging out again simply revokes again.
* After a logout, every token issued before answers `401`; the user has to log in again.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `USER_LOGOUT` | `USER` — the stored user (also `performedBy`) | — (none) |

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No user exists with the actor's identifier. |

---

# ChangeUserPasswordService

## Description

Changes the actor's own password and revokes every access token issued before, so the user has to log in again. Exposed as `changePassword(User, String currentPassword, String newPassword)` on the five role ports.

## Parameters

`execute(User actor, String currentPassword, String newPassword)`

| Parameter       | Type   | Description |
| --------------- | ------ | ----------- |
| actor           | User   | The authenticated user. Only its identifier is used. |
| currentPassword | String | Plain current password, checked with `PasswordHasherPort.matches`. |
| newPassword     | String | Plain new password, hashed (BCrypt) with `PasswordHasherPort.hash`. |

**Returns:** nothing

## Flow

1. Load the stored user by the actor's identifier.
2. Check that it is `ACTIVE` with `ValidateUserAuthorizationStatusService`.
3. Check `currentPassword` against the stored `passwordHash`.
4. Check that `newPassword` has at least 8 characters.
5. Store the new hash, call `User.bumpAuthTokenVersion()` and save the user.
6. Record the change (`PASSWORD_CHANGE`).

## Business Rules

* Available to every role, and only on the actor's own account.
* The new password must have at least 8 characters (the REST request also caps it at 72, the BCrypt limit).
* Every token issued before the change answers `401` afterwards.
* Neither password nor the hash is ever audited or logged.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `PASSWORD_CHANGE` | `USER` — the stored user (also `performedBy`) | — (none) |

## Exceptions

| Exception | When |
| --------- | ---- |
| `EntityNotFoundException` | No user exists with the actor's identifier. |
| `OperationNotAllowedException` | The stored user is not `ACTIVE`. |
| `IncorrectCurrentPasswordException` | `currentPassword` is missing or does not match the stored hash. |
| `InvalidPasswordException` | `newPassword` is missing or shorter than 8 characters. |

---

# RegisterBuyerService

## Description

Registers a new `Buyer`. Buyers self-register, so there is no actor.

## Parameters

`execute(Buyer buyer, String password)`

| Parameter | Type   | Description |
| --------- | ------ | ----------- |
| buyer     | Buyer  | New model: `identifier` (national ID or business tax ID), `fullName`, `email`, `primaryAddress`. Its status fields are ignored. |
| password  | String | Plain password, hashed with `PasswordHasherPort.hash`. |

**Returns:** `Buyer`

## Business Rules

* `identifier` and `email` must be unique across all users.
* A new buyer starts with `status = ACTIVE` and `commercialStatus = ENABLED`.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `BUYER_REGISTRATION` | `USER` — the new buyer, who is also `performedBy` (self-registration) | `newStatus` |

## Exceptions

| Exception | When |
| --------- | ---- |
| `DuplicateResourceException` | The `identifier` or the `email` is already used by another user. |

---

# RegisterSellerService

## Description

Registers a new `Seller` on behalf of an `Administrator`.

## Parameters

`execute(User actor, Seller seller, String password)`

| Parameter | Type   | Description |
| --------- | ------ | ----------- |
| actor     | User   | Administrator performing the registration. |
| seller    | Seller | New model: `identifier` (business tax ID), `fullName` (legal name), `email`. Its status fields are ignored. |
| password  | String | Plain password, hashed with `PasswordHasherPort.hash`. |

**Returns:** `Seller`

## Business Rules

* Sellers cannot self-register: only an active `ADMINISTRATOR` can register them, checked with `ValidateRoleAuthorizationService` before anything else.
* `identifier` and `email` must be unique across all users.
* A new seller starts with `status = ACTIVE` and `sellerStatus = ACTIVE`.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `SELLER_REGISTRATION` | `USER` — the new seller | `newStatus` |

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is missing, not `ACTIVE`, or not an `ADMINISTRATOR`. |
| `DuplicateResourceException` | The `identifier` or the `email` is already used by another user. |

---

# RegisterLogisticsOperatorService / RegisterSupervisorService

## Description

Register a new `LogisticsOperator` or `Supervisor` on behalf of an `Administrator`. Both services follow the same flow.

## Parameters

`execute(User actor, LogisticsOperator logisticsOperator, String password)` / `execute(User actor, Supervisor supervisor, String password)`

| Parameter | Type   | Description |
| --------- | ------ | ----------- |
| actor     | User   | Administrator performing the registration. |
| logisticsOperator / supervisor | LogisticsOperator / Supervisor | New model: `identifier` (national ID), `fullName`, `email`. Its status is ignored. |
| password  | String | Plain password, hashed with `PasswordHasherPort.hash`. |

**Returns:** `LogisticsOperator` / `Supervisor`

## Business Rules

* Only an active `ADMINISTRATOR` can register staff, checked with `ValidateRoleAuthorizationService` before anything else.
* `identifier` and `email` must be unique across all users.
* A new user starts with `status = ACTIVE`.

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `LOGISTICS_OPERATOR_REGISTRATION` / `SUPERVISOR_REGISTRATION` | `USER` — the new user | `newStatus` |

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `ADMINISTRATOR`. |
| `DuplicateResourceException` | The `identifier` or the `email` is already used by another user. |

---

# UpdateUserStatusService

## Description

Changes the operational `UserStatus` of any user (e.g. blocks an account).

## Parameters

`execute(User actor, User target, UserStatus status)`

| Parameter | Type       | Description |
| --------- | ---------- | ----------- |
| actor     | User       | Administrator performing the change. |
| target    | User       | Reference to the user to update. It is the abstract `User`; see [Input Ports](../Input-ports.md#abstract-user-parameters). |
| status    | UserStatus | New status. |

**Returns:** `User`

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `USER_STATUS_CHANGE` | `USER` — the target | `previousStatus`, `newStatus`, `role` (the target's role) |

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `ADMINISTRATOR`. |
| `EntityNotFoundException` | No user exists with the target's identifier. |

---

# UpdateSellerStatusService

## Description

Changes the commercial `SellerStatus` of a seller. It is independent from the `UserStatus`: a seller can be able to log in but not allowed to publish.

## Parameters

`execute(User actor, Seller sellerReference, SellerStatus status)`

| Parameter       | Type         | Description |
| --------------- | ------------ | ----------- |
| actor           | User         | Administrator performing the change. |
| sellerReference | Seller       | Reference to the seller to update. |
| status          | SellerStatus | New seller status. |

**Returns:** `Seller`

## Audit

Recorded through `RegisterOperationAndAuditService` after saving.

| Operation type | Affected entity | Details |
| -------------- | --------------- | ------- |
| `SELLER_STATUS_CHANGE` | `USER` — the seller | `previousStatus`, `newStatus` (seller statuses) |

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `ADMINISTRATOR`. |
| `EntityNotFoundException` | No user exists with the seller's identifier, or the user is not a `Seller`. |
