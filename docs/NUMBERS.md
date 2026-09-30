# 数字记录（每步都记）

> 口径见 [HANDOFF.md](HANDOFF.md) 第 4 节。**单次整树编译的数字是任意的**（javac 提前停手，
> 边界随文件列表浮动），所以下面每条都标了"怎么测的"。

## 2026-09-29 / 30 这一轮

| 步骤 | 测试方式 | 数字 | 说明 |
|---|---|---|---|
| 会话开始 | `errors.sh`（Gradle，classpath 已过期） | **462** | 含约 130 条 Lombok 假错误 |
| 重建 classpath 后 | `fastcompile.sh` + `gradle --rerun-tasks` | **43 / 42** | 假错误归零；但只检查了约 50 个文件 |
| 决定性反例 | 同一棵树，仅 ±3 个文件 | **5 ↔ 3572** | 证明"总数"取决于 javac 的停手边界 |
| 分块真值 | `truecount.sh`（233/299 块时） | **2573**（未跑完） | 唯一可靠口径，约 15–30 分钟 |
| 整树分类 | 整树日志 | **3572**＝运行时 **1865** + datagen **1706** | 其中 `required: no arguments` 仅 11 条 ⇒ 真错误 |
| **分块真值（首次跑完）** | `truecount.sh`（299 块，23:38 完成） | **3086**（197 个有错的包） | **这是目前最可信的里程碑数字**，见下方分布 |

## 已应用的改动（每条都验证过）

| 改动 | 测试方式 | 效果 |
|---|---|---|
| `hotBuilder` → `builder` | `apply_map.py`（逐符号 A/B） | **51 条**，1 个文件，0 回滚 |
| `coolBuilder` → `builder` | 同上 | **10 条**，1 个文件 |
| `modResource` → `commonResource` | `derive_calls.py` 推导（80 票）+ 应用 | **128 条**，10 个 datagen 文件 |
| `FluidTagProvider` 换上游文件 | `upstreamtake.py`（分块 + 整树取较大值） | **138 → 3**，再加 2 个 import 后归零 |
| `TinkerModule` 注册表改 Fabric | 分块编译 `common/*.java` | 该类 0 错；**级联来源之一**被切断 |
| `TinkerFluids` 实体数据序列化器 | 分块编译 | Forge 的注册表 → vanilla `EntityDataSerializers.registerSerializer` |
| `getForgeTag`/`getLocalTag` → `getTag` | `apply_map.py` | **自动回滚**（不是改名，结构变了） |
| 上游 104 个映射推导 | `derive_calls.py` | 供后续批量应用 |

## 结论（写进方法论）

### 首次跑完的分块真值分布（3086 条 / 197 个包）

```
401  smeltery/data                 <- datagen
351  common/data/tags              <- datagen
106  fluids                        <- 运行时的流体类型
 97  shared
 91  common/data/loot              <- datagen
 73  tools/data/material           <- datagen
...  （完整列表见 .port/true.txt）
```

datagen 相关包合计约占一半，与整树口径（1706/3572）一致。

### 之后的一轮：先用 `blame.py` 分出"元凶 / 受害者"

`scripts/port/blame.py` 会对每个报错文件做"整树计数 vs 该文件所在包的分块计数"对比，**自身有错的是元凶，
自身 0 错的是被级联污染的受害者**。对当时 top12 的结果：

| 文件 | 整树报告 | 自身 | 判定 |
|---|---|---|---|
| `SmelteryRecipeProvider` | 367 | **380** | 元凶 |
| `BlockTagProvider` | 105 | **115** | 元凶 |
| `TinkerFluids` | 102 | **102** | 元凶 |
| `TableRecipeProvider` | 60 | **60** | 元凶 |
| `BlockLootTableProvider` | 57 | **57** | 元凶 |
| `TinkerCommons` | 55 | **55** | 元凶 |
| `MaterialRecipeProvider` | 46 | **49** | 元凶 |
| `FluidTagProvider` / `ModifierRecipeProvider` / `ToolsRecipeProvider` / `ItemTagProvider` / `ModifierProvider` | 54–145 | **0** | 受害者（会自己好） |

