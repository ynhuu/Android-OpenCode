#!/usr/bin/env python3
"""Regenerate the complete frontend patch against an official source archive."""
import argparse
import difflib
import pathlib
import re
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--source', required=True, type=pathlib.Path)
parser.add_argument('--official', required=True, type=pathlib.Path)
parser.add_argument('--files-from', required=True, type=pathlib.Path)
parser.add_argument('--output', required=True, type=pathlib.Path)
args = parser.parse_args()
paths = list(dict.fromkeys(re.findall(r'^\+\+\+ b/(.+)$', args.files_from.read_text(), re.M)))
if not paths:
    parser.error('No frontend files found in the reference patch')
result = []
with zipfile.ZipFile(args.official) as archive:
    prefix = archive.namelist()[0].split('/')[0] + '/'
    names = set(archive.namelist())
    for name in paths:
        original = prefix + name
        before = archive.read(original).decode() if original in names else ''
        after = (args.source / name).read_text()
        result.extend(difflib.unified_diff(
            before.splitlines(True), after.splitlines(True),
            fromfile='a/' + name if original in names else '/dev/null', tofile='b/' + name,
        ))
args.output.write_text(''.join(result))
print(f'Generated patch for {len(paths)} frontend files: {args.output}')
