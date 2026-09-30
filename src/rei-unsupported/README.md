# REI integration — parked (not compiled)

This folder holds the Fork's REI (Roughly Enough Items) recipe-viewer integration. It is **outside the
compiled source set** and its entrypoints are removed from `fabric.mod.json`, because it was written
against the pre-3.12.1 Tinkers APIs that the 3.12.1 merge replaced (about 110 errors: `ModifierNBT` /
`StatsNBT` / `ToolDefinition` / `ToolRebuildContext` / `LazyMaterial` / `ModifierTooltip` constructors,
`ModifierEntry` serialization, `SlotCount.read/write`, ...).

Nothing else in the mod references it (verified: only `TinkersDisplay` and this folder mention
`plugin.rei`). Restoring it is a self-contained port job:

1. move the tree back to `src/main/java/slimeknights/tconstruct/plugin/rei`
2. re-add the `rei_client` / `rei_common` entrypoints in `fabric.mod.json`
3. port the calls to the 3.12.1 APIs (the JEI plugin under `plugin/jei` in the main source set is
   upstream's 3.12.1 code and is the reference for every construct that changed)

Tracked in `docs/BEHAVIOUR-DIFFERENCES.md`.
