#!/usr/bin/env python3
"""Copy the imports our files lost in the merge from the matching upstream file.

The 3.12.1 merge kept upstream's *bodies* but dropped a lot of headers, so a file often reports
"package X does not exist" / "cannot find symbol: class Y" purely because the import is gone while
the upstream file at the same path has it. This copies every upstream import that our file lacks,
skipping the Forge-only ones (those would not resolve on Fabric and need a real port).

    scripts/port/sync_imports.py [--apply] [--upstream DIR]

Without --apply it only reports what it would add.
"""
import argparse
import os
import sys

SKIP = ('import net.minecraftforge.', 'import org.jetbrains', 'import com.google.errorprone')


def imports_of(path):
    out = []
    for line in open(path, encoding='utf-8', errors='replace'):
        if line.startswith('import '):
            out.append(line.rstrip('\n'))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', action='store_true')
    ap.add_argument('--ours', default='src/main/java')
    ap.add_argument('--upstream',
                    default=os.path.expanduser('~/Desktop/repo/TinkersConstruct/src/main/java'))
    ap.add_argument('--only', default='', help='only files whose path contains this substring')
    args = ap.parse_args()

    changed = 0
    for dirpath, _dirs, files in os.walk(args.ours):
        for name in files:
            if not name.endswith('.java'):
                continue
            ours = os.path.join(dirpath, name)
            rel = os.path.relpath(ours, args.ours)
            if args.only and args.only not in rel:
                continue
            theirs = os.path.join(args.upstream, rel)
            if not os.path.exists(theirs):
                continue
            have = set(imports_of(ours))
            missing = [imp for imp in imports_of(theirs)
                       if imp not in have and not imp.startswith(SKIP)]
            if not missing:
                continue
            if not any(l.startswith('import ') for l in open(ours).read().split('\n')):
                print(f'{rel}: no imports block, skipped')
                continue
            print(f'{rel}: +{len(missing)} imports')
            for imp in missing:
                print(f'    {imp}')
            changed += 1
            if args.apply:
                lines = open(ours, encoding='utf-8').read().split('\n')
                last = max(i for i, l in enumerate(lines) if l.startswith('import '))
                lines[last + 1:last + 1] = missing
                open(ours, 'w', encoding='utf-8').write('\n'.join(lines))
    print(f'{changed} files would change' + ('' if not args.apply else ' (applied)'))


if __name__ == '__main__':
    main()
