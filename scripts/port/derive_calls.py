#!/usr/bin/env python3
"""Derive the missing-symbol -> replacement mapping from upstream's own files.

The port's errors are mostly calls to fork-era helpers that 1.11 renamed. The
same call site exists in upstream `v3.12.1.231` and uses the current name, so the
mapping can be *derived* instead of guessed:

  our file, line 42:   modResource("gold")
  upstream's file:     commonResource("gold")          <- the replacement

This walks every "cannot find symbol: method/variable X" error, finds a call with
the same arity and a similar name in upstream's copy of the same file, and votes
for it. High agreement across call sites means a mechanical rename is safe.

    scripts/port/derive_calls.py [log] [--min-votes N] [--json .port/call-map.json]
"""
import collections
import difflib
import json
import os
import re
import subprocess
import sys

LOG = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith('--') else '.port/errors_full2.txt'
MIN_VOTES = 2
for arg in sys.argv:
    if arg.startswith('--min-votes='):
        MIN_VOTES = int(arg.split('=')[1])
JSON_OUT = '.port/call-map.json'
for arg in sys.argv:
    if arg.startswith('--json='):
        JSON_OUT = arg.split('=', 1)[1]

UPSTREAM = 'v3.12.1.231'
CALL_RE = re.compile(r'\b([a-zA-Z_][A-Za-z_0-9]*)\s*\(')


def upstream_text(path):
    try:
        out = subprocess.run(['git', 'show', f'{UPSTREAM}:{path}'], capture_output=True, text=True)
    except OSError:
        return None
    return out.stdout if out.returncode == 0 else None


def arity(text, index):
    """Count the arguments of the call whose '(' is at index."""
    depth, count, seen = 0, 0, False
    for ch in text[index:index + 400]:
        if ch == '(':
            depth += 1
            if depth == 1:
                continue
        elif ch == ')':
            depth -= 1
            if depth == 0:
                return count if seen else 0
        elif ch == ',' and depth == 1:
            count += 1
            seen = True
        elif depth == 1 and not ch.isspace():
            seen = True
    return None


def main():
    log = open(LOG, encoding='utf-8', errors='replace').read().split('\n')
    votes = collections.defaultdict(collections.Counter)
    examples = collections.defaultdict(list)
    unresolved = collections.Counter()

    for i, line in enumerate(log):
        m = re.match(r'^/Users/[^ :]+/(src/main/java/[^ :]+\.java):(\d+): error: cannot find symbol$', line)
        if not m:
            continue
        path, lineno = m.group(1), int(m.group(2))
        ctx = '\n'.join(log[i + 1:i + 4])
        sym = re.search(r'symbol:\s+method\s+([A-Za-z_0-9]+)\(', ctx)
        if not sym:
            sym = re.search(r'symbol:\s+variable\s+([A-Za-z_0-9]+)', ctx)
        if not sym:
            continue
        missing = sym.group(1)
        our_lines = open(path, encoding='utf-8', errors='replace').read().split('\n')
        if lineno > len(our_lines):
            continue
        source_line = our_lines[lineno - 1]
        up = upstream_text(path)
        if up is None:
            unresolved[missing] += 1
            continue

        # arity of our (failing) call
        target_arity = None
        for call in CALL_RE.finditer(source_line):
            if call.group(1) == missing:
                target_arity = arity(source_line, call.end() - 1)
                break

        best = None
        for call in CALL_RE.finditer(up):
            name = call.group(1)
            if name == missing or name in ('if', 'for', 'while', 'switch', 'return', 'catch', 'synchronized', 'new'):
                continue
            if target_arity is not None and arity(up, call.end() - 1) != target_arity:
                continue
            ratio = difflib.SequenceMatcher(None, missing, name).ratio()
            if ratio < 0.45:
                continue
            if best is None or ratio > best[1]:
                best = (name, ratio)
        if best and best[1] >= 0.6:
            votes[missing][best[0]] += 1
            if len(examples[missing]) < 3:
                examples[missing].append((path.split('src/main/java/')[-1], source_line.strip()[:80]))
        else:
            unresolved[missing] += 1

    rows = []
    for missing, counter in votes.items():
        name, count = counter.most_common(1)[0]
        if count < MIN_VOTES:
            continue
        rows.append({'missing': missing, 'replacement': name, 'votes': count,
                     'alternatives': [n for n, _ in counter.most_common()[1:3]],
                     'examples': examples[missing]})
    rows.sort(key=lambda r: -r['votes'])

    print(f'log: {LOG}')
    print(f'derived mappings: {len(rows)}   (symbols with no upstream counterpart: {len(unresolved)})')
    print(f'{"votes":>5}  {"missing":28s} -> {"replacement":28s} alternate')
    for row in rows[:30]:
        print(f'{row["votes"]:5d}  {row["missing"][:27]:28s} -> {row["replacement"][:27]:28s} {", ".join(row["alternatives"])[:30]}')
    print('\nunresolved (no upstream file / low confidence), top:')
    for name, count in unresolved.most_common(10):
        print(f'{count:5d}  {name}')

    with open(JSON_OUT, 'w') as out:
        json.dump(rows, out, indent=2)
    print(f'\nwrote {JSON_OUT}')


if __name__ == '__main__':
    main()
