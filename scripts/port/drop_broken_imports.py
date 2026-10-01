#!/usr/bin/env python3
"""Delete import lines javac says do not resolve.

Handy after bulk-copying imports from upstream (see sync_imports.py): upstream imports classes this
port deleted or never had (Forge-only plugins, classes moved to another package). javac reports them
as "package X does not exist" or "cannot find symbol" with the *import line* as the source, so we can
find and remove exactly those lines and re-run until the log has no import errors left.

    scripts/port/drop_broken_imports.py [log]        # default .port/fast.txt
"""
import re
import sys

log = sys.argv[1] if len(sys.argv) > 1 else '.port/fast.txt'
text = open(log, encoding='utf-8', errors='replace').read()

removals = {}
for block in re.split(r'\n(?=\S.*\.java:\d+: error:)', text):
    m = re.match(r'^(src/main/java/[^:]+):(\d+): error: (.*)$', block, re.M)
    if not m:
        continue
    lines = block.split('\n')
    source = lines[1].strip() if len(lines) > 1 else ''
    if not source.startswith('import '):
        continue
    if 'does not exist' not in m.group(3) and 'cannot find symbol' not in m.group(3):
        continue
    removals.setdefault(m.group(1), set()).add(source)

total = 0
for path, bad in removals.items():
    lines = open(path, encoding='utf-8').read().split('\n')
    keep = [l for l in lines if l.strip() not in bad]
    if len(keep) != len(lines):
        total += len(lines) - len(keep)
        open(path, 'w', encoding='utf-8').write('\n'.join(keep))
        print(f'{path}: -{len(lines) - len(keep)}')
print(f'{total} broken imports removed')
