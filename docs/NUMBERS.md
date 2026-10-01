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

### 2026-09-30 深夜：datagen 家族继续消队（81 → 36）

技巧确认：**凡是"整树只报几条、且与上游 3.12.1 只差几十行"的文件，一律 `git checkout v3.12.1.231 -- <file>`
再补 Forge→Fabric 管线**，比逐个改 fork 版快一个数量级（fork 版是 1.18 时代的旧 TConstruct 代码，
用的是 `bronzeReinforcement`、`MaterialIds.bloodbone`、`ArmorItem.Type` 这些已经不存在的东西）。

| 步骤 | `--gen` 整树 |
|---|---|
| 上一轮结束 | 81（全在 `tools/data/ModifierRecipeProvider`） |
| 取上游 `ModifierRecipeProvider` + 管线（2 处 `CompoundIngredient`、6 处 `DifferenceIngredient`、15 处 `IntersectionIngredient`、2 处 `modLoaded`、`Fluids.MILK`+`FluidType.BUCKET_VOLUME`→`Milk.STILL_MILK`+`FluidConstants.BUCKET`） | 3 |
| 3 处 `FluidContainerIngredient` 需要 `.toVanilla()`（Fabric 下它是 `CustomIngredient`，不是 `Ingredient`） | 3（转到 `AbstractEnchantmentToModifierProvider`） |
| 取上游 `AbstractEnchantmentToModifierProvider`（`Target.DATA_PACK` + `saveJson`） | 2（转到 `ArmorModelProvider`） |
| `ArmorModelProvider`：`FabricDataOutput` + `SlimeskullItem.MODEL_LOCATION`→`TConstruct.getResource("slimeskull")`（本树的 `SlimeskullItem` 是 fork 的 Porting Lib 渲染实现，没有该常量） | 1（转到 `TrimMaterialPaletteGenerator`） |
| `TrimMaterialPaletteGenerator` / `TinkerTrimMaterialPaletteGenerator` 改成 `FabricDataOutput` | 8（转到 `ToolItemModelProvider`） |
| `ToolItemModelProvider` + `AbstractToolItemModelProvider.armor(...)`：`ArmorItem.Type` → 本移植的 `ArmorSlotType` | 1（转到 `ModifierModelMapProvider`） |
| `ModifierModelMapProvider` 改成 `FabricDataOutput` | 16（转到 `ModifierModel` / `AbstractMaterialDataProvider` / `MaterialDataProvider`） |
| `ModifierModel.EMPTY`：接口是 fork 的 `Mesh getQuads(...)`，上游文件还在覆写 `addQuads` → 改回 `getQuads` 返回 `EMPTY_MESH` | 16 |
| `AbstractMaterialDataProvider`：`JsonRedirect` 第三个参数（`Predicate<JsonObject>`）、`MaterialJson` 需要 `JsonCondition` 包装、新增 `fluidTagExistsCondition`（`FluidTags.create` 是 Forge 对 `FluidTags` 的补丁 API） | 10（转到 `MaterialDataProvider`）+ 2 |
| 取上游 `MaterialDataProvider`（`material(...)` builder API） | 2（转到 `AbstractMaterialStatsDataProvider`） |
| `AbstractMaterialStatsDataProvider`：`super(output, Target.DATA_PACK, ...)` + `saveJson(...serialize())`（fork 版的 `saveThing`/`convert` 都不存在） | **36**（全在 `MaterialRecipeProvider`） |

**顺手修掉一个真 bug**：上一轮把 Forge `CompoundIngredient.of` 映射成了 `DefaultCustomIngredients.all`
（= AND／交集），但 Forge 的 `CompoundIngredient` 是 **OR**（任一子项匹配即可），语义对应 `DefaultCustomIngredients.any`。
证据：上游 `ModifierRecipeProvider` 用 `ingredientFromTags(TinkerTags.Items.MELEE, ...HARVEST, ...LAUNCHERS, ...LEGGINGS)`
表示"任一工具类都行"；AND 的话这个 ingredient 永远为空。`ToolsRecipeProvider` 的过路人材质（`fakeIngot`）和
箭矢图案（图案 **或** 铸模）同理。已在 `ToolsRecipeProvider` 两处改成 `.any(...)`。
映射表：`CompoundIngredient`→`any`、`IntersectionIngredient`→`all`、`DifferenceIngredient`→`difference`。

