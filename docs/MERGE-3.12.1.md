# 上游 3.12.1 合并（`mcr/upstream-3.12.1`）

> 性质见 [ATTRIBUTION.md](../ATTRIBUTION.md)。本文件记录**这次合并怎么做的、哪些是机器第一遍、哪些必须人工复核**。
> 合并本身由 AI agent 执行（人类设定目标、审查、验证），逐条决策日志见
> [merge-3.12.1-decisions.txt](merge-3.12.1-decisions.txt)。

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
