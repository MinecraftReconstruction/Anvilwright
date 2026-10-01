#!/usr/bin/env bash
# Compile the project, keep the whole javac log, and print the error count.
#
#   scripts/port/errors.sh [log-file]     (default: .port/errors.txt)
#
# The project needs a JDK 21 launcher; javac itself runs on the Java 17 toolchain
# declared in build.gradle (Lombok 1.18.x crashes on 21, which is why the launcher
# and the toolchain differ).
set -uo pipefail
cd "$(dirname "$0")/../.." || exit 1
export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home}"
OUT="${1:-.port/errors.txt}"
mkdir -p "$(dirname "$OUT")"
./gradlew compileJava -I scripts/port/maxerrs.gradle -Dorg.gradle.jvmargs="-Xmx6G" --offline > "$OUT" 2>&1
echo "total: $(grep -cE '\.java:[0-9]+: error:' "$OUT")   log: $OUT"