### 2026-09-30 深夜（二）：整树口径收敛到个位数，但**分块口径暴露大批隐藏错误**

第二轮继续按"取上游 + 补管线"推进，整树 `--gen` 一度掉到 **1 条**（`ToolContainerScreen`），
但**整树口径会漏报**（javac 报错后停止归因，整个文件可以完全不出现）。于是跑了唯一可靠的口径：

```
scripts/port/truecount.sh .port/true_now.txt    # 299 个包目录逐个显式编译，约 20 分钟
```

进度到一半时：**49 个包有错、合计 417 条**。也就是说：

- **整树口径**：81 → 1（看着快完了）
- **分块口径**：还有数百条（真实的"还剩多少活"）

两类数字的差就是那批**从未被归因过的文件**（`GuiTankModule` 里 `this.horizontal` 这种字段都没声明、
`NormalModifierModel` 里 `textures[index]` 这种早就删掉的变量……都是这么被翻出来的）。

本轮新修（都在 `--gen`/分块下逐个验证过）：

| 文件/位置 | 问题 | 修法 |
|---|---|---|
| `MaterialRecipeProvider` + `IMaterialRecipeHelper` | fork 版还在用 `MaterialIds.bloodbone` 这类旧 id；helper 只有带 `forgeTag` 的签名 | 取上游文件；helper 补无 boolean 重载（用 `fluid.getForgeTag() != null` 复刻上游 `FluidObject#ingredient` 的语义）、`materialMelting(FluidObject)`、`compatMeltingCasting(..., altTag, folder)` |
| `MaterialMeltingRecipeBuilder` | `FluidValues` 是 long，builder 只收 int | 加 long 重载 |
| `MaterialManager` | 物料 tag 表还是 `Map<ResourceLocation,…>`（fork 版），packet/`GenericTagUtil` 是 `Map<TagKey,…>` | 字段与 `updateMaterialsFromServer` 统一成 `Map<TagKey<IMaterial>,List<IMaterial>>`；`getValues` 同步 |
| `MaterialManager`(GSON) | 引用了 Mantle 的 `JsonCondition`/`ConditionDeserializer` | 改回 TCon 自己的 `library.json.JsonCondition` + `ConditionSerializer` |
| `MaterialStatsManager` | 残留的 `deserializeMaterialStat`（用已删除的 `materialStatTypes`/`GSON`/`getStatsClass`） | 删除死代码（新实现已内联在 `deserializeMaterialStatsFromContent`） |
| `AbstractMaterialTraitDataProvider` | `saveThing`/`convert` 都不存在 | `super(output, Target.DATA_PACK, folder, MaterialTraitsManager.GSON)` + `saveJson(...build())` |
| `AbstractToolDefinitionDataProvider` | `PackType.SERVER_DATA`/`ToolDefinitionLoader.GSON`/`saveThing`/`definition.validate` | 上游写法：`Target.DATA_PACK` + `saveJson(id, ToolDefinitionData.LOADABLE.serialize(data))` |
| `AbstractModifierProvider` | fork 版的 `allModifiers`/`addModifier` 与上游新结构混在一起 | 取上游；条件用 `ConditionJsonProvider.write` 包成 `"condition": {"fabric:conditions":[…]}`（与 `ConditionSerializer` 读法一致） |
| `AbstractFluidEffectProvider` / `FluidEffectProvider` | `addFluid(...)` 只收 int（`FluidValues` 是 long）；`CraftingHelper.serialize` | 全改 long；条件写成 Fabric 的 `fabric:conditions` |
| `AbstractMaterialRenderInfoProvider` | `PackType.CLIENT_RESOURCES`、缺 `existingFileHelper`、`saveThing` | `Target.RESOURCE_PACK` + 3 参构造 + `saveJson` + `MaterialPartTextureGenerator.runCallbacks` |
| `MaterialModel` / `MaterialModifierModel` / `NormalModifierModel` / `OverslimeModifierModel` / `ModifierModel.EMPTY` | Fabric 渲染接口是 `Mesh getQuads(...)`，上游文件还在写 `addQuads`；`MantleItemLayerModel.getQuadsForSprite` 返回 `List<BakedQuad>` | 统一走 `ToolModel.ofQuads(...)`（把 vanilla quads 包成 Mesh，`ToolModel` 里已有该 helper，改为 public）；补 `getLoader()`；`MaterialModel.getMaterialSprite` 按上游补回 |
| `Config.CLIENT` | `renderSleevesItem` 只赋值没声明 | 补声明（builder 里的 `.define("renderSleevesItem", true)` 本来就在） |
| `ModifierClientEvents` | `offhand`/`mainhand` 是旧 fork 的变量名 | 改成 `held`/`player.getMainHandItem()`，并保留"只处理副手"的 else 分支 |
| `ToolRenderEvents` | 局部变量 `context`（`UseOnContext`）把 `WorldRenderContext` 参数遮蔽了；`ToolHarvestLogic` 已不存在；`matchType` 丢失 | 局部改名 `useContext`；恢复上游的 `IsEffectiveToolHook.isEffective` + `AOEMatchType` 计算；回调返回 `false`（交给 vanilla 画主方块轮廓） |
| `PlateArmorModel` | `ISafeManagerReloadListener.create(...)` 不存在（Fabric 侧要固定 ID） | 改用 `IdentifiableISafeManagerReloadListener` |
| `GuiTankModule` | `horizontal`/`fluidLoc` 字段没声明、缺 `isFluidHovered`/`getFluidUnderMouse` | 补字段 + 8 参构造（保留 7 参重载）、补两个方法 |
| `ToolContainerScreen` / `ToolContainerMenu` | `getSlots()`（Forge）→ `getSlotCount()`（Porting Lib）；`menu.getPlayer()` 缺 `@Getter`；tank 是 `Storage` 不是 `StorageView` | 逐个对症修（tank 传 `tank.iterator().next()`） |
| `ShieldBannerModifierSpriteSource` | `SpriteSources.register` 在 vanilla 是 private、`SpriteContents` 的 5 参构造是 Forge 的 | 加两条 accesswidener（`stringVertex`、`SpriteSources.register`，对应上游 AT）；`SpriteContents` 用 4 参 |