**这解释了为什么"按文件错误数排序"一直误导人**：一半的大文件根本没错，是级联。

### 对元凶执行"取上游文件"（`upstreamtake --apply`）

| 文件 | 结果 |
|---|---|
| `SmelteryRecipeProvider` | **380 → 10，已保留**（只差 10 条 import 映射） |
| `BlockTagProvider` / `TableRecipeProvider` / `BlockLootTableProvider` | 回滚（当时的分块基线又测到 0，保守回滚） |

回滚后整树报告数从 **3576 → 10**——`SmelteryRecipeProvider` 是最大的级联源。这 10 条是它自己的
Forge import：`Tags`、`CompoundIngredient`、`ConditionalRecipe`、`DifferenceIngredient`、
`AndCondition`/`ICondition`/`ItemExistsCondition`。其中
`Tags` → Porting Lib `Tags`、`ConditionalRecipe` → Porting Lib `data.ConditionalRecipe` 是直接映射；
`ICondition` 系 → Fabric 的 `ConditionJsonProvider`/`DefaultResourceConditions`；
`CompoundIngredient`/`DifferenceIngredient` → `DefaultCustomIngredients.all/any/difference`（**要改调用写法，不是改名**）。

⚠️ 但**别把"整树 10"当成进度**：这个数字的边界随时会回涨（本轮已经出现过 3 ↔ 3572 ↔ 10）。
下次继续前先跑一次 `truecount.sh` 拿真值。

1. **别用单次整树数字当进度条**，用 `truecount.sh`；日常迭代用"逐文件 A/B + 回滚"。
2. **对破损的树做 A/B 不可靠**：`ModifierRecipeProvider` 同一次会话里报过 117、也报过 0，
   两次都是"整树 + 分块"双口径 ⇒ 那些多是**级联**，不是它自己的错。
3. **所以按依赖顺序修**：`TinkerModule`、`library/tools/nbt/**`、`ModifierEntry/Modifier/ModifierManager`、
   `ToolStack` 这些底层类先通，下游成片消失。

---

## 2026-09-30 凌晨/上午：找到"合并本身是坏的"这条根因，然后开始按它修

### 0. 根因：这次合并是 `-X ours` 式的合并没有把上游内容带全

`c1877b30fb`（"Merge upstream Tinkers' Construct v3.12.1.231 into the Fabric port"）的两个父提交是
`85d3d6c80d`（本移植）与 `a5a0324954`（上游）。用 git 自己做一次干净三方合并对比：

```
git merge-tree --write-tree c1877b30fb^1 c1877b30fb^2
```

| 文件 | 提交里的合并结果 | 干净三方合并 |
|---|---|---|
| `common/data/tags/ItemTagProvider.java` | 786 行、`moltenTools` 19 处（**没有声明，只有 19 个调用点**） | 911 行、20 处（有声明） |
| `tools/TinkerTools.java` | 321 行、**0 处** `battlesign` | 613 行、2 处（有声明） |

⇒ 合并时**调用了上游新代码，却把上游的声明丢了**。这就是那 2000+ 条 `cannot find symbol` 的主要来源，
也是为什么"修一个文件，几百条错误跟着消失"。

### 1. 新工具：`scripts/port/missing_members.py`

对每条 `cannot find symbol`：取 `location:` 里的类，**在我们的树里找这个类的源文件**，
如果那个文件里根本没出现过这个成员名，而上游有同名声明 ⇒ 判定为"合并丢的声明"，按影响条数排序输出。

第一轮结果：**62 条被丢掉的声明，合计 322 条错误**。修复顺序（按影响）：
`TinkerMaterials.blazewood`(21) / `TinkerWorld.enderbark`(19) / `TinkerMaterials.steel`(15) /
`TinkerCommons.GENERAL_BLOCK_ITEM`(21) / `TinkerTools.*`(11 个工具) …

### 2. 另一条根因：Mantle 换版本后丢了 API（不在 TCon 里，在 Mantle 里）

