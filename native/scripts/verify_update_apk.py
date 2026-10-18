#!/usr/bin/env python3
"""Verify the actual APK, independently of Gradle configuration."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def inspect(apk, identity):
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
    build_tools = sdk / 'build-tools/36.0.0'
    certs = subprocess.check_output(
        [str(build_tools / 'apksigner'), 'verify', '--print-certs', str(apk)], text=True)
    digests = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]+)$', certs, re.M)
    if len(digests) != 1 or digests[0].lower() != identity['certificate_sha256']:
        raise ValueError('APK signer differs from the existing native Crusoe app')
    badging = subprocess.check_output([str(build_tools / 'aapt2'), 'dump', 'badging', str(apk)], text=True)
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.M)
    if not package or package[1] != identity['application_id']:
        raise ValueError('APK package differs from the existing native Crusoe app')
    if int(package[2]) < identity['minimum_version_code']:
        raise ValueError('APK versionCode predates the update baseline')
    with zipfile.ZipFile(apk) as archive:
        world_sha = hashlib.sha256(archive.read('assets/world.pack')).hexdigest()
        if world_sha != identity['world_sha256']:
            raise ValueError('Unexpected world data in APK')
        if any(name.lower().endswith(('.jar', '.jad')) for name in archive.namelist()):
            raise ValueError('Unexpected Java ME archive in native APK')
    return dict(application_id=package[1], version_code=int(package[2]), version_name=package[3],
                certificate_sha256=digests[0].lower(), world_sha256=world_sha,
                apk_sha256=hashlib.sha256(apk.read_bytes()).hexdigest(), apk_bytes=apk.stat().st_size)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--previous', type=Path, help='Also require a higher version than this installed APK')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    identity = json.loads((ROOT / 'config/android-identity.json').read_text())
    result = inspect(args.apk, identity)
    if args.previous:
        previous = inspect(args.previous, dict(identity, minimum_version_code=1))
        if result['version_code'] <= previous['version_code']:
            raise ValueError('New APK versionCode must be greater than the previous APK')
        result['previous_version_code'] = previous['version_code']
        result['same_package_and_signer'] = True
    serialized = json.dumps(result, indent=2) + '\n'
    if args.output:
        args.output.write_text(serialized)
    print(serialized, end='')


if __name__ == '__main__':
    main()
