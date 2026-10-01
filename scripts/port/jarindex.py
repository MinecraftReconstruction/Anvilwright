#!/usr/bin/env python3
"""Build .port/jarclasses.txt: every class name visible on the compile classpath.

Needs .port/compile-cp.txt (run: ./gradlew -I scripts/port/printcp.gradle printCompileCp).
Both `Outer.Inner` and `Outer$Inner` spellings are written, because source imports
use dots while class files use dollars.
"""
import os
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
cp_file = os.path.join(ROOT, ".port", "compile-cp.txt")
if not os.path.exists(cp_file):
    raise SystemExit("missing .port/compile-cp.txt - run: ./gradlew -I scripts/port/printcp.gradle printCompileCp")

jars = [j for j in open(cp_file).read().strip().split(os.pathsep) if j.endswith(".jar") and os.path.exists(j)]
names = set()
for jar in jars:
    for line in subprocess.run(["unzip", "-Z1", jar], capture_output=True, text=True).stdout.split("\n"):
        if line.endswith(".class"):
            name = line[:-6].replace("/", ".")
            names.add(name)
            names.add(name.replace("$", "."))

out = os.path.join(ROOT, ".port", "jarclasses.txt")
os.makedirs(os.path.dirname(out), exist_ok=True)
open(out, "w").write("\n".join(sorted(names)))
print(f"{len(jars)} jars -> {len(names)} class names -> {out}")
