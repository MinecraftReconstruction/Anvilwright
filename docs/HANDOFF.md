# 接力文件 —— Tinkers' Construct 3.12.1 → Fabric

> 最后更新：2026-09-29　|　分支 `mcr/upstream-3.12.1`　|　当时 HEAD `57a29656df`　|　**剩余 462 条编译错误**
> 性质见 [ATTRIBUTION.md](../ATTRIBUTION.md)。本文件是**给下一个接手的人 / AI 智能体**的入口，
> 细节都在 [MERGE-3.12.1.md](MERGE-3.12.1.md)（按时间倒序的日志 + 方法论）里。

## 0. 三句话讲清楚

1. **现状**：Tinkers' Construct（Fabric 版，源自 Alpha-s-Stuff 的 Hephaestus）已经**合并了上游 3.12.1 的整棵源码树**，
   正在逐个把 Forge API 换成 Fabric / Porting Lib。**目前编译不过（462 条错误），所以还不能热测试。**
2. **你的任务**：把 `compileJava` 的错误数降到 0 → 跑 `runServer` 热测试 → 按 [BEHAVIOUR-DIFFERENCES.md](BEHAVIOUR-DIFFERENCES.md)
   逐条验证，然后才谈发布。
3. **规矩**：每个里程碑 **commit 并 push**；任何非语义等价改动都要登记到行为差异表。

## 1. 协作约定（用户明确要求过，别踩）

- **每个 checkpoint 必须 commit + push。** 注意 `git push | tail -2` **永远返回 0**，要用 `git ls-remote` 复核。
- 中文沟通，先给结论 + 量化证据（错误数、文件数），少铺垫。
- **AI 参与的表述**必须是：`Unofficial, largely AI-assisted ("vibed") port.` /
  「非官方、由 AI 大幅辅助完成（"largely vibed"）的移植工程」。
  ⚠️ **不要**写成"整个项目是 AI 生成的"——上游代码是 SlimeKnights / AlphaMode 的，只有**迁移改动**是 AI 做的。
- 保留原作者 attribution；将来开 non-fork canonical 仓库时要**尽量带完整 git history**。
- 任何行为差异（不是语义等价替换的改动）都要写进 [BEHAVIOUR-DIFFERENCES.md](BEHAVIOUR-DIFFERENCES.md)，现在是 15 条。
- 进度/方法论要持续更新到 `docs/`，方便下一个 agent 接力（本文件 + MERGE-3.12.1.md）。

## 2. 环境与构建

- Gradle launcher 用 **JDK 21**（`/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`）；
  javac 走 build.gradle 里声明的 **Java 17 toolchain**。两者不同是必须的：Lombok 1.18.x 在 JDK 21 上会崩。
- 网络不稳，命令加 `--offline`。
- 用 [scripts/port/errors.sh](../scripts/port/errors.sh) 跑编译：它带上 `-Xmaxerrs 100000`（**不加的话 javac 到 100 条就截断**），
  并把完整日志写到 `.port/errors.txt`。
- ⚠️ **最容易误判进度的坑**：某个文件的父类/接口解析失败时，javac 会**跳过它后面的方法体检查**，单文件错误数会**少报**。
  实例：`ToolEvents` 只报 1 条错，实际有 9 处 hook API 不匹配；`ModifierLootingHandler` 报 **0** 条错，里面躺着
  `LivingEntity target = event.getEntity();` 这种根本编译不过的残留代码。
  ⇒ **每改一版都要看总数**，不要盯单文件。

## 3. 仓库拓扑

| 本地路径 | 仓库 | 说明 |
|---|---|---|
| `~/Desktop/repo/mr-tinkers` | `MinecraftReconstruction/TinkersConstruct` | 本仓库。remotes：`origin`(本仓库) / `hephaestus`(Alpha-s-Stuff 的 Fabric 移植来源) / `upstream`(SlimeKnights) |
| `~/Desktop/repo/mr-mantle-fabric` | `MinecraftReconstruction/Mantle-Fabric` | Fabric 版 Mantle，**canonical**。分支 `mcr/mantle-1.11`，已完成并验证（编译 + `runServer` + 自测 9 项） |
| `~/Desktop/repo/mr-mantle` | `SlimeKnights/Mantle` 的 fork | 只作 diff 基准 |

TCon 通过 `gradle.properties` 的 `mantle_version=1.11.DEV.ad2e7db0` 依赖 mavenLocal 里的 Mantle 制品。
**如果 Mantle 有改动，必须重新 `publishToMavenLocal` 并同步 bump 这个版本号。**

## 4. 进度与数字口径

- 错误数轨迹（每次测量都是"改一批 → 重测"）：`4540 → … → 1736 → 1069 → 848 → 790 → 534 → 462`（最新）。
- **⚠️ 462 这个数字里混着一批假错误**（Lombok 那一族，约 28 条，见第 7 节），所以它不等于"真实剩余难度"。
- 当前工作队列（按文件、按错误数、带"是否上游文件"标记）：
  [merge-3.12.1-workqueue.txt](merge-3.12.1-workqueue.txt)（脚本生成，别手改）。

