# 上游 3.12.1 合并（`mcr/upstream-3.12.1`）

> 性质见 [ATTRIBUTION.md](../ATTRIBUTION.md)。本文件记录**这次合并怎么做的、哪些是机器第一遍、哪些必须人工复核**。
> 合并本身由 AI agent 执行（人类设定目标、审查、验证），逐条决策日志见
> [merge-3.12.1-decisions.txt](merge-3.12.1-decisions.txt)。

---

## 2026-09-29 · 度量口径被推翻："Lombok 假错误"比想象的大得多，而且总数是**下界**

这一轮把前面几轮一直没解开的"Lombok 假错误"查清楚了，结论**改变了统计口径**，先看这段再干活。

### 1. 假错误不是 ~28 条，是 ~130 条

同一棵树、同一个 classpath，只换 javac 的停止策略：

| 运行方式 | 报错数 | getVariant/getId/isSuccess/"cannot be applied" 家族 |
|---|---|---|
| 默认（错误即停在 FLOW） | **462** | 有 |
| `-XDshould-stop.ifError=GENERATE` | **319** | **0** |

两次运行取差集：默认多出 **132 条**，它们**全部**是 `cannot find symbol`（缺 `getVariant()`/`isSuccess()`/`getId()`/
`getCraftingResult()`）、`constructor X cannot be applied to given types`（缺 `@RequiredArgsConstructor` 生成的构造器）、
`X is not abstract and does not override abstract method getId()`（缺 `@Getter` 生成的实现）这几类。
⇒ **这些是 javac 在"已经编译失败"状态下的产物，不是代码缺陷。**

### 2. 真因（不是 Lombok 坏了）

Lombok **没有**问题：单独用 `javac` 编一个 `@Getter` + `@RequiredArgsConstructor` 的类，成员生成正常。
问题在于 **javac 出错后会停在 FLOW 阶段，而在这个模式下 Lombok 注入的成员对其它编译单元的解析不可见**。
旁证：`RecipeResult.java`（有 `@Getter boolean success` / `@RequiredArgsConstructor`）在自己的文件里**零错误**，
但别的文件调用 `isSuccess()` 就报"找不到"；`PartRecipe`（与上游逐字节相同）也是同一现象。

### 3. 新发现：**错误总数是下界，不是真值**

`FluidTankBase.java` 里 `fill(FluidStack, FluidAction)` 的 `@Override` 根本不成立（Porting Lib 的 `FluidTank`
只有 `insert/extract(TransactionContext)`），单文件编译报 5 条错——但**任何一次全量编译日志里都没有它**。
验证方法：故意往 `FluidTankBase.java` 里塞一行 `private final int zzzProbe = "not an int";`，**全量编译依然 0 条报告**，
总数纹丝不动（439 → 439）。

原因还是那条老坑的放大版：javac 只对**它真正走到**的类做检查（`-verbose` 显示 2063 个源文件里只有 ~344 个
`[checking]`）。`FluidTankBase` 的检查入口在上游（`TankBlockEntity` → `FluidTankAnimated`）本身就编译失败，
于是它整棵子树都被跳过。

**这意味着**：随着真错误被修掉，**以前被隐藏的文件会浮出来，总数会阶段性上涨**。别把"涨了"当成改坏了，
要看 diff 是不是"新文件开始被检查"。

### 4. 顺带挖出 14 处**真实的重复定义**（合并时把两版代码都留下了）

这些错误只在 GENERATE 模式下才报（`method ... is already defined`），默认模式看不到：

| 文件 | 重复的东西 |
|---|---|
| `SideButtonsWidget` | `setFocused` / `isFocused` 各两份（底部那份带 `// TODO: do I need to use these?`） |
| `ToolAttackUtil` | `getAttributeAttackDamage` / `dealDefaultDamage` / `attackEntity` ×3 |
| `ModifiableArmorItem` | `canWalkOnPowderedSnow` / `canPerformAction` / `getStatInformation` |
| `MaterialMeltingRecipeBuilder` | `material(MaterialVariantId, FluidStack)` |
| `ISmelteryRecipeHelper` | `metalMelting(...)` / `gemMelting(...)` 各两份 |
| `common/config/Config$Client` | `showModifiersInJEI` 声明两次 |

### 5. 新的工具与口径

- `scripts/port/fastcompile.sh` —— 直接调用 javac 跑全量，**18 秒**一轮（Gradle 要 40 秒），错误列表可与 Gradle 对照；
  加 `--gen` 则用 GENERATE 停止策略（**只用来找重复定义，不能当主口径**，它的检查覆盖只有 283 个类 vs 344 个）。
- `scripts/port/summarize.py` —— 按"缺失的包 / 缺失的符号 / 错误种类 / 最差文件"归类，用来按家族干活。
- 主口径仍然是 `scripts/port/errors.sh`（Gradle），但要记住它**是下界**。

---

## 这次合并的基本事实

| 项 | 值 |
|---|---|
| 合并来源 | `SlimeKnights/TinkersConstruct` 的 tag **`v3.12.1.231`**（2026-09-21，`a5a0324954`） |
| 合并基点 | `d2826fb44b`（2023-11-18） |
| 上游新增提交 | **2454** |
| 冲突文件 | **4048** |
| 冲突 hunk | 1872（Java） |
| 结果规模 | 26610 文件变化，+313298 / −84628 |

## 冲突分类与处理规则（第一遍）

| 类别 | 数量 | 处理 | 理由 |
|---|---|---|---|
| `src/generated/**` | 3279 | **取我们这侧** | 生成产物不手工合并，Java 修完后重跑 datagen 覆盖 |
| `src/main/java/**`（我们的版本是 Fabric 适配过的） | 299 文件 | **整文件保留我们** | 这类文件里我们的改动是 Forge→Fabric/Porting Lib 的替换，取上游会直接抹掉移植成果。**上游在这些文件里的新逻辑必须人工重新套用** —— 这是主要工作队列 |
| `src/main/java/**`（我们的版本没碰 Fabric API） | 205 文件 | **整文件取上游** | 我们的改动只是旧逻辑，取上游等于直接拿到 3.12.1 的新实现；若引入 Forge API，编译错误会精确指出位置 |
| `src/main/resources/assets/tconstruct/models/item/**` | 61 | **接受上游的删除** | 上游把这批流体桶模型改成 datagen 生成（`src/generated/...`）。⚠️ 我们在那边改过 loader 名（`forge:bucket` → `porting_lib:fluid_container`），**这个改动必须重新落到 datagen 的 provider 代码里** |
| `src/main/resources/book/**` | 3 | 取上游内容 + **重新套用我们的 `forge:nbt` → `fabric:nbt` 归一化** | 共 17 处，已用脚本改写并校验 JSON |
| `src/test/**` | 17 | 保留我们这侧 | 移植把不能编译的测试放在 `src/test/javad`（非 source set）；上游新增测试暂未纳入，记为待办 |
| `build.gradle` / `gradle.properties` / `settings.gradle` / wrapper | 4 | **以我们的 Fabric 构建为准**，只挑上游的版本号 | 上游是 ForgeGradle/Neo，整份取过来等于放弃 Loom |
| `.github/**` / `README.md` | 4+1 | 上游模板 + 保留我们的归属声明 | README 的"非官方/AI 辅助"声明不得删改 |

