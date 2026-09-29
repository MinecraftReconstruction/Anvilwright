#!/usr/bin/env python3
"""Cluster a javac log into *symbol families* and propose replacements for each.

The port's remaining errors are dominated by a handful of helpers that the 1.11
line renamed (modResource -> commonResource, getRegistryName -> getKey, ...),
so the work is "one mapping per family" rather than "one fix per error".

    scripts/port/families.py [log] [--top N] [--json .port/family-map.json]

For every missing symbol it prints the error count and the best replacement
candidates. Candidates are taken from the *supertype chain of the calling class*
(resolved from our own sources and the Mantle sources), because a helper missing
from a data provider is almost always a method on that provider's base class.
Falling back to the global name pool when the chain is unknown.

Nothing here edits files: the output is meant to be reviewed, then applied by
`scripts/port/applyfamily.py`.
"""
import collections
import difflib
import json
import os
import re
import sys

LOG = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith('--') else '.port/errors_full2.txt'
TOP = 25
for arg in sys.argv:
    if arg.startswith('--top='):
        TOP = int(arg.split('=')[1])
JSON_OUT = '.port/family-map.json'
for arg in sys.argv:
    if arg.startswith('--json='):
        JSON_OUT = arg.split('=', 1)[1]

ROOT = os.getcwd()

CLASS_RE = re.compile(r'^(?:public |protected |private |abstract |final |static |sealed |non-sealed )*'
                      r'(class|interface|enum|record)\s+([A-Za-z_0-9]+)(?:<[^>]*>)?(?:\s+extends\s+([A-Za-z_0-9.<>,\[\] ]+?))?'
                      r'(?:\s+implements\s+([A-Za-z_0-9.<>,\[\] ]+?))?\s*\{', re.M)
METHOD_RE = re.compile(r'^\s*(?:public |protected |private |static |final |abstract |default |synchronized |native )*'
                       r'(?:<[^>]+>\s*)?[A-Za-z_0-9.<>,\[\]\? ]+?\s+([a-zA-Z_][A-Za-z_0-9]*)\s*\(([^)]*)\)\s*(?:throws [^{]+)?[{;]', re.M)
FIELD_RE = re.compile(r'^\s*(?:public |protected |private |static |final |volatile |transient )*'
                      r'[A-Za-z_0-9.<>,\[\]\? ]+?\s+([a-zA-Z_][A-Za-z_0-9]*)\s*(?:=|;)', re.M)


def source_dirs():
    dirs = ['src/main/java']
    for extra in ('../mr-mantle-fabric/src/main/java', '../mr-mantle/src/main/java'):
        if os.path.isdir(extra):
            dirs.append(extra)
    return dirs


def load_model(dirs):
    """Map simple class name -> {'file':..., 'extends':..., 'methods':..., 'fields':...}"""
    model = collections.defaultdict(lambda: {'files': [], 'extends': [], 'methods': set(), 'fields': set()})
    for base in dirs:
        for dirpath, _dirnames, filenames in os.walk(base):
            for name in filenames:
                if not name.endswith('.java'):
                    continue
                path = os.path.join(dirpath, name)
                try:
                    text = open(path, encoding='utf-8', errors='replace').read()
                except OSError:
                    continue
                simple = name[:-5]
                entry = model[simple]
                entry['files'].append(path)
                entry['methods'].update(METHOD_RE.findall(text)[i][0] for i in range(len(METHOD_RE.findall(text))))
                entry['fields'].update(FIELD_RE.findall(text))
                for _kind, cls, ext, impl in CLASS_RE.findall(text):
                    if cls != simple:
                        continue
                    for sup in (ext or '').split(','):
                        sup = sup.strip().split('<')[0].strip()
                        if sup:
                            entry['extends'].append(sup)
                    for sup in (impl or '').split(','):
                        sup = sup.strip().split('<')[0].strip()
                        if sup:
                            entry['extends'].append(sup)
    return model


def parse_log(path):
    """-> list of (file, line, message, symbol_text)"""
    lines = open(path, encoding='utf-8', errors='replace').read().split('\n')
    out = []
    for i, line in enumerate(lines):
        m = re.match(r'^(/Users/[^ :]+/src/main/java/[^ :]+\.java):(\d+): error: (.*)$', line)
        if not m:
            continue
        ctx = '\n'.join(lines[i + 1:i + 5])
        sym = re.search(r'symbol:\s+(?:method|variable|class|interface)\s+(\S+)', ctx)
        loc = re.search(r'location:\s+\S+\s+([\w$.]+)', ctx)
        out.append((m.group(1), int(m.group(2)), m.group(3), sym.group(1) if sym else '', loc.group(1) if loc else ''))
    return out


def chain_for(owner, model):
    """Supertype chain (simple names) of the given class, from our sources."""
    seen, stack, chain = set(), [owner], []
    while stack:
        name = stack.pop()
        name = name.split('<')[0].strip()
        if not name or name in seen:
            continue
        seen.add(name)
        chain.append(name)
        entry = model.get(name)
        if entry:
            stack.extend(entry['extends'])
    return chain


def candidates(symbol, owners, model):
    """Best replacement candidates for a missing symbol."""
    pool = []
    for owner in owners:
        for cls in chain_for(owner, model)[1:]:  # skip the class itself
            entry = model.get(cls)
            if entry:
                pool.extend(entry['methods'] | entry['fields'])
    pool = sorted(set(pool))
    if not pool:
        for entry in model.values():
            pool.extend(entry['methods'] | entry['fields'])
        pool = sorted(set(pool))
    return difflib.get_close_matches(symbol, pool, n=3, cutoff=0.35)


def main():
    model = load_model(source_dirs())
    entries = parse_log(LOG)

    families = collections.defaultdict(lambda: {'count': 0, 'owners': set(), 'files': set(), 'kind': ''})
    for path, line, msg, symbol, loc in entries:
        if not symbol:
            continue
        kind = 'method' if '(' in symbol else ('field' if symbol[:1].islower() else 'type')
        name = symbol.split('(')[0]
        key = (name, kind)
        fam = families[key]
        fam['count'] += 1
        fam['files'].add(path)
        fam['kind'] = kind
        owner = loc.split('.')[-1] if loc else os.path.basename(path)[:-5]
        if owner:
            fam['owners'].add(owner)

    rows = []
    for (name, kind), fam in sorted(families.items(), key=lambda kv: -kv[1]['count']):
        cands = candidates(name, fam['owners'], model) if kind in ('method', 'field') else []
        rows.append({
            'symbol': name,
            'kind': kind,
            'count': fam['count'],
            'files': len(fam['files']),
            'owners': sorted(fam['owners'])[:4],
            'candidates': cands,
        })

    print(f'log: {LOG}')
    print(f'families: {len(rows)}   errors covered by a family: {sum(r["count"] for r in rows)}')
    print(f'{"count":>5} {"files":>5}  {"symbol":38s} {"owners":30s} candidates (from supertype chain)')
    for row in rows[:TOP]:
        owners = ','.join(row['owners'])[:29]
        cands = ', '.join(row['candidates']) or ('-' if row['kind'] == 'type' else '(no candidate)')
        print(f'{row["count"]:5d} {row["files"]:5d}  {row["symbol"][:37]:38s} {owners:30s} {cands[:60]}')

    with open(JSON_OUT, 'w') as out:
        json.dump(rows, out, indent=2)
    print(f'\nwrote {JSON_OUT}')


if __name__ == '__main__':
    main()