⚠️ **Mantle 侧**：本轮还修了一个真 bug —— `Mantle#register()` 把 `mantle:tag_filled` 注册成了
`TagEmptyCondition.SERIALIZER::test`（语义反了）。TCon 生成的数据里有 **342 个** JSON 用这个条件，
兼容材料会在标签**不存在**时反而加载。已修、已 push、已 `publishToMavenLocal` 为
`1.11.DEV.1e53afad`，TCon 的 `mantle_version` 已同步（classpath 也已重新生成）。

### 2026-10-01 凌晨：JEI 15.20 与 world 模块清完，隐藏队列仍在（本轮交接）

本轮的 checkpoint（每个都 push + `git ls-remote` 复核过）：

| commit | 内容 |
|---|---|
| `32f391871f` | datagen 家族 8 个文件取上游 + 补管线（81 → 36） |
| `ca2544e034` | MaterialRecipeProvider / client model Mesh / AW 两条 |
| `05a08b41f1` | 停用 REI（移到 `src/rei-unsupported`）、JEI 15.20 适配开始 |
| `8044d90de8` | JEI 流体类型、TankBlockEntity/SearedTankBlock、ModifierManager tags |
| `7207abf7c7` | JEI 15.20 收尾 + world 模块（Config 选项、worldgen bootstrap） |

