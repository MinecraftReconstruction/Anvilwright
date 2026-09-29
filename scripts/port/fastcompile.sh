#!/usr/bin/env bash
# Fast standalone javac run over the whole main source set.
#
# Why this exists: `gradlew compileJava` takes ~40s per iteration, this takes ~18s.
# It compiles exactly the same file list with the same classpath and annotation
# processor path Gradle uses, so the error list is directly comparable.
#
#   scripts/port/fastcompile.sh [log]        (default: .port/fast.txt)
#
# Pass --gen as the first argument to add -XDshould-stop.ifError=GENERATE.
#
# IMPORTANT: with the default stop policy javac reports ~130 *phantom* errors
# (Lombok-generated members it cannot see once the compile is already failing).
# The --gen variant hides those phantoms but also skips some real checks, so use
# the default log as the primary metric and --gen to find the phantoms.
set -uo pipefail
cd "$(dirname "$0")/../.." || exit 1

JAVAC_DEFAULT=/Users/huangwenqin/.gradle/jdks/eclipse_adoptium-17-x86_64-os_x.2/jdk-17.0.20.1+1/Contents/Home/bin/javac
JAVAC="${JAVAC:-$JAVAC_DEFAULT}"

GEN=""
if [[ "${1:-}" == "--gen" ]]; then
  GEN="-XDshould-stop.ifError=GENERATE"
  shift
fi
OUT="${1:-.port/fast.txt}"

mkdir -p .port
if [[ ! -s .port/compile-cp.txt || ! -s .port/ap-cp.txt ]]; then
  echo "run first: ./gradlew -I scripts/port/printcp.gradle printCompileCp" >&2
  exit 1
fi

# The classpath file becomes STALE whenever Loom re-remaps the Minecraft jar (e.g. after changing
# mantle.accesswidener / tinkers.accesswidener, or after swapping the Mantle artifact). A stale file
# makes a large set of symbols unresolvable, which then makes Lombok-generated members invisible and
# inflates the error count by 4-5x. Always regenerate it after touching those inputs.
if [[ -n "$(find build.gradle gradle.properties src/main/resources/*.accesswidener \
              -newer .port/compile-cp.txt 2>/dev/null)" ]]; then
  echo "!! .port/compile-cp.txt is older than the build/access-widener inputs" >&2
  echo "   regenerate first: ./gradlew -I scripts/port/printcp.gradle printCompileCp" >&2
  exit 2
fi
find src/main/java -name '*.java' > .port/sources.txt

# shellcheck disable=SC2086
"$JAVAC" -nowarn -proc:full $GEN -Xmaxerrs 100000 \
  -processorpath "$(cat .port/ap-cp.txt)" -cp "$(cat .port/compile-cp.txt)" \
  -d "$(mktemp -d)" @.port/sources.txt > "$OUT" 2>&1

echo "total: $(grep -cE '\.java:[0-9]+: error:' "$OUT")   log: $OUT"
if [[ -z "$GEN" ]]; then
  echo "phantom (Lombok family): $(grep -cE 'symbol:   method (getVariant|isSuccess|getCost|getCraftingResult|getId|getIngredient|getSound|setPlayer|getSlotCount)\(\)' "$OUT")"
fi
