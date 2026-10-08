#!/usr/bin/env python3
"""Check that an APK contains no bundled assets or native libraries."""
import argparse
import pathlib
import re
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('apk', nargs='?', type=pathlib.Path,
                    help='APK path; defaults to the newest version in apk/, then the release build')
apk = parser.parse_args().apk
if apk is None:
    candidates = []
    for path in (ROOT / 'apk').glob('OpenCode-*.apk'):
        match = re.fullmatch(r'OpenCode-(\d+(?:\.\d+)*)\.apk', path.name)
        if match and path.is_file():
            candidates.append((tuple(map(int, match[1].split('.'))), path))
    apk = max(candidates)[1] if candidates else ROOT / 'app/build/outputs/apk/release/app-release.apk'
apk = apk.expanduser().resolve()
if not apk.is_file():
    parser.error(f'APK not found: {apk}; pass an existing APK path')
try:
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        if any(name.startswith('assets/') for name in names):
            parser.error('Unexpected bundled frontend assets')
        if any(name.startswith('lib/') for name in names):
            parser.error('Unexpected native libraries')
except (OSError, zipfile.BadZipFile) as error:
    parser.error(f'Cannot read APK {apk}: {error}')
print(apk)
print(f'No bundled frontend or native libraries; APK {apk.stat().st_size / 1024:.1f} KiB')
