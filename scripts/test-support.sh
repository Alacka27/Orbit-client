#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
java --module jdk.compiler/com.sun.tools.javac.Main --release 21 -encoding UTF-8 -d "$classes" \
  src/client/java/dev/alacka/orbit/config/SupportSettings.java \
  src/client/java/dev/alacka/orbit/config/CombatSettings.java \
  src/client/java/dev/alacka/orbit/config/BindPrefs.java \
  src/client/java/dev/alacka/orbit/config/VisualSettings.java \
  src/client/java/dev/alacka/orbit/config/ClientSettings.java \
  src/client/java/dev/alacka/orbit/config/OrbitConfig.java \
  src/client/java/dev/alacka/orbit/config/HudPrefs.java \
  src/client/java/dev/alacka/orbit/hud/HudLayout.java \
  src/client/java/dev/alacka/orbit/menu/ModuleGrid.java \
  src/client/java/dev/alacka/orbit/menu/ModuleSettingsLayout.java \
  src/client/java/dev/alacka/orbit/modules/SupportMath.java \
  src/client/java/dev/alacka/orbit/modules/SupportGate.java \
  src/client/java/dev/alacka/orbit/modules/ConsumeSession.java \
  tests/java/dev/alacka/orbit/SupportSelfTest.java
java -ea -Djava.awt.headless=true -cp "$classes" dev.alacka.orbit.SupportSelfTest