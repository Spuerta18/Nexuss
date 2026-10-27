# Nexus Market — Security, Authorization & Stress Test Report

**Target:** isolated stress instance on `http://localhost:8081`, database `nexusmarket_stress`
(a throwaway copy — the `8080` demo and `nexusmarket_demo` were never touched).
**Date:** 2026-10-26 · **Build under test:** commit `b86de44` (main), Spring Boot 4.1.1, Java 21, MongoDB 2.x single node.
**Method:** a single-file Java harness (`src/NexusTester.java`, `java.net.http` + `ExecutorService`, no external
dependencies) plus two read-only `mongosh` invariant scripts (`checks/`). No credentials are stored in any script;
everything sensitive is passed at runtime through environment variables.
**Rule honored:** no bug was fixed — this report only describes and reproduces them.

---

## 1. Executive summary

The **authorization and authentication core is solid**. Across 63 endpoints and 6 identities, role separation holds:
the right role is never wrongly blocked, every wrong role gets `403`, and anonymous access gets `401`. All 15
forged-token attacks (alg=none, wrong secret, tampered signature, a valid-signature token whose role claim was
changed to `ADMINISTRATOR`, expired, stale version, missing `ver`, blocked/inactive user, post-logout,
post-password-change, malformed headers) are correctly rejected with `401` and leak nothing. Input handling is
robust: NoSQL-operator injection, 1 MB strings, malformed JSON, wrong `Content-Type`, path traversal and null
fields never produce a `500` or a stack trace. The audit trail is complete (`operations` and `audit_logs` are 1:1,
15 064 each, with no password/hash/token material).

The serious problems are all **concurrency / data-integrity** defects with a single systemic root cause: the
persistence layer uses *read → check → mutate → save-whole-document* with **no optimistic locking (`@Version`), no
atomic conditional updates, and no transactions** (`grep -r "@Version\|@Transactional"` returns nothing). Under
concurrent load this produces **overselling**, **lost inventory updates**, and **double execution** of state
transitions (approve, refund, cancel, advance). There are also three smaller hardening gaps (login user-enumeration
timing oracle, no login rate-limiting, unbounded request-body size) and one low-severity ordering issue
(bean-validation runs before authorization).

### Totals

| Front | Checks | Passed | Deviations |
|---|---:|---:|---:|
| A — authorization matrix (63 ep × 6 id + invalid-body pass) | 288 | 269 | 19 |
| A — hostile identities / forged tokens | 15 | 15 | 0 |
| B — abuse, IDOR, roles, mass-assignment, domain rules | 51 | 51 | 0 |
| B — injection & hostile inputs | 23 | 23 | 0 |
| B — login uniformity / timing / lockout | 3 | 2 | 1 |
| B — session (password change, token reuse) | 3 | 3 | 0 |
| D — oversell (5 reps) | 5 | 0 | 5 |
| D — state-transition races | 4 | 0 | 4 |
| D — duplicate registration | 1 | 1 | 0 |
| D — load ramps + 500-login burst | 5 | 5 | 0 |
| D — big bodies / deep nesting | 4 | 4 | 0 |
| **Total** | **402** | **373** | **29** |

The 29 deviations collapse into **7 distinct findings** (one finding — the validation/authorization ordering —
accounts for 19 rows; oversell for 5; races for 4; timing for 1). Three further findings were logged as
`pass`-with-a-note (no lockout, duplicate-registration `500`s, no body-size cap).

### Findings by severity

| # | Severity | Finding |
|---|---|---|
| F1 | **Critical** | Overselling — order confirmation reserves stock without atomicity (TOCTOU + lost updates) |
| F2 | **High** | State-transition races — approve / refund / cancel / advance all execute twice concurrently |
| F3 | **Medium** | Login timing side-channel enables e-mail (user) enumeration |
| F4 | **Medium** | No rate-limiting / lockout on failed logins |
| F5 | **Medium** | Concurrent duplicate registration returns `500` (unmapped `DuplicateKeyException`) instead of `409` |
| F6 | **Medium** | No request-body size limit (5 MB accepted & stored; 50 MB → `500`) |
| F7 | **Low** | Authorization is evaluated *after* bean validation (wrong role + invalid body → `400`, not `403`) |