**口径提醒（最重要的一条）**：整树 `--gen` 现在在 **1～20 之间来回跳**，但那是假象 ——
每修好一个文件，javac 就"多归因一个文件"，于是又冒出新错误。真实剩余量看分块口径
（`scripts/port/truecount.sh`），本轮中途的完整快照是 **170 个包 / 1620 条**（约 3 小时前的数据，
之后又修了 ~60 处）。**不要用整树数字判断进度。**

本阶段验证过的"等价替换"清单（可直接脚本化，附错误签名）：

| 错误签名 | 替换 |
|---|---|
| `ForgeMod.BLOCK_REACH.get()` 之类 | `PortingLibAttributes.BLOCK_REACH`（字段，无 `.get()`） |
| `FluidType.BUCKET_VOLUME` | `FluidConstants.BUCKET` |
| `IIngredientHelper#getUid(...)` | `getUniqueId(...)` |
| `ITypedIngredient#cast(TYPE)` | `getIngredient(TYPE)`（返回 `Optional`） |
| `addRecipeArrowWidget().setPosition(x,y)` | `addRecipeArrow().setPosition(x,y)` |
| `addDrawableWidget(D).setPosition(x,y)[.setTooltip(t)]` | `builder.addDrawable(D,x,y)`（tooltip 用 `plugin/jei/util/TooltipWidget`） |
| `addIngredients(FabricTypes.FLUID_STACK, List<FluidStack>)` | 先 `stream().map(FluidIngredients::of).toList()` |
| `PackOutput`（datagen provider ctor） | `FabricDataOutput` |
| `BlockBehaviour.BlockStateBase.OffsetType` | `BlockBehaviour.OffsetType` |
| `Holder::get` | `Holder::value` |
| `getData().getTraits()` | `ToolTraitHook.getTraits(definition, MaterialNBT.EMPTY)` |
| 旧 fork 的枚举 key（`slimeLeaves.get(SlimeType.X)`） | 新 key（`FoliageType.X`，必要时 `.asSlime()` / `.asDirt()`） |

**下一步（按顺序）**：
1. 重跑 `scripts/port/truecount.sh` 拿最新完整清单（约 20 分钟），按包推平；
2. 还剩的已知簇：`BlockModelSkullRenderer.renderModelLists`（AW 可解）、`BuddingCrystalBlock`、
   `FluidEffectManager`（`CraftingHelper.processConditions` → Fabric 条件判断）、`GenericNBTProvider` 的
   `DataGenerator#getPackOutput`（已把该 ctor 删掉，需确认调用点）；
3. 全树 0 之后：`./gradlew build --offline` → `runData` → **把 `src/generated` 与上游 3.12.1 逐文件 diff**
   （这是最能抓语义错误的闸门）→ `runServer` → `runClient`。

### 2026-10-01 凌晨（二）：第二次分块口径 + "级联" 的发现

第二次 `truecount`（`.port/true_now2.txt`，22:09 启动）跑到 100/299 包时的快照：**793 条**，
最重的包：

| 包 | 条数 |
|---|---|
| `common/data/tags` | 81 |
| `library/recipe/casting/material` | 52 |
| `shared` | 50 |
| `library/client/model/block` | 36 |
| `library/recipe/ingredient` | 35 |
| `library/recipe/casting` | 29 |
| `library/data/recipe` | 24 |
| `library/utils` | 21 |

⚠️ **注意这些数字里有相当一部分是"级联"**：例如 `common/data/tags` 这个 chunk 单独编译时，
`ToolStack` 会因为 `IToolContext#getDefinition` 未实现而报 10 条、`ModifierNBT`/`ModDataNBT` 各报十几条 ——
这些**不是各自独立的问题**，而是"根因文件坏了，javac 找不到成员"的连带噪声。
**判据：修完根因后要重跑同一个 chunk，看这些连带的数字是否一起消失。**

本阶段（22:00–23:00）修完并已 push 的内容见 `HANDOFF.md` 第 21 节。

