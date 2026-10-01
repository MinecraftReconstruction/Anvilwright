#!/usr/bin/env python3
"""Regenerate docs/merge-3.12.1-workqueue.txt from the two compile logs.

  scripts/port/fastcompile.sh            # default log  (.port/fast.txt)
  scripts/port/fastcompile.sh --gen .port/gen.txt
  python3 scripts/port/workqueue.py

The "real" count is the intersection of the two logs: errors that show up both when javac stops at
FLOW (default) and when it is told to keep going (GENERATE). Everything that only shows up in the
default log is the Lombok phantom family - see docs/MERGE-3.12.1.md.
"""
import collections
import re
import subprocess


def locations(path):
    found = set()
    for line in open(path):
        m = re.match(r'^(src/main/java/[^ :]+\.java):(\d+): error:', line)
        if m:
            found.add((m.group(1), m.group(2)))
    return found


default = locations('.port/fast.txt')
gen = locations('.port/gen.txt')
real = default & gen

per_file = collections.Counter(path for path, _ in real)
phantom = collections.Counter(path for path, _ in (default - gen))

upstream = set(subprocess.run(
    ['git', 'ls-tree', '-r', '--name-only', 'v3.12.1.231'],
    capture_output=True, text=True).stdout.split('\n'))

lines = [
    '# 3.12.1 Fabric port - error work queue',
    f'# real errors: {len(real)} in {len(per_file)} files   (reported: {len(default)})',
    f'# phantom (Lombok, only in the default log): {len(default - gen)}',
    '# "real" = reported by both stop policies, so it is a genuine compile error',
    '# NOTE: this list is a LOWER BOUND - javac only checks the classes it reaches (see docs/HANDOFF.md)',
    '# format: real <tab> phantom <tab> origin <tab> path',
    '',
]
for path, count in sorted(per_file.items(), key=lambda kv: (-kv[1], kv[0])):
    origin = 'upstream-identical' if path in upstream else 'not-in-upstream'
    if path in upstream:
        same = subprocess.run(['git', 'diff', '--quiet', 'v3.12.1.231', '--', path]).returncode == 0
        origin = 'upstream-identical' if same else 'divergent'
    lines.append(f'{count}\t{phantom.get(path, 0)}\t{origin}\t{path}')

with open('docs/merge-3.12.1-workqueue.txt', 'w') as out:
    out.write('\n'.join(lines) + '\n')
print(f'real: {len(real)}  files: {len(per_file)}  reported: {len(default)}  phantom: {len(default - gen)}')
