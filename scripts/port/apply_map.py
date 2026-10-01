#!/usr/bin/env python3
"""Apply one derived symbol mapping, verify per file, and roll back what regresses.

    scripts/port/apply_map.py --from modResource --to commonResource            # dry run
    scripts/port/apply_map.py --from modResource --to commonResource --apply    # rewrite + verify

Verification is per file: each touched file is compiled on its own (with the
whole tree on the sourcepath) before and after the rewrite, and only the errors
reported *for that file* are compared. A file whose error count goes up is
restored from git. That gives every mapping an automatic safety net, which the
whole-tree error count cannot provide - see docs/HANDOFF.md 4.2.1.
"""
import argparse
import os
import re
import subprocess
import sys
import tempfile

JAVAC = os.environ.get(
    'JAVAC',
    '/Users/huangwenqin/.gradle/jdks/eclipse_adoptium-17-x86_64-os_x.2/jdk-17.0.20.1+1/Contents/Home/bin/javac',
)


def compile_file(path):
    """-> the compile output for `path` when compiled on its own"""
    cp = open('.port/compile-cp.txt').read().strip()
    ap = open('.port/ap-cp.txt').read().strip()
    with tempfile.TemporaryDirectory() as out:
        proc = subprocess.run(
            [JAVAC, '-nowarn', '-proc:full', '-Xmaxerrs', '100000', '-processorpath', ap,
             '-cp', cp, '-sourcepath', 'src/main/java', '-d', out, path],
            capture_output=True, text=True)
    return proc.stdout + proc.stderr


def symbol_errors(output, path, name):
    """Errors in this file that are about the given symbol being missing.

    The *total* per-file error count is useless for verification: a single-file compile pulls its
    dependencies in through the sourcepath, where Lombok does not run, so it is full of
    "constructor X cannot be applied" noise that has nothing to do with this rewrite. Counting the
    symbol itself is what actually answers "did the rename land".
    """
    block = re.split(r'^(?=\S+:\d+: error:|Note: )', output, flags=re.M)
    count = 0
    for chunk in block:
        if not chunk.startswith(f'{path}:'):
            continue
        if re.search(rf'symbol:\s+\S+\s+{re.escape(name)}\b', chunk):
            count += 1
    return count


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--from', dest='old', required=True)
    parser.add_argument('--to', dest='new', required=True)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--files', default='src/main/java')
    args = parser.parse_args()

    pattern = re.compile(rf'\b{re.escape(args.old)}\b')
    touched = []
    for dirpath, _dirs, files in os.walk(args.files):
        for name in files:
            if not name.endswith('.java'):
                continue
            path = os.path.join(dirpath, name)
            text = open(path, encoding='utf-8', errors='replace').read()
            if pattern.search(text):
                touched.append(path)

    print(f'{args.old} -> {args.new}: {len(touched)} files')
    for path in touched[:20]:
        print('   ', path)
    if len(touched) > 20:
        print(f'    ... and {len(touched) - 20} more')
    if not args.apply:
        print('\n(dry run; add --apply to rewrite and verify)')
        return

    # before: how many errors mention the OLD symbol; after: OLD must be gone and NEW must not be missing
    before = {path: symbol_errors(compile_file(path), path, args.old) for path in touched}
    for path in touched:
        text = open(path, encoding='utf-8', errors='replace').read()
        open(path, 'w', encoding='utf-8').write(pattern.sub(args.new, text))

    kept, reverted = [], []
    for path in touched:
        output = compile_file(path)
        old_left = symbol_errors(output, path, args.old)
        new_missing = symbol_errors(output, path, args.new)
        if old_left > 0 or new_missing > 0:
            subprocess.run(['git', 'checkout', '--', path], check=False)
            reverted.append((path, f'old={old_left} new_missing={new_missing}'))
        else:
            kept.append((path, before[path]))

    print(f'\nkept:     {len(kept)} files, {sum(n for _, n in kept)} "{args.old}" errors resolved')
    print(f'reverted: {len(reverted)} files (the new name is missing there too, or the old name survived)')
    for path, why in reverted[:10]:
        print(f'    {why}  {path}')
    print('\nnext: scripts/port/fastcompile.sh (and truecount.sh for a milestone)')


if __name__ == '__main__':
    main()
