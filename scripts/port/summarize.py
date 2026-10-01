#!/usr/bin/env python3
"""Group a javac error log by missing package / missing symbol so the remaining
work can be attacked family by family instead of file by file.

  python3 scripts/port/summarize.py [.port/fast.txt] [--files N]
"""
import collections
import re
import sys

path = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith('--') else '.port/fast.txt'
lines = open(path).read().split('\n')

errfiles = collections.Counter()
symbols = collections.Counter()
packages = collections.Counter()
kind = collections.Counter()

for i, line in enumerate(lines):
    m = re.match(r'^(?:/Users/[^ :]+/)?(src/main/java/[^ :]+\.java):(\d+): error: (.*)$', line)
    if not m:
        continue
    errfiles[m.group(1)] += 1
    msg = m.group(3)
    kind[msg.split(':')[0][:60]] += 1
    ctx = '\n'.join(lines[i + 1:i + 4])
    sm = re.search(r'symbol:\s+(?:method|class|variable|interface)\s+(\S+)', ctx)
    if sm:
        symbols[sm.group(1)] += 1
    pk = re.search(r'package ([a-z0-9_.]+) does not exist', msg)
    if pk:
        packages[pk.group(1)] += 1

total = sum(errfiles.values())
print(f'total errors: {total}   files: {len(errfiles)}')
print(f'\n== missing packages ==')
for k, v in packages.most_common(30):
    print(f'{v:5d}  {k}')
print(f'\n== missing symbols ==')
for k, v in symbols.most_common(35):
    print(f'{v:5d}  {k}')
print(f'\n== error kinds ==')
for k, v in kind.most_common(12):
    print(f'{v:5d}  {k}')
print(f'\n== worst files ==')
for k, v in errfiles.most_common(int(next((a.split("=")[1] for a in sys.argv if a.startswith('--files=')), 25))):
    print(f'{v:5d}  {k}')