### 2026-10-01 凌晨（三）：改用 Gradle 全量口径，791 → 292

> 口径：`./gradlew compileJava -I scripts/port/maxerrs.gradle --offline`（javac 带 `-Xmaxerrs 100000`，
> 不会提前停手），日志存 `.port/errors_gradleN.txt`。**这是当前唯一可信的进度条。**

| 检查点 commit | 条数 | 本轮修的东西 |
|---|---|---|
| （接手） | **791** | — |
| … | 431 | datagen/tag provider、JEI 15.20、Forge 钩子替换、客户端模型 |
| `3138afa6d3` | **423** | `MaterialBlockModel`：FRAPI `emitBlockQuads` + `CustomParticleIconModel` |
| `17f35d671e` | **390** | tag provider 家族：Fabric datagen 构造函数、builder 链式顺序、`CostTagAppender` 换类型 |
| `5bd3fc3076` | **364** | 精灵/材质数据生成器：`FabricDataOutput`、补回上游 `outputPath`/overrides |
| `7b2479ff84` | **307** | 装饰模型改 FRAPI mesh（`Mesh getQuads`）、烘焙 quad 上色/自发光 helper（行为差异 #34/#35） |
| （本轮末） | **292** | 滑翔弹弓（补回 `IchorSlimeSlingItem`）、`saveThing`→`saveJson`、gadget 配方 provider |

**踩坑记录（本轮新增）**
- `MantleBakedModel.Builder` 无参 `build()`（Forge 版要传 `RenderTypeGroup`）。
- Fabric `QuadTransform` 只能作用在发射期的 `MutableQuadView`，**没有** `toBakedQuad`，所以 Forge 的
  `IQuadTransformer#processInPlace(List<BakedQuad>)` 在 Fabric 上要么改成 mesh 烘焙、要么直接改
  `BakedQuad#getVertices()`（本移植两种都用，见行为差异 #34/#35）。
- `TagAppender`/`FabricTagBuilder` 的**静态类型**决定链式调用能不能继续：`getOrCreateTagBuilder(...)`
  返回 `FabricTagBuilder`，而 `tag(...)` 返回 vanilha 的 `TagAppender`（没有 `add(T...)`）。
  想继续链式必须用前者，且 `add(...)` 要放在 `addTag(...)`/`addTags(...)` **之前**。

### 2026-10-01 凌晨（四）：继续推平 292 → 147

| 检查点 commit | 条数 | 修的东西 |
|---|---|---|
| `0a591fa729` | **254** | 书本内容（材料页取上游 + Fabric 适配）、谓词注册表去掉 `AND/OR/INVERTED`（Mantle 自己注册） |
| `f1d62a23b0` | **224** | 客户端模型 helper、`ClampedItemPropertyFunction`、储罐光照、melter 菜单的 `FluidTransferHelper` |
| `d631ec1bac` | **208** | 流体储罐/管道改 Transfer API、容量 long 化、drain 的 `getListenerPos` |
| `e9328fb090` | **177** | 代理储罐的流体视图、爆炸事件（Porting Lib `ExplosionEvents`）、`TinkerDamageTypes.source`、配置字段补回 |
| `ba45575de6` | **160** | datagen 构造函数、条件求值（`ResourceConditions`）、材料谓词序列化器、JsonUtils/Util |
| `52992e8b15` | **147** | 客户端 loader helper、材料贴图 quad、block tag 原料、书里头盔槽位 |
| `6db0fea7f7` | **139** | 战利品表 helper（`COPY_NAME`/`COPY_MATERIAL`/`ADD_ANVIL`）、`Target.DATA_PACK`、DynamicTextureLoader |
| `3fa173e336` | **131** | gadget 配方（叶子蛋糕用 `FoliageType`）、`ItemFrameRenderer.blockRenderer` 的 AW |
| （本轮结束） | **123** | 旧版装饰模型（染液/破损/材料）改 mesh + `ColorLoadable.parseString` |

