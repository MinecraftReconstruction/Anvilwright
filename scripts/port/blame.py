#!/usr/bin/env python3
"""Split the error list into *culprits* and *victims*.

A file that appears in the whole-tree log with 300 errors may have none of its own: when a class it
depends on fails, every use of that class cascades into it (measured this session: the same data
provider reported 117 errors in one whole-tree run and 0 when its own package was compiled).

So for each file in the log, compile its package with every file in that package passed explicitly -
which guarantees this file is attributed - and compare:

    culprits = errors the file has on its own        -> fix these first, they are what cascades
    victims  = only errors in the whole-tree run      -> will disappear on their own

    scripts/port/blame.py [log] [--top N] [--apply-order .port/fix-order.txt]
"""
import argparse
import collections
import glob
import os
import re
import subprocess
import sys
import tempfile

JAVAC = '/Users/huangwenqin/.gradle/jdks/eclipse_adoptium-17-x86_64-os_x.2/jdk-17.0.20.1+1/Contents/Home/bin/javac'


def whole_tree_counts(path):
    counts = collections.Counter()
    for line in open(path, encoding='utf-8', errors='replace'):
        m = re.match(r'^(src/main/java/[^ :]+\.java):\d+: error:', line)
        if m:
            counts[m.group(1)] += 1
    return counts


def package_errors(path, cp, ap):
    files = sorted(glob.glob(os.path.join(os.path.dirname(path), '*.java')))
    with tempfile.TemporaryDirectory() as out:
        proc = subprocess.run([JAVAC, '-nowarn', '-proc:full', '-Xmaxerrs', '100000',
                               '-processorpath', ap, '-cp', cp, '-sourcepath', 'src/main/java',
                               '-d', out, *files], capture_output=True, text=True)
    out = proc.stdout + proc.stderr
    return len(re.findall(rf'^{re.escape(path)}:\d+: error:', out, re.M))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('log', nargs='?', default='.port/fast.txt')
    ap.add_argument('--top', type=int, default=20)
    ap.add_argument('--order', default='.port/fix-order.txt')
    args = ap.parse_args()

    cp = open('.port/compile-cp.txt').read().strip()
    ap_cp = open('.port/ap-cp.txt').read().strip()
    counts = whole_tree_counts(args.log)

    rows = []
    for path, reported in counts.most_common(args.top):
        own = package_errors(path, cp, ap_cp)
        rows.append((path, reported, own))
        print(f'{reported:5d} 报告 / {own:4d} 自身  {"CULPRIT" if own else "victim "}  {path.split("src/main/java/")[-1]}')

    culprits = [r for r in rows if r[2] > 0]
    culprits.sort(key=lambda r: -r[2])
    with open(args.order, 'w') as out:
        for path, reported, own in culprits:
            out.write(f'{own}\t{reported}\t{path}\n')
    print(f'\nculprits: {len(culprits)}   victims: {len(rows) - len(culprits)}')
    print(f'wrote the culprit order to {args.order}')


if __name__ == '__main__':
    main()
