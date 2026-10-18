#!/usr/bin/env python3
"""Restore pinned world data from the previously published native APK."""
import hashlib
import io
import json
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASELINE_URL = ('https://raw.githubusercontent.com/fuloplevente1998/Crusoe_HD-Android/'
                '57ae401381990305da143deaa994db087a2cffca/Crusoe-Native-0.6.0.apk')
BASELINE_SHA = '25b85eeaacc43f0056ab6fb886798e2e778905308d3e8da8a2c9426fac2a4b8c'


def main():
    identity = json.loads((ROOT / 'config/android-identity.json').read_text())
    target = ROOT / 'app/src/main/assets/world.pack'
    if target.is_file():
        if hashlib.sha256(target.read_bytes()).hexdigest() != identity['world_sha256']:
            raise ValueError('Existing world.pack differs; refusing to overwrite local data')
        print('Existing world.pack verified')
        return
    with urllib.request.urlopen(BASELINE_URL, timeout=120) as response:
        apk = response.read()
    if hashlib.sha256(apk).hexdigest() != BASELINE_SHA:
        raise ValueError('Baseline APK checksum mismatch')
    with zipfile.ZipFile(io.BytesIO(apk)) as archive:
        world = archive.read('assets/world.pack')
    if hashlib.sha256(world).hexdigest() != identity['world_sha256']:
        raise ValueError('Baseline world.pack checksum mismatch')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(world)
    print('Restored and verified world.pack from pinned native 0.6.0 APK')


if __name__ == '__main__':
    main()