### 2026-10-01：**Gradle 全量编译归零（0 条）** ✅

`./gradlew compileJava -I scripts/port/maxerrs.gradle --offline` → **BUILD SUCCESSFUL，0 条 error**，
产出 2830 个 class 文件。继续推进的记录：

| 检查点 commit | 条数 | 修的东西 |
|---|---|---|
| `52992e8b15` | 147 | 客户端 loader、材料贴图 quad、书里头盔槽位 |
| `6db0fea7f7` | 139 | 战利品表 helper、`Target.DATA_PACK`、DynamicTextureLoader |
| `3fa173e336` | 131 | gadget 配方、`ItemFrameRenderer` AW |
| `9579adc097` | 123 | 旧装饰模型改 mesh、采矿变量 |
| `e788e3084e` | 64 | 命令/战利品/储罐、datagen 注册表 provider、屏幕与渲染器 |
| `1b8a100801` | 39 | render data 桥接、储罐监听、slime 方块、clone item stack |
| `2ee641db4e` | 22 | 渲染器/网络/import、删掉 Forge path type 覆写 |
| （最后一轮） | **0** | 配置字段补回、爆炸交互枚举与构造函数、gadget 杂项 |

⚠️ **数字为什么会"变多"**：javac 分层报错。**归属阶段**的错误（找不到符号、不重写）一消，
**解析阶段**的错误（构造函数不匹配、类型转换）才会被报出来；再加上修坏一处会连带一片。
所以只有"整树全量、修完再看"的数字才可信，中间出现 +N 不代表倒退。

**新发现的坑**
- Mantle 1.11 的 `Loadable` 用 `convert(JsonElement, String)` / `getIfPresent(JsonObject, String)`，
  老代码里的 `JsonHelper.parseColor/getJson/convertToItemStack` 这些垫片都要换成
  `ColorLoadable.ALPHA.parseString(...)` / `JsonHelper.getJson(resource, location)` / `ItemStackLoadable.*`。
- `PackOutput.Target` 在 Fabric 侧是 `DATA_PACK` / `RESOURCE_PACK`（不是 `SERVER_DATA`）。
- `Ingredient#isSimple/isVanilla` 是 Forge 补丁，Fabric 侧要改用 `CustomIngredient#requiresTesting()`
  和 `instanceof CustomIngredient` 判断。

## 2026-10-01 夜：客户端冒烟口径（紫黑物品清零）

这一轮把口径从"编译/datagen 是否通过"换成了**客户端能不能画**，为此在
`slimeknights.tconstruct.testing.TConstructClientSmokeTest`（开发环境专用，`fabric.mod.json` 的 `client` 入口）
里加了三个可量化的检查，进世界后自动跑一次并打 `[smoketest]`：

1. `CreativeModeTabs.tryRebuildTabContents` + 每个创造页签的图标/全部物品 `ItemRenderer#getModel`
2. 所有 `tconstruct:` 物品里有多少个落到"missing model"（就是玩家看到的紫黑格）
3. 全量纹理（13506 张）里有多少张没进 block atlas，并把完整清单写到 `run/smoketest-atlas-missing.txt`

| 指标 | 修之前 | 修之后 |
|---|---|---|
| `Unable to bake model` 条数 | **109**（38 工具 + 71 桶） | **0** |
| 用 missing model 的 `tconstruct:` 物品 | 数百（工具/盔甲/桶/铸模/方块） | **0 / 682** |
| 进 block atlas 失败的地图集纹理 | 744 | 621（全部是**本来就不该进** block atlas 的：`tinker_armor/**` 473 张是盔甲层贴图、`particle/entity/mob_effect/colormap` 各有自己的 atlas、`gui/*` 30 张是直接绑定的界面贴图） |
| 生成的 `models/item` 文件 | 71 | **501**（上游 3.12.1 是 499，多出的两个是本移植独有的血内容） |
| `runData` | 绿 | 绿（新增 8 个 provider 后仍然绿） |
| `runClient` | 能进世界，开创造栏崩 | 能进世界，0 崩溃、0 烘焙失败 |

