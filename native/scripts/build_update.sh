#!/usr/bin/env bash
# The same signer, application ID and version policy on every build machine.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME to a full JDK 21}"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
mkdir -p artifacts/update
# Do not leave a previous APK looking like the result of a failed build.
rm -f artifacts/update/Crusoe-Native-Update.apk artifacts/update/SHA256SUMS artifacts/update/verification.json
./gradlew --no-daemon --console=plain verifyUpdateSigningIdentity
python3 scripts/restore_world.py
bash scripts/test_engine.sh
./gradlew --no-daemon --console=plain testDebugUnitTest assembleDebug
python3 scripts/verify_update_apk.py app/build/outputs/apk/debug/app-debug.apk \
  --output artifacts/update/verification.json
cp app/build/outputs/apk/debug/app-debug.apk artifacts/update/Crusoe-Native-Update.apk
(cd artifacts/update && sha256sum Crusoe-Native-Update.apk > SHA256SUMS)
echo 'Verified native update: artifacts/update/Crusoe-Native-Update.apk'
