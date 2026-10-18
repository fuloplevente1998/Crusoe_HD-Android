#!/usr/bin/env bash
# Build isolated Crusoe HD runtime on a machine with Android SDK, NDK, Git,
# Python 3 and JDK 21. No original game resources are fetched or bundled.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UPSTREAM_COMMIT="13e29830632464719a643043e1113310fe6e64cc"
WORKDIR="$ROOT/.work/loader-hd"
if [[ $# -gt 0 ]]; then WORKDIR="$1"; fi
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
# Install beside stock Loader without overwriting its settings.
sed -i "s/applicationIdSuffix '.debug'/applicationIdSuffix '.crusoehd'/" "$WORKDIR/app/build.gradle"
grep -F "applicationIdSuffix '.crusoehd'" "$WORKDIR/app/build.gradle"
keytool -genkeypair -noprompt \
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
find "$WORKDIR/app/build/outputs/apk/open/debug" -name '*.apk' -print