整树 `cannot find symbol` 按"类不在我们树里"筛一遍，前几名全是 Mantle 的：

| 缺的成员 | 次数 | 原因 |
|---|---|---|
| `FluidObject.getLocalTag()` / `getTag()` / `getForgeTag()` | 71 + 70 + 33 | 1.11 分支把三者合并成了 `getCommonTag()` |
| `ItemObject.getRegistryName()` | 44 | 1.11 分支只留了 `getId()` |
| `IRecipeExtrasBuilder.addDrawableWidget/addRecipeArrowWidget` | 16 | JEI 版本：上游要 15.59.0.210，本仓库钉的是 15.20.0.118 |

（`FluidObject.getLocalTag` 在 **TCon 1.20.1 分支**里也有 39 处调用，说明删掉这三个方法是我们引入的回归。）

处理：在 **`Mantle-Fabric` 侧**补回 `getLocalTag()/getForgeTag()/getTag()/getBlock()` 与 `getRegistryName()`，
`publishToMavenLocal` 后把 TCon 的 `mantle_version` 从 `1.11.DEV.ad2e7db0` 换到 `1.11.DEV.b1d5ab97`。
Mantle 侧记在行为差异 #24，TCon 侧记在 #19。

### 3. 本轮的整树数字（**只当参考，不当进度条**）

| 步骤 | 整树 javac |
|---|---|
| 开工（SmelteryRecipeProvider 的 import 修完之前） | 3545 |
| SmelteryRecipeProvider 的 10 条 import 修完 | 3521 |
| Mantle 补 API + 换版本 | 3300 |
| `TinkerTools` 补回 11 个工具 + 4 个实体 + 1 个粒子 | 3194 |
| `TinkerCommons`/`TinkerMaterials`/`TinkerGadgets`/`TinkerFluids`/`SlimeType` | **2844** |

### 4. 仍然没解的两件大事

1. **`SlimeType` vs `FoliageType`/`DirtType` 的重构**：上游 3.12.1 把史莱姆类型拆成了
   `SlimeType`（4 个）＋`FoliageType`（含 `BLOOD`）＋`DirtType`，本移植还是旧的单一枚举。
   整树里 `incompatible types: FoliageType cannot be converted to SlimeType` 54 条、
   `DirtType → SlimeType` 19 条、反向 12 条，都是这一件事。
2. **JEI 版本**：把 `jei_version` 提到上游的 `15.59.0.210`（Fabric 侧是否有对应版本需要联网确认），
   可以一次性消掉 ~16 条；不升级就要改写那 16 处调用。

---

## 2026-09-30 中午：`--gen` 口径从 1 → 0 的路上（本轮交接）

**口径提醒（本轮实测到的第二个坑）**：整树 `--gen` 会**少报**。例：`TinkerGadgets` 有 5 条实体注册错误，
在只改过 `ToolContainerMenu` 那一版的整树日志里**一条都没出现**，单独编译该文件才报出来。所以
"total: N" 只能当"至少 N 条"，每轮都要用"单文件编译"复核刚碰过的文件。

| 步骤 | `--gen` 整树 | 说明 |
|---|---|---|
| 会话开始（HEAD `9a1a4ffc`） | **1** | 真错是 `ToolContainerMenu:257`：`ToolFluidHandler` 不是 `Storage<FluidVariant>`（不是 Handoff 里写的 Mantle 缺方法——那两个方法在 `0f373c6d` 里已经有了） |
| `SimpleFluidTank extends Storage<FluidVariant>` | **11** | 修掉那 1 条后立刻暴露 10 条（`TinkerTools` 的实体注册 + 弩/弓构造器） |
| 实体注册改用 Mantle 新增的 Fabric builder 重载 + 弩/弓 tab 参数 | 1→0 | 之后进入"一轮只留 1 条"的连锁 |
| 连续 12 轮单点修复（见下） | 1 | 领域从 tools/item 走到 fluid hook、再到 datagen |
| 本轮结束 | **6** | 全部集中在铸造配方的 long 化（`AbstractMaterialCastingRecipe` / `PartSwapCastingRecipe`） |