### 判定"我们的版本是否 Fabric 适配过"的规则

对每个冲突文件，取 **stage 2（我们的版本）** 全文，若命中以下任一记号则整文件保留我们：

```
net.fabricmc | porting_lib | fabricators_of_create | ConditionJsonProvider | FluidStorage
ContainerItemContext | ItemStorage | StorageView | RenderDataBlockEntity | FabricBakedModel
QuadEmitter | ModInitializer | ClientTooltipComponent | DefaultResourceConditions | EnvType
FabricLoader | FluidVariant | ItemVariant | TriState | TransferVariant | RenderContext
```

这样做的**代价**（必须知道）：这 299 个文件里上游的 3.12.1 改动**没有进来**。它们是本次合并的
主要人工队列 —— 见下面的待办。

## 构建改动（已做）

- `mod_version` → `3.12.1`（合并带入）
- `mantle_version` → **我们自己移植的 Mantle 1.11**（`1.11.DEV.<sha>`，从 `mavenLocal` 解析）
- `mantle_range` → `[1.11.113,)`，与上游要求一致
- `build.gradle`：**去掉 jar-in-jar 的 `include(...)`**，Mantle 变成独立前置；新增 `mavenLocal()`
  （构建前需要先在 `Mantle-Fabric` 跑 `./gradlew publishToMavenLocal`）
- `minecraft_range` 修正为 `[1.20.1,1.20.2)`

## 合并后立刻做的完整性检查

1. **冲突标记清零**：`git grep -lE "^(<{7,}|={7,}|>{7,}|\|{7,})"` → 0
   （注意：重命名冲突用的是 **8 个** `<`，第一遍按 7 个字符检测漏了 22 个文件，已修）
2. **JSON 有效性**：全仓库 14462 个 JSON 逐个解析，37 个"不合法"里
   - **0 个是合并造成的**
   - 12 个在我们合并前就是那个状态（`book/*/appearance.json` 用了 `0xE5C682` 这种 Minecraft/mantle
     宽松解析器接受的十六进制颜色，严格 JSON 解析器会报错 —— 属误报）
   - 25 个是上游新增文件，**上游自己也不合法**（例如 `book/encyclopedia/pt_br/armor/tconstruct_slime_helmet.json`
     的 `text` 数组两个对象之间少逗号、`tr_tr/.../tconstruct_stripping.json` 少一个 `}`）——
     这是投给上游的 bug 报告素材

## 待办（按优先级）

1. **编译循环**：`./gradlew compileJava`，按错误逐个修。预计会遇到两类：
   - 取上游的那 205 个文件把 Forge API 带回来了 → 按 [docs/PLAN.md](PLAN.md) 的映射表重新套用 Fabric 转换
   - 保留我们的那 299 个文件缺少上游新逻辑 → 用 `git diff <base> v3.12.1.231 -- <file>` 逐处补
2. **重复类清理**：有 34 个文件是我们改过、上游"删除/移动"的同名类（例如
   `ToolFluidCapability`、`ToolActionsModule`），现在两边可能同时存在 → 编译期会报 duplicate class
3. **datagen 侧要补的改动**：流体桶模型的 loader 名改回 `porting_lib:fluid_container`
4. **重跑 datagen**：Java 修完后 `./gradlew runDatagen`，让 `src/generated` 与代码一致
5. **测试**：把上游新增的测试纳入（或明确放弃），并确认 `src/test/java` 仍可编译

## 合并后的错误分类（2026-09-29 07:00 实测）

`./gradlew compileJava`（`-Xmaxerrs` 全量）→ **2734 errors / 578 files**。

### 按"这个文件当时怎么解的"分布

| 来源 | 错误数 | 占比 | 含义 |
|---|---|---|---|
| 自动合并 / 未冲突 | 1707 | 62% | 合并没有冲突，但上游 3.12.1 的新代码与我们的 Fabric 上下文拼在一起，需要重新套用转换 |
| 保留我方（OURS） | 856 | 31% | 3.6.4 时代、按 **Mantle 1.9** 写的代码，现在要升到 **Mantle 1.11 / 3.12** 的 API |
| 取上游（THEIRS） | 171 | 6% | 上游的 Forge 代码照搬进来了，需要重新套 Fabric 转换 |

错误最多的文件（含当时的决策）：`fluids/TinkerFluids`(71,OURS)、`tools/TinkerModifiers`(66,OURS)、
`tables/TinkerTables`(60,OURS)、`tools/logic/ModifierEvents`(55,自动合并)、`library/modifiers/Modifier`(45,OURS)。

### 按根因分布

| 根因 | 数量 | 说明 |
|---|---|---|
| `net.minecraftforge.*` 包不存在 | **561** | 最大一类。细分：`fluids` 118、`common` 53、`fluids.capability.IFluidHandler` 46、`common.crafting.conditions` 31、`common.capabilities` 31、`eventbus` 19、`registries` 16、`client.model.geometry` 12、`network.NetworkEvent` 11 …… 全部需要按 [PLAN.md](PLAN.md) 的映射表重新套 Fabric |
| `slimeknights.mantle.*` 包/类缺失 | 101 | 见下表，多为 **Mantle 1.9 → 1.11 的包移动**，部分是**我们的 Mantle 缺上游 1.11 的功能** |
| 其它符号缺失（`cannot find symbol`） | 1919 | 混在上述各类里 |
| 可选兼容模组的 Forge API | ~30 | `mezz.jei.api.forge`(10)、`dev.gigaherz.jsonthings`(12)、`com.illusivesoulworks.diet`、`blusenrize.immersiveengineering`、`com.google.errorprone.annotations`(3) —— 这些是**只有 Forge 版**的可选兼容，Fabric 侧要么移除、要么改用对等 API |
| `dev.onyxstudios.cca.api.v3.*` | 10 | CCA 是 Fabric 依赖，但**合并后的 build.gradle 里没有**（上游 3.12 用 CCA 做实体组件？）—— 需要确认是否补上依赖 |

### Mantle 1.9 → 1.11 的包移动（可直接机械修）

