#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
java --module jdk.compiler/com.sun.tools.javac.Main --release 21 -encoding UTF-8 -d "$classes" \
  src/client/java/dev/alacka/orbit/menu/ClickMenuLayout.java \
  tests/java/dev/alacka/orbit/ClickMenuLayoutSelfTest.java
java -ea -cp "$classes" dev.alacka.orbit.ClickMenuLayoutSelfTest
