#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
cache="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
classpath=""
for module in com.google.code.gson/gson com.mojang/authlib com.google.guava/guava \
              com.google.guava/failureaccess org.slf4j/slf4j-api; do
  jar="$(find "$cache/$module" -name '*.jar' -type f | sort -V | tail -1)"
  if [[ -z "$jar" ]]; then
    echo "Build once with Gradle to populate $module, then run test-account-skins.sh." >&2
    exit 1
  fi
  classpath="${classpath:+$classpath:}$jar"
done
java --module jdk.compiler/com.sun.tools.javac.Main --release 21 -encoding UTF-8 -cp "$classpath" -d "$classes" \
  src/client/java/dev/alacka/orbit/accounts/{AuthFailure,AuthSession,AuthTransport,AuthHttp,AuthProtocol,AccountPreferences,ElySessionService}.java \
  tests/java/dev/alacka/orbit/accounts/ElySessionServiceSelfTest.java
java -ea -cp "$classes:src/main/resources:$classpath" dev.alacka.orbit.accounts.ElySessionServiceSelfTest