| TCon 里引用的旧位置 | 我们 Mantle 1.11 里的实际位置 | 处理 |
|---|---|---|
| `slimeknights.mantle.data.GenericLoaderRegistry`（26 处报错，52 个文件引用） | `slimeknights.mantle.data.registry.GenericLoaderRegistry` | 包名移动。**但 API 也变了**：新的是 `GenericLoaderRegistry<T extends IHaveLoader>`（`RecordLoadable`），旧的用 `IGenericLoader` —— 不是纯改 import，见下面的策略讨论 |
| `slimeknights.mantle.client.model.data.{IModelData,ModelDataMap,SinglePropertyData}` | 我们这边是 `client.model.{ModelData,ModelProperty}`（没有 `IModelData`） | 需要把调用点改成 Fabric 的 `ModelData` 语义 |
| `slimeknights.mantle.client.model.fluid.{FluidCuboid,FluidsModel}` | `client.render.FluidCuboid`（有）、`FluidsModel` 无 | 部分可改 import，部分需要重写 |
| `slimeknights.mantle.client.model.inventory.{InventoryModel,ModelItem}` | 无（我们的是 `client.render.RenderItem`） | 需要重写 |
| `slimeknights.mantle.data.fabric.IdentifiableISafeManagerReloadListener` | 无（Fabric 侧直接实现 `IdentifiableResourceReloadListener`） | 需要重写 |
| `slimeknights.mantle.data.predicate.fluid.FluidPredicate` | 无 | 需要重写或用 Porting Lib 的 fluid predicate |
| `slimeknights.mantle.loot.builder.GenericLootModifierBuilder` | 无 | 需要重写 |
| **`slimeknights.mantle.util.html.*`**（22 处） | **我们的 Mantle 完全没有这个包** | ⚠️ **上游 Mantle 1.11.115 有这个包，我们没有** —— 说明我们的 Mantle 1.11 也落后于上游 Mantle 1.11，需要再做一次 **Mantle 侧的上游合并**（把 SlimeKnights/Mantle 的 1.11 分支合进我们的 1.11） |

## 策略问题：下一个会话应该先回答的

这次合并对 299 个文件选了"**保留我们的 Fabric 适配**，上游新逻辑待人工补"。实测证明**方向可能是反的**：

| | 保留我们 | 取上游 |
|---|---|---|
| 优点 | 不丢移植成果 | **与 Mantle 1.11 API 天然配套**（上游 3.12.1 就是配 Mantle 1.11 的），编译错误会少很多 |
| 缺点 | 这些文件是 3.6.4 + **Mantle 1.9** 时代代码，与 Mantle 1.11 的 API 不匹配（`GenericLoaderRegistry` 就是典型） | 丢掉 Fabric 适配，需要重新套转换（但转换模式是可重复的机械替换） |

**建议**：下一个会话先做一个小实验 —— 挑 3~5 个"保留我们"里报错最多的文件（例如 `fluids/TinkerFluids`、
`tools/TinkerModifiers`、`tables/TinkerTables`），改用**上游版本 + 重新套 Fabric 转换**，比较两者的
错误数与人工成本，再决定是否整体翻转那 299 个文件的规则。`git diff <base> 1.20.1 -- <file>` 就是该文件的
"移植配方"，可以照着在新版本上重做一遍。

### ✅ 这个实验已经做过了（2026-09-29 08:35）—— 结论是**不要翻转**

做法：建 `mcr/flip-experiment` 分支，把那 299 个文件**全部换成上游 3.12.1 版本**（不重新套转换），
然后编译：

| 方案 | 结果 |
|---|---|
| 当前分支（保留我们的 Fabric 适配） | **2223 errors，编译跑完** |
| 全量翻转成上游版本 | **≥3472 errors**，而且 javac 在 6G 堆下 OOM，**编译都没跑完** |

原因很清楚：上游版本是 **Forge** 代码，换过去等于把 Forge API 全部请回来（`FluidAction`、`IFluidHandler`、
`SubscribeEvent`、`Capability` …），而我们的版本虽然用的是旧 API，但至少是 **Fabric** API。

**采纳的结论**：保持当前策略 —— 保留我们的 Fabric 适配，逐文件把上游 3.12 的新逻辑补进来。
遇到"上游把整个类重写了"的文件（例如 `library/data/material/AbstractMaterialDataProvider`），
才按那次的做法单独重新套转换（那是本次唯一一个这样做成功并验证的例子）。
`mcr/flip-experiment` 分支已删除，实验数据记在这里备查。

## 已知会误导人的检查

- `git diff` 的 rename detection 在 15987 个文件规模下会**自动跳过**（git 会打印 warning），
  所以"上游删除/移动了多少文件"这类统计需要用 `git diff --no-renames` 或直接比对 tree。
- 冲突标记有两种写法：普通冲突是 **7 个** `<`，`rename` 类冲突是 **8 个** `<` **且带文件路径**。
  只按 7 个字符检测会漏掉 22~535 个文件（本次就踩了这个坑）。

## 错误数收敛过程（`mcr/upstream-3.12.1`）

| 阶段 | 错误数 | 做了什么 |
|---|---|---|
| 合并后首次编译 | **4540** | 那时还没解决冲突（OOM 截断前） |
| 解决完冲突 + 修 17 个结构损坏 | **2734** | 见上文 |
| 按移植历史推断并套用 Forge→Porting Lib import 映射（182 文件）+ 补 errorprone 注解依赖 | **2223** | `FluidStack`→`porting_lib.fluids.FluidStack` 等 10 组 |
| 自动补齐合并丢掉的 import（135 处 / 76 文件） | **2037** | `RecordLoadable`、`LoadableField`、`ModelData`、`TooltipKey`、`IJsonPredicate`… |
| 符号改名：`TooltipKey` 路径搬迁、`SimpleFlowableFluid`→`SimpleFlowingFluid`、`ICondition` 用法改名 | **1920**（当前） | 见提交历史 |
| 用 Porting Lib 垫片顶掉 `FluidAction`（46 文件） | **1639** | 见提交历史 |
| 删除只有 Forge 版的可选集成（jsonthings 21 文件 + diet + IE） | **1594** | 见提交历史 |
| **合并上游 Mantle 1.20**（Mantle 侧，见下） | **1539** | `mantle_version` 切到 `1.11.DEV.ad2e7db0` |
| hook/module 体系第一波：257 个「上游版本不含 Forge API」的文件直接取上游 | **1494**（当前） | 358 个相关文件里的 257 个 |
| 「管线类」文件按套路迁移：`TinkerModifiers` → `TinkerTables`/`DisplayCastingRecipe`/`PartRecipe` → datagen provider（+ 补 `model_generators` 模块）→ `ToolActions` 残留 → `TinkerModule` → `TinkerStationBlockEntity` | **1302**（当前） | 每批都 commit |

## 当前剩余 1302 个错误的分类（2026-09-29）

| 类别 | 错误 | 文件 | 说明 |
|---|---|---|---|
| **F. 其它 / 级联** | 797 | 292 | 绝大多数是下面几类的级联：某个类型解析失败后同文件的后续错误都会归到这里。**按类别清掉下面几类，F 会跟着塌** |
| **B. Forge 事件 / capability / datagen** | 314 | 122 | 事件映射表见下节；capability 需要换成 Fabric 的 lookup API 或 Porting Lib |
| **A. Forge 流体 API** | 63 | 32 | `IFluidHandler` 的语义（`Transaction`/`StorageUtil`），`FluidAction` 已用垫片顶掉 |
| **D. hook / module 体系** | 57 | 35 | 剩余的是「上游版本含 Forge API」的那 79 个文件里的 |
| **C. Mantle API** | 49 | 24 | 主要是 `IGenericLoader` → Mantle 1.11 的 `RecordLoadable`/`IHaveLoader` 体系 |
| **E. 可选兼容** | 22 | 15 | JEI 的 `api.forge`、`IClientItemExtensions` 之类 |

