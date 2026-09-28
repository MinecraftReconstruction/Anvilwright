# 同步方案（思路）

> 性质见 [ATTRIBUTION.md](../ATTRIBUTION.md)。本文件说明**怎么做**，[STATUS.md](STATUS.md) 说明**做到哪了**。

## 总原则

1. **顺序不可颠倒：先 Mantle，后 Tinkers。** 上游 3.12.1 要求 Mantle `[1.11.113,)`，而 Fabric Mantle 只发布到 `1.9.296`。Mantle 不通，其余都是空谈。
2. **生成产物不手工合并。** 3343 个 JSON 冲突来自 datagen，正确做法是解完 Java 后重新跑数据生成。
3. **借用已有移植作为参照，而不是凭空重写。** Hephaestus（`hephaestus` remote）已经把 Forge API → Fabric/Porting Lib 的映射解过一遍。冲突调和时，优先参考它在对应文件上的写法。
4. **按子系统推进，每块跑通再进下一块。** 批量改写 700 个文件看似高效，实则会把"同一个错误犯 700 次"复制 700 遍。
5. **每步可验证。** 编译只是最低标准，行为要能跑出来。

## 阶段划分

### Phase 1 — Mantle 1.11 Fabric（当前卡点）

1. 在 `Mantle-Fabric` 的 `1.20.1-update` 分支上修完编译错误。
   根因已知：Porting Lib `2.3.16-beta.81` 移除了 Forge 的 geometry API
   （`IGeometryBakingContext`、`RenderTypeGroup`、`ForgeRenderTypes`），
   且 `IUnbakedGeometry.bake` 的上下文类型改成了 `BlockModel`，`CompositeModel.Baked.Builder.addQuads` 不再接受 render type。
   → 不能只改类型名，**必须确认渲染类型语义如何在新 API 下表达**（这是最需要人工判断的一处）。
2. 编译通过后 `./gradlew build` 出 jar，并发布（`publishToMavenLocal` 即可，或先用 `flatDir`/本地 jar 引用）。
3. 记录 Mantle 版本号策略（建议延续 `1.20.1-1.11.DEV.<sha>` 形式，与 Alpha 的 CI 一致）。

**完成标准**：`Mantle-Fabric` 的 `1.11` 分支能编译出 jar，且 1.20.1 下能启动。

#### Phase 1 实际结果（2026-09-29 更新）

Phase 1 已完成，但**结论和当初设想的"先拆依赖再改 Tinkers"不一样**，两条实测记录如下。

**1. Mantle 侧已完成**（canonical 仓库 `MinecraftReconstruction/Mantle-Fabric`，分支 `mcr/mantle-1.11`）：

- 编译错误 158 → 0；`./gradlew build` 通过（含 datagen 与 access widener 校验）
- `runServer` `Done (28.575s)`、`runClient` 启动到主菜单，服务器/客户端日志 ERROR+FATAL **都是 0**
- 发布方式已验证：`./gradlew publishToMavenLocal` 现在产出的是 **remap（intermediary）** jar。
  注意**之前是错的**：`publishing` 里写的是 `artifact jar`，而 Loom 的 `jar` 是 dev（named）jar，
  发布的文件名带 `-dev` 分类器、类引用是 `net/minecraft/commands/...`。
  下游若 `include()` 这种东西，玩家侧必然崩。已改成 `artifact remapJar` 并核实：
  `Mantle-1.20.1-1.11.DEV.<sha>.jar` 里是 `net/minecraft/class_1268`。

**2. "直接拆掉 jar-in-jar、换成 Mantle 1.11" —— 实测不可行（差 4540 个错误）**

做法：TCon 的 `gradle.properties` 改 `mantle_version=1.11.DEV.<sha>`，
`build.gradle` 里把 `modImplementation(include("slimeknights.mantle:Mantle:..."))` 去掉 `include`，
并加 `mavenLocal()`。然后 `./gradlew compileJava`。

结果：**4,540 errors / 100 warnings**（javac 完整跑完并给出了这个计数；之后 Gradle daemon 因为
GC thrashing 被自己的看门狗杀掉，所以这数字是完整的、不是被截断的）。

错误分布（按源码目录）与根因：

| 目录 | 错误数 |
|---|---|
| `library/` | 2014 |
| `tools/` | 934 |
| `smeltery/` | 698 |
| `tables/` | 293 |
| `plugin/` | 190 |
| `fluids/` | 156 |

