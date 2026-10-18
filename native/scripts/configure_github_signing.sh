#!/usr/bin/env bash
# Run with the repository owner's gh account, with Actions secrets write access.
# Never prints the key, its base64 representation, or an authentication token.
set -euo pipefail
cd "$(dirname "$0")/.."
export CRUSOE_SIGNING_STORE_FILE="${CRUSOE_SIGNING_STORE_FILE:-private/signing/native-debug.keystore}"
./gradlew --no-daemon --console=plain verifyUpdateSigningIdentity
python3 - <<'PY'
import base64
import os
from pathlib import Path
import subprocess
key = Path(os.environ['CRUSOE_SIGNING_STORE_FILE']).read_bytes()
subprocess.run(['gh', 'secret', 'set', 'CRUSOE_SIGNING_KEYSTORE_B64',
                '--repo', 'fuloplevente1998/Crusoe_HD-Android'],
               input=base64.b64encode(key), check=True)
print('Repository Actions signing secret configured; no key was printed.')
PY