**建议顺序**：B（映射表已备齐）→ D（35 个文件，其中 `ModifierEvents` 最大）→ C（loader API）→ A（流体语义）→ E。

### B 类的后续批次（同一晚继续）

| 批次 | 内容 | 结果 |
|---|---|---|
| Mantle 发布 POM 带依赖 + TCon 显式声明 CCA | Mantle 的 publication 之前**不带任何依赖**，导致 TCon 从 mavenLocal 取到 Mantle 后丢失 CCA（36 个错误）。Mantle 改成 `from components.java`（产出 remap jar + 31 个依赖 + `.module`）；TCon 也显式声明 CCA | 1226 → **1190** |
| `BlockItemProviderCapability` | Forge capability → 直接 `instanceof BlockItem` 判断（Fabric 无 per-stack capability；上游只有一个默认实现） | 1190 → **1176** |
| 世界生成整块 | `WorldgenProvider` 去掉 Forge 生物群系修改器（Fabric 用 `WorldEvents` 的 `BiomeModifications` 覆盖）并接进 `TConstructData.buildRegistry`；`TinkerWorld`/`TinkerStructures` 跟随上游改名（`configuredEarthGeodeKey`→`configuredEarthGeode` 等）、`DeferredRegister`→Porting Lib `LazyRegistrar`、补 `enderSlimeTreeTall`；`TinkerStructures` 整体取上游 + 管线替换 | 1176 → **1121**（当前） |

同时新建了本仓库的 [BEHAVIOUR-DIFFERENCES.md](BEHAVIOUR-DIFFERENCES.md)（4 条：渲染层、capability 扩展点、删掉的三个 Forge 独占集成、`FluidAction` 垫片的 simulate 语义待核对）。

## ⏩ 接手点（B/D 进行中，2026-09-29 晚）

当前 **1291 errors**。已经做完的 B 类零散项：`BreakSpeed` 的 import（24 个文件）、
Forge→Porting Lib 的 `ToolActions`（11 个文件）、`TinkerModule` 的注册表映射、
`TinkerStationBlockEntity` 的四个 Forge 钩子。

**接下来最值得做的两块，各自都有现成模板**：

### 1. 自定义 ingredient（B 类里约 30 个错误，6 个文件）

3.12 新增了 `library/recipe/ingredient/{BlockTagIngredient,InstrumentIngredient,MaterialValueIngredient,
NoContainerIngredient,ToolHookIngredient,NestedIngredient}.java`，它们用 Forge 的
`IIngredientSerializer`/`CraftingHelper`，Fabric 上没有这套 API。

**模板就在我们端口里**：`library/recipe/ingredient/MaterialIngredient.java`（1.20.1 分支）
用的是 Fabric API 的 **`net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient` +
`CustomIngredientSerializer`**，并且把 JSON 写成 `"fabric:type": ...`（和我们在书本 JSON 里做的
`forge:nbt` → `fabric:nbt` 归一化是同一套东西）。照它改即可。

### 2. 客户端模型 API（B 类里约 45 个错误，3 个文件）

**进度**：`UniqueGuiModel` **已移植完**（1272 → 1263）。它的模板是我们端口里的
`TankModel.BakedGuiUniqueModel`：Fabric 的 `ForwardingBakedModel`（`wrapped` 字段）+
Porting Lib 的 `TransformTypeDependentItemBakedModel`（**4 参数**的
`applyTransform(ItemDisplayContext, PoseStack, boolean, DefaultTransform)`），
`DefaultTransform` 是那个接口的嵌套类型、不用 import。bake 签名是
`bake(BlockModel owner, ModelBaker, Function<Material,TextureAtlasSprite>, ModelState, ItemOverrides, ResourceLocation, boolean isGui3d)`。

**`FluidContainerModel`（20 个错误）比预想的深**：它是 Forge 的
`net.minecraftforge.client.model.DynamicFluidContainerModel` 的**整份克隆**——这个类 vanilla 根本没有，
所以要自己实现流体容器模型那套逻辑。涉及的 Forge 专有物：
`RenderTypeGroup` / `DynamicFluidContainerModel.getLayerRenderTypes(boolean)` /
`IClientItemExtensions`（`net.minecraftforge.client.extensions.common`）。
其中 render type 部分可以照 Mantle 的做法用 `RenderTypeUtil.get(id)`；
`IClientItemExtensions` 要看我们端口里 `library/client/item/ModifiableItemClientExtension` 是怎么处理的。

**`MaterialBlockModel`（15 个错误）**：除了 `IGeometryBakingContext`，它还依赖 **旧版 Mantle 的模型 API**
（`ExtraTextureContext`、`SimpleBlockModel.bakeDynamic(ctx, transform)`、`bakedBuilder(owner, overrides)`、
Forge 的 `IQuadTransformer`）。Mantle 1.11 的对应物是：
`SimpleBlockModel.bakeDynamic(BlockModel owner, ModelState transform)`、
`SimpleBlockModel.bakedBuilder(BlockModel, ItemOverrides, boolean isGui3d)`、
Porting Lib 的 `QuadTransformers`/`QuadTransform`（见 `ColoredBlockModel`），
而 `ExtraTextureContext` **已经不存在**——需要把"替换贴图"改到 sprite getter 那一层。

`library/client/model/tools/MaterialBlockModel.java`、`library/client/model/UniqueGuiModel.java`、
`library/client/model/FluidContainerModel.java` 都是 3.12 新增文件，用 Forge 的
`IGeometryBakingContext`（还有 `ExtraTextureContext` 那种"包一层 context 换贴图"的写法）。

**模板在 Mantle 侧**（我们刚合并过的那个仓库）：

- `MantleItemLayerModel` / `SimpleBlockModel`：`resolveParents(..., BlockModel owner)`、
  `bake(BlockModel owner, ..., ResourceLocation location, boolean isGui3d)` —— Porting Lib 2.3.16
  的 `IUnbakedGeometry` 就是这套签名；Forge 的 `RenderTypeGroup`/`getMaterial` 概念已经不存在，
  贴图直接从 `owner.getMaterial(name)` 取
