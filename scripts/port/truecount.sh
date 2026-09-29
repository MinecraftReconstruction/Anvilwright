#!/usr/bin/env bash
# Count the errors of the whole source set RELIABLY by compiling it in chunks.
#
# Why: javac stops attributing classes as soon as the compilation is failing, and where it stops
# depends on the file list/order. Measured on this repo: the same tree reported 5 errors when three
# unrelated files were left out of the list, and 3572 when they were included. Any "total errors"
# number from a single whole-tree run is therefore arbitrary.
#
# This script compiles each "chunk" (a package directory) with all of its files passed explicitly, so
# everything in that chunk is attributed, then sums the errors reported *for files in that chunk*.
# Errors reported for files outside the chunk are ignored (they belong to another chunk's run and
# would be counted twice), which also filters the cross-chunk Lombok artefacts.
#
#   scripts/port/truecount.sh [log]        # default: .port/true.txt   (~10 minutes)
set -uo pipefail
cd "$(dirname "$0")/../.." || exit 1

JAVAC_DEFAULT=/Users/huangwenqin/.gradle/jdks/eclipse_adoptium-17-x86_64-os_x.2/jdk-17.0.20.1+1/Contents/Home/bin/javac
JAVAC="${JAVAC:-$JAVAC_DEFAULT}"
CP="$(cat .port/compile-cp.txt)"
AP="$(cat .port/ap-cp.txt)"
OUT="${1:-.port/true.txt}"

mkdir -p .port/chunks
: > "$OUT"

# chunks = every directory that directly contains java sources
find src/main/java -name '*.java' -exec dirname {} \; | sort -u > .port/chunkdirs.txt

total=0
while read -r dir; do
  files=$(find "$dir" -maxdepth 1 -name '*.java')
  [[ -n "$files" ]] || continue
  log=.port/chunks/$(echo "$dir" | tr '/' '_').txt
  # shellcheck disable=SC2086
  "$JAVAC" -nowarn -proc:full -Xmaxerrs 100000 -processorpath "$AP" -cp "$CP" \
    -sourcepath src/main/java -d "$(mktemp -d)" $files > "$log" 2>&1
  n=$(grep -cE "^$dir/[^/]+\.java:[0-9]+: error:" "$log" || true)
  if [[ "$n" -gt 0 ]]; then
    printf '%4d  %s\n' "$n" "$dir" >> "$OUT"
    total=$((total + n))
  fi
done < .port/chunkdirs.txt

sort -rn -o "$OUT" "$OUT"
echo "true errors: $total   log: $OUT   chunks: $(wc -l < .port/chunkdirs.txt)"
echo "worst chunks:"
head -10 "$OUT"
