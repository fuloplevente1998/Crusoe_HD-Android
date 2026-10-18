#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME to a full JDK 21}"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
[[ -f app/src/main/assets/world.pack ]] || { echo 'Prepare the local world pack first.' >&2; exit 2; }
for name in objects robinson title terrain; do
  [[ -f "app/src/main/assets/art/$name.png" ]] || { echo "Missing local art: $name.png" >&2; exit 2; }
done
./gradlew --no-daemon --console=plain testDebugUnitTest assembleDebug
bash scripts/test_engine.sh
mkdir -p artifacts
cp app/build/outputs/apk/debug/app-debug.apk artifacts/Crusoe-Native-0.3.apk
sha256sum artifacts/Crusoe-Native-0.3.apk
