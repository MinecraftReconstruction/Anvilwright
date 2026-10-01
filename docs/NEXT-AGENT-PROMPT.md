# 给下一个 agent 的 prompt（直接复制粘贴）

---

我们在把 **Tinkers' Construct 3.12.1（Forge）移植到 Fabric 1.20.1**。仓库在
`~/Desktop/repo/mr-tinkers`（remote：`MinecraftReconstruction/TinkersConstruct`，工作分支
**`mcr/upstream-3.12.1`**），Mantle 的 Fabric 移植在 `~/Desktop/repo/mr-mantle-fabric`
（分支 `mcr/mantle-1.11`，remote `MinecraftReconstruction/Mantle-Fabric`，版本号形如
`1.11.DEV.<git短hash>`，TCon 的 `gradle.properties` 里的 `mantle_version` 必须与之一致）。

## 硬规则（用户明确要求过，别踩）

1. **全程用中文交流**；先给结论和量化证据，再讲过程。
2. 自称 AI 的措辞**只能用**：`Unofficial, largely AI-assisted ("vibed") port.` /
   「非官方、由 AI 大幅辅助完成（"largely vibed"）的移植工程」。**绝不写 "AI-generated"**——
   上游代码是 SlimeKnights / AlphaMode 的，只有"迁移改动"是 AI 在人类设定目标、审查、验证下做的。
3. **每完成一小块就 commit 并 push 做 checkpoint**（`git push` 管道到 `tail` 会吞掉退出码，
   要用 `git ls-remote` 复核）。网络偶尔 SSL 失败，重试即可。
4. 任何**非语义等价**的改动都要登记到 `docs/BEHAVIOUR-DIFFERENCES.md`（Mantle 侧同理）。
5. 进展/方法论文档要持续更新，方便下一个 agent 接力：`docs/HANDOFF.md`（先读第 12、13 节）、
   `docs/NUMBERS.md`（数字口径与工具）、`scripts/port/README.md`。

## 现在的状态

**编译已经几乎通过**：`scripts/port/fastcompile.sh --gen .port/gen_cur.txt` 只剩 **1 条错误**：

```
FluidTransferHelper.interactWithStack / handleUIResult 不存在（TCon 的 ToolContainerMenu 等几处在用）
```

原因：这两个方法是 **Mantle 1.11（Forge）** 里的，我们的 Mantle-Fabric 移植版没搬过来。
Forge 参考实现在 `~/Desktop/repo/Mantle/src/main/java/slimeknights/mantle/fluid/FluidTransferHelper.java`
（`interactWithStack` 约 360 行、`handleUIResult` 约 491 行），Fabric 版要改成
`ContainerItemContext` + `FluidStorage.ITEM` + Mantle 现成的 `tryTransfer(Storage<FluidVariant>, Storage<FluidVariant>, ...)`。

⚠️ **口径很重要**：`fastcompile.sh` 的**默认**输出里 ~90% 是 Lombok 幻影（javac 一旦失败就看不到
Lombok 生成的成员），**只有 `--gen` 口径有意义**。`truecount.sh` 的分块口径对跨包 Lombok 类同样会造幻影。
另外：修好一个文件后**别的文件会被暴露出来**，错误数阶段性上涨是正常的，别慌。

## 你要做的事（按顺序）

1. **补 Mantle 的两个方法**（`mr-mantle-fabric/src/main/java/slimeknights/mantle/fluid/FluidTransferHelper.java`）→ 目标 0 错误。
   改完照抄这个流程：`./gradlew publishToMavenLocal --offline` → 更新 TCon `gradle.properties` 的
   `mantle_version`（当前 `1.11.DEV.0f373c6d`）→ **必须**重跑
   `./gradlew -I scripts/port/printcp.gradle printCompileCp --offline`（改 AW / 换 Mantle 后 classpath 会失效）。
2. **跑真 Gradle 构建**：`./gradlew build --offline`。javac 过了不代表 Gradle 过（datagen provider 注册、
   资源、mixin、AW 校验都可能再报）。
3. **冒烟测试**：`./gradlew runServer` 打底，再 `runClient`。重点验：
   工具定义/修饰符加载（`tinkering/tool_definitions`）、冶炼炉方块实体、盔甲与工具渲染、
   以及 `docs/BEHAVIOUR-DIFFERENCES.md` 里标"未验证/近似"的条目。
4. **迁移到 canonical 仓库**（用户的发布计划，务必照做）：
   - 建一个 **non-fork** 仓库，把 `mcr/upstream-3.12.1` 的完整历史推上去（保留 attribution）；
   - 两条分支：**带 checkpoint 的**（现有细粒度历史）与**不带 checkpoint 的**（把上百个 commit 按逻辑
     压成十几~二十块，例如：合并上游 3.12.1 / Mantle API 恢复 / 注册表补全 / 流体链式 API 重写 /
     datagen 修复 / import 级联 / 模型与盔甲 / 事件与网络 / 权限与 access widener …），
     **不带 checkpoint 的那条设为默认分支**；
   - README/ATTRIBUTION 用第 2 条的固定措辞，写明上游作者。

## 容易被坑的地方（都在文档里，先读再动手）

- **别把整树数字当进度条**：同一棵树在不同文件列表下报过 3 ↔ 3572 条。
- 合并是坏的：3.12.1 那次合并**把上游的调用点带进来了、声明没带进来**。判断"某成员是不是合并丢的"用
  `scripts/port/missing_members.py`；补齐 import 用 `scripts/port/sync_imports.py --apply` 再跟一轮
  `scripts/port/drop_broken_imports.py`（它会删掉 javac 报"不存在"的 import 行）。
- 大量 Forge 专属钩子在 Fabric 上没有对应物（`ForgeEventFactory`、`IItemHandler`、`IFluidHandler` 等），
  这些位置的替换要**记进行为差异**，不要默默删掉。
- 上游的 `META-INF/accesstransformer.cfg` 里的 public/protected 改动，需要翻译成 `src/main/resources/tinkers.accesswidener`
  的 `transitive-accessible field/method`（本轮已补 ThrownTrident、ShapedRecipe.result/width/height、
  ResourceLocation.ERROR_INVALID、FishingHook.life、LivingEntity.lastPos、CraftingMenu.slotChangedCraftingGrid 等）。

先把 `docs/HANDOFF.md` 第 12、13 节和 `docs/NUMBERS.md` 读完再开始。
