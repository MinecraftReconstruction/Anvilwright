# 行为差异披露（Fabric 版 Tinkers' Construct vs Forge 上游）

> 性质见 [ATTRIBUTION.md](../ATTRIBUTION.md)。本文件记录**所有已知的、本移植与 Forge 上游行为不一致的地方**。
> 任何改动只要不是"语义等价替换"，都要在这里登记一行。Mantle 侧的同名文件在
> [Mantle-Fabric/docs/BEHAVIOUR-DIFFERENCES.md](https://github.com/MinecraftReconstruction/Mantle-Fabric/blob/mcr/mantle-1.11/docs/BEHAVIOUR-DIFFERENCES.md)。

## 差异清单

| # | 位置 | Forge 上游行为 | Fabric 版行为 | 类别 | 影响面 | 验证状态 |
|---|---|---|---|---|---|---|
| 1 | `FluidContainerModel`（流体容器模型） | 通过 `RenderTypeGroup` / `DynamicFluidContainerModel.getLayerRenderTypes(boolean)` 给 quad 指定渲染层（含流体发光时换用另一层） | Porting Lib 的 `CompositeModel.Builder` 只收 quad，不再接受渲染层，所以**渲染层被丢弃** | 功能缺失（同 Mantle #6） | 流体容器（桶/罐）的渲染层可能退化：半透明、发光（emissive）表现可能不对。发光本身仍通过 `QuadTransformers.settingEmissivity` 生效，丢的是**层选择** | 未验证（需视觉比对） |
| 2 | `BlockItemProviderCapability`（方块物品提供者） | 是挂在 `ItemStack` 上的 Forge capability，任何 mod 都能为自己的物品注入提供者 | Fabric 没有 per-stack capability；改用**直接类型判断**（`getItem() instanceof BlockItem`）。上游自带的实现只有一个（默认给 `BlockItem`），所以行为一致，但**addon 无法再注入自定义提供者** | 功能缺失（扩展点丢失） | 只有需要"让非 BlockItem 的物品充当方块来源"的 addon 受影响；TCon 自身行为不变 | — |
| 3 | 可选的 Forge 独占兼容 | `jsonthings`、`Diet`、`Immersive Engineering` 三个集成 | **已删除**（21 + 1 + 1 个文件）。这三个 mod 只有 Forge 版，Fabric 上不可能运行 | 功能缺失（有意为之） | 装了上述 Forge 模组时不再有对应集成；Fabric 侧本来也没有这些模组 | — |
| 4 | `FluidAction`（垫片） | Forge 的 `IFluidHandler.FluidAction`：`EXECUTE`/`SIMULATE` 直接驱动流体操作 | 新建了 `tconstruct.library.fluid.FluidAction` 垫片，API 完全一致，但**底层必须由调用方翻译成 Fabric 的 `Transaction`/`StorageUtil.simulateInsert` 语义** | 待核对 | 如果某个调用点只是"传下去"而没有被真正翻译，simulate 就会变成 execute（可能凭空消耗/产生流体）。**这是流体相关最需要实测的一点** | ⚠️ 未验证（需逐点核对 simulate 语义） |

## 如何更新本文件

- 新增一条差异 → 在表格末尾追加，并在提交信息里点名"行为差异"
- 验证通过某条 → 把"验证状态"改成"已验证（日期 + 验证方式）"，**不要删除该行**
