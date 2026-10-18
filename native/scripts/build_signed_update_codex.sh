#!/usr/bin/env bash
# Codex release gate: build a genuine UPDATE for the installed 0.6.0 app.
# Requires the already-preserved keystore; never create a replacement key.
set -euo pipefail
cd "$(dirname "$0")/.."

: "${JAVA_HOME:?JDK 21 JAVA_HOME must be set}"
: "${ANDROID_HOME:?Android SDK ANDROID_HOME must be set}"

KEY=private/signing/native-debug.keystore
WORLD=app/src/main/assets/world.pack
APK=app/build/outputs/apk/debug/app-debug.apk
SIGNTOOL="$ANDROID_HOME/build-tools/36.0.0/apksigner"
EXPECTED_CERT=ce51e3426040b1497c65fd230f875e602d4766143ba3f505475e0996599b40c1

if [[ ! -f "$KEY" ]]; then
  echo "STOP: previous signing key unavailable: $KEY" >&2
  echo "Never generate a new signing key for an in-place update." >&2
  exit 2
fi
if [[ ! -f "$WORLD" ]]; then
  echo "STOP: existing local world.pack missing: $WORLD" >&2
  exit 2
fi
printf '%s  %s\n'   '324279ccfdb618d769572f1233d64c99a71725f5cf138acc84a7e1c0231bc6ce'   "$WORLD" | sha256sum -c -
test -x "$SIGNTOOL" || { echo "Missing apksigner: $SIGNTOOL" >&2; exit 2; }
grep -Fq "applicationId 'hu.crusoe.standalone'" app/build.gradle
grep -Fq "versionCode 8" app/build.gradle
grep -Fq "versionName '0.6.1-native-alpha'" app/build.gradle

echo "Building a verified same-package versionCode=8 update..."
bash scripts/test_engine.sh
./gradlew --no-daemon --console=plain testDebugUnitTest assembleDebug
test -s "$APK"

CERTS="$("$SIGNTOOL" verify --print-certs "$APK")"
printf '%s\n' "$CERTS"
if ! printf '%s\n' "$CERTS" | grep -Fqi "$EXPECTED_CERT"; then
  echo "STOP: built APK has DIFFERENT signing certificate from installed 0.6.0." >&2
  echo "Do not install, publish, or rename this APK." >&2
  exit 3
fi

mkdir -p artifacts
cp "$APK" artifacts/Crusoe-Native-0.6.1-Update.apk
sha256sum artifacts/Crusoe-Native-0.6.1-Update.apk   > artifacts/Crusoe-Native-0.6.1-Update.apk.sha256
echo "PASS: versionCode=8, same package and signer. Backup before phone test."
