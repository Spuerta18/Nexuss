# stress-tests

Security, authorization and stress test campaign for the Nexus Market API. All artifacts are in English and contain
**no stored credentials** — everything sensitive is read at runtime from environment variables.

## Contents

| Path | What it is |
|---|---|
| `src/NexusTester.java` | Single-file Java harness (`java.net.http` + `ExecutorService`, no dependencies). Subcommands: `bootstrap`, `authz`, `hostile`, `abuse`, `injection`, `login`, `session`, `oversell`, `races`, `dupreg`, `load`, `bigbody`. |
| `checks/invariants.js` | Read-only `mongosh` data-integrity invariants (negative stock, reserved vs open-order demand, unique e-mail, invoice totals, one refund per return, operation↔audit, no secrets). |
| `checks/audit_checks.js` | Read-only `mongosh` audit-trail coverage (operation/audit counts, type distribution, login auditing, sensitive-data scan). |
| `REPORT.md` | Full findings report (executive summary, authorization matrix, 7 findings with severity, reproduction, probable cause and suggested fix). |

## Environment variables (never hard-coded)

| Var | Purpose |
|---|---|
| `BASE` | target base URL (default `http://localhost:8081`) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | initial administrator of the target instance (for `bootstrap`) |
| `JWT_SECRET` | base64 HMAC secret of the target instance (only for the token-forgery tests) |
| `STATE` | path to the JSON state file produced by `bootstrap` and read by the rest |
| `OUT` | path to the JSONL results file |
| `RUN` | short run id used to namespace generated identifiers |

## Running

The target must be an **isolated** instance (e.g. port `8081`, database `nexusmarket_stress`) — never the demo on
`8080` / `nexusmarket_demo`. See `REPORT.md` §8 for the exact command sequence.

```bash
javac -d /tmp/classes src/NexusTester.java
STATE=/tmp/state.json OUT=/tmp/authz.jsonl RUN=r1 \
  ADMIN_EMAIL=… ADMIN_PASSWORD=… JWT_SECRET=… BASE=http://localhost:8081 \
  java -cp /tmp/classes NexusTester bootstrap
# …then authz, hostile, abuse, injection, login, session, oversell, races, dupreg, load, bigbody
mongosh "mongodb://localhost:27017/nexusmarket_stress" checks/invariants.js
```

Each result row is one JSON object: `{frente, test, identity, target, expected, actual, pass, note}`.

> The harness only **reports**; it never attempts to fix a defect. The commerce side-effects it creates (throwaway
> users, carts, orders) live in the disposable `nexusmarket_stress` database.