## 5. 已验证的移植配方（照这个来，收益最高）

### 配方 1：**上游文件 + 只换管道**（收益最高）

如果上游那个文件用 Forge 只是为了**管道**（注册表、`RegistryObject`、datagen 生成器、事件总线注册、
`ExistingFileHelper`），就直接**取上游版本**，然后把那几行换成 Porting Lib / Fabric。实测：
`TinkerModifiers` −51、`TinkerTables` 等 −46、datagen providers −41、`TinkerModule` −32、
`TinkerStructures`/worldgen −41、**`ToolEvents`（591 行）一次编译就 0 错误**。

### 配方 2：**逐文件移植**（上游用 Forge 做**逻辑**时）

事件、capability、`IClientItemExtensions`、流体动作这类，只能逐文件改。
实测反例：把 79 个这类 hook 文件整体换成上游版本 = **+123 条错误**（已回滚）；
把 299 个"保留我方"的文件整体换掉 = **≥3472 条错误 + OOM**（已回滚）。

### 配方 3：写之前先查三件事

1. **Porting Lib 有没有现成的**：先看 `port_lib_modules`（gradle.properties）和 jar 里的类；
   `model_generators` 曾经不在模块列表里，白找半天。
2. **我方是不是已经有了**：好几次 Mantle 侧已经有移植好的对应类（`ExtraTextureContext`、`ColoredBlockModel`、
   `AbstractIngredient`、`ClientFluidAttributeRegistry`）。
3. **上游是不是把它删了**：`git cat-file -e v3.12.1.231:<path>`。删了就**别修**，直接删掉让我们保留的替代物接管；
   3.12 把一大堆 `XxxModifier` 换成了 `XxxModule` / 纯 datagen 数据 + 集中式 `ModifierEvents`。

### 常用机械映射（完整表在 MERGE-3.12.1.md）

| Forge | Fabric / Porting Lib |
|---|---|
| `RegistryObject` / `LazyOptional` / `Lazy` | `porting_lib.util.*` |
| `Tags` / `ToolAction(s)` / `ItemHandlerHelper` | `porting_lib.tags.Tags` / `porting_lib.tool.*` / `porting_lib.transfer.item.ItemHandlerHelper` |
| `ExistingFileHelper` | `porting_lib.data.ExistingFileHelper` |
| `FluidStack` / `FluidType` / `FluidType.BUCKET_VOLUME` | `porting_lib.fluids.FluidStack` / `porting_lib.fluids.FluidType` / `FluidConstants.BUCKET` |
| `FluidAction` | `slimeknights.tconstruct.library.fluid.FluidAction`（本移植的垫片，**simulate 语义需要逐点核对**） |
| `@SubscribeEvent` + `@EventBusSubscriber` | 显式 `X.register(...)`，注册顺序表达 EventPriority |
| `FMLEnvironment.dist == Dist.CLIENT` | `FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT` |
| `ForgeEventFactory.onArrowLoose/Nock` | 没有对等物，扩展点丢弃并登记（#10） |
| `LivingGetProjectileEvent` | 没有对等物，逻辑内联进 `BowAmmoModifierHook.getBallistaAmmo`（#12） |
| `LivingKnockBackEvent` | 自建 `KnockbackEvent` + `LivingEntityKnockbackMixin`（#11） |
| `ICondition` | `net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider` |
| `LoggingRecipeSerializer` | Mantle 1.11 里是 **interface**（不是 class），要 `implements` |
| `ForgeMod.STEP_HEIGHT_ADDITION` / `ENTITY_GRAVITY` | `PortingLibAttributes.*` |
| `MobEffectInstance.duration` | 已有 accesswidener 放行 |
| `IntrinsicHolderTagsProvider.IntrinsicTagAppender` | 已加 accesswidener（protected 嵌套类） |

## 6. 自动化工具（`scripts/port/`）

```bash
scripts/port/errors.sh                      # 编译 + 完整日志 → .port/errors.txt，打印总数
./gradlew -I scripts/port/printcp.gradle printCompileCp && python3 scripts/port/jarindex.py
python3 scripts/port/portfix.py             # 默认 dry run
python3 scripts/port/portfix.py --apply     # 补 import / 改搬迁 import / 删死 import
```

`portfix.py` 故意**不**碰 `net.minecraftforge.*`、`mezz.jei.api.forge.*`，也**不猜**同名类（JEI/REI 各有一份时会跳过）。
详见 [scripts/port/README.md](../scripts/port/README.md)。
**这套刷子已经刷干净了**（最新一轮 0 提案），剩下必须逐文件改 API。

## 7. 未结案：一批"不可能"的 Lombok 假错误（约 28 条）

**现象**：`PartRecipe`（与上游**逐字节相同**）报 `PartRecipe is not abstract and does not override abstract method getCost()`
——但 `@Getter protected final int cost;` 明明会生成 `getCost()`；`getVariant()` / `getId()` / `getCraftingResult()`
也是同一族（`MaterialVariant`、`TinkerStationBlockEntity.getCraftingResult`）。
更怪的是：**同一个 javac 进程里，别的文件调用 `MaterialVariant.getVariant()` 能正常解析**。

