#!/usr/bin/env bash
# Build isolated Crusoe HD runtime on a machine with Android SDK, NDK, Git,
# Python 3 and JDK 21. No original game resources are fetched or bundled.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UPSTREAM_COMMIT="13e29830632464719a643043e1113310fe6e64cc"
WORKDIR="$ROOT/.work/loader-hd"
if [[ $# -gt 0 ]]; then WORKDIR="$1"; fi
TASK_JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
TASK_KEYTOOL="${JAVA_HOME:+$JAVA_HOME/bin/}keytool"
command -v "$TASK_JAVAC" >/dev/null || { echo 'A full JDK 21 is required (javac is missing).' >&2; exit 2; }
command -v "$TASK_KEYTOOL" >/dev/null || { echo 'The JDK keytool is missing.' >&2; exit 2; }
[[ "$("$TASK_JAVAC" -version 2>&1 | tail -n 1)" == javac\ 21* ]] || {
  echo 'This pinned build expects JDK 21. Set JAVA_HOME to your full JDK 21.' >&2; exit 2;
}
[[ -d "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]] || {
  echo 'Set ANDROID_HOME or ANDROID_SDK_ROOT to your Android SDK directory.' >&2; exit 2;
}
mkdir -p "$(dirname "$WORKDIR")"
if [[ -e "$WORKDIR" ]]; then
  echo "Refusing to overwrite existing workspace: $WORKDIR" >&2
  exit 2
fi
git clone --filter=blob:none https://github.com/nikita36078/J2ME-Loader.git "$WORKDIR"
git -C "$WORKDIR" checkout "$UPSTREAM_COMMIT"
git -C "$WORKDIR" submodule update --init --recursive
sed -i 's/COMPILE_SDK = 37/COMPILE_SDK = 36/' "$WORKDIR/build.gradle"
grep -F 'COMPILE_SDK = 36' "$WORKDIR/build.gradle"
sed -i 's/ch.acra:acra-dialog:5.14.2/ch.acra:acra-dialog:5.13.1/' "$WORKDIR/app/build.gradle"
grep -F 'ch.acra:acra-dialog:5.13.1' "$WORKDIR/app/build.gradle"
python3 "$ROOT/scripts/apply_hd_patch.py" "$WORKDIR"
sed -i "s/new ResValue('JL-Debug', null)/new ResValue('Crusoe HD Test', null)/" "$WORKDIR/app/build.gradle"
# Install beside stock Loader without overwriting its settings.
sed -i "s/applicationIdSuffix '.debug'/applicationIdSuffix '.crusoehd'/" "$WORKDIR/app/build.gradle"
grep -F "applicationIdSuffix '.crusoehd'" "$WORKDIR/app/build.gradle"
"$TASK_KEYTOOL" -genkeypair -noprompt \
  -keystore "$WORKDIR/app/debug-ci.jks" \
  -alias androiddebugkey -storepass android -keypass android \
  -keyalg RSA -keysize 2048 -validity 365 \
  -dname 'CN=Crusoe HD Local Test,O=Debug,C=HU'
cat > "$WORKDIR/keystore.properties" <<'PROPS'
storeFile=debug-ci.jks
storePassword=android
keyAlias=androiddebugkey
keyPassword=android
PROPS
chmod +x "$WORKDIR/gradlew"
(
  cd "$WORKDIR"
  ./gradlew --no-daemon --stacktrace assembleOpenDebug
)
echo "If successful, the HD runtime APK is under:"
find "$WORKDIR/app/build/outputs/apk/open/debug" -name '*.apk' -print -exec sha256sum {} \;
