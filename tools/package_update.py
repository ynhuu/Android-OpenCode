#!/usr/bin/env python3
"""Produce a root-level dist ZIP and its HTTPS update manifest."""
import argparse
import hashlib
import json
import os
import pathlib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
ZIP_URL = 'https://github.com/ynhuu/Android-OpenCode/releases/latest/download/dist.zip'
parser = argparse.ArgumentParser()
parser.add_argument('--version-code', required=True, type=int)
parser.add_argument('--version', required=True)
parser.add_argument('--zip-url', default=ZIP_URL)
parser.add_argument('--notes', default='')
parser.add_argument('--dist', type=pathlib.Path, default=os.environ.get('OPENCODE_DIST'),
                    help='Frontend dist directory (or set OPENCODE_DIST)')
parser.add_argument('--output', type=pathlib.Path, default=ROOT / 'updates')
args = parser.parse_args()
if args.dist is None:
    parser.error('Specify --dist or set OPENCODE_DIST to the frontend build directory')
args.dist = args.dist.expanduser().resolve()
args.output = args.output.expanduser().resolve()
if args.version_code <= 0:
    parser.error('Version code must be positive')
if args.zip_url != ZIP_URL:
    parser.error(f'ZIP URL must be {ZIP_URL}')
if not (args.dist / 'index.html').is_file():
    parser.error(f'Missing frontend index.html: {args.dist / "index.html"}')
if args.output == args.dist or args.dist in args.output.parents:
    parser.error('Output directory must be outside the frontend dist directory')
args.output.mkdir(parents=True, exist_ok=True)
archive = args.output / 'dist.zip'
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as zip:
    for path in sorted(args.dist.rglob('*')):
        if path.is_file() and path.suffix != '.map' and path.name not in {'sw.js', '_headers', '_redirects'}:
            zip.write(path, path.relative_to(args.dist))
manifest = dict(versionCode=args.version_code, version=args.version, zipUrl=args.zip_url,
                sha256=hashlib.sha256(archive.read_bytes()).hexdigest(), notes=args.notes)
(args.output / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
print(archive)
print(json.dumps(manifest, ensure_ascii=False, indent=2))
