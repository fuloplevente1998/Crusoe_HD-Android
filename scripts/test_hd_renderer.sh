#!/usr/bin/env bash
# Run real Android/Skia pixel tests against an already patched local checkout.
# Uses synthetic resources; no original game assets are loaded or bundled.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORKDIR="${1:-$ROOT/.work/loader-hd}"
UPSTREAM_COMMIT=13e29830632464719a643043e1113310fe6e64cc
[[ "$(git -C "$WORKDIR" rev-parse HEAD)" == "$UPSTREAM_COMMIT" ]] || {
  echo 'Unexpected upstream checkout; use the pinned HD build first.' >&2; exit 2;
}
[[ -f "$WORKDIR/app/src/main/java/javax/microedition/lcdui/CrusoeHD.java" ]] || {
  echo 'The checkout has no HD patch.' >&2; exit 2;
}
# Only the isolated upstream build is changed, never a public Actions workflow.
python3 - "$WORKDIR/app/build.gradle" <<'PY'
from pathlib import Path
import sys
p = Path(sys.argv[1])
s = p.read_text()
if '// Crusoe synthetic renderer tests' not in s:
    p.write_text(s + '''
// Crusoe synthetic renderer tests
dependencies { testImplementation 'org.robolectric:robolectric:4.16.1' }
android { testOptions { unitTests {
    includeAndroidResources = true
    all {
        systemProperty 'robolectric.dependency.repo.url', 'https://repo.maven.apache.org/maven2'
    }
} } }
''')
PY
TARGET="$WORKDIR/app/src/test/java"
mkdir -p "$TARGET"
cp -R "$ROOT/tests/java/." "$TARGET/"
"$WORKDIR/gradlew" -p "$WORKDIR" --no-daemon --console=plain --stacktrace \
  testOpenDebugUnitTest --tests javax.microedition.lcdui.CrusoeRenderingTest