### 本轮修掉的东西（按领域）

- **流体链**：`SimpleFluidTank` 现在是 `Storage<FluidVariant>`（Fabric 的 `IFluidHandler` 对应物），带事务化
  默认实现；`TankModule` / `SmashingModule` 改成 Fabric 版 hook 签名（`FluidVariant` + `long` + `TransactionContext`）；
  `ToolFluidCapability` 的匿名默认 hook 与接口签名对齐；`FluidModifierHookIterator.drain` 删掉残留的 FluidStack 收缩代码。
- **新增工具类**：`library/tools/nbt/ToolNbtSnapshots`（事务中止时回滚工具持久 NBT；库存 / 罐 / smashing 三处共用）、
  `library/utils/SoundTypeHelper`（Forge `IForgeBlock#getSoundType` → Porting Lib `CustomSoundTypeBlock` + 原版回退）。
- **物品/实体**：`ModifiableLauncherItem` 用 `getItemStackLimit`（Porting Lib 名字）、去掉四个 Forge-only hook 的 `@Override`；
  `ModifiableArrowItem implements InfiniteArrowItem`；`ModifiableBowItem`/`ModifiableCrossbowItem` 补 `ResourceKey<CreativeModeTab>`；
  `IndestructibleItemEntity` 改成 `age = Integer.MIN_VALUE`（既不会消失、又保留客户端旋转）；`war_pick` 加入工具创造栏。
- **Forge 钩子替换**：`getSoundType`（5 处）、`EnchantmentHelper.getTagEnchantmentLevel`、`BucketItem.getFluid`、
  `collisionExtendsVertically`、`TntBlock#onCaughtFire`、`ForgeHooks.getProjectile`、`LogicHelper.orElseNull(LazyOptional)`。
- **datagen**：`AbstractStationSlotLayoutProvider` 的 `PackType.SERVER_DATA` → `Target.DATA_PACK`、`saveThing` → `saveJson`、
  补回上游的 `definePattern(Pattern)`。
- **accesswidener 新增**：`ItemEntity.age`（+mutable）、`ItemEntity.pickupDelay`、`IntegerProperty.min/max`
  （对应上游 `accesstransformer.cfg`；改完**必须**重跑 `printCompileCp`）。

### Mantle 侧（都已 push）

| 版本 | 内容 |
|---|---|
| `1.11.DEV.78ffdf1a` | `EntityTypeDeferredRegister` 新增收 `FabricEntityTypeBuilder` 实例的重载（原版 builder 表达不了 `forceTrackedVelocityUpdates`，行为差异 #26） |
| `1.11.DEV.292ad3e8` | `LogicHelper.orElseNull` 增加 `LazyOptional` 重载（上游 Forge Mantle 本来就只有这一版；1.11 移植只留了 `Optional`，下游上游形状的调用点编不过） |

TCon 的 `mantle_version` 已跟到 `1.11.DEV.292ad3e8`，classpath 也重新生成过。

### 2026-09-30 下午续：long 化收尾 + 一次 `upstreamtake`

| 步骤 | `--gen` 整树 | 说明 |
|---|---|---|
| 本轮开工 | 6 | 6 条全在铸造配方：`AbstractMaterialCastingRecipe` 的 `getFluidAmount` 还是 `int`（接口早已是 `long`），`PartSwapCastingRecipe` 用 `mapToInt` 取流体量 |
| 修完这两处 | **55** | 暴露的是 datagen `ToolsRecipeProvider`（合并时"上游主体进来了、fork 的辅助声明没了"） |
| 对 `ToolsRecipeProvider` 跑 `scripts/port/upstreamtake.py --apply` | **44** | 该文件自身 55 → 4；脚本判定 KEPT（整树也从 55 → 44） |
| 剩余 | 44 | **全部**集中在 `ToolsRecipeProvider`（外加 `TinkerTools:396` 的 `addProvider` 歧义） |

`ToolsRecipeProvider` 现在剩下的 6 类问题（都是"上游文件 + Fabric 管线"要补的活）：

