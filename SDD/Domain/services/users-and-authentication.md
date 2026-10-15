# Users and Authentication — Domain Services — NexusMarket

## Introduction

Authentication and the registration and status of users.

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
5. Issue a token for the user through `TokenServicePort.generateToken` and return it with the user. The domain never knows the token format (JWT, in `infrastructure/security`).

## Business Rules

* Only users whose `status` is `ACTIVE` can authenticate.
* An unknown email, a wrong password and a user who is not `ACTIVE` all produce the same error, so a failed login never reveals whether an account exists, whether the password was right, or whether the account is blocked.
* No token is issued for a failed login.

## Exceptions

| Exception | When |
| --------- | ---- |
| `InvalidCredentialsException` | Unknown email, missing password hash, wrong password, or a user who is not `ACTIVE` (always `"Invalid email or password"`). |

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

## Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not an active `ADMINISTRATOR`. |
| `EntityNotFoundException` | No user exists with the seller's identifier, or the user is not a `Seller`. |
