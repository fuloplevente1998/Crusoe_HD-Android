#!/usr/bin/env bash
# Asset-free test: safe stone restoration on the original 180x180 grid.
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
TASK_JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
mkdir -p build/stone-tests
"$TASK_JAVAC" -encoding UTF-8 -d build/stone-tests \
  app/src/main/java/hu/crusoe/standalone/ResourceRespawn.java \
  tests/ResourceRespawnChecks.java
"$TASK_JAVA" -ea -cp build/stone-tests ResourceRespawnChecks