典型根因（都是 Mantle 1.9 → 1.11 的 API 换代，不是 Fabric 适配问题）：

- `slimeknights.mantle.data.GenericLoaderRegistry` **整个包不存在了**（80 处）—— 1.11 换成了
  `slimeknights.mantle.data.loadable.*`；TCon 3.6.4 里 `IGenericLoader` 到处都是
- `mantle.client.model.data` / `client.model.inventory` / `client.model.fluid` / `data.fabric` 等包消失
- `FluidAttributes.Builder` 的注册签名变了（55 处）
- `ElementScreen` / `ScalableElementScreen` 构造函数签名变化（83 处）
- 材料统计类的构造函数（`LimbMaterialStats`/`GripMaterialStats`/`SkullStats`，共 77 处）
- `method does not override` 397 处（接口签名变更）

**结论**：Mantle 1.11 的 API 就是 TCon 3.12 时代的 API。所以顺序必须是
**先把 TCon 合到 3.12.1（Phase 2），而不是先把 Mantle 换成 1.11**。
`1.9 → 1.11` 这一步本身等价于 Phase 2 的一大半工作量，先拆依赖只会得到 4540 个错误。
jar-in-jar 的拆分应该放在 Phase 2 末尾（届时 TCon 已改到 1.11 API，只需改 build.gradle 两行 + 发布 Mantle）。

### Phase 2 — Tinkers' Construct 合并

1. 基线确认：用新 Mantle 编译**现有**端口（`1.20.1` 分支），先确保不引入新错误。
2. `git checkout -b mcr/upstream-3.12.1 && git merge v3.12.1.231`（`mcr` = MinecraftReconstruction）。
3. 先处理 9 个非 Java/JSON 冲突（build.gradle、gradle.properties 等）。
4. 处理 696 个 Java 冲突，建议按依赖顺序：
   注册表/事件 → Capability → 流体 → 工具 → 冶炼炉 → 客户端/渲染。
   每个文件都问一句：**上游这处改动是否触碰 Forge API？**（280 个是，需要重新套用转换；其余多为纯逻辑改动，通常可直接取上游版本）
5. 跑 datagen（`./gradlew datagen`，输出到 `src/generated/resources`），用生成结果覆盖 JSON 冲突。
6. 提交策略：每完成一个子系统一个 commit，commit message 注明对应上游范围，便于回溯。

**完成标准**：`./gradlew build` 通过；`src/generated` 与代码一致（datagen 后无 diff）。

### Phase 3 — 验证（不能只靠编译）

按可信度递增：

1. **编译 + datagen 无 diff**（最低）
2. **专用服务器启动**：`./gradlew runServer`，检查注册表/配方/标签加载无报错
3. **GameTest**：Fabric `fabric-gametest-api-v1` 可在无头服务器跑真实世界断言 —— 这是让 AI 能自验证的关键
4. **玩法链路**：挖矿 → 部件 → 拼装工具 → 冶炼 → 浇铸
5. **渲染比对**：截图对比（AI 能看图，可以半自动迭代）

### Phase 4 — 发布

- 版本号、Modrinth/CurseForge 页面文案（**必须**写明非官方 + largely vibed + 归属）
- 与上游的差异说明（CHANGELOG）

## 优先人工设卡的四处

1. **流体语义**：`simulate` 标志、容量计算、槽位混合、NBT 往返 —— 编译能过、运行会刷物品/掉流体
2. **Capability 生命周期**：附加/失效/缓存，表现为低频崩溃
3. **渲染类型**（正是 Mantle 当前卡住的地方）：模型半透明/裁剪行为可能静默退化
4. **批量改写的一致性**：同样的错误可能成片出现，必须靠测试而非肉眼

## 建议的工作方式（AI 智能体）

1. 先读 [STATUS.md](STATUS.md)，再读本文件的对应阶段。
2. 一次只推进一个子系统；每完成一块就更新 STATUS.md 并提交。
3. 冲突调和时**先看 `hephaestus` remote 对应文件的历史写法**，再决定取哪一侧。
4. 长耗时命令（首次 Gradle 依赖下载约 9 分钟）不要连续盲跑，先看缓存是否已就绪。
5. 任何"看起来对了"的结论都必须配一个可复现的验证命令。
