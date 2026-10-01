#!/usr/bin/env bash
# Find files that reference Forge-only types but have NO error in the full-tree log.
#
# Why: javac stops attributing classes as soon as the compile is failing, so whole files can be
# missing from `errors.sh` output while being completely uncompilable. Verified by hand: a
# deliberate `int x = "string";` inside FluidTankBase.java produced no report at all, and
# TinkerNetwork.java had ~30 errors that never showed up. Fixing such a file is invisible in the
# error count - it only shows up later, or at runtime.
#
#   scripts/port/scanhidden.sh            # list candidates (fast, heuristic)
#   scripts/port/scanhidden.sh --check    # additionally compile each one on its own (slow)
#
# A candidate is NOT proof: the type name may only appear in a comment or javadoc. Use --check.
set -uo pipefail
cd "$(dirname "$0")/../.." || exit 1

LOG=.port/fast.txt
[[ -s "$LOG" ]] || { echo "run scripts/port/fastcompile.sh first" >&2; exit 1; }

# Forge-only type names. Keep this list to things that cannot exist on Fabric.
TOKENS='IFluidHandler|IItemHandler|IFluidHandlerItem|IEnergyStorage|IClientItemExtensions|ICapabilityProvider|ForgeCapabilities|ForgeHooks|IItemHandlerModifiable|SidedInvWrapper|EmptyFluidHandler|ItemsUpdatedCallback|S2CPacket'

python3 - "$LOG" "$TOKENS" <<'PY' > .port/hidden.txt
import re, subprocess, sys
log, tokens = sys.argv[1], sys.argv[2]
errfiles = set(re.findall(r'(src/main/java/[^:]+\.java):\d+: error:', open(log).read()))
hidden = set()
for token in tokens.split('|'):
    out = subprocess.run(['rg', '-l', '--no-heading', rf'\b{token}\b', 'src/main/java', '-g', '*.java'],
                         capture_output=True, text=True).stdout.split()
    hidden.update(f for f in out if f not in errfiles)
print('\n'.join(sorted(hidden)))
PY

echo "candidates: $(wc -l < .port/hidden.txt)   list: .port/hidden.txt"
if [[ "${1:-}" != "--check" ]]; then
  head -40 .port/hidden.txt
  exit 0
fi

JAVAC_DEFAULT=/Users/huangwenqin/.gradle/jdks/eclipse_adoptium-17-x86_64-os_x.2/jdk-17.0.20.1+1/Contents/Home/bin/javac
JAVAC="${JAVAC:-$JAVAC_DEFAULT}"
while read -r file; do
  [[ -n "$file" ]] || continue
  count=$("$JAVAC" -nowarn -proc:full -processorpath "$(cat .port/ap-cp.txt)" -Xmaxerrs 100000 \
    -cp "$(cat .port/compile-cp.txt)" -sourcepath src/main/java -d "$(mktemp -d)" "$file" 2>&1 \
    | grep -cE "^$file:[0-9]+: error:")
  [[ "$count" -gt 0 ]] && echo "$count $file"
done < .port/hidden.txt | sort -rn