**根因不是"资源缺文件"，是 Forge 与 Fabric 的两处机制差**（详见 BEHAVIOUR-DIFFERENCES 36/37）：

- Fabric 不会自动把"模型/代码用到的贴图"塞进 atlas，也不会像 Forge 那样在流体 type 上提供渲染扩展 ——
  所以流体的 `FluidRenderHandler` 与精灵图目录源必须显式注册；
- Forge 给 `Material#sprite` 打了"用当前烘焙的 atlas"的补丁，Fabric 没有：烘焙期间
  `Minecraft#getTextureAtlas` 还是**上一次 reload** 的 atlas（首次为空），于是任何在烘焙里取贴图的代码都会拿到
  `null`。38 个工具模型和 71 个桶模型的失败都是这一条。

**本轮 5 个 commit**（都已 push 到 `mcr/upstream-3.12.1`）：

| commit | 内容 |
|---|---|
| `9f379612e0` | 流体渲染：补回被合并丢掉的 4 个流体 provider；创造栏剩余 12 处 `TableBlockItem` 强转改防御式 |
| `735d5a7b07` | 修掉 109 个烘焙失败的模型（`MaterialModel` 透传 spriteGetter、`FluidContainerModel` 用烘焙期 getter）；Mantle 侧 `ClientFluidTextureHandler`（`c7098eb1`） |
| `b03b62708f` | 补回 `ToolItemModelProvider` / `ArmorModelProvider` / `TinkerTrimMaterialPaletteGenerator` / `ModifierModelMapProvider`，精灵图源合并进已注册的 provider，补注册盔甲与装饰模型 loader |
| `f40e5a3987` | 补回 `TinkerCommons` 的 5 个客户端 provider（`models/item` 71 → 501，与上游对齐）+ `TinkerData` 让 existing-file-helper 也能看到 Porting Lib 的资源 |
| （本轮最后） | `FluidTextureCameraProvider`（62 张相机贴图）+ 本文档 |

### 2026-10-01 深夜：冒烟审计抓出来的最后一批紫黑格（已修，未跑热测试）

把审计从"物品有没有模型"扩到"物品的模型有没有画到缺失贴图"+"物品有没有语言键"之后，一次客户端运行就列出了全部剩余问题：

| 审计 | 修前 | 修后（静态判断题，热测试暂缓） |
|---|---|---|
| `models/` 用 missing model 的物品 | 0 / 678 | 0 / 678 |
| `sprites/` 画到缺失贴图的物品 | **7**：`mud_bricks`、`mud_bricks_slab`、`mud_bricks_stairs`、`lavawood`、`lavawood_slab`、`lavawood_stairs`、`ichor_bottle` | 0（两个方块整组删除、灵浆瓶 id 对齐上游） |
| `lang/` 没有翻译的物品 | **10**：上述 6 个方块物品 + `blood_slime`、`blood_congealed_slime`、`blood_enderbark_roots`、`blood_slime_ball`、`blood_bottle`、`blood_bucket`、`ichor_bottle` | 0（血内容按旧 fork 的英文名补回，见 BEHAVIOUR-DIFFERENCES #26/#43/#44） |

另外补了 mod 元数据（Mod Menu 卡片）：名字从 fork 的 `Hephaestus` 改成
`Tinkers' Construct (Unofficial Fabric Port)`、版本号基准从 `3.6.4` 改成 **`3.12.1`**
（现在是 `1.20.1-3.12.1.DEV.<hash>`）、补了 128×128 的方形 `icon.png`
（Mod Menu 要求方形，fork 的 `logo.png` 是 600×100，所以一直显示灰色问号）、
来源/issue 指向 canonical 仓库、authors 加上 `MinecraftReconstruction`。

## 2026-10-01 深夜（二）：Tier A 收尾 —— 行为差异从 70 条压到多少

### 口径：先把"差异"分类

