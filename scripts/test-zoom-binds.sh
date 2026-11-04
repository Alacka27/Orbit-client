#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
java --module jdk.compiler/com.sun.tools.javac.Main --release 21 -encoding UTF-8 -d "$classes" \
  src/client/java/dev/alacka/orbit/config/CombatSettings.java \
  src/client/java/dev/alacka/orbit/config/SupportSettings.java \
  src/client/java/dev/alacka/orbit/config/BindPrefs.java \
  src/client/java/dev/alacka/orbit/config/VisualSettings.java \
  src/client/java/dev/alacka/orbit/config/ClientSettings.java \
  src/client/java/dev/alacka/orbit/config/HudPrefs.java \
  src/client/java/dev/alacka/orbit/config/OrbitConfig.java \
  src/client/java/dev/alacka/orbit/hud/HudLayout.java \
  src/client/java/dev/alacka/orbit/input/HoldGate.java \
  src/client/java/dev/alacka/orbit/input/PressQueue.java \
  src/client/java/dev/alacka/orbit/zoom/ZoomState.java \
  tests/java/dev/alacka/orbit/ZoomBindsSelfTest.java
java -ea -cp "$classes" dev.alacka.orbit.ZoomBindsSelfTest