**已排除**（都实测过）：Lombok 没跑（`-XprintProcessorInfo` 显示它 claim 了注解）；Lombok 版本（1.18.22 / 1.18.30 一样，
已升到 1.18.30）；`lombok.config`；重复注解污染（修掉 `TinkerDataCapability` 的重复 `@SuppressWarnings` 无效）；
Gradle 增量缓存（手写 javac 直接编 2094 个源文件，结果一致）；`-XDshould-stop.ifNoError=ATTR`；jar 里有同名类；
`delombok` 输出里 `getCost()` 和 8 参构造器**都生成了**。

**结论 / 策略**：这是 javac 的二次诊断假错误，**先别单独修**。把根因（`package net.minecraftforge.* does not exist` 之类）
清掉后，这批文件会自己好一大半。

## 8. 剩余工作（462 条，按建议顺序）

| 顺序 | 家族 | 条数 | 目标 API / 备注 |
|---|---|---|---|
| 1 | **Mantle 模型数据** | ~17 | `ModelData getModelData()` + `ModelProperties`。样板：`CastingTankBlockEntity`、`TinkerStationBlockEntity`。涉及 `RetexturedTableBlockEntity`、`SmelteryInputOutputBlockEntity`、`TankBlockEntity`、`HeatingStructureBlockEntity` 和 6 个 model 类 |
| 2 | **Mantle loadable** | ~20 | 1.9 的 `IGenericLoader` → 1.11 的 `Loadable`/`RecordLoadable`（`FallbackAOEIterator`、`TagHarvestLogic` 系列；它们被 `TinkerTools` 引用，是活代码） |
| 3 | **Forge 流体** | ~35 | 从 `SimpleFluidTank`/`EmptyFluidHandlerItem`/`ScaledFluidTank` 建立模板，再套到 `ProxyItemTank`/`MultitankFuelModule`/`SolidFuelModule`。Porting Lib 的 `FluidTank` 只有 `insert/extract(TransactionContext)`，**行为差异 #4 点名要实测** |
| 4 | **Forge capability** | ~42（22 文件） | `ForgeCapabilities.FLUID_HANDLER` → `FluidStorage.SIDED`、`ITEM_HANDLER` → `ItemStorage.SIDED`。Porting Lib 2.3.15 **没有** capabilities 模块（缓存里 27 个模块都列过） |
| 5 | **JEI 插件** | ~10 | `mezz.jei.api.forge.ForgeTypes` → `mezz.jei.api.fabric.constants.FabricTypes.FLUID_STACK`；成分类型从 `FluidStack` 变 `IJeiFluidIngredient`，**不是纯改名**；`plugin/rei/**` 是本移植自己写的，别当成上游代码 |
| 6 | **`IAreaOfEffectIterator` / `IHarvestLogic`** | ~15 | 上游把这两个接口换成了嵌套 `Loadable`；`ToolHarvestLogic`、`FallbackAOEIterator`、`FixedTier/Modified/TagHarvestLogic` 还是老 API |
| 7 | Lombok 假错误 | ~28 | 见第 7 节，**别单独修** |
| 8 | 其余零散 | ~295 | 每文件 1–3 条 |

## 9. 编译通过之后（发布路线）

1. `runServer` 热测试（用户允许用本机 HMCL 的 Minecraft 做热测试；**必须先能编译**）——优先验：
   - 击退强度/方向（#11，自建 mixin）
   - 经验/掉落/暴击集中化后的行为（#14）
   - 流体 simulate/execute 语义（#4，最重要）
   - 这轮补回的运行时注册：loader 注册表（`registerLoaders`）、`PersistentDataCapability` 事件（`registerCapabilities`）、
     工作台必需布局（`TinkerTables.init`）、datagen providers
2. 建 **non-fork canonical 仓库**（`MinecraftReconstruction`）并尽量带完整 git history，作为面向用户 / Modrinth 的主仓库。
3. 发布顺序：先 alpha（用户还在犹豫时机——他认为最好等 Tinker 的现代化（拆出地幔部分等）做完再上平台）。

## 10. 文档地图

| 文件 | 记什么 |
|---|---|
| `docs/HANDOFF.md`（本文件） | 入口：现状、约定、环境、剩余工作顺序 |
| `docs/MERGE-3.12.1.md` | **按时间倒序的进度日志** + 每批的测量结果 + 方法论 + Lombok 调查全文 |
| `docs/merge-3.12.1-workqueue.txt` | 自动生成的待办（错误数 / 是否上游文件 / 路径） |
| `docs/BEHAVIOUR-DIFFERENCES.md` | 全部行为差异（15 条），每改一处就补一行 |
| `docs/PLAN.md` | 总方案（顺序：先 Mantle 后 TCon；产物不手工合并…） |
| `docs/STATUS.md` | 早期的交接文档，部分内容已过时，**以本文件为准** |
| `scripts/port/*` | 编译/批量修复脚本，见第 6 节 |
| `~/Desktop/repo/mr-mantle-fabric/docs/*` | Mantle 侧的同一套文档（STATUS / BEHAVIOUR-DIFFERENCES / I18N） |
