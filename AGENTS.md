# AGENTS.md — 给接手的 AI 智能体

本仓库是 `MinecraftReconstruction` 组织下的**非官方、AI 生成（largely vibed）** Tinkers' Construct Fabric 移植工程。

## 动手之前

1. 读 [ATTRIBUTION.md](ATTRIBUTION.md) —— 归属声明与风险告知。**不得**删除或弱化其中"非官方 / AI 生成 / 未经原作者审核"的表述。
2. 读 [docs/STATUS.md](docs/STATUS.md) —— 当前进度、已核实的数据、已完成的步骤。
3. 读 [docs/PLAN.md](docs/PLAN.md) —— 阶段划分与验证标准。

## 硬性规则

- **归属**：任何新增文档、发布页面、CHANGELOG 都必须保留对 SlimeKnights（Tinkers' Construct / Mantle）与 AlphaMode（Hephaestus / Mantle-Fabric）的署名，并说明本仓库是 AI 生成的衍生作品。
- **许可证**：`LICENSE`（MIT，SlimeKnights 版权）不得修改或删除。第三方依赖（尤其 Porting Lib 的 LGPL）条款必须遵守。
- **不得**暗示原作者认可、参与或背书本仓库。
- **不要**在没跑过验证命令的情况下把任务标记为完成。

## 仓库拓扑

```
origin      = MinecraftReconstruction/TinkersConstruct   （本仓库）
hephaestus  = Alpha-s-Stuff/TinkersConstruct             （Fabric 移植来源，冲突调和的主要参照物）
upstream    = SlimeKnights/TinkersConstruct              （原版 mod）
```

同组织的配套仓库：`MinecraftReconstruction/Mantle-Fabric`（Fabric 版 Mantle，**当前卡点**）、
`MinecraftReconstruction/Mantle`（上游 Forge 版，仅作 diff 基准）。

## 环境

- 构建需 **JDK 21**：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`
- 首次 Gradle 依赖下载约 9 分钟，之后编译 1–3 分钟
- 本机 Minecraft 目录与实例清单见 [docs/STATUS.md](docs/STATUS.md)「热测试环境」一节（**注意本机没有 1.20.1 实例**）

## 提交与文档纪律

- 每完成一个子系统：跑通验证 → 更新 `docs/STATUS.md` → 提交（commit message 注明对应的上游范围或阶段）。
- 学到的新事实（API 变更、坑、失败尝试）写进 `docs/STATUS.md`，不要只留在对话里 —— 下一位接手者看不到你的对话。
- 失败但值得记录的尝试也要写，注明"已试过、无效及原因"。
