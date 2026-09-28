# 进度状态

> 最后更新：2026-09-28　|　维护者：AI 智能体（人类指导）　|　性质见 [ATTRIBUTION.md](../ATTRIBUTION.md)

本文件是**给下一个接手者/AI 智能体的交接文档**。请在每个里程碑后更新它。

## 一句话现状

Fabric 移植版（fork 自 Hephaestus）目前停在 **Tinkers' Construct 3.6.4 + 328 个端口提交**（最后提交 2026-01-08），
距上游 **3.12.1** 落后 **2469 个提交 / 约 3 年**。同步是可行的，但工作量按"重写一个中型模组"计。

## 仓库拓扑

```
MinecraftReconstruction/TinkersConstruct   ← 本仓库（fork of Alpha-s-Stuff/TinkersConstruct）
  ├── origin      = 本仓库
  ├── hephaestus  = https://github.com/Alpha-s-Stuff/TinkersConstruct.git  (Fabric 移植来源)
  └── upstream    = https://github.com/SlimeKnights/TinkersConstruct.git    (原版 mod)

MinecraftReconstruction/Mantle-Fabric      ← Fabric 版 Mantle（fork of Alpha-s-Stuff/Mantle）
MinecraftReconstruction/Mantle             ← 上游 Forge 版 Mantle（仅作 diff 基准）
```

本地克隆位于 `~/Desktop/repo/mr-tinkers`、`~/Desktop/repo/mr-mantle-fabric`。

## 关键坐标（已核实）

| 项目 | 值 |
|---|---|
| 本仓库分支 | `1.20.1`，tip `058cd8946`，2026-01-08 |
| 上游目标 | `v3.12.1.231`（`upstream/1.20.1` tip `5492b20d7d`，2026-09-28） |
| 共同祖先 | `d2826fb44b`（2023-11-18） |
| 端口自有的提交 | 328 |
| MEM 端口所包含的最后一个上游 tag | `3.6.4.113` |
| 依赖的 Mantle | `slimeknights.mantle:Mantle:1.20.1-1.9.29x`（来自 `https://mvn.devos.one/snapshots/`） |
| 上游 3.12.1 要求的 Mantle | `[1.11.113,)` ← **这是必须先解决的依赖** |

## 量化差距（实测，不是估算）

**上游 `3.6.4.113 → v3.12.1.231`（仅 `src/main/java`）**

- 1924 个文件改动，**+119,995 / −49,844 行**

**与端口改动的重叠**

| 类别 | 文件数 | 含义 |
|---|---|---|
| 双方都改（冲突面） | **750** | 需要逐个人工/AI 调和 |
| └ 其中上游侧含 Forge 引用 | 292 | 需要重新套用 Fabric 转换 |
| 仅上游改 | 1174 | 可以较干净地带入 |
| 仅端口改 | 157 | 需要保留端口的 Fabric 实现 |

**真实合并实验**（`git merge --no-commit v3.12.1.231`）

```
冲突文件总数   4048
  ├── JSON     3343   ← 生成产物，应当用 datagen 重新生成，不要手工合并
  ├── Java      696   ← 真正的战场（其中 280 个上游侧含 Forge 引用）
  └── 其它        9   （gradle/properties/toml/yml/md）
自动合并成功   2659
```

> 关键判断：**83% 的冲突是生成产物**。先解决 Java，再跑 datagen 重生成 JSON，冲突量会塌缩两个数量级。

## Mantle 侧的现状（另一条必须走通的路）

`MinecraftReconstruction/Mantle-Fabric` 的 `1.20.1-update` 分支是 **Mantle 1.11 的 Fabric 移植 WIP**，
提交信息本身记录了进度：`87 errors left` → `43 errors left` → **`6 errors left (I'm lazy ok)`**（2026-01-12）。

⚠️ **但 `6 errors left` 是假象。** javac 默认 `-Xmaxerrs 100` 截断输出，且在符号无法解析时会抑制依赖它的后续错误。
本机复现（JDK 21，`./gradlew compileJava`，首次约 9 分钟含依赖下载）首轮确实只报：

```
5 errors，全部集中在同一个文件 src/main/java/slimeknights/mantle/client/model/util/MantleItemLayerModel.java
  cannot find symbol: IGeometryBakingContext   (x2)
  cannot find symbol: RenderTypeGroup          (x3)
```

这 5 个已在 Mantle 侧修复（提交 `756dad64`，Porting Lib 2.3.16 移除了 Forge geometry API）。
**修完之后用 `-Xmaxerrs 100000` 重跑，真实剩余错误是 `158 errors / 55 files`** —— 与 Tinkers 侧同一教训：
流体 API（49/158）是最大的一块，其次是 Capability 与注册表迁移。

详见 [Mantle-Fabric/docs/STATUS.md](https://github.com/MinecraftReconstruction/Mantle-Fabric/blob/1.20.1/docs/STATUS.md)。

## 已完成

- [x] fork 三个仓库到 `MinecraftReconstruction`
- [x] 三个 remote 配置、全量历史拉取
- [x] 量化上游差距、重叠面、真实合并冲突
- [x] 定位 Mantle 依赖来源（`mvn.devos.one/snapshots`）与 Fabric Mantle 源码仓库
- [x] 复现 Mantle `1.20.1-update` 的编译错误并定位根因（Porting Lib geometry API 移除）
- [x] Mantle 首批 5 个编译阻断已修复；并揭穿 `6 errors left` 假象（真实 158 errors / 55 files）
- [x] 归属声明与本文档

## 未完成 / 下一步

1. **继续修 Mantle 的编译错误（还剩 158 个）**：流体(49) → Capability → 注册表 → datagen 类型 → 零散项
   （详见 Mantle 的 STATUS，含按文件/按符号的完整清单）
2. **把 Mantle 1.11 Fabric 构建出来并发布**（`publishToMavenLocal` 或自有 maven）
3. 用新 Mantle 编译现有 TCon 端口，确认基线不回归
4. 执行 TCon 合并：`git merge v3.12.1.231` → 先啃 696 个 Java 冲突
5. 跑 `datagen` 重生成 JSON，替代 3343 个 JSON 冲突
6. 验证：数据生成 → 专用服务器启动 → 冶炼炉/工具玩法链路 → 渲染截图比对
7. 发布与版本号策略

## 热测试环境（已勘察，尚未执行）

本机 HMCL 配置（`~/Desktop/.hmcl.json`）指向两个游戏目录：

| 配置 | 游戏目录 | 可用版本 |
|---|---|---|
| `Home`（最近使用） | `~/Library/Application Support/minecraft` | `1.21.11`、`26.2-Fabric` |
| `Default` | `~/Desktop/.minecraft` | `1.16.5-Forge`、`1.21.1-Fabric`、`1.21.1-Forge`、`1.21.10-Fabric`、`1.21.11-Fabric`、`26.1-Forge` 等 |

**注意：两个目录都没有 1.20.1 实例**，而当前目标是 1.20.1。热测试前需要先做其中一件事：

(a) 用 HMCL 或脚本装一个 1.20.1 + Fabric Loader 实例；
(b) 直接用 Loom 的 `./gradlew runClient`（会自动下载 1.20.1 并启动开发客户端，最快，但不带本机的资源包/模组）；
(c) 若改打 1.21.1 目标，则本机已有 `1.21.1-Fabric` 实例可直接用（Alpha 的 Mantle 也有 `1.21.1` 分支）。

Java：本机有 JDK 8 / 11 / 21 / 23，**1.20.1 模组请用 JDK 21**（`/Library/Java/JavaVirtualMachines/jdk-21.jdk`）。