---

## 2. Front A — authorization matrix

Every endpoint was called with each identity. Expectation: the correct role is never `401/403`; every other
authenticated role is `403`; anonymous is `401`. Valid bodies were sent so a `403` is never masked by a `400`.

**Result: fully conforming.** Summary by prefix (identical pattern for every endpoint in the group):

| Endpoint group (role) | anon | BUYER | SELLER | ADMIN | LOGISTICS | SUPERVISOR |
|---|---|---|---|---|---|---|
| `/api/auth/*`, `/api/me` (public / any) | ok | ok | ok | ok | ok | ok |
| `/api/buyer/**` (BUYER) | 401 | ✅ | 403 | 403 | 403 | 403 |
| `/api/seller/**` (SELLER) | 401 | 403 | ✅ | 403 | 403 | 403 |
| `/api/admin/**` (ADMINISTRATOR) | 401 | 403 | 403 | ✅ | 403 | 403 |
| `/api/logistics/**` (LOGISTICS_OPERATOR) | 401 | 403 | 403 | 403 | ✅ | 403 |
| `/api/supervisor/**` (SUPERVISOR) | 401 | 403 | 403 | 403 | 403 | ✅ |

✅ = allowed (never `401/403`; business status such as `404` on a non-existent id is fine). No cross-role leak was
found on any of the 63 endpoints.

The **only** deviation: when a wrong-role caller sends an **invalid** body to a body-bearing endpoint, the response
is `400 (Validation failed)` instead of `403` (19 endpoints). See **F7** — validation runs before authorization.
With a *valid* body every wrong-role call is correctly `403`, so no unauthorized action is ever executed.

### Hostile identities / forged tokens — all `401`

| Attack | Result |
|---|---|
| `alg=none` forged admin token | 401 ✅ |
| signed with a different secret | 401 ✅ |
| real token, signature bytes tampered | 401 ✅ |
| **role claim changed to `ADMINISTRATOR`, re-signed with the real secret** | **401 ✅** (filter reloads the user, rejects the mismatched role) |
| valid signature, expired | 401 ✅ |
| valid signature, stale `ver=999` | 401 ✅ |
| valid signature, no `ver` claim | 401 ✅ |
| token of a `BLOCKED` user | 401 ✅ |
| token of an `INACTIVE` user | 401 ✅ |
| token reused after logout | 401 ✅ |
| token reused after password change | 401 ✅ |
| `Authorization` without `Bearer` / `Bearer` only / garbage / empty | 401 ✅ |

This is the strongest area of the system: `JwtAuthenticationFilter` reloads the stored user on every request and
checks status, role and `authTokenVersion`, so a validly-signed token with a forged role claim is still rejected.

---

## 3. Front B — abuse, business rules, injection

All 51 abuse checks and all 23 injection checks **pass**. Highlights:

- **IDOR / ownership:** a buyer cannot read or modify another buyer's cart or orders (`403`); a seller cannot
  touch another seller's product status, variants, inventory inbound or adjustments (`403`); a non-published
  (SUSPENDED) competitor product is not readable by another seller (`403`). A `PUBLISHED` product *is* readable by
  any active user — this is **by design** (`ConsultProductService` exposes only published products to non-owners).
- **Roles:** a seller cannot register a MARKETPLACE warehouse; no non-admin can register staff; a supervisor cannot
  write; an administrator cannot touch a cart; a logistics operator cannot approve returns or process refunds; a
  seller cannot change its own `SellerStatus`; a user cannot change its own status. All `403`.
- **Mass assignment:** registering a buyer with extra `role=ADMINISTRATOR`, `status`, `sellerStatus`,
  `authTokenVersion`, `passwordHash` fields — all ignored; the account is created as `BUYER/ACTIVE` and is denied
  admin endpoints.
