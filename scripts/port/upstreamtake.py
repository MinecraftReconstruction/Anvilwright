#!/usr/bin/env python3
"""Take upstream's version of a file, port the plumbing, verify, and roll back if it regresses.

This is the "recipe 1" that has worked best all session: when a file's errors are not a rename but a
*different structure* upstream (data providers, models, events), take upstream's file and swap only
the Forge plumbing lines, then let the compiler judge it.

    scripts/port/upstreamtake.py src/main/java/..../FluidTagProvider.java            # dry run: show diff size
    scripts/port/upstreamtake.py --apply src/main/java/..../FluidTagProvider.java    # swap + verify

Verification compiles the whole tree before and after and compares the errors *reported for that
file* - a whole-tree run is the only thing that gives a file's real errors (Lombok runs on every
source there, so there is no implicit-compilation noise).
"""
import argparse
import re
import subprocess
import sys
import tempfile

UPSTREAM = 'v3.12.1.231'
JAVAC = '/Users/huangwenqin/.gradle/jdks/eclipse_adoptium-17-x86_64-os_x.2/jdk-17.0.20.1+1/Contents/Home/bin/javac'

# Forge -> Fabric / Porting Lib, plus the renames this session has already validated.
IMPORT_MAP = {
    'net.minecraftforge.fluids.FluidStack': 'io.github.fabricators_of_create.porting_lib.fluids.FluidStack',
    'net.minecraftforge.fluids.FluidType': 'io.github.fabricators_of_create.porting_lib.fluids.FluidType',
    'net.minecraftforge.fluids.capability.templates.FluidTank': 'io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank',
    'net.minecraftforge.common.ForgeMod': 'io.github.fabricators_of_create.porting_lib.attributes.PortingLibAttributes',
    'net.minecraftforge.data.loading.DatagenModLoader': None,
    'net.minecraftforge.client.extensions.common.IClientItemExtensions': None,
}


def run_tree_compile():
    cp = open('.port/compile-cp.txt').read().strip()
    ap = open('.port/ap-cp.txt').read().strip()
    sources = open('.port/sources.txt').read().split()
    with tempfile.TemporaryDirectory() as out:
        proc = subprocess.run([JAVAC, '-nowarn', '-proc:full', '-Xmaxerrs', '100000',
                               '-processorpath', ap, '-cp', cp, '-d', out, *sources],
                              capture_output=True, text=True)
    return proc.stdout + proc.stderr


def errors_for(output, path):
    return len(re.findall(rf'^{re.escape(path)}:\d+: error:', output, re.M))


def port_imports(text):
    changed = []
    for old, new in IMPORT_MAP.items():
        if old not in text:
            continue
        if new is None:
            changed.append(f'   (needs manual attention) {old}')
            continue
        text = text.replace(old, new)
        changed.append(f'   {old} -> {new}')
    return text, changed


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('files', nargs='+')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    if not args.apply:
        for path in args.files:
            up = subprocess.run(['git', 'show', f'{UPSTREAM}:{path}'], capture_output=True, text=True)
            if up.returncode != 0:
                print(f'{path}: NOT in upstream (not a candidate)')
                continue
            text, changed = port_imports(up.stdout)
            print(f'{path}: upstream {len(up.stdout.splitlines())} lines, '
                  f'{len(text.splitlines())} after import mapping')
            for line in changed:
                print(line)
        print('\n(dry run; add --apply to write and verify)')
        return

    print('compiling baseline ...')
    baseline_output = run_tree_compile()
    kept, reverted = [], []
    for path in args.files:
        up = subprocess.run(['git', 'show', f'{UPSTREAM}:{path}'], capture_output=True, text=True)
        if up.returncode != 0:
            print(f'{path}: NOT in upstream, skipped')
            continue
        before = errors_for(baseline_output, path)
        text, _changed = port_imports(up.stdout)
        open(path, 'w', encoding='utf-8').write(text)
        after_output = run_tree_compile()
        after = errors_for(after_output, path)
        if after < before:
            kept.append((path, before, after))
            print(f'KEPT     {before} -> {after}  {path}')
        else:
            subprocess.run(['git', 'checkout', '--', path], check=False)
            reverted.append((path, before, after))
            print(f'REVERTED {before} -> {after}  {path}')
        baseline_output = after_output if kept and kept[-1][0] == path else baseline_output

    print(f'\nkept: {len(kept)}   reverted: {len(reverted)}')
    print('note: a kept file still needs the behaviour-difference check and, for data providers, a datagen run')


if __name__ == '__main__':
    main()
