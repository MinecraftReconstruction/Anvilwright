# Attribution / 归属声明

## ⚠️ 关于本仓库的性质，先读这一段

本仓库是 **非官方、由 AI 大幅生成（largely vibed）** 的试验性移植工程，隶属于 `MinecraftReconstruction` 组织。

- **绝大多数代码不是我们原创的**，而是上游作者的作品；我们的改动是在其基础上做的移植与同步。
- 本仓库**与任何原作者都没有隶属或背书关系**。原作者未参与、未审核、未认可本仓库的任何内容。
- 本仓库的代码由 AI 智能体在人类指导下生成/改写，**"largely vibed" 是准确描述**：请把它当作草稿而非参考实现。已知风险包括但不限于：流体语义错误（`simulate` 标志、容量、槽位混合）、Capability 生命周期问题、渲染类型丢失、以及批量机械改写中成片出现的一致错误。
- 出问题请找我们，不要去找原作者。
- 请勿把本仓库的内容当作官方发布的替代品。想要稳定可用的版本，请使用下面的官方/上游项目。

## 上游与原作者

| 项目 | 作者 / 维护者 | 许可 | 说明 |
|---|---|---|---|
| [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct) | **SlimeKnights**（boni、KnightMiner 等） | MIT | 原始模组。本仓库的绝大多数逻辑代码出自这里 |
| [Hephaestus](https://github.com/Alpha-s-Stuff/TinkersConstruct) | **AlphaMode (Alpha)** | MIT | Tinkers' Construct 的 Fabric 移植版。本仓库直接 fork 自它，现有的 Fabric 适配层主要出自 Alpha 之手 |

> **命名说明**：本项目（canonical 仓库 `MinecraftReconstruction/Anvilwright`）曾经沿用了上游 Fabric 移植的名字
> *Hephaestus*，2026-10-02 应其作者 AlphaMode 的要求改名为 **Anvilwright**。上面的链接仍然指向他的项目——那是署名，
> 不是本项目的名字。mod id 仍为 `tconstruct`，以便从旧版移植升级上来的存档继续可用。
| [Mantle](https://github.com/SlimeKnights/Mantle) | **SlimeKnights** | MIT | Tinkers' Construct 的强制前置库 |
| [Mantle (Fabric)](https://github.com/Alpha-s-Stuff/Mantle) | **AlphaMode (Alpha)** | MIT | Mantle 的 Fabric 移植版；本仓库依赖它 |

同时依赖且应一并致谢的第三方项目（各自版权归其作者）：

- [Porting Lib](https://github.com/Fabricators-of-Create/Porting-Lib) — The Fabricators of Create，LGPL（自由软件，详见其 LICENSE）。**它承担了本移植中 Forge API 兼容层的绝大部分工作**
- [Fabric API](https://github.com/FabricMC/fabric) — FabricMC，Apache-2.0
- [Architectury API](https://github.com/architectury/architectury-api) — Architectury 团队，LGPL-3.0
- [Cardinal Components API](https://github.com/Ladysnake/Cardinal-Components-API) — Ladysnake，LGPL-3.0
- [MixInSquared](https://github.com/Bawnorton/MixinSquared) — Bawnorton
- [milk-lib](https://github.com/tropheusj/milk-lib) / [dripstone-fluid-lib](https://github.com/tropheusj/dripstone-fluid-lib) — tropheusj
- [Reach Entity Attributes](https://github.com/JamiesWhiteShirt/reach-entity-attributes) — JamiesWhiteShirt
- [Roughly Enough Items](https://github.com/shedaniel/RoughlyEnoughItems) — shedaniel
- 以及 `build.gradle` 中列出的其余依赖

`LICENSE` 文件保留上游的 MIT 许可证与版权声明，**未做任何修改**。

## 如果你要引用或再分发

1. 保留所有上游版权声明与 MIT/LICENSE 文件。
2. 明确标注这是非官方的、AI 生成的衍生作品，且未经原作者审核。
3. 遵守各第三方依赖（尤其 Porting Lib 的 LGPL）的许可条款。
4. 不要暗示原作者为这些改动负责。

---

*一句话：功劳归 SlimeKnights 和 AlphaMode，锅归我们。*
