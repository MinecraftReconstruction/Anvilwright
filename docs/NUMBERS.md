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
