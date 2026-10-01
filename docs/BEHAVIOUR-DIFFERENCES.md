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
| 5 | `TinkerItemDisplays`（自定义物品显示上下文） | 通过 Forge 的 `DISPLAY_CONTEXTS` 注册表创建 `melter`/`table`/`casting_table`/`casting_basin`/`fluid_cannon`/`thrown` 六个自定义 `ItemDisplayContext`，用于冶炼炉、工作台、铸造台、流体炮、投掷时调整工具姿态 | vanilla 的 `ItemDisplayContext` 是**枚举**、Fabric 没有扩展注册表，因此这六个常量退化为上游自己声明的 **fallback 值**（`NONE`/`FIXED`） | 功能缺失 | 上述界面里工具/物品的**摆放位置与朝向**会用 vanilla 的姿态，可能与 Forge 版视觉不同（不影响功能） | 未验证（需视觉比对） |
| 6 | `TinkerAttributes`（属性挂载范围） | Forge 的 `EntityAttributeModificationEvent` 把 TCon 的全部属性加到**所有**生物实体上（`event.getTypes()` 遍历全部） | Fabric 的 `FabricDefaultAttributeRegistry` 是**按实体类型**注册且必须给出完整默认值表，无法表达"所有实体"。本移植给**玩家**注册全套；TCon 自己的实体在 `TinkerWorld` 里注册相关项 | 功能缺失 | 其它模组/vanilla 生物不会拥有 TCon 的属性（例如 `bouncy`、`protection_cap`）。TCon 自身用到这些属性的地方主要是玩家护甲/修饰符，因此影响面有限 | 未验证 |
| 7 | 击退抗性客户端同步 | `commonSetup` 里按配置 `Config.COMMON.syncKnockbackResistance` 调用 Forge 的 `Attributes.KNOCKBACK_RESISTANCE.setSyncable(true)`，让客户端能拿到该值 | vanilla/Fabric 没有 `setSyncable`，该调用被移除；配置项也不再有效 | 功能缺失 | 依赖客户端读取击退抗性做显示的场合会显示 vanilla 值 | 未验证 |
| 8 | 击退抗性客户端同步（配置项） | 见第 7 条，由 `Config.COMMON.syncKnockbackResistance` 控制 | 该配置项现在没有任何作用 | 功能缺失（配置失效） | 与第 7 条同源 | 未验证 |
| 9 | `Dummmmmmy` / `Crafting Tweaks` 兼容插件 | 两个插件（`DummmmmmyPlugin`、`plugin/craftingtweaks/**`）在 Forge 上编译期依赖这两个模组，分别用于"禁用测试假人的盾牌"和"给 Tinkers 工作台接入 Crafting Tweaks 的整理按钮" | **暂时移除**（文件已删，git 历史里仍可找回）。这两个模组**都有 Fabric 版**，所以只是"还没接上依赖坐标"，不是无法移植 —— 补上 `modCompileOnly` 依赖即可恢复 | 功能缺失（可恢复） | 装了这两个模组时不再有对应集成 | — |

## 如何更新本文件

- 新增一条差异 → 在表格末尾追加，并在提交信息里点名"行为差异"
- 验证通过某条 → 把"验证状态"改成"已验证（日期 + 验证方式）"，**不要删除该行**
