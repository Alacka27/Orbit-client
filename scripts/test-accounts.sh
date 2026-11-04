#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
cache="${GRADLE_USER_HOME:-$HOME/.gradle}"
gson="$(find "$cache/caches/modules-2/files-2.1/com.google.code.gson/gson" -name '*.jar' -type f | sort -V | tail -1)"
if [[ -z "$gson" ]]; then echo 'Build once with Gradle to populate Gson, then run test-accounts.sh.' >&2; exit 1; fi
java --module jdk.compiler/com.sun.tools.javac.Main --release 21 -encoding UTF-8 -cp "$gson" -d "$classes" \
  src/client/java/dev/alacka/orbit/accounts/{AuthFailure,AuthSession,AuthTransport,AuthHttp,AuthProtocol,AccountPreferences,SessionGuard,AccountFormLayout}.java \
  tests/java/dev/alacka/orbit/AccountsSelfTest.java
java -ea -cp "$classes:$gson" dev.alacka.orbit.AccountsSelfTest