- 文档见 Mantle 仓库的 [docs/BEHAVIOUR-DIFFERENCES.md](https://github.com/MinecraftReconstruction/Mantle-Fabric/blob/mcr/mantle-1.11/docs/BEHAVIOUR-DIFFERENCES.md) 第 6/13/14/15 条
  （逐层 render type 无法传递，是已知且已披露的保真缺口）

### 3. 再往后

- **D 类**：`tools/logic/{ModifierEvents,ToolEvents,InteractionHandler,EquipmentChangeWatcher,...}`，
  事件映射表见上一节；`ModifierEvents` 是 3.12 新增的中央事件类，要**和它托管的那批 modifier 一起迁**，
  否则会重复触发（我们的 `BouncyModifier`/`DoubleJumpModifier`/`EnderferenceModifier`/`DragonbornModifier`
  里各有一份同样的处理）
- **C 类**：`IGenericLoader` → Mantle 1.11 的 `RecordLoadable`/`IHaveLoader`
- **A 类**：`IFluidHandler` 的 simulate/execute 语义

## hook 体系迁移的分批实测（358 个文件）

## ✅ 已验证有效的推进套路（按这个顺序做最省力）

1. **先看上游版本的 Forge 用法是「管线」还是「逻辑」**：
   - **管线**（注册表、`RegistryObject`、datagen 生成器、事件总线注册、`ExistingFileHelper`）→
     **直接取上游版本，再把这几行换成 Porting Lib / Fabric 对应物**。这一步的收益/成本比最高：

     | 批次 | 结果 |
     |---|---|
     | `TinkerModifiers`（629→1141 行，换新 modifier 集合） | 1494 → **1443** |
     | `TinkerTables` + `DisplayCastingRecipe` + `PartRecipe` | 1443 → **1397** |
     | datagen 模型 provider 两个新文件 | 1397 → **1356** |
     | 残留的 generator/`ExistingFileHelper` import | 1356 → **1350**（当前） |

   - **逻辑**（Forge 事件、capability、`IClientItemExtensions`）→ 只能逐文件移植（实测整体翻转 +123 个错误，见下）。
2. **缺类时先查 Porting Lib 的模块清单**：`TinkerBlockStateProvider` 报
   `porting_lib.models.generators.block does not exist`，原因是 TCon 的
   `port_lib_modules` 里没有 `model_generators`。同一个类在 Porting Lib 里有、且包名/类名与 Forge 一一对应时，
   加模块 + 改 import 就能一次解决一批。
3. **Forge → Porting Lib 的固定映射**（已多次验证）：
   `RegistryObject`/`LazyOptional`/`ToolAction(s)`/`Tags`/`ItemHandlerHelper`/`ExistingFileHelper`/
   `FluidStack` → `io.github.fabricators_of_create.porting_lib.{util,tool,tags,transfer.item,data,fluids}.*`；
   `client.model.generators.{ModelFile,ModelBuilder,ModelProvider,CustomLoaderBuilder}` 与
   `{block,item}` 子包 → `porting_lib.models.generators.*`（包结构完全一致）；
   `IFluidHandler.FluidAction` → 我们的 `tconstruct.library.fluid.FluidAction` 垫片。
4. **Loom 的 `jar` 是 dev jar**：发布/被依赖必须用 `remapJar`（Mantle 侧已修，见其 STATUS.md）。

358 个文件引用被上游替换掉的 hook API。按「上游版本是否依赖 Forge API」分成三组，
并对前两组做了对照测量：

| 组 | 文件数 | 处理 | 实测结果 |
|---|---|---|---|
| 上游版本**不含** Forge API | 257 | **取上游版本**（其中只有 10 个我们这边有 Fabric 专有代码，逐个补回） | 1539 → **1494**（-45）✅ 采纳 |
| 上游版本**含** Forge API | 79 | 试过整体取上游 | 1494 → **1617**（**+123**）❌ 已回滚；这 79 个必须逐文件把 Forge 用法（事件、capability、`IClientItemExtensions`）换成移植侧写法 |
| 移植独有、上游没有 | 22 | 尚未处理 | — |

**结论**：hook 体系可以按「上游是否 Forge-clean」这个判据分批推进 —— 干净的整批取，含 Forge 的
必须逐个移植。这也解释了为什么之前"整体翻转 299 个文件"的对照实验会失败（把大量含 Forge 的文件一起翻了）。

顺带记录一个容易踩的坑：`ModifierCrystalItem` 属于第一组，但我们的**调用方**（尚未迁移的
`EnchantmentConvertingRecipe` / `ExtractModifierRecipe` / `ModifierIngredientHelper`）依赖它的
`withModifier`，而我们的版本还带着 Fabric 的创造标签页注册（`ItemGroupEvents`）。最终解法是
**取上游实现 + 把标签页注册按 Fabric 方式接回去**（用上游自己的 `addVariants()` 喂
`FabricItemGroupEntries`），而不是保留旧类。

## 下一步：hook 系统的迁移（最大的一块人工工作）

## ✅ Mantle 侧的上游合并已完成（2026-09-29）

这一项原本列在"下一步"里：TCon 3.12.1 用了一些**我们 Mantle 1.11 里根本没有**的类
（最典型的是 `slimeknights.mantle.util.html`）。已经解决 ——
`Mantle-Fabric` 的 `mcr/mantle-1.11` 合并了上游 `SlimeKnights/Mantle` 的 **`1.20` 分支**
（commit `ad2e7db0`，78 个上游提交，22 个冲突，全部解决；`./gradlew build` 通过、
`runServer` 0 ERROR/FATAL、开发自检仍然 9 passed / 0 failed）。

这次合并给 TCon 端**直接补上了缺的东西**：

| TCon 报错的符号 | 现在来自 |
|---|---|
| `slimeknights.mantle.util.html.*`（22 处报错） | 上游新增的 `util/html/{HtmlElement,HtmlGroup,HtmlString,RawHtml,HtmlSerializable}` |
| `slimeknights.mantle.data.predicate.fluid.FluidPredicate` | 上游新增的流体 predicate（已按 Porting Lib 的 `FluidType` 移植） |
| `slimeknights.mantle.network.packet.BlockEntityPacket` | 上游新增，已移植到 Mantle 自己的 `ISimplePacket.Context` |
| 顺手还拿到 | `client/book/{IHTML,HTMLUtils}`、`command/HungerCommand`、`MantleEvents`（灵魂绑定）、`BaseRegistryLoadable`/`LazyRegistryLoadable`、`HasLootContextSetCondition` |

**TCon 侧错误：1594 → 1539**（`mantle_version` 已切到 `1.11.DEV.ad2e7db0`）。

上游 3.12 把 modifier hook 体系重构了，这是当前错误里最集中的结构性变化：

| 我们代码里的旧名字 | 上游 3.12.1 的新家 |
|---|---|
| `slimeknights.tconstruct.library.modifiers.ModifierHook` | `slimeknights.tconstruct.library.modifiers.ModifierHooks`（复数，hook 定义容器）+ 各 hook 接口挪到 `library/modifiers/hook/<类别>/` 下（例如 `hook/armor/DamageBlockModifierHook`） |
| `slimeknights.tconstruct.library.modifiers.TinkerHooks` | 上游已**删除**（并入 `ModifierHooks`） |
| `IncrementalModifier` | `IncrementalModifierEntry`（`library/modifiers/`） |
| `IGenericLoader`（Mantle 1.9 的 `GenericLoaderRegistry`） | Mantle 1.11 的 `GenericLoaderRegistry<T extends IHaveLoader>` + `RecordLoadable`（`mantle.data.registry`） |

**建议做法**：这些文件基本都在"保留我们"的 299 个里。逐文件用
`git diff <merge-base> v3.12.1.231 -- <file>` 看上游怎么改的，再把我们的 Fabric 适配套上去。
先做 `library/modifiers/`（hook 定义与 Modifier 基类），因为 `tools/`、`smeltery/` 都依赖它，
顺序反了会反复返工。

### 还剩下的三类

1. **Forge 流体 API**：`FluidAction`(61)、`IFluidHandler`(15)、`FluidAttributes` —— 需要用 Fabric 的
   `Transaction` / `StorageUtil.simulateInsert` / `Storage` 语义重写（`net.minecraftforge.fluids.FluidStack`
   那一层已经用 Porting Lib 顶掉了，剩下的是动作语义）
2. **Forge 事件**：`SubscribeEvent`(28)、`BreakSpeed`(15) —— 需要换成 Fabric 的事件注册
3. **可选兼容**：JEI 的 `api.forge`、jsonthings、diet、Immersive Engineering 在 Fabric 上没有对等物，
   建议直接把这几处集成**移除**并在 CHANGELOG 里写明（而不是硬凑）

## 剩余工作的分类清单（自动生成）

[merge-3.12.1-workqueue.txt](merge-3.12.1-workqueue.txt) 是按当前编译日志自动分类的待办清单，
每一条都带文件与错误数，可以直接照着清。1736 个错误的分布：

| 类别 | 错误 | 说明 |
|---|---|---|
| **B. Forge 通用/事件/datagen API** | 377 | `SubscribeEvent`、`BreakSpeed`、`IGeometryBakingContext`、`ExistingFileHelper`、`ItemModelBuilder`… |
| **A. Forge 流体 API** | 168 | `FluidAction`、`IFluidHandler`、`FluidAttributes`（77 个文件） |
| **D. hook/module 体系** | 76 | `TinkerHooks`、`ModifierHook`、`IncrementalModifier` |
| **C. Mantle 1.9 → 1.11 API** | 51 | `IGenericLoader` 为主 |
| **E. 可选兼容 / 依赖** | 42 | JEI `api.forge`、jsonthings、diet、IE、CCA |
| F. 其它 / 级联 | 1022 | **多数是上面几类的级联**：某个类型解析失败后，同文件后续错误都会被归到这里 |

### 两个需要人来定的决策

1. **`FluidAction`（61 处，46 个文件）**：Fabric 的 Transfer API 用 `Transaction` 表达
   simulate/execute，Porting Lib 也没有 `FluidAction`。两条路：
   (a) 在 TCon 里保留一个自己的 `FluidAction` 垫片枚举，把语义翻译成 `Transaction`；
   (b) 逐处重写成 `StorageUtil.simulateInsert` / 真实调用。前者改动小但多一层抽象，后者更"正统"但工作量大。
2. **只有 Forge 版的可选兼容**（JEI `api.forge` 等）：建议移除对应集成，而不是硬凑。
   这会让 `plugin/` 目录缩小一部分，需要写进 CHANGELOG。

## 进度日志（倒序，最新的在最上面）

### 2026-09-29 · D 类「hook / module 体系」+ 上游已删文件的清理 —— 1069 → 828

本轮把整个 D 类（hook/module 体系）和它顺带暴露出来的"上游已删文件"一起清掉了，
中途两次编译测量的净效果：**1069 → 848 → 847 → 828**。

**做掉的事**

1. **删掉 58 个上游已经删掉的类**（我们这边是 1.20.1 的旧实现，上游 3.12 用 datagen + 集中式事件替代）：
   - 模块搬迁残骸：`library/modifiers/modules/{MobEffectModule,EnchantmentModule,SwappableSlotModule,ToolActionsModule,ConditionalMiningSpeedModule}`（上游搬进了 `modules/{combat,build,mining,behavior}/`，新路径文件我们本来就有）
   - `XxxModifier` → `XxxModule` 的老包装：`dynamic/**`(8)、`impl/{InventoryModifier,TankModifier}`、`util/{ModifierAttribute,ModifierStatBoost}`、`armor/{ToolBelt,ShieldStrap,Protection,Slurping,Wetting,LongFall,Unarmed,walker/*}`、`ability/{BlockTransform,BulkQuiver,TrickQuiver,Melting,Glowing}`、`defense/*`、`upgrades/*` 等
   - 其它搬迁：`library/json/RandomMaterial`→`library/materials/`、`hook/BowAmmoModifierHook`→`hook/ranged/`、`library/tools/definition/weapon/*`→`definition/module/weapon/`、`shared/TinkerDamageTypes`→`common/`、`tools/item/ModifiableBowItem`→`library/tools/item/ranged/`
   - 集中化事件后多余的那些：`ExperiencedModifier`、`SoulboundModifier`、`LeapingModifier`、`RicochetModifier`、`DragonbornModifier`、`MagicProtectionModifier`、`BlastProtectionModifier`、`MithridatismModifier`、`ReinforcedModifier`、`DenseModifier`、`FeatherFallingModifier`、`HasteModifier`、`LightspeedArmorModifier`、`AchievementEvents` 等
2. **移植 `ModifierEvents`（54 个错误 → 0）**：这是 D 类的核心，16 个 Forge 事件处理器全部换成 Porting Lib / Fabric 回调，改成显式 `init()` 注册（由 `FabricEvents` 调用）。上游用 `EventPriority.LOW` 的地方改用**注册顺序**表达。
3. **新增击退垫片**：`library/events/KnockbackEvent` + `mixin/LivingEntityKnockbackMixin`。
   Porting Lib 的 `KNOCKBACK_STRENGTH` 不给受击者实体也**不给方向 ratio**，而 `KNOCKBACK_MULTIPLIER` 是受击者的属性、
   crystalstrike 还要改方向，所以只能自己开事件（见行为差异 #11）。
4. **重写 `ToolEvents`（对齐上游 3.12 的 hook API）**：上游 3.12 把 `TinkerHooks` 并进 `ModifierHooks`，
   签名也变了（`BREAK_SPEED` 现在吃 `BreakSpeedContext`、`PROJECTILE_HIT` 多了 `notBlocked`）。
   我们的 `ToolEvents` 还是 1.20.1 版，直接**取上游文件 + 补 Fabric 事件注册**，一次编译就 0 错误。
   顺带删掉了移植自己加的砂轮保护（上游 3.12 没有这功能，见行为差异 #13）。
5. **移植 `DoubleJumpHandler`（12 → 0）**。
6. **`BowAmmoModifierHook` / `ModifiableBowItem`**：`ForgeHooks.getProjectile`（即 `LivingGetProjectileEvent`）没有 Fabric 对等物，
   把弩炮换弹药的逻辑内联进 `BowAmmoModifierHook.getBallistaAmmo`；`ArrowLoose/NockEvent` 两个扩展点丢弃并登记（#10、#12）。
7. **`FluidType.BUCKET_VOLUME` → `FluidConstants.BUCKET`**（流体类 5 个文件的简单项）。

**测量方式**（可复现）

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  ./gradlew compileJava -I work/maxerrs.gradle -Dorg.gradle.jvmargs="-Xmx6G" --offline
# work/maxerrs.gradle 只是把 -Xmaxerrs 抬到 100000，否则 javac 到 100 条就截断
```

**仍然有效的经验（本轮再次验证）**

- **"上游文件 + 只换管道"依然是收益最高的做法**：`ToolEvents`（591 行）整体换成上游 + 补注册，一次编译 0 错误；
  而逐条改旧代码要同时猜 9 个 hook 的新签名。
- **判据：类是不是"上游已删"** —— `git cat-file -e v3.12.1.231:<path>`。删了就别修，直接删并让上游的替代物接管。
- **同名/换名残留很容易找**：`XxxModifier.java` 在 `extra` 列表里、而 `XxxModule.java` 在树上存在 ⇒ 上游做的是"改名+改组合方式"。
- ⚠️ **`-Xmaxerrs` 之外还有"隐藏错误"**：某文件的父类解析失败时，javac 会跳过它整个方法体的检查
  （例：`ToolEvents` 只报 1 条错误，实际有 9 处 `TinkerHooks.X` 未解析）。**别拿单文件错误数当进度**，
  要看总数 + 逐文件 diff。
- **注意事项**：`ModifierEvents` 和各个 modifier 类必须**同时**迁移 —— 3.12 之前这些行为散在各 modifier 里，
  两边都留着会**重复触发**。

### 2026-09-29 · 探查「Lombok 生成物看不见」的假错误（未结案，已记录复现步骤）

**现象**：822 条错误里有一批看起来"不可能"的错误，例如

- `PartRecipe.java:43: PartRecipe is not abstract and does not override abstract method getCost() in IPartBuilderRecipe`
  —— 但 `@Getter protected final int cost;` 明明会生成 `getCost()`；
- `PartRecipe.java:166: cannot find symbol: method getVariant() / location: var material of type MaterialVariant`
  —— 但 `MaterialVariant` 与上游 **逐字节相同**，且它在**同一个 javac 进程**里从另一个文件调用时能正常解析。

**已排除的可能**（都实测过）：

| 猜想 | 结论 |
|---|---|
| Lombok 没跑 | 否。`-XprintProcessorInfo` 显示 `AnnotationProcessorHider$ClaimingProcessor matches [@Getter, @RequiredArgsConstructor, @Accessors]` |
| Lombok 版本太旧 | 否。1.18.22 与 1.18.30 结果一致（1.18.22 不能在 JDK 21 下跑，已顺手升到 1.18.30） |
| `lombok.config` 的 `config.stopBubbling` | 否 |
| 有文件重复注解把 Lombok 整轮带崩 | 否。修掉 `TinkerDataCapability` 的重复 `@SuppressWarnings` 只减少 2 条，`PartRecipe` 不变 |
| Gradle 增量编译/缓存 | 否。直接手写 javac（2094 个源文件、`--release 17`、`-proc:full`、`-Xmaxerrs 100000`）得到同样的 821 条 |
| `-XDshould-stop.ifNoError=ATTR` 提前停 | 同样 821 条 |
| 依赖 jar 里有多余的同名类 | 否。compileClasspath 里没有 TConstruct/Hephaestus jar |
| Lombok 对 `@RequiredArgsConstructor` + 显式委托构造器的组合失效 | 单独最小复现**不能**稳定复现；`delombok` 输出里 `getCost()` 和 8 参构造器**都生成了** |

**已确认的两个真实 bug（已修）**：

1. Mantle 1.11 的 `LoggingRecipeSerializer` 是 **interface**（上游 Mantle `1.20` 分支也是 interface），
   但 6 个 1.20.1 时代的类写的是 `extends`，导致 6 条 `no interface expected here`。
   已改成 `implements` 并把 `fromNetworkSafe`/`toNetworkSafe` 提为 `public`。
2. `TinkerDataCapability` 被合并弄出了**重复的 `@SuppressWarnings` 和重复的 `computeIfAbsent`**，已去重。

**复现方式**（给下一个接手的人）：

```bash
JAVA_HOME=... ./gradlew -I /tmp/printcp.gradle printCompileCp   # 导出 compileClasspath / annotationProcessor
# 只编译 PartRecipe 及其隐式依赖：
$JDK17/bin/javac -encoding UTF-8 -proc:full -processorpath "$AP" -cp "src/main/java:$CP" \
  -d /tmp/out src/main/java/slimeknights/tconstruct/library/recipe/partbuilder/PartRecipe.java
# 把 PartRecipe 复制成 PartRecipeProbe.java（类名一起改）放同目录，仍然复现
# 但裁到只剩「imports + 字段 + 构造器 + getSerializer」时 getCost() 又存在了
```

**判断**：这批错误里**有一部分是级联/假错误**（依赖类型解析失败后 javac 的二次诊断），
所以**当前 820 这个数字不能当作"真实剩余工作量"**。策略上：
先把 `cannot find symbol: package net.minecraftforge.*` 这类**根因**清掉，
每清一批就重新看 `PartRecipe` / `MaterialVariant` 这批文件是否自己好了，不要单独去"修"它们。

### 2026-09-29 · 根因集群清理 —— 790 → 548

按"每轮挑一个**根因**、清完立刻重测"的顺序推进，中途每一批都 commit+push。

| commit | 做了什么 | 错误数 |
|---|---|---|
| `41beeca7b7` | 补回合并时丢掉的 import（112 个） | 790 → 624 |
| `0e721bfe1d` | import 指向已搬迁类的重写（`RandomMaterial`/武器攻击模块/`PotionFluidEffect`/`EnumObject`） | 624 → 615 |
| `fcd9f4bc22` | `shared.TinkerDamageTypes`→`common.`、`world.item.ItemLike`→`world.level.`、JEI `IRecipeTooltipReplacement` 路径 | 615 → 599 |
| `8c9dccaae6` | 补回 `IdentifiableISafeManagerReloadListener`（Mantle 1.20 合并丢掉的适配器，现放在 TCon `library/utils`） | 599 → 588 |
| `fdd58bf5fb` | 收尾 `TinkerHooks`→`ModifierHooks`；`ModifierLootingHandler` 半成品方法重写；删 `Airborne`/`Maintained` | 588 → 580 |
| `b1e6f1f27f` | 传送事件改挂 Porting Lib 的 `EntityEvents.Teleport` | 580 → 564 |
| `7238c99bc3` | 再删 9 个上游已移除的 modifier 类 | 564 → 553 |
| `235a68daa2` | 删掉 3.12 之前的整套岛屿（island）实现 | 553 → 548 |

**方法论（可复制）**

1. **补 import 是可以自动化的**。从编译日志里抓 `symbol: class X` → 在源码树 + `slimeknights.mantle` 源码里找同名类；
   只有一个候选、且该包名在 `good prefix`（vanilla / JDK / Lombok / Porting Lib / Fabric API / 自家类）内才动手。
   还要**排除 Forge-only 前缀**（`net.minecraftforge.*`、`mezz.jei.api.forge.*`）——否则会往里塞根本不存在的 import。
2. **判断"这个 import 到底存不存在"要查 jar**。用 `compileClasspath` 里的 160 个 jar 建一个 4.1 万个类的索引
   （`unzip -Z1`，把 `Outer$Inner` 也写成 `Outer.Inner`），否则 `net.minecraft.world.item.ItemLike` 这种
   "看起来像 vanilla 但 1.20 已经搬走"的 import 会被误判成存在。
3. **重写 import 时禁止跨包乱猜**：候选必须与原 FQN 段数相同、且前 3 段相同。否则
   `AttributeModifier.Operation` 会被改到 `com.llamalad7...Operation` 这种莫名其妙的地方。
4. **同名类有两个（JEI/REI 各一份）就跳过**，不要猜。
5. **"上游已删 + 全树无引用"= 放心删**。用 `git ls-tree v3.12.1.231` 判定"上游已删"，再用
   "当前磁盘上没有别的文件提到它的 FQN"判定无引用。靠这一条这轮删掉了 9 个 modifier 类和 13 个岛屿类。
   ⚠️ 注意排除 `mixin/*`（靠 mixin json 引用）和 `plugin/rei/**`（Fabric 专属实现，只在 extra 集合内部互相引用）。
6. 不要凭"某个文件报错少"判断它没事：`ModifierLootingHandler` 报 0 条错误，实际里面躺着
   `event.getEntity()` 这种编译不过的残留代码（javac 跳过了它的方法体检查）。**改完文件后一定看总数。**

**仍然没解开的**：`PartRecipe` 看不到 Lombok 生成物（见上一节），`getVariant()`/`getId()`/`getCraftingResult()`
这一批共 ~28 条错误都属于它，**先别单独去修**。

### 2026-09-29 · 补回"藏在 Forge 事件处理器里的运行时注册" —— 548 → 534

清 `@SubscribeEvent` 残余时发现三处**在 Fabric 上从来没执行过**的注册（属于功能缺失，不只是编译问题）：

| 位置 | 问题 | 处理 |
|---|---|---|
| `TinkerModifiers.registerSerializers(RegisterEvent)` | **整个** loader 注册表（modifier modules、fluid effects、level displays、tank helpers…，约 300 行）被包在 `if (registry == RECIPE_SERIALIZER)` 里，靠 Forge 的 `RegisterEvent` 触发。Fabric 没有这个事件 ⇒ **运行时这些注册表是空的** | 抽成 `public static void registerLoaders()`，由 `TinkerModifiers` 析构函数调用 |
| `TinkerModifiers.commonSetup(FMLCommonSetupEvent)` | 是 `PersistentDataCapability.register()` 的**唯一调用点**，而它要注册 4 个 Fabric 玩家事件（copy/respawn/换维度/join）| 抽成 `registerCapabilities()`，同样由构造函数调用 |
| `TinkerModifiers/TinkerTables.gatherData(GatherDataEvent)` | Forge 版 datagen，`TConstructData` 调的其实是不存在的方法（javac 没报错，见上面"假错误"一节） | 补上 `FabricDataGenerator.Pack` 版本 |
| `TinkerTables.commonSetup` | 唯一在注册"必需的工作台布局" | 抽成 `TinkerTables.init()`，由 `FabricEvents` 调用 |
| `WorldEvents.wanderingTrades` / `ModifierClientEvents.playerLoggedOut` | Forge 的 `WandererTradesEvent` / `ClientPlayerNetworkEvent.LoggingOut` | 换成 Fabric 的 `TradeOfferHelper`（rare pool = 2）/ `ClientPlayConnectionEvents.DISCONNECT` |

**教训**：`@SubscribeEvent` 残留不能一律当"死代码"删。这里有一半是**真正没跑起来的业务逻辑**，
删之前一定先搜这个 handler 里调用的方法还有没有别的调用点（`rg <method>` 全树查一遍）。

### 2026-09-29 · 批量机械化清理的极限 —— 534 → 462

这一轮全部用"能批量就批量"的手段，几步都做了测量：

| 手法 | 效果 |
|---|---|
| 网络包：删掉 Forge 的 `NetworkEvent(Context)` import（TCon 的包都是 `IThreadsafePacket`，`Context` 直接继承自 `ISimplePacket`，不需要 import） | 514 → 503 |
| **把每个失效的 `net.minecraftforge.*` import 拿去 jar 类索引里找同名替代**：TierSortingRegistry / CraftingHelper / SoundActions / NonNullConsumer / FluidType / BlockSnapshot / LootModifierManager / ConditionalRecipe / TrueCondition / QuadTransformers / SimpleModelState / UnbakedGeometryHelper / ForgeI18n / MobEffectEvent / TablePrinter / SpriteSourceProvider | 534 → 489 |
| 删**未被引用**且**上游已删**的遗骸：`TagBlockPredicate`/`SetBlockPredicate`/`TagEntityPredicate`（改用 Mantle 的 `BlockPredicate.LOADER.tag/setOf`）+ CraftTweaker 插件（CraftTweaker 没有 1.20.1 Fabric 版，依赖本来也没接进 build.gradle） | 489 → 476 |
| **给 accesswidener 加一行**：`IntrinsicTagAppender` 是 protected 嵌套类，`CostTagAppender` 要直接用 | 476 → 470 |
| 删"指向不存在类的未使用 import"（自动判断文件正文有没有用到那个简单名） | 470 → 462 |

**这轮之后，纯机械手段基本用尽**：再跑自动补 import / 删死 import 都已经 0 提案。
剩下的 462 条需要**逐文件改 API**，按家族分：

| 家族 | 约多少条 | 说明 |
|---|---|---|
| Forge capability（`ForgeCapabilities`/`Capability`/`ICapabilityProvider`/`IItemHandler`） | ~42（22 个文件） | Fabric 侧要改成 `FluidStorage.SIDED` / `ItemStorage.SIDED`；Porting Lib 2.3.15 **没有** capabilities 模块（缓存里 27 个模块都列过了） |
| Forge 流体（`IFluidHandler`/`FluidTank`/`IFluidHandlerItem`/`EmptyFluidHandler`） | ~35 | Porting Lib 的 `transfer.fluid.FluidTank` 只有 `insert/extract(TransactionContext)`，没有 `fill/drain(FluidAction)`，调用点要按 Transaction 语义重写（**行为差异 #4 点名要实测的正是这里**） |
| Mantle 模型数据（`client.model.data` / `IModelData`） | ~17 | 目标 API 已经明确：`ModelData getModelData()` + `ModelProperties`（`CastingTankBlockEntity`/`TinkerStationBlockEntity` 已经是这个写法，照抄即可） |
| Mantle loadable（`IGenericLoader` / `TagPredicateLoader` / `StatPredicate`） | ~20 | 1.9 的 `IGenericLoader` → 1.11 的 `Loadable`/`RecordLoadable` |
| JEI 插件（`mezz.jei.api.forge.ForgeTypes`） | ~10 | JEI Fabric 版是 `mezz.jei.api.fabric.constants.FabricTypes.FLUID_STACK`，但成分类型从 `FluidStack` 变成 `IJeiFluidIngredient`，**不是纯改名** |
| **Lombok 假错误**（`getVariant`/`getId`/`getCraftingResult`） | ~28 | 见上面的调查，**别单独修** |
| 其余零散 | ~310 | 每个文件 1–3 条 |
