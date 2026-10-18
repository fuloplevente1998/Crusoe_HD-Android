#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
TASK_JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
mkdir -p build/engine-tests
"$TASK_JAVAC" -encoding UTF-8 -d build/engine-tests \
  app/src/main/java/hu/crusoe/standalone/GameData.java \
  app/src/main/java/hu/crusoe/standalone/GameEngine.java \
  app/src/main/java/hu/crusoe/standalone/HarvestGame.java tests/EngineChecks.java
"$TASK_JAVA" -ea -cp build/engine-tests EngineChecks app/src/main/assets/world.pack
