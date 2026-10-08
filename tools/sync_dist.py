#!/usr/bin/env python3
"""Copy only the official dist launcher icon; never embed frontend assets."""
import argparse
import os
import pathlib
import shutil

ROOT = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('dist', nargs='?', type=pathlib.Path,
                    default=os.environ.get('OPENCODE_DIST'),
                    help='Frontend dist directory (or set OPENCODE_DIST)')
args = parser.parse_args()
if args.dist is None:
    parser.error('Specify the frontend dist directory or set OPENCODE_DIST')
source = args.dist.expanduser().resolve()
icon = source / 'icons/prod/web-app-manifest-512x512.png'
if not icon.is_file():
    parser.error(f'Missing dist launcher icon: {icon}')
launcher = ROOT / 'app/src/main/res/drawable-nodpi/ic_launcher.png'
launcher.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(icon, launcher)
print('Copied dist launcher icon; no frontend assets bundled')
