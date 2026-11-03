#!/usr/bin/env bash
# Launch the Nexus Market API against the demo database on port 8080 (offline java -cp route).
# No credentials are stored here. NEXUS_JWT_SECRET is read from the environment; if it is not
# set, a random one is generated for this run (tokens do not survive a restart, which is fine
# for a local demo). The initial administrator is only created when NONE exists.
set -euo pipefail
cd "$(dirname "$0")/Application"

export NEXUS_MONGO_URI="${NEXUS_MONGO_URI:-mongodb://localhost:27017/nexusmarket_demo}"
if [ -z "${NEXUS_JWT_SECRET:-}" ]; then
  export NEXUS_JWT_SECRET="$(head -c 48 /dev/urandom | base64 -w0)"
  echo "[run-app] NEXUS_JWT_SECRET not set; generated a random one for this run." >&2
fi

# Build classes / classpath offline if missing.
[ -d target/classes ] || mvn -o clean compile -q
[ -f target/classpath.txt ] || mvn -o dependency:build-classpath -Dmdep.outputFile=target/classpath.txt -q

# Windows (Git Bash) uses ';' as the classpath separator; Linux/macOS use ':'.
SEP=';'; case "$(uname -s)" in Linux|Darwin) SEP=':';; esac
exec java -cp "target/classes${SEP}$(cat target/classpath.txt)" nexussMarket.Application
