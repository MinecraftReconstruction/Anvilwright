# 接力文件 —— Tinkers' Construct 3.12.1 → Fabric

> 最后更新：2026-09-29（第二轮）　|　分支 `mcr/upstream-3.12.1`　|　当时 HEAD `e53ca1632b`
> |　**剩余 167 条真错误**（Gradle 口径报 299，其中 132 条是假错误，见第 7 节；另有**整文件级的隐藏错误**，见 4.3）
> 性质见 [ATTRIBUTION.md](../ATTRIBUTION.md)。本文件是**给下一个接手的人 / AI 智能体**的入口，
> 细节都在 [MERGE-3.12.1.md](MERGE-3.12.1.md)（按时间倒序的日志 + 方法论）里。

## 0. 三句话讲清楚

1. **现状**：Tinkers' Construct（Fabric 版，源自 Alpha-s-Stuff 的 Hephaestus）已经**合并了上游 3.12.1 的整棵源码树**，
   正在逐个把 Forge API 换成 Fabric / Porting Lib。**目前编译不过，所以还不能热测试。**
   错误数：**真错误 167 条 / 91 个文件**（Gradle 报 299，扣掉 132 条假错误）。
2. **你的任务**：把真错误降到 0（并用第 4 节的方法确认"没有再冒出隐藏文件"）→ 跑 `runServer` 热测试 →
   按 [BEHAVIOUR-DIFFERENCES.md](BEHAVIOUR-DIFFERENCES.md) 逐条验证，然后才谈发布。
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

## 4. 进度与数字口径（本轮被推翻过一次，务必先读）

### 4.1 三个数字

| 口径 | 怎么测 | 含义 |
|---|---|---|
| **报告数** | `scripts/port/errors.sh`（Gradle，默认停止策略） | javac 实际报出来的条数。**含有 ~132 条假错误** |
| **真错误数** | `scripts/port/fastcompile.sh --gen .port/gen.txt` 与默认日志取**交集** | 唯一可信的"还剩多少活" |
| 假错误数 | 两个日志的**差集** | 全是 Lombok 生成物不可见导致的，不是代码问题（第 7 节） |

- 轨迹：`4540 → … → 534 → 462（相当于真错误 ~319）→ 报告 362 / 真错误 224`（本轮结束）。
- 一键出数：`scripts/port/fastcompile.sh && scripts/port/fastcompile.sh --gen .port/gen.txt && python3 scripts/port/workqueue.py`
  —— 最后一句会重写 [merge-3.12.1-workqueue.txt](merge-3.12.1-workqueue.txt)（**别手改那个文件**）。

### 4.2 ⚠️ 总数是**下界**，不是真值

javac 只对**它真正走到的类**做检查。实测：2063 个源文件里只有 ~344 个出现 `[checking]`。
`FluidTankBase.java` 里那条不成立的 `@Override` 单文件编译会报 5 条错，但**任何一次全量日志里都没有它**；
往文件里故意塞一行 `private final int zzzProbe = "not an int";`，全量编译**依然 0 报告**。

原因：检查它的入口（`TankBlockEntity` → `FluidTankAnimated`）自己先编译失败，整棵子树被跳过。

**结论**：随着真错误被修掉，之前被隐藏的文件会浮出来，**总数会阶段性上涨**。
看到数字涨了先别慌 —— 对比 workqueue 里"新出现的文件"就知道是不是这个原因。

### 4.2.1 ⚠️⚠️⚠️ 全量单次编译的数字是**任意的**，别再拿它当进度条

2026-09-29 实测的**决定性反例**：同一棵树，只因为我把 3 个新文件（`RetexturedBlockItem`、
`SupplierCreativeTab`、`MaterialRenderInfoJson`）加进 `find` 出来的文件列表，

| 编译的文件列表 | 报告错误数 | 被检查的文件数 |
|---|---|---|
| 去掉那 3 个文件 | **5** | ~50 |
| 包含那 3 个文件 | **3572** | ~505 |

也就是说 javac 的"提前停手"边界对文件列表/顺序极其敏感，**单次全量的总数可以是几倍到几十倍的偏差**。
这一轮里 `462 → 43 → 5` 的"进度"其实就是在这个浮动的边界上测出来的：数字下降既包含真实修复，
也包含"这次恰好只检查了很少的文件"。

**可靠的数法**：`scripts/port/truecount.sh` —— 按**包目录**分块编译（每块的文件全部显式传入，
保证这一块被完整检查），只统计属于该块的错误再求和。代价约 15–30 分钟，用来做里程碑级的真值测量。
第一次跑出来的分布（112/299 块时）已经暴露出一大片此前从未被检查过的代码：

```
351  slimeknights/tconstruct/common/data/tags      <- 标签 datagen
 91  slimeknights/tconstruct/common/data/loot
 13  slimeknights/tconstruct/common/data/render
  7  slimeknights/tconstruct/common/data/model
```

这些 datagen provider 用的是**旧 API**（`modResource` / `getForgeTag` / `getRegistryName` / `hotBuilder` /
`melting(...)` 等），上游 1.11 已经换掉了。**这是目前最大的一块剩余工作**，之前所有轮次都没看见它。

### 4.3 ⚠️⚠️ **整文件**都可以是隐藏的（比 4.2 严重得多）

4.2 说的是"某个方法体没被检查"。实测下来，**javac 会整文件跳过**：

| 文件 | 全量日志 | 单独编译 | 说明 |
|---|---|---|---|
| `TinkerNetwork.java` | **0 条** | ~30 条 | 网络层的 API 漂移（`NetworkDirection` 包名、`SimpleChannel.initServerListener(channel)` 变成静态、`ISimplePacket` 不再是 `S2CPacket`）——**所有发包都是坏的** |
| `FuelModule.java` | **0 条** | 43 条 | 缺 `tankSupplier`/`mainTank`/`NULL_POS`/`reset()`（上游把 multitank 逻辑合进基类时漏了） |
| `MelterContainerMenu.java` | **0 条** | 9 条 | 还在用 `IFluidHandler` |
| `ModifiableArmorItem.java` | 0（当时） | 14 条 | 合并残留 |

**判据**：`scripts/port/scanhidden.sh` —— 扫"正文里出现 Forge 独有类型名、但全量日志里 0 错"的文件。
加 `--check` 会逐个单独编译确认（**注意**：单独编译对**跨文件**的 Lombok 生成物（`getXxx()`、生成构造器）仍会报假错误，
所以见到 `getTemperature()`/`constructor X cannot be applied` 这类先怀疑是假错误，去看目标类有没有 Lombok 注解）。

