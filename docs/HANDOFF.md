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

## 8. 剩余工作（真错误 224 条 / 103 文件，按建议顺序）

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
