#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
# Using the compiler module also supports JDK installations without a javac launcher.
java --module jdk.compiler/com.sun.tools.javac.Main --release 21 -encoding UTF-8 -d "$classes" \
  src/client/java/dev/alacka/orbit/freelook/FreeLookState.java \
  src/client/java/dev/alacka/orbit/config/FreeLookConfig.java \
  tests/java/dev/alacka/orbit/FreeLookSelfTest.java
java -cp "$classes" dev.alacka.orbit.FreeLookSelfTest