1. `import net.minecraftforge.common.Tags` → Porting Lib `io.github.fabricators_of_create.porting_lib.tags.Tags`（11 条）
2. `ArmorItem.Type` → 本移植的 `ArmorSlotType`（21 条）
3. `CompoundIngredient` / `DifferenceIngredient` / `ModLoadedCondition` → Fabric 的
   `DefaultCustomIngredients.all/any/difference` 与 `DefaultResourceConditions`（需改调用写法）
4. `ToolsRecipeProvider(PackOutput)` → `(FabricDataOutput)`（基类要的是 Fabric 的）
5. `buildRecipes(Consumer<FinishedRecipe>)` 的覆写签名与基类 `BaseRecipeProvider` 不一致
6. `toolBuilding(consumer, item, folder, Pattern)` 多了一个 `Pattern` 参数；`TinkerTools:396` 的
   `pack.addProvider(ToolsRecipeProvider::new)` 在 Fabric 下 `addProvider` 有歧义（要显式指定
   `FabricDataGenerator.Pack.Factory`）

### 2026-09-30 傍晚续：datagen 集群修完，翻到"伤害类型常量"

| 步骤 | `--gen` 整树 |
|---|---|
| 上一轮结束 | 44（全在 `ToolsRecipeProvider` + `TinkerTools` 1 条） |
| 修完 `ToolsRecipeProvider` 的 6 类管线问题 | 8 |
| 修 `IToolRecipeHelper` / `IMaterialRecipeHelper` 的残留（`modResource`→`location`、Forge `CompoundIngredient`→`DefaultCustomIngredients`、`MaterialIngredient.fromItem`→`of(part, ANY)`、补 `Objects` 导入、新增 4 参 `toolBuilding` 重载） | **8**（全在 `DamageSpillingEffect`） |

`DamageSpillingEffect`（**fork 独有文件，上游 3.12.1 没有**）用了 8 个伤害类型常量，本树里没有：
`PLAYER/MOB_ATTACK_{FIRE,MAGIC,EXPLOSION,BYPASS_ARMOR}`。它们在 fork 的
`slimeknights/tconstruct/shared/TinkerDamageTypes.java`（第 20–27 行定义、第 34 行起 `context.register(...)`）里，
本树现在只有 `slimeknights/tconstruct/common/TinkerDamageTypes.java`（`SMELTERY_HEAT` 等，没有这 8 个）。

补法（下一位接力）：把这 8 个 `ResourceKey.create(Registries.DAMAGE_TYPE, TConstruct.getResource(...))` 常量、
它们的 `DamageType` 注册，以及 `src/generated/resources/data/tconstruct/damage_type/{player,mob}_attack_*.json`
一并从 fork 取回来（fork 里都有），然后重跑 `--gen`；这一步之后应该就摸到 0 了。

### 2026-09-30 夜：伤害类型补齐，翻到 `ModifierRecipeProvider`

| 步骤 | `--gen` 整树 |
|---|---|
| 上一轮结束 | 8（全在 `DamageSpillingEffect`） |
| 补回 8 个 `PLAYER/MOB_ATTACK_*` 伤害类型常量 + 8 个 `damage_type/*.json` | 5 |
| 删掉 fork 遗留的 `SpillingFluidProvider` / `AbstractSpillingFluidProvider`（无任何引用；本树已用 3.12 的 `FluidEffectProvider` 生成 `tinkering/fluid_effects/*.json`，上游也没有这两个文件） | **81** |

新集群（`tools/data/ModifierRecipeProvider.java`）：`TinkerModifiers.bronzeReinforcement` 之类的常量没了、
`modResource(...)` 之类的 fork helper 仍在用。**这正是上一轮 `ToolsRecipeProvider` 的翻版**——建议直接上
`python3 scripts/port/upstreamtake.py --apply src/main/java/slimeknights/tconstruct/tools/data/ModifierRecipeProvider.java`
（该脚本会自己比较该文件的错误数并回滚）。之后大概率还有 `MaterialRecipeProvider` / `TableRecipeProvider` /
`SmeltryRecipeProvider` 等几个同源 datagen 文件排队。