**纪律**：改完一批之后，除了跑 `workqueue.py`，再跑一次 `scanhidden.sh`；**动过的文件要单独编译一次**，
否则会以为改完了、其实那一批压根没进统计。

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
scripts/port/errors.sh                        # Gradle 全量编译 → .port/errors.txt（40s，权威但含假错误）
scripts/port/fastcompile.sh                   # 直接 javac 全量（18s）→ .port/fast.txt，同口径可用
scripts/port/fastcompile.sh --gen .port/gen.txt   # 换停止策略，用来找重复定义
python3 scripts/port/workqueue.py             # 取交集/差集 → 重写 docs/merge-3.12.1-workqueue.txt
python3 scripts/port/summarize.py [log]       # 按缺失包/缺失符号/最差文件归类
scripts/port/scanhidden.sh [--check]          # 找"全量日志里 0 错但其实是编译不过"的隐藏文件（见 4.3）
./gradlew -I scripts/port/printcp.gradle printCompileCp && python3 scripts/port/jarindex.py
python3 scripts/port/portfix.py               # 默认 dry run
python3 scripts/port/portfix.py --apply       # 补 import / 改搬迁 import / 删死 import
```

`portfix.py` 故意**不**碰 `net.minecraftforge.*`、`mezz.jei.api.forge.*`，也**不猜**同名类（JEI/REI 各有一份时会跳过）。
详见 [scripts/port/README.md](../scripts/port/README.md)。
**这套刷子已经刷干净了**（最新一轮 0 提案），剩下必须逐文件改 API。

## 7. 结案：Lombok"假错误"的真因（**有 132 条，不是 28 条**）

**现象**：`PartRecipe`（与上游逐字节相同）报 `does not override abstract method getCost()`，`@Getter` 明明会生成；
`getVariant()` / `getId()` / `isSuccess()` / `getCraftingResult()` 以及一堆 `constructor X cannot be applied` 都是同一族。

**真因（已定案）**：Lombok 没问题（单独编译一个 `@Getter` + `@RequiredArgsConstructor` 的类，成员生成正常）。
问题在 javac：**出错后它会停在 FLOW 阶段，而在这个模式下 Lombok 注入的成员对其它编译单元的解析不可见**。

**证据**（同一棵树、同一 classpath，只换停止策略）：

| 运行方式 | 报错数 | 这一族 |
|---|---|---|
| 默认（错误即停 FLOW） | 462（本轮起点） | 有，132 条 |
| `-XDshould-stop.ifError=GENERATE` | 319 | **0 条** |

两次取差集，默认多出来的 132 条**全部**属于"缺 Lombok 生成物"这几类。
旁证：`RecipeResult`（有 `@Getter boolean success`）在自己的文件里零错误，别的文件调 `isSuccess()` 就报找不到。

**注意**：GENERATE 口径**不能当主口径**——它的检查覆盖更少（283 个类 vs 344 个），会漏掉真错误。
它只有一个用途：**找重复定义**（两版代码都被合并留下的地方），这类错误在默认日志里看不到。

**策略**：这一族**一律不要单独修**。修掉真错误后它们自己会消失；如果最后卡在某一条，那就是真的，
再去看那个类的 Lombok 注解（`@Getter` 的字段名/可见性）是不是和上游一致。

## 8. 剩余工作（**先读 [NUMBERS.md](NUMBERS.md) 的最新一节**；下表是 9-29 第二轮的口径，已过时）

⚠️ 9-30 上午重算了根因：整树数字是 2844（不是 224），那 224 是用当时的口径测的、只覆盖了当时被 javac 走到的那部分。
现在的做法是 **`scripts/port/missing_members.py` 按"合并丢的声明"驱动** + 整树数字只做参考。
两件大事在 [NUMBERS.md](NUMBERS.md) 末尾：`SlimeType`/`FoliageType` 重构（~85 条）与 JEI 版本（~16 条）。

完整清单见 [merge-3.12.1-workqueue.txt](merge-3.12.1-workqueue.txt)（三列：真错误 / 假错误 / origin）。

| 顺序 | 家族 | 剩余 | 说明 |
|---|---|---|---|
| 1 | **工具 capability 的 Fabric 化**（原第 1 批的剩余，**隐藏文件为主**） | ~40 | `ToolFluidCapability`(13)、`ToolInventoryCapability`(12)、`ToolCapabilityProvider`（它现在只留了 `clearCache()`，能力靠 `ModifiableItem` 里的 `FluidStorage.ITEM.registerForItems` / `ItemItemStorages.ITEM` 提供）、`ContainerFillingRecipeBuilder`(5)、`ModifiableArmorItem`(14) |
| 2 | **客制化 item client extension** | ~13 | `IClientItemExtensions` 在 Fabric **没有对应物**（Porting Lib 也没有）。涉及第一/第三人称工具动画、盔甲模型、`ArmorModelManager`、`ModifiableItemClientExtension`、`FancyItemFrameRenderer`。要在 Fabric 上用 `ItemRendererRegistry`（+ 盔甲模型的现有路径）重写，**必须视觉比对** |
| 3 | **Forge 事件残留**（原第 2 批） | ~30 | `net.minecraftforge.event*`(13)、`common.crafting.conditions`(7)、`registries`(4)、`eventbus`(4)、`common`(15)：`AntigravityEffect`、`FakeRegistryEntry`、`TinkerTags`、`AddReloadListenerEvent`、`FinalizeSpawn`、`AbstractModifierProvider` 等，逐个换成 Fabric API |
| 4 | **JEI 插件** | ~10 | `mezz.jei.api.forge.ForgeTypes` → `mezz.jei.api.fabric.constants.FabricTypes.FLUID_STACK`；成分类型从 `FluidStack` 变 `IJeiFluidIngredient`，**不是纯改名** |
| 5 | **流体类型** | ~13 | `TinkerFluids`(5)、`SlimeFluidType`(3)、`FluidEffectManager`(5)、`FluidEffectProjectile`(5)：`FluidType`/`ForgeFlowingFluid` 的 Fabric 对应物 |
| 6 | **Mantle loadable 残留** | ~8 | `IGenericLoader` 还剩 `StatPredicate`/`ToolPredicate` 这一族（1.9 的 `GenericLoaderRegistry` → 1.11 的 `Loadable`/`RecordLoadable`） |
| 7 | **Lombok 假错误** | 132（报告数） | 见第 7 节，**别单独修** |
| 8 | 其余零散 | ~80 | 每文件 1–4 条 |

**第 1 批（原计划里的"流体/capability"）本轮已经做完了**：单罐抽象层、`GaugeBlockEntity`、`CastingTankBlockEntity`、
`FluidCannonBlockEntity`、`ProxyTankBlockEntity`、`ProxyItemTank`、`DuctTankWrapper`、`FuelModule`、
`MultitankFuelModule`、`SolidFuelModule`、两个熔炉菜单、`TinkerNetwork`、`ToolEnergyCapability` 都已改到 Fabric API。
**第 2 批（Forge 事件/条件/注册表）还没动**，就是上表第 3 行。

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

## 11. 本轮完成（2026-09-29 第二轮，报告数 462 → 299 / 真错误 ~319 → 167）

四个 checkpoint（每个都已 push，`git ls-remote` 核对过）：

| commit | 内容 | 数字 |
|---|---|---|
| `ef51374b38` | Mantle 模型数据家族（`ModelProperties` 补全、`IModelData`→`ModelData`、`SinglePropertyData`→不可变 `ModelData`）+ 删掉 14 处合并残留的重复方法 + 新工具/文档 | 462 → 426 |
| `5cedfca395` | 删掉上游 3.12.1 已删且无 JSON 使用的 `CastingModel`/`MelterModel`/`TableModel`、无引用的 `GenericRegistryEntrySerializer`；`AlloyRecipe` 补回上游的 `record AlloyIngredient` | 426 → 401 |
| `34ef2b472a` | **`TinkerTools` 的注册表按上游重写**（工具定义模块 / 工具属性 / 两套谓词），删掉 4 个只被旧注册块引用的 `harvest`/`aoe` 类；`ToolModuleHooks`→`ToolHooks` | 401 → 375 |
| `3b45a0462e` | 单罐流体抽象层改到 Fabric 形状：`SimpleFluidTank` 不再继承 Forge、`FluidTankBase` 实现它并保留 `onContentsChanged` 语义、`EmptyFluidHandlerItem`/`ScaledFluidTank` 重写；补回 `ToolInventoryCapability.CraftingType` | 375 → 361 |
| `1e3b0e6f03` | 冶炼炉方块实体能力改到 Fabric lookup API：`GaugeBlockEntity`/`CastingTankBlockEntity`/`FluidCannonBlockEntity`/`ProxyTankBlockEntity` 用 `SidedStorageBlockEntity`，`ProxyItemTank` 变成物品内流体罐的 `Storage<FluidVariant>`，`DuctTankWrapper` 走 `SlottedStorage`，新增 `EmptyFluidStorage`（Forge `EmptyFluidHandler.INSTANCE` 的替代） | 362 → 327 |
| `dc535af3a2` | 燃料模块收尾：`FuelModule` 补回 `tankSupplier`/`mainTank`/`NULL_POS`/`reset()`、`SOLID_TEMPERATURE` 换成上游写法；`MultitankFuelModule` 变成 `Storage<FluidVariant>`、`SolidFuelModule` 只剩单罐位置 + 物品燃料显示；两个熔炉菜单跟着改 | 327 → 310 |
| `e53ca1632b` | **修好网络层**（`TinkerNetwork` 的 `NetworkDirection` 包名 / 静态 `initServerListener` / `S2CPacket` 三处漂移，之前**所有发包都是坏的**）+ 去掉 Forge 能量 capability（行为差异 #18） | 310 → 304 |

顺带修掉的**真 bug**（不只是编译问题）：

1. `HeatingStructureBlockEntity.updateDisplayFluid` 丢了 `displayFluid = ...`（冶炼炉显示的流体本来会永远停在空）。
2. `TinkerTools` 少了**全部**工具定义/谓词注册 → 50 个 `tool_definitions/*.json`、94 处 `tconstruct:tool` 谓词在运行时都找不到 loader（工具会变成空定义）。
3. 14 处重复方法定义（合并把两版代码都留下了）。

新增的行为差异：#16（`tconstruct:tool_hook` 原料类型没接上，2 个配方受影响）、#17（上面第 2 条的修复记录）。

### 下一轮从这里接着干

1. 按第 8 节的顺序打家族，**每批之后跑 `workqueue.py` 看"真错误"列**，不要只看总数。
2. 流体家族现在有可用的单罐基类了：先把 holder 类型统一到 `SimpleFluidTank` 或 `Storage<FluidVariant>`，再逐个改调用点的 `simulate/execute` 语义（行为差异 #4 那条仍然要点名实测）。
3. `IClientItemExtensions` 家族是本轮唯一**没动**的大块（Fabric 无对应物，需要重写 + 视觉比对），别在不看画面的情况下"顺手改完"。

---

## 12. 2026-09-30 上午：这一轮的交接（数字、结论、下一步）

**一句话**：找到了本轮所有"修不完"的根因 —— 3.12.1 的合并是 `-X ours` 式的，
**上游的调用点进来了、声明没进来**；另外 Mantle 从 AlphaMode 版换成我们的 1.11 时丢了 5 个 API。

### 这一轮做了什么（每个 commit 都已经 push）

| commit | 内容 | 整树数字 |
|---|---|---|
| `579d548a` | Mantle 补 `getLocalTag/getForgeTag/getTag/getBlock/getRegistryName` 并换版本；`SmelteryRecipeProvider` 的 10 条 Forge import；`FluidValues.SIP/LANTERN_CAPACITY`、`JsonUtils.debugLogResourceValues`、`TinkerDataCapability.getData`、`MeltingFuelBuilder.solid`；新增 `scripts/port/missing_members.py` | 3545 → 3300 |
| `a42324ee` | `TinkerTools` 补回 3.11 的 11 个工具 + 4 个实体类型 + `bonk` 粒子；`TinkerToolParts.withTabsBefore` 指向本移植的 TinkerTabs | 3194 |
| `fe5b6a0e` | `TinkerCommons`/`TinkerMaterials`/`TinkerGadgets`/`TinkerFluids`/`SlimeType` 补回合并丢的方块、物品、流体（含 60 处流体注册改成 Mantle 1.11 链式 API） | **2844** |
| `cbbd92e0` | 文档：NUMBERS.md 写下根因与本轮数字；BEHAVIOUR-DIFFERENCES #23–#26 | — |

### 下一个 agent 从哪继续（按性价比）

1. **`python3 scripts/port/missing_members.py .port/fast_cur.txt`** —— 每次先跑这个，它给出"还有多少声明是合并丢的"，
   现在应该已经降到 40 条左右；`TinkerWorld` 的 17 个字段（clusters/shards/enderbark/spawn）还没补。
2. **`TinkerWorld` 的 `SlimeType.BLOOD` → `FoliageType.BLOOD`/`DirtType`**：上游 3.12.1 把史莱姆类型拆开了，
   本移植没跟上，这一件事约 85 条错误（`FoliageType/DirtType cannot be converted to SlimeType`）。
3. **`TinkerSmeltery`**：它自己的错误会让 `seared*/scorched*` 那一大批字段"看不见"，从而级联到所有冶炼炉数据。
   它的字段缺失是合并丢的（`searedGlass`/`searedDrain`/`searedChute`/`searedFluidCannon`…）。
4. **JEI 版本**：`gradle.properties` 的 `jei_version` 提到上游的 `15.59.0.210`（要联网确认 Fabric 有没有），
   或改写 16 处 `addDrawableWidget/addRecipeArrowWidget`。
5. **底层类再体检一遍**：`library/tools/nbt/**`、`ModifierEntry`、`ModifierManager`、`ToolDefinition` —— 它们是级联的源头，
   但里面很多报错是**分块编译的 Lombok 幻影**（单独编译一包时 `getXxx()` 会"找不到"，整树编译却正常）。
   判据仍是"目标类有没有对应注解"，见第 7 节。

### 别再踩的坑

- 整树数字会因为"javac 到底走到哪些类"而跳（本轮见过 3 ↔ 3545），**只当参考**。
- `truecount.sh` 的分块数字**也有 Lombok 幻影**（分块编译时 `-sourcepath` 隐式加载的类不走注解处理器）：
  本轮实测 `library/tools/nbt` 单包 20 条错，其中 `getLevel()/getName()/getVariant()/getData()` 一类全是幻影。
- 换 Mantle 版本后**必须**重跑 `./gradlew -I scripts/port/printcp.gradle printCompileCp` 并重编，
  否则 classpath 里还是旧 Mantle。

---

## 13. 2026-09-30 傍晚：编译已经收敛到 1 条错误（本轮交接）

### 13.1 现在的状态（一句话）

`./gradlew compileJava` 那套 javac 口径（`scripts/port/fastcompile.sh --gen`）**只剩 1 条错误**：
Mantle 的 `FluidTransferHelper` 缺 `interactWithStack(...)` 与 `handleUIResult(...)`
（Forge 版 Mantle 有这两个方法，我们的 Fabric 版没搬，见 13.4 第 1 条）。

### 13.2 本轮做了什么（8 个 commit，全部 push 到 `mcr/upstream-3.12.1`）

| commit | 内容 |
|---|---|
| `579d548a` | Mantle 补 `FluidObject.getLocalTag/getForgeTag/getTag/getBlock`、`ItemObject.getRegistryName`；新增 `scripts/port/missing_members.py` |
| `a42324ee` | `TinkerTools` 补回 3.11 的 11 个工具 + 4 个实体 + bonk 粒子 |
| `fe5b6a0e` | `TinkerCommons`/`TinkerMaterials`/`TinkerGadgets`/`TinkerFluids`/`SlimeType` 补回合并丢的方块/物品/流体（60 处流体注册改成 Mantle 1.11 链式 API） |
| `cbbd92e0`/`43e6d105` | NUMBERS.md 写下"合并丢声明"的根因与测量方法；HANDOFF 第 12 节 |
| `9eb2778e` | 全树 long→int 扫尾、14 个 datagen provider 回 `FabricDataOutput`、**accesswidener 补 ThrownTrident 等** |
| `0e863702` | **import 补全轮**：新增 `sync_imports.py` + `drop_broken_imports.py`，一次清掉 800+ 条 import 级联（整树 2057 → 个位数量级） |
| `5315710c`/`ffb7d85a` | 逐文件扫尾：ToolModel/MaterialModel 的 FRAPI Mesh 桥接、盔甲模型入口、事件类构造器、`RecipeResult<LazyToolStack>`、`ToolHarvestLogic.runBlockBreak`、`ToolAttackUtil` 两个方法、`ToolContainerMenu` 构造器等 |

### 13.3 本轮新增/更新的工具（都在 `scripts/port/`）

| 工具 | 用途 |
|---|---|
| `sync_imports.py [--apply]` | 从上游同名文件补齐本文件缺的 import（跳过 Forge-only） |
| `drop_broken_imports.py [log]` | 把 javac 报"不存在"的 **import 行本身**删掉，可反复跑 |
| `missing_members.py [log]` | 找"上游有声明、我们整棵树没有"的成员（合并丢的声明） |
| `fastcompile.sh [--gen]` | 20 秒整树 javac；**`--gen` 口径才是现在的真值** |

⚠️ 不要再拿"默认口径"的整树数字当进度：本轮它显示 132 时，`--gen` 只有 6。
默认口径里绝大多数是 Lombok 幻影（javac 在失败状态下看不到 Lombok 生成的成员）。

### 13.4 接着干（按顺序）

1. **补 Mantle 的两个方法** → 应该就能 0 错误：
   `Mantle-Fabric/src/main/java/slimeknights/mantle/fluid/FluidTransferHelper.java`
   - `@Nullable public static TransferResult interactWithStack(Storage<FluidVariant> tank, ItemStack stack, TransferDirection direction)`
   - `public static ItemStack handleUIResult(Player player, ItemStack emptyStack, @Nullable TransferResult result)`
   参考 Forge 版（本地 `~/Desktop/repo/Mantle/src/main/java/slimeknights/mantle/fluid/FluidTransferHelper.java:360` 与 `:491`），
   Fabric 版用 `ContainerItemContext` + `FluidStorage.ITEM` + 现成的 `tryTransfer(...)` 重写。
   改完照旧：`publishToMavenLocal` → 改 TCon 的 `mantle_version`（当前 `1.11.DEV.0f373c6d`）→
   **必须**重跑 `./gradlew -I scripts/port/printcp.gradle printCompileCp` 再编译。
2. **跑真正的 Gradle 构建**：`./gradlew build --offline`。javac 过了不代表 Gradle 过
   （datagen provider 的注册、资源、mixins、AW 校验都可能再报）。新错误优先看 `build/reports`。
3. **冒烟测试**：`./gradlew runServer`（先）→ `runClient`。重点看：
   工具定义/修饰符能否加载（`tinkering/tool_definitions`）、冶炼炉方块实体、盔甲模型、
   以及 BEHAVIOUR-DIFFERENCES 里标"未验证"的那些点。
4. **迁移到 canonical 仓库**（用户明确的发布计划）：
   - 建一个 **non-fork** 仓库（不要 fork 关系），把 `mcr/upstream-3.12.1` 的历史整体推上去；
   - 两条分支：**带 checkpoint 的**（现有细粒度历史）与 **不带 checkpoint 的**（把这一百来个 commit
     按逻辑压成十几~二十块，例如"合并上游 3.12.1""Mantle API 恢复""注册表补全""流体重写""datagen 修复"
     "import 级联""盔甲/模型""事件与网络""权限与 AW"…），**后者设为默认分支**；
   - README/ATTRIBUTION 用固定措辞：`Unofficial, largely AI-assisted ("vibed") port.`（中文：
     「非官方、由 AI 大幅辅助完成（"largely vibed"）的移植工程」），并明确上游作者
     SlimeKnights / AlphaMode，且 **不要**写"AI-generated"。

### 13.5 仍然要小心的事

- **别信单个数字**：任何"总错误数"都要说明口径（`--gen` 才有意义），且修好一个文件后**别的文件会被暴露出来**，
  数量阶段性上涨是正常的。
- **分块编译（`truecount.sh`）对跨包 Lombok 类会造幻影**，本轮实测 `library/tools/nbt` 单包 20 条里大部分是幻影。
- **改 accesswidener 之后必须重新生成 classpath**，否则测得的是旧 MC jar。
- 本轮为了编译通过，有几处是"先让它能编译"的保守处理（例如 `Modifier.getModule(Class)` 返回 null、
  若干 Forge 专属钩子退化成普通方法），**这些都需要在冒烟测试里逐条确认**，必要时补行为差异条目。

---

## 14. 2026-09-30 中午：`--gen` 只剩 6 条，但都是在"铸造配方 long 化"上（本轮交接）

> 数字与逐项清单见 [NUMBERS.md](NUMBERS.md) 最后一节。HEAD 见 `git log -1`。

### 14.1 一句话

Handoff 第 13 节说的"缺 Mantle 的 `interactWithStack` / `handleUIResult`"**是误判**——那两个方法在已发布的
`1.11.DEV.0f373c6d` 里就有；当时真正的 1 条错误是 TCon 侧 `ToolContainerMenu:257` 把 `ToolFluidHandler`
传给了要 `Storage<FluidVariant>` 的方法。修好它之后是一连串"每轮只暴露 1 条"的连锁（共 12 轮），
现在 `--gen` 剩 **6 条**，全部集中在铸造配方的 `long` 化。

### 14.2 这一轮做了什么

见 [NUMBERS.md](NUMBERS.md) 的"本轮修掉的东西"：流体 hook 全面改到 Fabric 签名、新增 `ToolNbtSnapshots`
与 `SoundTypeHelper`、物品/实体/附魔/声音/桶/TNT 的 Forge 钩子逐个换掉、datagen 基类对齐。
Mantle 侧发了两个版本（`78ffdf1a`、`292ad3e8`），`mantle_version` 已跟到后者。

### 14.3 下一个 agent 从哪继续

1. **继续"单点连锁"**：`scripts/port/fastcompile.sh --gen .port/gen_cur.txt`，修当前那一条（现在在
   `library/recipe/casting/**` 的 `getFluidAmount` 返回类型上），再跑一次。**每轮都要跑**，因为一次只会暴露
   一小批。⚠️ 别只看总数：整树 `--gen` **会漏报**（`TinkerGadgets` 的 5 条实体错误在只改过别的文件的日志里
   根本没出现），所以刚碰过的文件要单独编译复核：
   ```bash
   out=$(mktemp -d); javac -nowarn -proc:full -Xmaxerrs 100000 \
     -processorpath "$(cat .port/ap-cp.txt)" -cp "$(cat .port/compile-cp.txt)" \
     -sourcepath src/main/java -d "$out" <文件.java> 2>&1 | grep "error:"
   ```
2. **long 化是成片的**：`IOreRate`、`FluidStack#getAmount()`、`MeltingFuel#getAmount`、`ICastingRecipe#getFluidAmount`
   这些接口在本移植里都是 `long`，上游那些把它们当 `int` 的调用点会一条条冒出来。判据是"下游用得上的量"：
   流体用 long（droplet），物品/伤害/时长仍用 int；溢出风险点加显式 `(int)` 转换并留注释。
3. **改 `tinkers.accesswidener` 后必须重跑** `./gradlew -I scripts/port/printcp.gradle printCompileCp --offline`，
   否则测的是旧 MC jar（本轮加过 `ItemEntity.age/pickupDelay`、`IntegerProperty.min/max`）。
4. 之后才是第 13.4 节的 2→4 步：真 Gradle 构建、冒烟测试、canonical 仓库两分支。

### 14.4 本轮新增的行为差异（都在 docs/BEHAVIOUR-DIFFERENCES.md）

- #27 `SimpleFluidTank` 现在是 `Storage<FluidVariant>`（API 结构改动，语义等价）
- #28 物品创造栏用 `ItemGroupEvents`（构造器多一个 tab 参数）
- #29 `Config.COMMON.toolTweaks` 恒为空（上游两条附魔槽位扩展需要给 `Enchantment.slots` 加 AW）
- #30 弩炮的 `ArrowNockEvent` 扩展点丢失（与 #10 同源）
- #31 灵魂疾行用 `isFaceSturdy(..., UP)` 取代 `collisionExtendsVertically`

---

## 15. 2026-09-30 下午：long 化收尾，停在 datagen 的 `ToolsRecipeProvider`（本轮交接）

- `--gen` 从 6 → 44，但那是**换了个领域**：铸造配方的 `long` 化已经修完（`AbstractMaterialCastingRecipe.getFluidAmount`
  改 `long`、`PartSwapCastingRecipe` 用 `mapToLong`），暴露出来的是 datagen 的 `ToolsRecipeProvider`。
- 对这个文件跑了配方 1：`python3 scripts/port/upstreamtake.py --apply src/main/java/slimeknights/tconstruct/tools/data/ToolsRecipeProvider.java`
  → 该文件自身 **55 → 4**，整树 **55 → 44**，脚本 KEPT。**注意**：upstreamtake 之后文件里还是 Forge 的
  import/调用，需要按 [NUMBERS.md](NUMBERS.md) 里那 6 条把管线换成 Fabric（ArmorSlotType、Porting Lib Tags、
  `DefaultCustomIngredients`/`DefaultResourceConditions`、`FabricDataOutput`、`buildRecipes` 签名、`Pattern` 重载、
  `addProvider` 歧义）。
- **热测试还跑不了**：编译没到 0，`runServer`/`runClient` 无从谈起；第 13.4 节的第 2–4 步（真 Gradle 构建、
  冒烟、canonical 仓库两分支）仍未开始。
- 提醒：`ToolsRecipeProvider` 是 datagen 文件，改完除了 javac 还要跑一次 `./gradlew runData`（或 `build`）看
  条件/原料写法是否真的能被 Fabric 接受。

---

## 16. 2026-09-30 傍晚：datagen 集群已清，停在 fork 独有的伤害类型（本轮交接）

- `--gen`：44 → **8**。`ToolsRecipeProvider` 的 6 类 Fabric 管线问题全部修完（`ArmorSlotType`、
  Porting Lib `Tags`、`DefaultCustomIngredients`、`DefaultResourceConditions.allModsLoaded`、`FabricDataOutput`、
  `buildRecipes` 公开签名、4 参 `toolBuilding` 重载、`addProvider` 显式 `FabricDataGenerator.Pack.Factory`）。
  注意 `TinkerToolParts.plating` / `TinkerSmeltery.dummyPlating` 仍然是 `ArmorItem.Type` 键（只在这 4 行里用），
  所以那两个 `EnumObject` **没有**跟着改成 `ArmorSlotType`。
- 现在剩下 8 条全在 `library/modifiers/spilling/effects/DamageSpillingEffect.java`：它是 fork 独有文件，
  引用了 8 个本树不存在的伤害类型常量。细节与补法见 [NUMBERS.md](NUMBERS.md) 最后一节。
- 修完这 8 条后按第 13.4 节继续：真 Gradle 构建（`./gradlew build --offline`，datagen 建议再跑一次
  `runData`）→ `runServer` → `runClient` 热测试。

---

## 17. 2026-09-30 夜：伤害类型已补，队列里还有一批"同源 datagen 文件"（本轮交接）

- `--gen`：8 → 5 → **81**。前两步是收尾（补 8 个伤害类型常量与 JSON；删掉两个 fork 遗留的 spilling provider），
  第三步是"javac 走得更远"暴露出的新集群：`tools/data/ModifierRecipeProvider.java` 81 条。
- 处理方式照抄上一轮验证过的配方：`scripts/port/upstreamtake.py --apply <文件>`（KEPT/REVERTED 自判），
  然后按 `NUMBERS.md` 的方式补 Fabric 管线（Porting Lib `Tags`、`DefaultCustomIngredients`、
  `DefaultResourceConditions`、`FabricDataOutput`、`buildRecipes` 公开、`modResource`→`location`）。
- ⚠️ 每修完一个 datagen 文件都会再翻出一批同级文件，**数字先涨后落是正常的**；判据始终是"文件自身的错误数"，
  不要看整树总数。最后才轮到 `./gradlew build --offline` → `runData` → `runServer` → `runClient`。

---

## 18. 2026-09-30 深夜：datagen 家族按"取上游"策略连消（81 → 36，本轮交接）

**关键结论（省时间的那个）**：本树的这批 datagen 文件是 **1.18 时代的旧 fork 代码**（`bronzeReinforcement`、
`MaterialIds.bloodbone`、`ArmorItem.Type`、`modResource(...)`、`saveThing(...)` 全是那个时代的），
而 `--gen` 报出的错误条数与"与上游 3.12.1 的 diff 行数"高度相关。所以
**`git checkout v3.12.1.231 -- <file>` 再补 Fabric 管线**，比在 fork 版上逐个改快得多，也和
`ToolsRecipeProvider` 那一轮的结论一致。本轮按这个策略连消了 8 个文件（明细见 [NUMBERS.md](NUMBERS.md) 最后一节）。

本轮修完（全部已编译验证）：`ModifierRecipeProvider`、`AbstractEnchantmentToModifierProvider`、
`ArmorModelProvider`、`TrimMaterialPaletteGenerator`(+`Tinker…`)、`ToolItemModelProvider`(+`AbstractToolItemModelProvider`)、
`ModifierModelMapProvider`、`ModifierModel.EMPTY`、`AbstractMaterialDataProvider`、`MaterialDataProvider`、
`AbstractMaterialStatsDataProvider`。

**顺手修掉一个真 bug**：Forge `CompoundIngredient` 是 OR，不是 AND；上一轮把它映射成了
`DefaultCustomIngredients.all`。映射应为 `CompoundIngredient`→`any`、`IntersectionIngredient`→`all`、
`DifferenceIngredient`→`difference`。`ToolsRecipeProvider` 两处已改。

**下一步**：`--gen` 现在 **36 条全在 `tools/data/material/MaterialRecipeProvider.java`**（同一个套路），
之后大概率还有 `tables/data/TableRecipeProvider`、`smeltery/data/SmelteryRecipeProvider` 等。全部到 0 之后才是
第 13.4 节的 `./gradlew build --offline` → `runData` → `runServer` → `runClient`。

⚠️ 另外记一笔**待补的注册**（编译不会报，但 datagen 会少文件）：`TinkerTools.gatherData` 目前只注册了
8 个 provider，上游注册了 13 个 —— 缺 `ToolItemModelProvider`、`MaterialPaletteDebugGenerator`、
`ArmorModelProvider`、`TinkerTrimMaterialPaletteGenerator`、`ModifierModelMapProvider`；
`MaterialRenderInfoProvider` 也少了 `existingFileHelper` 参数（上游是 3 参）。这些类本轮都已经能编译，
补注册本身是几行的事，但要等 `runData` 才能验证产物。

---

## 19. 2026-09-30 深夜（二）：**整树口径已经骗人了**，改用分块口径（本轮交接）

**一句话**：整树 `--gen` 已经掉到 1 条，但那是假象 —— 分块编译（`scripts/port/truecount.sh`）
跑到一半就有 **49 个包 / 417 条**。**不要再拿整树数字当进度条。**

原因（老坑，这次量化了）：javac 一旦在某个类上报错，就不再给同一批里的其它类做归因，
**整个文件的错误可以一条都不出现**。本轮翻出来的例子：`GuiTankModule` 里 `this.horizontal` 字段根本
没声明、`NormalModifierModel` 里的 `textures[index]` 变量早就删了、`MaterialModel.getPartQuads`
返回值类型和 `return` 语句不匹配 —— 这些在整树日志里**一条都没有**。

**接手建议（省时间）**：

1. 先跑 `scripts/port/truecount.sh .port/true_now.txt`（约 20 分钟）拿到**按包的完整错误清单**，
   然后按清单成批修，不要再用整树日志一条条钓。
2. 每次改完仍然跑一次 `scripts/port/fastcompile.sh --gen`，确认**没有新增**即可。
3. 本轮已验证的两条高效套路：
   - 数据生成器/`*Provider` 类：`git checkout v3.12.1.231 -- <文件>` → 补 4 类管线
     （`FabricDataOutput`、`DefaultCustomIngredients`、`DefaultResourceConditions`、Porting Lib `Tags`）
   - 客户端模型类：接口是 fork 的 `Mesh getQuads(...)`，把 `addQuads` 改写回去，
     vanilla quads 一律用 `ToolModel.ofQuads(...)` 包成 Mesh
4. Mantle 侧本轮修了 `mantle:tag_filled` 反相的真 bug（342 个 JSON 受影响），
   版本推进到 `1.11.DEV.1e53afad`；**动了 Mantle 就要重新 `publishToMavenLocal` + bump + 重跑
   `printCompileCp`**（本轮已做过）。
5. 本轮新增两条 accesswidener（对应上游 `accesstransformer.cfg`）：
   `FishingHookRenderer.stringVertex`、`SpriteSources.register`；**改 AW 必须重跑 `printCompileCp`**。

---

## 20. 2026-10-01 凌晨：整树数字已经彻底不可信，请用分块口径（本轮交接）

**本轮最重要的一句话**：整树 `--gen` 在 1～20 之间来回跳，**不代表进度**。javac 每归因成功一个文件，
就会把下一个"从没被检查过"的文件暴露出来。要看真实剩余量必须跑
`scripts/port/truecount.sh`（分块编译，约 20 分钟）。

**本轮结束时的状态**：
- 已 push 的 checkpoint：`32f391871f` → `ca2544e034` → `05a08b41f1` → `8044d90de8` → `7207abf7c7`
- 目录：`src/rei-unsupported/java/**`（停用的 REI 插件，见行为差异 #32）
- Mantle 版本：`1.11.DEV.1e53afad`（修了 `mantle:tag_filled` 反相）
- AW 新增：`FishingHookRenderer.stringVertex`、`SpriteSources.register`、
  `MangroveRootPlacer.mangroveRootPlacement`
- 当前阻塞的簇（接手时直接从这里开始）：`BlockModelSkullRenderer`（`renderModelLists` 私有 → AWS）、
  `BuddingCrystalBlock`（多余的 `@Override`）、`FluidEffectManager`（`CraftingHelper.processConditions`
  → Fabric 的 `ResourceConditions`）、`GenericNBTProvider`（已删掉用 `DataGenerator#getPackOutput` 的 ctor，
  要检查还有没有人调用那种构造）

**给接手的你（或下一个 agent）的最小流程**：
1. `scripts/port/truecount.sh .port/true_now.txt`（拿全量清单）
2. 按清单逐包修；每修完一轮跑 `scripts/port/fastcompile.sh --gen`（只看"有没有新增"）
3. 每 1～2 个簇 commit + push（用 `git ls-remote` 复核，别信管道退出码）
4. 非语义等价改动 → `docs/BEHAVIOUR-DIFFERENCES.md`
5. 编译 0 之后：`./gradlew build --offline` → `runData` → 与上游 3.12.1 的 `src/generated` 做 diff
   （**这是最能抓语义错误的验收手段**）→ `runServer` → `runClient`

**已知的、必须靠"看"而不是靠"编译"确认的地方**（信心最低的三块）：
- 流体单位：本移植是 droplet（1 桶 = 81000），上游是 mB（1000），行为差异 #20/#23
- 渲染链路：`IBakedModifierModel` 是 fork 的 `Mesh getQuads(...)`，本轮把若干 `addQuads` 改回 `getQuads`
  并用 `ToolModel.ofQuads` 包 vanilla quads —— 编译过 ≠ 画面对
- 那些 fork 独有内容（血史莱姆、geode、bonus chest、REI、JEI 自写 tooltip widget）与上游数据集的一致性

---

## 21. 2026-10-01 凌晨（二）：第二轮清理（本轮交接）

**已 push 的 checkpoint（全部用 `git ls-remote` 复核）**：
`32f391871f` → `ca2544e034` → `05a08b41f1` → `8044d90de8` → `7207abf7c7` → `18827a42d8` → `6e70c5aa8b` → `324e8879e5` → `33e4f3da59`

**Mantle 又推进了两版**（都要 publish + bump + 重跑 `printCompileCp`）：
- `1.11.DEV.1e53afad`：修 `mantle:tag_filled` 注册反相（342 个 JSON 受影响）
- `1.11.DEV.31f6e9eb`：`RecipeHelper.readItem/writeItem`（Forge 的 `RegistryHelper` 那套）

**这一轮换掉的 Forge 独占钩子（都有注释 + 部分记入行为差异）**：
`curePotionEffects`（两处 → 内联 vanilla 牛奶疗法）、`doesSneakBypassUse`、`onItemUseFirst`、
`invalidateCaps`、`BlockPlaceContext(任意 LivingEntity)`、`ItemRenderer.renderModelLists`（→ AW）、
`Enchantment.slots`（→ AW）、`LivingEntity.spawnItemParticles`（→ AW）、
`ForgeHooks.getCriticalHit`（→ Porting Lib 事件）、`Holder#get`（→ `#value`）。

**新发现的坑（重要）**：分块口径里有很多**级联错误**。比如 `common/data/tags` 这个 chunk 单编译时报 81 条，
其中 `ToolStack` 的 10 条、`ModifierNBT`/`ModDataNBT` 的二十多条都是"根因类缺成员"的连带噪声。
⇒ 修完根因务必重跑同一个 chunk 验证，不要按条数排优先级。

**下一步的推荐顺序（基于第二轮快照）**：
1. `common/data/tags`（`ItemTagProvider` 48 + `BlockTagProvider` 22；都是 tag provider 管线，套路已成熟）
2. `library/recipe/casting/*`（PotionCastingRecipe / 材料铸造 / 铸造 builder，约 80 条）
3. `shared`、`library/client/model/block`、`library/recipe/ingredient`
4. 全树 0 → `./gradlew build --offline` → `runData` → 与上游 `src/generated` diff → `runServer` → `runClient`

## 22. 2026-10-01 凌晨（三）：统一到 Gradle 全量口径，791 → 292

**口径改变（重要）**：放弃 `--gen`/分块数字当进度条，改用
`./gradlew compileJava -I scripts/port/maxerrs.gradle --offline`（`-Xmaxerrs 100000`，不会提前停手）。
接手时 **791**，本轮结束 **292**。逐检查点数字见 `NUMBERS.md` 的"凌晨（三）"一节。

**已 push 的 checkpoint（`git ls-remote` 复核）**：
`3138afa6d3` → `17f35d671e` → `5bd3fc3076` → `7b2479ff84`（本轮结束前的最后一个）

**本轮修完的簇**
1. `MaterialBlockModel`：Forge 的 `getQuads(...IModelData)` / `getParticleIcon(IModelData)` → FRAPI
   `emitBlockQuads` + Porting Lib `CustomParticleIconModel#getParticleIcon(Object)`。
2. tag provider 家族（`EntityTypeTagProvider` / `BlockEntityTypeTagProvider` / `FluidTagProvider` /
   `BiomeTagProvider` / `PotionTagProvider` / `MenuTypeTagProvider` / `CreativeTabTagProvider` /
   `InstrumentTagProvider` / `DamageTypeTagProvider`）：
   - 构造函数统一成 `(FabricDataOutput, CompletableFuture<HolderLookup.Provider>)`；
   - `tag(...)`（vanilla `TagAppender`，没有 `add(T...)`）→ `getOrCreateTagBuilder(...)`；
   - **`add(...)` 必须放在 `addTag/addTags(...)` 之前**，否则链式返回的类型会掉回 `TagAppender`；
   - `ItemTags.create(...)` → `TagKey.create(Registries.ITEM, ...)`；
   - `CostTagAppender` 的 appender 类型换成 `FabricTagProvider<Item>.FabricTagBuilder`；
   - `BlockEntityTypeTagProvider` 换回上游的 `SIDE_INVENTORIES` 版本（fork 里的 `CRAFTING_STATION_BLACKLIST`
     上游已删除），并补回 `ironchest(...)` 的兼容 tag。
3. 精灵/材质数据生成：`GenericDataProvider`/`GenericTextureGenerator` 的 ctor 只收 `FabricDataOutput`；
   `saveThing` → `saveJson`；`MaterialPartTextureGenerator` 补回上游的 `outputPath(...)`/`StatOverride` 重载。
4. 装饰模型（modifier model）管线：`IBakedModifierModel` 要求 `Mesh getQuads(...)`，把剩下 12 个还写着
   `addQuads(..., Consumer<Collection<BakedQuad>>)` 的实现全部改成 Mesh 版本（纯搬运，不改数值）。
5. 烘焙 quad 的颜色/自发光：新增 `library/client/model/ModelHelper#applyColor/applyEmissivity`
   （直接改 `BakedQuad#getVertices()` 的顶点 int），替代 Forge 的 `IQuadTransformer#processInPlace`。
6. gadget：补回 `IchorSlimeSlingItem`（只在 fork 历史里，上游 3.12 已删除滑翔弹弓）、
   `TinkerGadgets` 的 register 改成带 properties 的 lambda、补 `Util` 等 import。

**删除的死代码**：`library/client/ResourceColorManager`（fork 遗留的 `@Deprecated` 垫片，上游没有，且无调用点）。

**下一步**：继续按 Gradle 全量口径推平剩余 292 条（热点：`GadgetRecipeProvider` 尾巴、`shared`、
`smeltery/*`、`library/client/book/content`、`fluids/data`）。

## 23. 2026-10-01 凌晨（四）：292 → 147（本轮继续）

**已 push 的 checkpoint（`git ls-remote` 复核）**：
`0a591fa729`(254) → `f1d62a23b0`(224) → `d631ec1bac`(208) → `e9328fb090`(177) → `ba45575de6`(160) → `52992e8b15`(147) → `6db0fea7f7`(139) → `3fa173e336`(131) → 本轮结束 **123**

**修完的簇（按 commit）**
1. 书本内容：`AbstractMaterialContent` 直接取上游 3.12.1 版（fork 版少了 HTML 那几个方法），
   只改 `ForgeI18n`/`FluidStack`/`fluid.getDisplayName()` 三处 Fabric 适配；
   `ContentTool` 的 parts 逻辑改回上游的 `ToolPartsHook.parts(definition)`。
2. 谓词注册表：Mantle 的 `PredicateRegistry` 自己注册 `and/or/inverted`，TCon 里那 6 行删掉。
3. 客户端模型：`MaterialModel.getQuadsForMaterial(...)` 补回（fork 只有 `getPartQuads`）、
   `MaterialRenderInfoLoader.hasFallback(...)` 补回、`DynamicTextureLoader` 清理、`ClampedItemPropertyFunction`。
4. 流体链：`ProxyItemTank` 拆出独立的 `Storage<FluidVariant>` 视图（同一个类不能同时实现
   `Storage<ItemVariant>` 和 `Storage<FluidVariant>`）、`ScaledFluidTank` 用 `SimpleFluidTank.super.fill/drain`
   并显式 `iterator()`、`DuctTankWrapper`/`DuctItemHandler` 改 Transfer API。
5. 杂项：`ExplosionEvents.START` 取代 `ForgeEventFactory.onExplosionStart`（`hitPlayers` 有公开 getter，
   不需要 AW）、`TinkerDamageTypes.source(...)`/`SMELTERY_HEAT`、Config 的 minimap 字段、`Util.testConditions`
   走 `ResourceConditions.conditionMatches`、`Util.isNeo/isForge` 在 Fabric 上恒 false
   （这两条按规则 4 记入 `BEHAVIOUR-DIFFERENCES.md` 待办：**尚未登记，下轮补**）。

**剩余 147 条的热点（下一轮从这里开始）**

| 文件 | 条数 | 备注 |
|---|---|---|
| `gadgets/data/GadgetRecipeProvider` | 7 | `FoliageType`/`SlimeType` 混用、`SlimeType.TRUE_SLIME` 已删 |
| `common/data/model/TinkerSpriteSourceProvider` | 4 | `PalettedPermutations` 构造器变私有、`SpriteSourceProvider` ctor |
| `common/data/model/TinkerBlockStateProvider` | 1 | `paneBlock(...)` 参数表变了 |
| `fluids/block/BurningLiquidBlock` + `MobEffectLiquidBlock` | 4 | `LiquidBlock` 构造器（Fabric 只有 `(Fluid, Properties)`） |
| `fluids/data/FluidTooltipProvider` | 6 | 上游 tag 常量（`WATER_TOOLTIPS`/`SOUP_TOOLTIPS`）没有对应物 |
| `gadgets/entity/*`（EFLN/Explosion/FancyArmorStand/FancyItemFrame） | 8 | `Explosion` 包名、`getExplosionResistance` 参数、AW `blockRenderer` |
| `library/client/modifiers/*Model` | 8 | 仍有个别 `addQuads` 残留 + `parseColor` |
| `common/data/loot/BlockLootTableProvider` | 5→? | 已补 `COPY_NAME/COPY_MATERIAL/ADD_ANVIL` 与 import，待重编译确认 |
| `smeltery/*`、`shared/*` | ~40 | 长尾，多为 1–3 条/文件 |

**下一步（严格按这个顺序）**
1. 重跑 Gradle 全量，确认最新条数（预计 <147，因为上面还有几处已改未编译）。
2. 按上表顺序推平；每修完一个文件簇就再跑一次全量确认没有级联回归。
3. `Util.isNeo/isForge` 的状态要补进 `BEHAVIOUR-DIFFERENCES.md`（规则 4）。
4. 全树 0 → `./gradlew build --offline`（datagen/资源/mixin/AW 校验）→ `runData` 与上游 `src/generated` 结构化 diff
   → `runServer` → `runClient`。

## 24. 2026-10-01：**javac 全量归零** ✅（下一棒的起点）

`./gradlew compileJava -I scripts/port/maxerrs.gradle --offline` → **BUILD SUCCESSFUL，0 error**，
`build/classes/java/main` 下 2830 个 class。逐检查点数字见 `NUMBERS.md` 的最末两节。

**最后一轮修的东西（都是小尾巴）**
- Config：补回 `syncKnockbackResistance` 的赋值，删掉没有任何赋值的 `extraToolTips` 字段。
- `EFLNEntity`/`EFLNExplosion`：`Explosion.BlockInteraction.KEEP`（vanilla 枚举，不是 `Level.ExplosionInteraction`）、
  爆炸抗性改走 Vanilla `Block#getExplosionResistance()` + Porting Lib `ExplosionResistanceBlock` 接口。
- `FancyArmorStandEntity`：**删掉** Forge 独占的 `getPickedResult`/`brokenByPlayer`/`brokenByAnything`
  （掉落改由 `entities/armor_stand` 战利品表负责，`src/generated` 里已有该表）→ **需要补进行为差异**。
- `SlimesteelBlock`：`canStickTo` 是 Forge 方法，删掉；`isSlimeBlock` 走 Porting Lib `CustomSlimeBlock`。
- `KnightMetalBlock`/`KnightMetalFluidCannonBlock`：Forge 的 `getBlockPathType(...)` 没有 Fabric 对应物，
  **删掉覆写** → **需要补进行为差异**（怪物寻路不再把这两种方块当成伤害方块）。
- 其它：`PiggyBackPackItem` 的 `ElementScreen#draw`、`GadgetRecipeProvider` 的 `folder` 变量、
  `ModelSpriteProvider` 的 ctor、`FluidBucketModelProvider` 的桶流体查询、`SmelteryTank` 的 `IMultitankListChange` 实现等。

**⚠️ 数字波动的解释（写下来免得下一棒怀疑自己）**：javac 分层报错。归属阶段的错误一消，解析阶段
（构造函数不匹配、long/int 转换）才会冒出来；另外修坏一处会连带十几条（本轮出现过一次 2 → 117 → 39）。
**只有"整树全量、改完再看"的数字可信。**

**下一棒的顺序（按用户拍板的计划）**
1. 规则 4 欠账先补：`BEHAVIOUR-DIFFERENCES.md` 追加
   #36 精致的盔甲架掉落/拾取钩子（Forge 独占，改用战利品表）、
   #37 `Util.isNeo/isForge` 在 Fabric 上恒 false、
   #38 knightmetal 的 `getBlockPathType`（寻路伤害方块）被删除、
   #39 `ClientGeneratePartTexturesCommand`/`MaterialRenderInfoLoader.createContext` 的调试上下文简化。
2. `./gradlew build --offline`（真正闸门：datagen 注册、资源、mixin、AW 校验）。
3. `runData` → 把 `src/generated` 与上游 3.12.1 做结构化 diff（最能抓语义错误）。
4. `runServer`（验 `tinkering/tool_definitions`、冶炼炉方块实体）→ `runClient`（验盔甲/工具渲染、JEI）。
5. 最后：canonical 非 fork 仓库 + 两条分支（细粒度 checkpoint / 压成 15–20 个逻辑提交，压缩版设默认分支）。

## 25. 2026-10-01：canonical 仓库与分支约定（已建好）

| 仓库 | 作用 | 分支 |
|---|---|---|
| **`MinecraftReconstruction/TinkersConstruct-Fabric`**（非 fork，public） | **canonical**，发布用 | **`main`（默认）** = 20 个逻辑提交；`checkpoints` = 143 个细粒度 checkpoint |
| `MinecraftReconstruction/TinkersConstruct`（fork） | 对外同步/浏览用的 fork 基线 | `mcr/upstream-3.12.1` = 细粒度开发分支 |
| `SlimeKnights/TinkersConstruct`（upstream remote） | 上游只读参考 | tag `v3.12.1.231` |

**推送方式**：本地 `git remote add canonical ...`（已加好），日常在 `mcr/upstream-3.12.1` 上提交并
`git push origin` 与 `git push canonical mcr/upstream-3.12.1:checkpoints`；文档类改动 cherry-pick 到 `main`。

**压缩分支怎么来的**（可复现）：`main` 从合并提交 `c1877b30fb` 起，按逻辑把 143 个 checkpoint 分成 20 组，
每组用 `git read-tree --reset -u <该组最后一个 commit>` + `git commit` 生成。已核对
`git diff --name-only checkpoints main` = **0**（两边代码树完全一致，只有历史形状不同）。

**commit 计数（回答"我们做了多少"）**

| 口径 | 条数 |
|---|---|
| 分支总提交（含上游 2012 年起的全部历史） | 11043 |
| 我们写的（`--author=Winston_Huang`） | 150 |
| 其中 3.12.1 合并点之后的移植工作 | **143**（2026-09-29 → 10-01） |

**⚠️ 待办**：fork 仓库 `MinecraftReconstruction/TinkersConstruct` 的 **描述**里目前写着
"Unofficial, AI-generated (largely vibed) ..."，与规则 2 冲突（只能写
`Unofficial, largely AI-assisted ("vibed") port.`）。canonical 仓库的描述已经是合规措辞，
fork 那条描述建议一并改掉（`gh repo edit MinecraftReconstruction/TinkersConstruct --description ...`）。

## 20. 2026-10-01 夜：客户端冒烟 —— 紫黑物品清零（本轮交接）

### 20.1 现在的状态（一句话）

`runData` 绿、`runClient` 能进世界且**开创造栏不再崩**；本轮把口径升级成"客户端画得出来没有"，
读数：**0 个模型烘焙失败、0/682 个 `tconstruct:` 物品落到 missing model、109 个紫黑格全清**。
详细数字见 [NUMBERS.md](NUMBERS.md) 最后一节，机制差异见 [BEHAVIOUR-DIFFERENCES.md](BEHAVIOUR-DIFFERENCES.md) 36–42。

### 20.2 本轮做了什么（5 个 commit，都在 `mcr/upstream-3.12.1` 且已 push）

1. `9f379612e0` 流体渲染：补回被合并丢掉的流体 provider；创造栏 12 处 `TableBlockItem` 强转改防御式
2. `735d5a7b07`（+ Mantle `c7098eb1`）修掉 109 个烘焙失败的模型：烘焙期不能用全局 atlas 取贴图
3. `b03b62708f` 补回工具/盔甲/装饰模型 provider 与对应的 loader 注册
4. `f40e5a3987` 补回 `TinkerCommons` 的 5 个客户端 provider（`models/item` 71 → 501，与上游对齐）
5. 本轮最后：`FluidTextureCameraProvider`（62 张贴图）+ 文档

### 20.3 新增的冒烟工具（照着用）

`slimeknights.tconstruct.testing.TConstructClientSmokeTest`（`fabric.mod.json` 的 `client` 入口，**只在开发环境运行**）：

- 进世界后自动跑一次，打 `[smoketest]` 行：创造栏重建 + 3281 个物品的模型解析、missing model 计数、atlas 审计；
- 完整缺失清单写到 `run/smoketest-atlas-missing.txt`，两次运行可以直接 `diff`；
- `ToolModel` / `FluidContainerModel` 烘焙失败时会在开发环境打**完整堆栈**（vanilla 只打一行消息，之前就是
  因为看不到堆栈才多绕了两轮）。

### 20.4 下一个 agent 从哪继续（按性价比）

1. **实机截图确认三件事**（我这边键盘焦点拿不到 Minecraft 窗口，只能靠 `F2` 由人按或者看日志）：
   盔甲层渲染（本轮才注册 `ArmorModelManager`）、书里的工具/材料图标（`gui/modifiers` 目录源本轮才接上）、
   流体外观（本轮才有 `FluidRenderHandler`）。
2. **药水流体的按 stack 染色**：现在用的是 `fluid_texture/potion.json` 里的固定色；上游走
   `PotionFluidAttributes`。要接的话：注册 `FluidVariantRendering.register(potion, handler)`，getColor 从
   stack tag 算（`PotionFluidAttributes` 里已有逻辑）。
3. **`MaterialPaletteDebugGenerator`** 还没注册（debug 用，上游注册了，可忽略）。
4. canonical 同步：`main`（压缩历史）还落后本轮 5 个 commit，需要重新 cherry-pick 或重压。