| 类别 | TCon | Mantle | 说明 |
|---|---|---|---|
| 语义等价 / 只是换了挂点 | 20 | 10 | 玩家与 addon 都感知不到 |
| **功能缺失** | 16 | 7 | 真正少了能力 |
| 近似（表现可能略不同） | 5 | 5 | 需实机比对 |
| 移植多余内容 | 2 | — | 血史莱姆 |
| 待核对 | 1 | 1 | `FluidAction` simulate；`ShapedRetexturedRecipe` |
| 修复 / API 恢复（不计入差异） | 4 | 5 | — |

### Tier A 做完的结果（本轮验证：编译 + `runData` + 一次 `runClient`）

| 条目 | 之前 | 之后 | 依据 |
|---|---|---|---|
| TCon #1 流体容器渲染层 | 功能缺失 | **闭合/等价** | Fabric 把所有非方块物品按 `item_entity_translucent_cull` 渲染；`[smoketest] layers/` 实测 `copper_can`、`molten_iron_bucket` 均为 translucent |
| TCon #16 `tool_hook` 原料 | "Fabric 没有原料类型注册表" | **闭合** | 实际上 `TinkerMaterials` 早已用 `CustomIngredientSerializer.register` 注册；注释是过时的 |
| TCon #29 附魔槽位开关 | 功能缺失 | **闭合** | 加 `transitive-mutable` AW 后恢复两条 action（并挪到 `ConfigEvents.LOADING`，否则 dev 环境抛 "Cannot get config value before config is loaded"） |
| TCon #39 无 FluidType 流体的汽化 | 功能缺失 | **闭合** | Mantle `FluidTypes.getType` 给这类流体一个默认 type，钩子对所有流体可达 |
| Mantle #5 `PlayerDestroyItemEvent` | 功能缺失 | **闭合为 N/A** | Fabric 上没有任何代码能订阅该事件，本来就无可观察差异 |
| Mantle #13 书本结构预览的渲染层 | 功能缺失 | **闭合** | 新增 `ModelLayers`（记录每层 RenderType+quads）+ `StructureElement` 逐层绘制 |
| Mantle #6/#14 模型渲染类型 | 功能缺失 | **已缩小** | 层信息不再丢弃（`MantleItemLayerModel`/`NBTKeyModel`/TCon 的两个流体模型都记录），Fabric 侧由物品/方块层落地；Fabric 本身没有逐 quad 渲染层 |

### 顺手修掉的**真 bug**（不属于"行为差异"，是内容加载失败）

客户端日志里原本有 **12 个配方解析失败**，两个根因：

1. `NoContainerIngredient.toJson` 用 `JsonUtils.withType` 写 Forge 的 `"type"` 键，Fabric 读的是 `"fabric:type"`
   → 6 个配方（`seared/scorched` 的 `fluid_cannon`/`alloyer`/`melter`/`gauge`）解析失败。
2. `MaterialIngredient.toJson` 直接写 `material.toString()`（Java 对象字符串）而不是序列化谓词
   → 另外 6 个配方（`travelers` 盔甲四件、`fake_ingot_to_block`/`fake_block_to_ingots`）解析失败。

修完实测：`Parsing error loading recipe` **12 → 0**，同时
`models/` 0、`sprites/` 0、`lang/` 0（`tconstruct` 物品 672 个全量审计）。

### 剩下的（Tier B/C）

* 需要自建 Fabric 事件 + mixin：TCon #10/#30（弓/弩事件）、#5（自定义显示上下文）、#7/#8（击退抗性同步）、#2（提供者扩展点）、Mantle #2（可取消的暴击事件）。
* 大工程：TCon #32（REI 插件，约 110 处 API 适配）。
* 不可消除：Mantle #1（Forge tag `remove`）、Mantle #21（流体单位 droplet vs mB）、TCon #3（只有 Forge 版的模组集成）。
* 需要联网加依赖：TCon #18（能量 API）、#9（Dummmmmmy / Crafting Tweaks）。
