#!/usr/bin/env python3
"""Mechanically repair the fallout of the 3.12.1 merge.

Everything here is dry-run by default; pass --apply to write.

Passes
  1. restore imports    "cannot find symbol: class X" at an import-less use site,
                        where exactly one class named X exists in the sources/jars
  2. rewrite imports    import points at a class that no longer exists, and exactly
                        one class with that simple name exists elsewhere
  3. drop imports       import points at a class that no longer exists and the file
                        never uses the simple name

Deliberate limits, learned the hard way:
  * never propose net.minecraftforge.* / mezz.jei.api.forge.* (Forge-only names)
  * never propose a same-package or self import
  * a rewrite must keep the segment count and the first three package segments
    (otherwise `AttributeModifier.Operation` gets "fixed" to
    `com.llamalad7...Operation`)
  * ambiguous simple names (two JEI/REI helpers, JDK vs Mojang clashes) are skipped

Inputs: .port/errors.txt (scripts/port/errors.sh) and .port/jarclasses.txt
(scripts/port/jarindex.py). The jar index is optional but strongly recommended.
"""
import collections
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
LOG = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("-") else os.path.join(ROOT, ".port", "errors.txt")
APPLY = "--apply" in sys.argv
MANTLE = os.path.join(os.path.dirname(ROOT), "mr-mantle-fabric")  # sibling checkout

GOOD_PREFIX = (
    "net.minecraft.", "java.", "javax.", "com.google.", "com.mojang.", "it.unimi.",
    "lombok.", "io.github.fabricators_of_create.porting_lib.", "net.fabricmc.",
    "slimeknights.tconstruct.", "slimeknights.mantle.", "org.jetbrains.annotations.",
)
BAD_PREFIX = (
    "net.minecraftforge.", "mezz.jei.api.forge.", "dev.architectury.",
    "slimeknights.mantle.client.model.data.",  # dropped in Mantle 1.11
)
PREFER = {
    "FluidStack": "io.github.fabricators_of_create.porting_lib.fluids.FluidStack",
    "Supplier": "java.util.function.Supplier",
    "Function": "java.util.function.Function",
    "Nullable": "javax.annotation.Nullable",
    "ItemLike": "net.minecraft.world.level.ItemLike",
    "Component": "net.minecraft.network.chat.Component",
    "RecipeType": "net.minecraft.world.item.crafting.RecipeType",
    "Slot": "net.minecraft.inventory.Slot",
    "Getter": "lombok.Getter",
    "Setter": "lombok.Setter",
    "Accessors": "lombok.experimental.Accessors",
    "RequiredArgsConstructor": "lombok.RequiredArgsConstructor",
    "ToString": "lombok.ToString",
}


def rel(path):
    marker = "/src/main/java/"
    return path.split(marker, 1)[1] if marker in path else path[len("src/main/java/"):]


def is_good(fqn):
    return fqn.startswith(GOOD_PREFIX) and not fqn.startswith(BAD_PREFIX)


def same_package_or_self(path, fqn):
    own = rel(path)[:-5].replace("/", ".")
    own_pkg, own_class = own.rsplit(".", 1)
    return fqn.rsplit(".", 1)[0] == own_pkg or own_class in fqn.split(".")[:-1]