- **Domain rules (all enforced, sequentially):** quantity `0`/negative → `400`; price `0`/negative/bad currency →
  `400`; mixing currencies in a cart → `409`; confirm empty cart → `409`; add SUSPENDED product → `409`; insufficient
  stock → `409`; cancel/return a `DELIVERED` order → `409`; cancel a `PAID` order → `409`; skip/reverse order status →
  `400/409`; dispatch a shipment before packing → `409`; approve/reject a return twice → `409`; process a refund twice
  → `409`; duplicate e-mail/identifier → `409`; negative stock adjustment → `409`.
- **Injection:** NoSQL operators (`{"$ne":null}`, `{"$gt":""}`, `{"$regex":".*"}`) as login e-mail/password or any id
  field → `400` (they hit typed `String` fields and never reach a query); 1 MB strings → `400`/`201`; malformed JSON →
  `400`; wrong `Content-Type` → `415`; path traversal / unicode / special chars → `400`/`404`; null required fields →
  `400`; numbers-as-strings → `400`/`404`. **No `500`, no stack trace, ever.**
- **Session:** change password with a wrong current password → `400`; too-short new password → `400`; a token reused
  after a password change → `401`.

---

## 4. Front C — service coverage & audit

### 4.1 Coverage

All **63 endpoints** (every method of the 6 role ports + `/api/me` + auth) were exercised for **authorization**
(an allowed identity that is never `401/403`, and at least one forbidden identity that is `403`). The full commerce
lifecycle was exercised with real **business happy paths**: buyer/staff registration, login, cart create / add line,
order confirm / cancel / return, pay (status advance), shipment create / pack / dispatch / deliver, return approve,
refund process, product publish / status change, inventory inbound / adjustment, operations & audit-log consultation,
`/api/me`, logout, password change.

Endpoints exercised **only at the authorization layer** (no dedicated business happy-path assertion), listed for
completeness: `PUT /carts/{id}/lines/{productId}` (update quantity), `DELETE /carts/{id}/lines/{productId}`,
`PUT /seller/products/{id}/variants`, `POST /admin/warehouses/{id}/deactivate`, `POST /logistics/inventory/damaged`,
`POST /logistics/inventory/returns` (direct), `POST /admin/returns/{id}/reject` (reject path), and the three
`consultInventory` / `consultUser` read endpoints. None of these showed an authorization defect.

### 4.2 Audit verification (`checks/audit_checks.js`)

