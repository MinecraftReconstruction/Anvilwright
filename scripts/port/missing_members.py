#!/usr/bin/env python3
"""Find "cannot find symbol" errors whose member exists in upstream but not in our tree.

The port is a merge of upstream 3.12.1 (Forge) into an older Fabric port (Hephaestus). That merge
dropped hunks: `ItemTagProvider` calls `moltenTools(...)` 19 times while the declaration only exists
upstream, `ModifiableLauncherItem` uses `KEY_DRAWBACK_AMMO` while upstream declares it at line 76.
The call sites came from upstream, the declarations did not.

So for every `cannot find symbol` in the javac log: locate the class named in `location:` in OUR
tree. If our file never mentions that member, the merge dropped it - and we print the upstream
declaration so it can be restored (with the Forge-only parts translated) instead of guessed.

Rows are sorted by how many errors each missing member accounts for, so the biggest wins come first.

    scripts/port/missing_members.py [log] [--upstream DIR] [--ours DIR] [--out FILE]
"""
import argparse
import os
import re
import sys
from collections import defaultdict

DECL_RE = re.compile(
    r'^(?:public|protected|private|static|final|abstract|default|transient|volatile|'
    r'synchronized|@\w+(?:\([^)]*\))?| )*'
    r'(?:[\w.<>,?\[\]\s]+?)\s+(\w+)\s*(\(|=|;)')

CLASS_RE = re.compile(r'^\s*(?:@\w+(?:\([^)]*\))?\s*)*'
                      r'(?:public|protected|private|static|final|abstract|sealed|non-sealed|strictfp|\s)*'
                      r'(?:class|interface|enum|record)\s+(\w+)')


def index_upstream(root):
    """simple class name -> list of (path, lines)"""
    classes = defaultdict(list)
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith('.java'):
                continue
            path = os.path.join(dirpath, name)
            try:
                lines = open(path, encoding='utf-8', errors='replace').read().splitlines()
            except OSError:
                continue
            for line in lines:
                m = CLASS_RE.match(line)
                if m:
                    classes[m.group(1)].append((path, lines))
    return classes


def upstream_has(simple, member, kind):
    """Does any upstream class named `simple` declare `member`?"""
    hits = []
    for path, lines in SIMPLE.get(simple, ()):
        pat = re.compile(r'\b' + re.escape(member) + r'\b')
        for i, line in enumerate(lines, 1):
            if not pat.search(line):
                continue
            stripped = line.strip()
            if kind == 'variable':
                if re.match(r'^(?:[\w@]+\s+)*[\w.<>,?\[\]]+\s+' + re.escape(member) + r'\s*[=;]', stripped):
                    hits.append(f'{path}:{i}: {stripped}')
            else:
                if re.search(r'\b' + re.escape(member) + r'\s*\(', stripped) and (
                        '(' in stripped and stripped.endswith(';') or '{' in stripped):
                    if re.match(r'^(?:public|protected|private|static|final|abstract|default|\s|@)+[\w.<>,?\[\]\s]*\b'
                                + re.escape(member) + r'\s*\(', stripped):
                        hits.append(f'{path}:{i}: {stripped}')
    return hits


def parse_log(path):
    """yield (symbol_kind, symbol_name, location_class)"""
    text = open(path, encoding='utf-8', errors='replace').read()
    # each error block: ... error: cannot find symbol \n <source line> \n symbol: <kind> <name>\n location: ...
    for block in re.split(r'\n(?=\S.*\.java:\d+: error:)', text):
        if 'cannot find symbol' not in block:
            continue
        m_sym = re.search(r'^\s*symbol:\s+(method|variable|class|constructor)\s+(\S+)', block, re.M)
        m_loc = re.search(r'^\s*location:\s+(?:variable \w+ of type|class|interface)\s+([\w.]+)', block, re.M)
        if not m_sym or not m_loc:
            continue
        kind, name = m_sym.group(1), m_sym.group(2)
        name = re.sub(r'\(.*', '', name).strip()
        cls = m_loc.group(1).split('.')[-1]
        yield kind, name, cls


def main():
    global SIMPLE
    ap = argparse.ArgumentParser()
    ap.add_argument('log', nargs='?', default='.port/fast.txt')
    ap.add_argument('--upstream', default=os.path.expanduser('~/Desktop/repo/TinkersConstruct/src/main/java'))
    ap.add_argument('--ours', default='src/main/java')
    ap.add_argument('--out', default='.port/missing-members.txt')
    args = ap.parse_args()

    SIMPLE = index_upstream(args.upstream)
    ours = index_upstream(args.ours)
    impact = defaultdict(int)
    for kind, name, cls in parse_log(args.log):
        impact[(kind, name, cls)] += 1

    rows = []
    for (kind, name, cls), count in impact.items():
        our_files = ours.get(cls)
        if not our_files:
            continue  # the location class is not ours (vanilla / Porting Lib / Mantle)
        # if our file mentions the member, it is a resolution problem or an inherited member,
        # not a dropped declaration - skip it
        if any(re.search(r'\b' + re.escape(name) + r'\b', '\n'.join(lines))
               for _path, lines in our_files):
            continue
        hits = upstream_has(cls, name, kind)
        if not hits:
            continue
        rows.append((count, cls, kind, name, hits))

    rows.sort(key=lambda r: (-r[0], r[1], r[3]))
    with open(args.out, 'w') as out:
        for count, cls, kind, name, hits in rows:
            out.write(f'## {count:4d}x  {cls}.{name} ({kind})  -> our tree never mentions it\n')
            for h in hits[:3]:
                out.write(f'   {h}\n')
    print(f'{len(rows)} dropped declarations, {sum(r[0] for r in rows)} error occurrences -> {args.out}')
    for count, cls, kind, name, _ in rows[:50]:
        print(f'  {count:4d}x  {cls}.{name} ({kind})')


if __name__ == '__main__':
    SIMPLE = {}
    main()