def main():
    sources = subprocess.run(["bash", "-c", f"cd {ROOT} && find src/main/java -name '*.java'"],
                             capture_output=True, text=True).stdout.split()
    own = set()
    by_class = collections.defaultdict(list)
    for path in sources:
        fqn = rel(path)[:-5].replace("/", ".")
        own.add(fqn)
        by_class[fqn.rsplit(".", 1)[1]].append(fqn)
    if os.path.isdir(MANTLE):  # Mantle's classes are a jar dependency, not part of this tree
        for path in subprocess.run(["bash", "-c", f"cd {MANTLE} && find src/main/java -name '*.java'"],
                                   capture_output=True, text=True).stdout.split():
            fqn = path.split("src/main/java/", 1)[1][:-5].replace("/", ".")
            own.add(fqn)
            by_class[fqn.rsplit(".", 1)[1]].append(fqn)
    jar = set()
    jar_file = os.path.join(ROOT, ".port", "jarclasses.txt")
    if os.path.exists(jar_file):
        jar = set(open(jar_file).read().split("\n"))

    # --- parse the log: file -> {(line, import-or-None, simple name)} ---------------
    lines = open(LOG).read().split("\n")
    import_errors = collections.defaultdict(list)   # import points at a missing class
    symbol_errors = collections.defaultdict(set)    # class used with no import at all
    dead_imports = collections.defaultdict(set)
    cur = None
    idx = 0
    while idx < len(lines):
        head = re.match(r"^(/[^:]+\.java):(\d+): error: (.*)$", lines[idx])
        if head:
            cur = (head.group(1), int(head.group(2)), head.group(3))
            if head.group(3).startswith("cannot find symbol"):
                code = lines[idx + 1] if idx + 1 < len(lines) else ""
                sym = None
                for probe in range(idx + 1, min(idx + 4, len(lines))):
                    found = re.match(r"^\s*symbol:\s+class (\w+)", lines[probe])
                    if found:
                        sym = found.group(1)
                imported = re.match(r"^\s*import\s+([\w.]+)\.(\w+);", code)
                if sym and imported and imported.group(2) == sym:
                    import_errors[cur[0]].append((cur[1], imported.group(1) + "." + sym, sym))
                elif sym:
                    symbol_errors[cur[0]].add(sym)
        idx += 1

    changed = 0

    # --- pass 1 + 2: missing imports and moved imports -----------------------------
    for path, items in list(import_errors.items()):
        if not os.path.exists(path):
            continue
        src = open(path).read()
        src_lines = src.split("\n")
        drop = set()
        add = []
        for line_no, fqn, simple in items:
            body = "\n".join(l for l in src_lines if not l.strip().startswith("import "))
            exists = fqn in own or fqn in jar
            if fqn.startswith("slimeknights.") and fqn not in own and fqn not in jar:
                exists = False
            if exists:
                continue
            if not re.search(r"\b" + re.escape(simple) + r"\b", body):  # pass 3
                drop.add(line_no)
                continue
            candidates = {c for c in by_class.get(simple, []) if c != fqn and c in own and is_good(c)}
            if not candidates and fqn.startswith(("net.minecraft.", "io.github.fabricators_of_create.", "net.fabricmc.")):
                candidates = {c for c in jar if c.endswith("." + simple)}
            candidates = {c for c in candidates
                          if not same_package_or_self(path, c)
                          and len(c.split(".")) == len(fqn.split("."))
                          and c.split(".")[:3] == fqn.split(".")[:3]}
            if len(candidates) == 1:
                target = candidates.pop()
                src_lines = [l.replace(f"import {fqn};", f"import {target};") for l in src_lines]
                print(f"  rewrite  {os.path.basename(path)}: {fqn} -> {target}")
                changed += 1
            else:
                print(f"  leave    {os.path.basename(path)}: {fqn}  (candidates: {sorted(candidates)[:3] or 'none'})")
        if drop:
            print(f"  drop     {os.path.basename(path)}: {len(drop)} unused import(s)")
            src_lines = [l for n, l in enumerate(src_lines, 1) if n not in drop]
            changed += len(drop)
        if APPLY:
            open(path, "w").write("\n".join(src_lines))

    # --- pass 1: symbols with no import at all -------------------------------------
    for path, syms in symbol_errors.items():
        if not os.path.exists(path):
            continue
        src_lines = open(path).read().split("\n")
        existing = {l.strip() for l in src_lines if l.startswith("import ")}
        add = []
        for simple in sorted(syms):
            if any(e.endswith("." + simple + ";") for e in existing):
                continue
            if simple in PREFER and (PREFER[simple] in own or PREFER[simple] in jar):
                add.append(PREFER[simple])
                continue
            candidates = [c for c in by_class.get(simple, []) if is_good(c) and not same_package_or_self(path, c)]
            if not candidates:
                continue
            if len(candidates) == 1:
                add.append(candidates[0])
            else:
                print(f"  leave    {os.path.basename(path)}: {simple}  (ambiguous: {sorted(candidates)[:3]})")
        if not add:
            continue
        anchor = max(n for n, l in enumerate(src_lines) if l.startswith("import ") or l.startswith("package "))
        for fqn in sorted(add):
            print(f"  add      {os.path.basename(path)}: import {fqn};")
        if APPLY:
            src_lines = src_lines[:anchor + 1] + [f"import {f};" for f in sorted(add)] + src_lines[anchor + 1:]
            open(path, "w").write("\n".join(src_lines))
        changed += len(add)

    print(f"{changed} change(s); {'applied' if APPLY else 'dry run, pass --apply to write'}")


if __name__ == "__main__":
    main()