- `operations` = `audit_logs` = **15 064** records, **1:1**, no orphans (invariant #6).
- Every state-changing type carries correct `previousStatus`/`newStatus` details, e.g.
  `RETURN_APPROVAL {previousStatus:REQUESTED,newStatus:APPROVED,refundId:…}`,
  `REFUND_PROCESSING {previousStatus:PENDING,newStatus:PROCESSED,amount,currency}`,
  `ORDER_CANCELLATION {previousStatus:PENDING_PAYMENT,newStatus:CANCELLED}`.
- **Only successful logins are audited** (`USER_LOGIN` = 13 872). The hundreds of failed logins from the timing and
  burst tests left **no** records — rejected attempts (`401/403/400/409`) do not create false audit entries.
- **No password, hash or token** material appears in any `operations` or `audit_logs` detail. The one hit in the app
  log scan is a false positive: `password=null` inside the MongoClient connection-settings line.

---

## 5. Front D — stress & concurrency

### 5.1 Oversell (F1, Critical)

Product stock **10**, **60** buyers each confirming **1** unit simultaneously, repeated 5×:

| Rep | Accepted (expected 10) | Rejected | Oversold by |
|---|---:|---:|---:|
| 1 | 60 | 0 | +50 |
| 2 | 60 | 0 | +50 |
| 3 | 60 | 0 | +50 |
| 4 | 60 | 0 | +50 |
| 5 | 59 | 1 | +49 |

`checks/invariants.js` #2 then shows the inventory items left in an impossible state — `reservedQuantity` is **7/8/9/2/4**
while the real open-order demand is **60/60/60/60/59**:

```
MISMATCH OS-…-r1 reserved=7 openOrderDemand=60
MISMATCH OS-…-r4 reserved=2 openOrderDemand=60
```

Two bugs compound: (a) stock is **oversold ~6×**, and (b) the whole-document saves **lose updates**, so `reserved`
never reflects the real count (and `available=3` is fictitious). No value goes negative only because every concurrent
save rewrites the document back to a sum of 10.

### 5.2 State-transition races (F2, High)

Two identical requests fired simultaneously, each should yield exactly one success:

| Scenario | Successes (expected 1) |
|---|---|
| approve the same return twice | **2** |
| process the same refund twice | **2** |
| advance the same order `PENDING_PAYMENT→PAID` twice | **2** |
| cancel the same order twice | **2** |

Double-approve produces duplicate `INVENTORY_RETURN` movements (stock credited twice); double-cancel releases the
reservation twice; double-refund processes the payout twice. Pure read-check-mutate-save with no guard.

### 5.3 Duplicate registration (F5, Medium)

30 concurrent registrations of the same e-mail: **exactly 1 account** is created (the unique index on `users.email`
holds), 11 return `409`, but **18 return `500`**. The service does `existsByEmail` → `save` (TOCTOU); the losing
threads hit `E11000 duplicate key`, which surfaces as `org.springframework.dao.DuplicateKeyException` and is **not**
mapped in `GlobalExceptionHandler`, so it becomes a generic `500` (logged as "Unhandled error"). Uniqueness is
preserved; the status code and the 18 ERROR log lines are the problem. The client body is the generic masked error —
no stack trace is leaked.

### 5.4 Load ramps (no errors)

Login + read mix, 30 s per ramp:

| Concurrency | ops | throughput | p50 | p95 | p99 | 5xx errors |
|---:|---:|---:|---:|---:|---:|---:|
| 10 | 8 287 | 276/s | 7.6 ms | 115.2 ms | 145.9 ms | 0 |
| 50 | 10 742 | 358/s | 115.3 ms | 363.9 ms | 528.1 ms | 0 |
| 100 | 10 971 | 366/s | 240.4 ms | 634.4 ms | 828.9 ms | 0 |
| 200 | 11 140 | 371/s | 457.0 ms | 1259.3 ms | 1658.3 ms | 0 |

Throughput plateaus around **370 ops/s** and latency grows with concurrency — the login portion is **BCrypt-bound**,
not pool-starved: no `5xx`, no Mongo connection-pool exhaustion, no thread-exhaustion errors in the app log. A burst
of **500 failed logins** completed in 3.7 s, all `401`, and the app stayed healthy (`200` afterward).

### 5.5 Big bodies (F6, Medium)

| Body | Result |
|---|---|
| 5 MB JSON (`fullName`) | **`201` — accepted and persisted** (no size or max-length cap) |
| 50 MB JSON | **`500`** (no clean `413`; app survived — `/v3/api-docs` returned `200` afterward) |
| deeply nested JSON (100 000 levels) | `400` "Malformed…" (Jackson depth limit) — clean |

No request-body size limit is configured. A 5 MB field is stored verbatim; 50 MB causes a `500`. Concurrent large
bodies are a memory-pressure / DoS risk.

### 5.6 Invariants after the campaign (`checks/invariants.js`)

| # | Invariant | Result |
|---|---|---|
| 1 | no negative inventory quantities | **PASS** |
| 2 | `reserved` == open-order demand | **FAIL** (5/15 — the oversold products; this is F1's fingerprint) |
| 3 | unique e-mail / identifier | **PASS** |
| 4 | invoice total == sum of order lines | **PASS** (12/12) |
| 5 | ≤ 1 refund per return | **PASS** |
| 6 | every Operation has an AuditLog | **PASS** (15 064 = 15 064) |
| 7 | no secrets in the audit trail | **PASS** |

---

## 6. Detailed findings

### F1 — Overselling: non-atomic stock reservation `[Critical]`
- **Expected:** with stock 10, exactly 10 concurrent confirmations succeed; the rest get `409`.
- **Actual:** 60/60 (and 59/60) succeed; `reserved` ends at 2–9 while 60 orders are open; `available` is fictitious.
- **Reproduce:** `drive.sh oversell 5`, then `mongosh … checks/invariants.js` (invariant #2).
- **Probable cause:** `domain/services/ReserveInventoryService.java:31-39` —
  `findAllByProductId` → `if (available >= quantity)` → `item.reserve(quantity)` → `save(item)`. The read-check-reserve
  is not atomic and the save replaces the whole document. No `@Version` and no `@Transactional` exist anywhere in
  `src/main/java`. Called from `ConfirmOrderService.execute` (`domain/services/ConfirmOrderService.java:66-…`).
- **Suggested fix:** make the reservation a single atomic conditional update —
  `updateOne({_id, availableQuantity: {$gte: q}}, {$inc: {availableQuantity: -q, reservedQuantity: +q}})` and treat a
  zero `modifiedCount` as insufficient stock; or add `@Version` to `InventoryItemDocument` and retry on
  `OptimisticLockingFailureException`; wrap multi-line confirmation in a transaction (requires a replica set).

### F2 — State-transition races: double approve / refund / cancel / advance `[High]`
- **Expected:** one of two simultaneous identical state changes succeeds; the other gets `409`.
- **Actual:** both succeed (2/2) for return approval, refund processing, order advance and order cancel.
- **Reproduce:** `drive.sh races`.
- **Probable cause:** same read-check-mutate-save pattern without locking:
  `ApproveReturnService.execute` (findById → `returnRequest.approve()` → save),
  `domain/services/ProcessRefundService.java:35-39`,
  `domain/services/CancelOrderService.java:47-56` (releases stock, then saves — double release),
  `AdvanceOrderStatusService` + `domain/models/Order.java:79` (`advanceTo`). The in-memory status guard
  (`approve()`, `process()`, `advanceTo`) is evaluated on a stale read, so two threads both pass it.
- **Suggested fix:** optimistic locking (`@Version`) with retry, or conditional updates that match the expected
  current status (`updateOne({_id, status:"REQUESTED"}, {$set:{status:"APPROVED"}})` and treat `modifiedCount==0`
  as already-decided). Side effects (inventory return, refund creation, stock release) must be guarded by the same
  atomic transition.

### F3 — Login timing side-channel → user enumeration `[Medium]`
- **Expected:** indistinguishable response (code, body and timing) for an existing vs non-existing e-mail.
- **Actual:** code and body are identical (`401` / `"Invalid email or password"`), but timing differs ~15×:
  existing e-mail ≈ **104.7 ms**, missing e-mail ≈ **6.7 ms** (Δ ≈ 98 ms, 40 samples each).
- **Reproduce:** `drive.sh login`.
- **Probable cause:** `domain/services/AuthenticateUserService.java:38-42` —
  `findByEmail(email).filter(u -> passwordHasherPort.matches(password, hash))`. BCrypt runs **only when the e-mail
  exists**; a missing e-mail returns early without hashing, so response time reveals which e-mails are registered.
- **Suggested fix:** always perform a constant-time dummy BCrypt verification against a fixed hash when the user is
  not found, so every login path pays the same hashing cost.

### F4 — No rate-limiting / lockout on failed logins `[Medium]`
- **Expected:** repeated failures should be throttled or the account temporarily locked.
- **Actual:** 30 consecutive wrong-password attempts, then a correct login still succeeds (`200`); a burst of 500
  failed logins completes in 3.7 s with no throttling.
- **Reproduce:** `drive.sh login`, `drive.sh load` (burst section).
- **Probable cause:** no rate-limiter, lockout counter or CAPTCHA in `AuthenticateUserService` or the security filter
  chain. Combined with **F3**, an attacker can enumerate e-mails and then brute-force them unbounded.
- **Suggested fix:** per-account and per-IP failed-attempt counters with exponential back-off / temporary lock, and/or
  an edge rate-limiter.

### F5 — Duplicate registration race returns `500` `[Medium]`
- **Expected:** `409 Conflict` for every losing concurrent duplicate registration.
- **Actual:** 1 success, 11 × `409`, **18 × `500`** (uniqueness itself is preserved — only one account exists).
- **Reproduce:** `drive.sh dupreg`.
- **Probable cause:** `domain/services/RegisterBuyerService.java:30-40` does `existsById`/`existsByEmail` → `save`
  (TOCTOU). The unique index then throws `E11000`, surfacing as `org.springframework.dao.DuplicateKeyException`,
  which is **not** handled in `adapters/in/rest/exception/GlobalExceptionHandler.java` → generic `500`.
- **Suggested fix:** add a `@ExceptionHandler(DuplicateKeyException.class)` mapping to `409`, or catch it in the
  service and rethrow `DuplicateResourceException`. The same race applies to any unique-indexed insert
  (identifiers, product ids, etc.).

### F6 — No request-body size limit `[Medium]`
- **Expected:** oversized requests rejected with `413 Payload Too Large`, bounded field lengths.
- **Actual:** a 5 MB `fullName` is accepted and stored (`201`); a 50 MB body yields a `500`; deep nesting is cleanly
  `400`.
- **Reproduce:** `drive.sh bigbody`.
- **Probable cause:** no `server.tomcat.max-swallow-size` / `spring.servlet.multipart` / `@Size` bound on free-text
  fields; large payloads are buffered into memory.
- **Suggested fix:** configure a request-size limit and return `413`; add `@Size` to free-text DTO fields.

### F7 — Authorization evaluated after bean validation `[Low]`
- **Expected:** a wrong-role caller gets `403` regardless of body validity (authorization before validation).
- **Actual:** wrong role + **invalid** body → `400 (Validation failed)` on all 19 body-bearing endpoints; wrong role
  + **valid** body → `403` (correct).
- **Reproduce:** `drive.sh authz` (rows tagged `matrix-invalidbody`).
- **Probable cause:** Spring MVC resolves and validates `@Valid @RequestBody` arguments **before** the method-security
  advice (`@PreAuthorize`) runs. No business action executes, but a forbidden caller can observe validation details.
- **Suggested fix:** if strict ordering is required, enforce role at the filter/`HandlerInterceptor` level (URL-based
  `authorizeHttpRequests` by path prefix) so authorization precedes body binding. Low priority — access control is
  intact.

---

## 7. Real bugs vs. known / documented limitations

**Real bugs (this report):** F1–F7 above.

**Known / documented limitations (already noted in `CLAUDE.md`, not re-counted as new bugs):**
- Audit writes are **not transactional** with the business write (an operation/audit pair can in principle diverge
  from the business change). Consistent with F1/F2's lack of transactions.
- `USER_LOGIN` is recorded **after** the token is issued; the initial-administrator seeder records no operation.
- Legacy pre-Block-1 `audit_logs` entries and the old `user_occurred` index are skipped on read.
- No data migration for the Money format; documents written before Batch B cannot be read.

These are design trade-offs the team has already accepted; they are listed here only so they are not mistaken for
new findings.

---

## 8. How to run

```bash
# 1. Start an isolated instance on 8081 against a throwaway DB (never the demo):
export NEXUS_JWT_SECRET=$(head -c 48 /dev/urandom | base64 -w0)
export NEXUS_MONGO_URI=mongodb://localhost:27017/nexusmarket_stress
export NEXUS_ADMIN_EMAIL=stress-admin@nexus.local
export NEXUS_ADMIN_PASSWORD='<generated at runtime>'
java -cp "Application/target/classes;<deps>" -Dserver.port=8081 nexussMarket.Application

# 2. Compile and drive the harness (env: BASE, ADMIN_EMAIL, ADMIN_PASSWORD, JWT_SECRET, STATE, OUT, RUN):
javac -d out/classes stress-tests/src/NexusTester.java
java -cp out/classes NexusTester bootstrap     # creates roles + base data, writes the state file
java -cp out/classes NexusTester authz         # then: hostile abuse injection login session
java -cp out/classes NexusTester oversell 5    # then: races dupreg load bigbody

# 3. Data-integrity & audit invariants (read-only):
mongosh "mongodb://localhost:27017/nexusmarket_stress" stress-tests/checks/invariants.js
mongosh "mongodb://localhost:27017/nexusmarket_stress" stress-tests/checks/audit_checks.js
```

No secret is ever written to disk by these scripts; the `STATE`/`OUT` files live outside the repository.
