package slimeknights.tconstruct.tools;

import io.github.fabricators_of_create.porting_lib.util.ItemPredicateRegistry;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTabs;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.config.ConfigurableAction;
import slimeknights.tconstruct.common.data.tags.MaterialTagProvider;
import slimeknights.tconstruct.library.client.data.TinkerSpriteSourceGenerator;
import slimeknights.tconstruct.library.client.data.material.GeneratorPartTextureJsonGenerator;
import slimeknights.tconstruct.library.client.data.material.MaterialPaletteDebugGenerator;
import slimeknights.tconstruct.library.client.data.material.MaterialPartTextureGenerator;
import slimeknights.tconstruct.library.json.loot.AddToolDataFunction;
import slimeknights.tconstruct.library.json.predicate.tool.HasMaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.HasModifierPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.HasStatTypePredicate;
import slimeknights.tconstruct.library.json.predicate.tool.HasToolHookPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.PersistentDataPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.StatInRangePredicate;
import slimeknights.tconstruct.library.json.predicate.tool.StatInSetPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolActionPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolContextPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolStackItemPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolStackPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolVariableRangePredicate;
import slimeknights.tconstruct.library.json.predicate.tool.VolatileDataPredicate;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.modules.capacity.OverslimeModule;
import slimeknights.tconstruct.library.modifiers.modules.interaction.edible.EdibleModule;
import slimeknights.tconstruct.library.recipe.ingredient.ToolHookIngredient;
import slimeknights.tconstruct.library.tools.IndestructibleItemEntity;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.capability.BlockItemProviderModifierHook;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.capability.ToolEnergyCapability;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolFluidCapability;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.BoxAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.CircleAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.ConditionalAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.TreeAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.VeiningAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.build.MultiplyStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.SetStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolActionsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolSlotsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.VolatileFlagModule;
import slimeknights.tconstruct.library.tools.definition.module.build.VolatileIntModule;
import slimeknights.tconstruct.library.tools.definition.module.display.CustomMaterialName;
import slimeknights.tconstruct.library.tools.definition.module.display.FixedMaterialToolName;
import slimeknights.tconstruct.library.tools.definition.module.display.MaterialToolNameModule;
import slimeknights.tconstruct.library.tools.definition.module.display.SimpleToolName;
import slimeknights.tconstruct.library.tools.definition.module.display.StatTypesToolNameModule;
import slimeknights.tconstruct.library.tools.definition.module.display.UniqueMaterialToolName;
import slimeknights.tconstruct.library.tools.definition.module.interaction.AttackInteraction;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.definition.module.interaction.PreferenceSetInteraction;
import slimeknights.tconstruct.library.tools.definition.module.interaction.ToggleableSetInteraction;
import slimeknights.tconstruct.library.tools.definition.module.material.DefaultMaterialsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialRepairModule;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialTraitsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.PartStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.PartsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.RemappingMaterialsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.StatlessPartRepairModule;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.MaxTierModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningSpeedModifierModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.OneClickBreakModule;
import slimeknights.tconstruct.library.tools.definition.module.weapon.CircleWeaponAttack;
import slimeknights.tconstruct.library.tools.definition.module.weapon.ParticleWeaponAttack;
import slimeknights.tconstruct.library.tools.definition.module.weapon.SweepWeaponAttack;
import slimeknights.tconstruct.library.tools.helper.ModifierLootingHandler;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.ModifiableArrowItem;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.item.ModifiableShurikenItem;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.item.armor.MultilayerArmorItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableBowItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.BlockSideHitListener;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.tools.data.ArmorModelProvider;
import slimeknights.tconstruct.tools.data.ModifierIds;
import slimeknights.tconstruct.tools.data.StationSlotLayoutProvider;
import slimeknights.tconstruct.tools.data.ToolDefinitionDataProvider;
import slimeknights.tconstruct.tools.data.ToolItemModelProvider;
import slimeknights.tconstruct.tools.data.ToolsRecipeProvider;
import slimeknights.tconstruct.tools.data.client.ModifierModelMapProvider;
import slimeknights.tconstruct.tools.data.material.MaterialDataProvider;
import slimeknights.tconstruct.tools.data.material.MaterialIds;
import slimeknights.tconstruct.tools.data.material.MaterialRecipeProvider;
import slimeknights.tconstruct.tools.data.material.MaterialRenderInfoProvider;
import slimeknights.tconstruct.tools.data.material.MaterialStatsDataProvider;
import slimeknights.tconstruct.tools.data.material.MaterialTraitsDataProvider;
import slimeknights.tconstruct.tools.data.sprite.TinkerMaterialSpriteProvider;
import slimeknights.tconstruct.tools.data.sprite.TinkerPartSpriteProvider;
import slimeknights.tconstruct.tools.data.sprite.TinkerTrimMaterialPaletteGenerator;
import slimeknights.tconstruct.tools.entity.CombatFishingHook;
import slimeknights.tconstruct.tools.entity.ModifiableArrow;
import slimeknights.tconstruct.tools.entity.ThrownShuriken;
import slimeknights.tconstruct.tools.entity.ThrownTool;
import slimeknights.tconstruct.tools.item.CrystalshotItem;
import slimeknights.tconstruct.tools.item.CrystalshotItem.CrystalshotEntity;
import slimeknights.tconstruct.tools.item.ModifiableSwordItem;
import slimeknights.tconstruct.tools.item.SlimeskullItem;
import slimeknights.tconstruct.tools.logic.EquipmentChangeWatcher;
import slimeknights.tconstruct.tools.logic.ModifiableArrowDispenserBehavior;
import slimeknights.tconstruct.tools.logic.ModifiableShurikenDispenserBehavior;
import slimeknights.tconstruct.tools.menu.ToolContainerMenu;
import slimeknights.tconstruct.tools.modules.MeltingFluidEffectiveModule;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * Contains all complete tool items
 */
public final class TinkerTools extends TinkerModule {
  public TinkerTools() {
    SlotType.init();
    BlockSideHitListener.init();
    ModifierLootingHandler.init();
    RandomMaterial.init();
    commonSetup();
    registerRecipeSerializers();
  }

  /** Loot function type for tool add data */
  public static final RegistryObject<LootItemFunctionType> lootAddToolData = LOOT_FUNCTIONS.register("add_tool_data", () -> new LootItemFunctionType(AddToolDataFunction.SERIALIZER));

  /*
   * Items
   */
  private static final Item.Properties TOOL = new FabricItemSettings().stacksTo(1);

  public static final ItemObject<ModifiableItem> pickaxe = ITEMS.register("pickaxe", () -> new ModifiableItem(TOOL, ToolDefinitions.PICKAXE, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> sledgeHammer = ITEMS.register("sledge_hammer", () -> new ModifiableItem(TOOL, ToolDefinitions.SLEDGE_HAMMER, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> veinHammer = ITEMS.register("vein_hammer", () -> new ModifiableItem(TOOL, ToolDefinitions.VEIN_HAMMER, TinkerTabs.TAB_TOOLS));

  public static final ItemObject<ModifiableItem> mattock = ITEMS.register("mattock", () -> new ModifiableItem(TOOL, ToolDefinitions.MATTOCK, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> pickadze = ITEMS.register("pickadze", () -> new ModifiableItem(TOOL, ToolDefinitions.PICKADZE, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> excavator = ITEMS.register("excavator", () -> new ModifiableItem(TOOL, ToolDefinitions.EXCAVATOR, TinkerTabs.TAB_TOOLS));

  public static final ItemObject<ModifiableItem> handAxe = ITEMS.register("hand_axe", () -> new ModifiableItem(TOOL, ToolDefinitions.HAND_AXE, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> broadAxe = ITEMS.register("broad_axe", () -> new ModifiableItem(TOOL, ToolDefinitions.BROAD_AXE, TinkerTabs.TAB_TOOLS));

  public static final ItemObject<ModifiableItem> kama = ITEMS.register("kama", () -> new ModifiableItem(TOOL, ToolDefinitions.KAMA, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> scythe = ITEMS.register("scythe", () -> new ModifiableItem(TOOL, ToolDefinitions.SCYTHE, TinkerTabs.TAB_TOOLS));

  public static final ItemObject<ModifiableItem> dagger = ITEMS.register("dagger", () -> new ModifiableDaggerItem(TOOL, ToolDefinitions.DAGGER, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> sword = ITEMS.register("sword", () -> new ModifiableSwordItem(TOOL, ToolDefinitions.SWORD, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> cleaver = ITEMS.register("cleaver", () -> new ModifiableSwordItem(TOOL, ToolDefinitions.CLEAVER, TinkerTabs.TAB_TOOLS));

  public static final ItemObject<ModifiableLauncherItem> crossbow = ITEMS.register("crossbow", () -> new ModifiableCrossbowItem(TOOL, ToolDefinitions.CROSSBOW, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableLauncherItem> longbow = ITEMS.register("longbow", () -> new ModifiableBowItem(TOOL, ToolDefinitions.LONGBOW, TinkerTabs.TAB_TOOLS));

  public static final ItemObject<ModifiableItem> flintAndBrick = ITEMS.register("flint_and_brick", () -> new ModifiableItem(TOOL, ToolDefinitions.FLINT_AND_BRICK, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> skyStaff = ITEMS.register("sky_staff", () -> new ModifiableStaffItem(TOOL, ToolDefinitions.SKY_STAFF, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> earthStaff = ITEMS.register("earth_staff", () -> new ModifiableStaffItem(TOOL, ToolDefinitions.EARTH_STAFF, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> ichorStaff = ITEMS.register("ichor_staff", () -> new ModifiableStaffItem(TOOL, ToolDefinitions.ICHOR_STAFF, TinkerTabs.TAB_TOOLS));

  // armor
  public static final EnumObject<ArmorSlotType,ModifiableArmorItem> travelersGear = ITEMS.registerEnum("travelers", ArmorSlotType.values(), type -> new TravelersGearItem(ArmorDefinitions.TRAVELERS, type, TOOL, TinkerTabs.TAB_TOOLS));
  public static final EnumObject<ArmorSlotType,ModifiableArmorItem> plateArmor = ITEMS.registerEnum("plate", ArmorSlotType.values(), type -> new PlateArmorItem(ArmorDefinitions.PLATE, type, TOOL, TinkerTabs.TAB_TOOLS));
  public static final EnumObject<ArmorSlotType,ModifiableArmorItem> slimesuit = new EnumObject.Builder<ArmorSlotType,ModifiableArmorItem>(ArmorSlotType.class)
    .putAll(ITEMS.registerEnum("slime", new ArmorSlotType[] {ArmorSlotType.BOOTS, ArmorSlotType.LEGGINGS}, type -> new SlimesuitItem(ArmorDefinitions.SLIMESUIT, type, TOOL, TinkerTabs.TAB_TOOLS)))
    .put(ArmorSlotType.CHESTPLATE, ITEMS.register("slime_chestplate", () -> new SlimelytraItem(ArmorDefinitions.SLIMESUIT, TOOL, TinkerTabs.TAB_TOOLS)))
    .put(ArmorSlotType.HELMET, ITEMS.register("slime_helmet", () -> new SlimeskullItem(ArmorDefinitions.SLIMESUIT, TOOL, TinkerTabs.TAB_TOOLS)))
    .build();
  public static final ItemObject<MultilayerArmorItem> slimeWings = ITEMS.register("slime_wings", () -> new MultilayerArmorItem(ArmorDefinitions.SLIMESUIT, ArmorItem.Type.CHESTPLATE, UNSTACKABLE_PROPS, ArmorDefinitions.SLIME_WINGS, TinkerTools.slimeWings.getId()));

  // shields
  public static final ItemObject<ModifiableItem> travelersShield = ITEMS.register("travelers_shield", () -> new ModifiableStaffItem(TOOL, ArmorDefinitions.TRAVELERS_SHIELD, TinkerTabs.TAB_TOOLS));
  public static final ItemObject<ModifiableItem> plateShield = ITEMS.register("plate_shield", () -> new ModifiableStaffItem(TOOL, ArmorDefinitions.PLATE_SHIELD, TinkerTabs.TAB_TOOLS));

  // arrows
  public static final ItemObject<ArrowItem> crystalshotItem = ITEMS.register("crystalshot", () -> new CrystalshotItem(new Item.Properties()/*.tab(TinkerTabs.TAB_TOOLS)*/));

  /* Particles */
  public static final RegistryObject<SimpleParticleType> hammerAttackParticle = PARTICLE_TYPES.register("hammer_attack", () -> FabricParticleTypes.simple(true));
  public static final RegistryObject<SimpleParticleType> axeAttackParticle = PARTICLE_TYPES.register("axe_attack", () -> FabricParticleTypes.simple(true));

  /* Entities */
  public static final RegistryObject<EntityType<IndestructibleItemEntity>> indestructibleItem = ENTITIES.register("indestructible_item", () ->
    FabricEntityTypeBuilder.<IndestructibleItemEntity>create(MobCategory.MISC, IndestructibleItemEntity::new)
                      .dimensions(EntityDimensions.fixed(0.25F, 0.25F))
                      .fireImmune());
  public static final RegistryObject<EntityType<CrystalshotEntity>> crystalshotEntity = ENTITIES.register("crystalshot", () ->
    FabricEntityTypeBuilder.<CrystalshotEntity>create(MobCategory.MISC, CrystalshotEntity::new)
                      .dimensions(EntityDimensions.fixed(0.5F, 0.5F))
                      .trackRangeChunks(4)
                      .trackedUpdateRate(20));

  /* Containers */
  public static final RegistryObject<MenuType<ToolContainerMenu>> toolContainer = MENUS.register("tool_container", ToolContainerMenu::forClient);


  /*
   * Events
   */

  void commonSetup() {
    EquipmentChangeWatcher.register();
    ToolCapabilityProvider.register(ToolFluidCapability.Provider::new);
    ToolCapabilityProvider.register(ToolInventoryCapability.Provider::new);
    for (ConfigurableAction action : Config.COMMON.damageSourceTweaks) {
      action.run();
    }
    event.enqueueWork(() -> {
      DispenserBlock.registerBehavior(TinkerTools.arrow.get(), ModifiableArrowDispenserBehavior.INSTANCE);
      DispenserBlock.registerBehavior(TinkerTools.shuriken.get(), ModifiableShurikenDispenserBehavior.INSTANCE);
      DispenserBlock.registerBehavior(TinkerTools.throwingAxe.get(), ModifiableShurikenDispenserBehavior.INSTANCE);
      ModifierUtil.registerShieldDisabler(entity -> {
        if (entity instanceof Player player && player.isBlocking()) {
          player.disableShield(true);
        }
      }, EntityType.PLAYER);
    });
    ModifierHooks.init();
    ToolHooks.init();
  }

  void registerRecipeSerializers() {
    ItemPredicateRegistry.register(ToolPredicate.ID, ToolPredicate::deserialize);

      // register tool stats that are not defined directly in the class; safer than static init registration
      ToolStats.register(OverslimeModule.OVERSLIME_STAT);
      ToolStats.register(ToolTankHelper.CAPACITY_STAT);
      ToolStats.register(ToolEnergyCapability.MAX_STAT);
      ToolStats.registerConditional(EdibleModule.HUNGER);
      ToolStats.registerConditional(EdibleModule.SATURATION);
      ToolStats.register(EdibleModule.EAT_DURATION);
      ToolStats.registerConditional(EdibleModule.COUNTER_CHANCE);

      ToolModule.LOADER.register(getResource("empty"), ToolModule.EMPTY.getLoader());
      // tool definition components
      ToolModule.LOADER.register(getResource("base_stats"), SetStatsModule.LOADER);
      ToolModule.LOADER.register(getResource("multiply_stats"), MultiplyStatsModule.LOADER);
      ToolModule.LOADER.register(getResource("tool_actions"), ToolActionsModule.LOADER);
      ToolModule.LOADER.register(getResource("traits"), ToolTraitsModule.LOADER);
      ToolModule.LOADER.register(getResource("modifier_slots"), ToolSlotsModule.LOADER);
      ToolModule.LOADER.register(getResource("volatile_flag"), VolatileFlagModule.LOADER);
      ToolModule.LOADER.register(getResource("volatile_int"), VolatileIntModule.LOADER);
      // harvest
      ToolModule.LOADER.register(getResource("is_effective"), IsEffectiveModule.LOADER);
      ToolModule.LOADER.register(getResource("mining_speed_modifier"), MiningSpeedModifierModule.LOADER);
      ToolModule.LOADER.register(getResource("max_tier"), MaxTierModule.LOADER);
      ToolModule.LOADER.register(getResource("one_click_break"), OneClickBreakModule.LOADER);
      // material
      ToolModule.LOADER.register(getResource("material_stats"), MaterialStatsModule.LOADER);
      ToolModule.LOADER.register(getResource("part_stats"), PartStatsModule.LOADER);
      ToolModule.LOADER.register(getResource("material_traits"), MaterialTraitsModule.LOADER);
      ToolModule.LOADER.register(getResource("tool_parts"), PartsModule.LOADER);
      ToolModule.LOADER.register(getResource("material_repair"), MaterialRepairModule.LOADER);
      ToolModule.LOADER.register(getResource("default_materials"), DefaultMaterialsModule.LOADER);
      ToolModule.LOADER.register(getResource("remapping_materials"), RemappingMaterialsModule.LOADER);
      ToolModule.LOADER.register(getResource("statless_part_repair"), StatlessPartRepairModule.LOADER);
      // aoe
      AreaOfEffectIterator.LOADER.register(getResource("empty"), AreaOfEffectIterator.EMPTY.getLoader());
      AreaOfEffectIterator.register(getResource("box_aoe"), BoxAOEIterator.LOADER);
      AreaOfEffectIterator.register(getResource("circle_aoe"), CircleAOEIterator.LOADER);
      AreaOfEffectIterator.register(getResource("tree_aoe"), TreeAOEIterator.LOADER);
      AreaOfEffectIterator.register(getResource("vein_aoe"), VeiningAOEIterator.LOADER);
      AreaOfEffectIterator.register(getResource("conditional_aoe"), ConditionalAOEIterator.LOADER);
      // attack
      ToolModule.LOADER.register(getResource("sweep_melee"), SweepWeaponAttack.LOADER);
      ToolModule.LOADER.register(getResource("circle_melee"), CircleWeaponAttack.LOADER);
      ToolModule.LOADER.register(getResource("melee_particle"), ParticleWeaponAttack.LOADER);
      // generic tool modules
      ToolModule.LOADER.register(getResource("attack_interaction"), AttackInteraction.LOADER);
      ToolModule.LOADER.register(getResource("dual_option_interaction"), DualOptionInteraction.LOADER);
      ToolModule.LOADER.register(getResource("preference_set_interaction"), PreferenceSetInteraction.LOADER);
      ToolModule.LOADER.register(getResource("toggleable_set_interaction"), ToggleableSetInteraction.LOADER);
      // special tool modules
      ToolModule.LOADER.register(getResource("melting_fluid_effective"), MeltingFluidEffectiveModule.LOADER);
      // display name
      ToolModule.LOADER.register(getResource("item_name"), SimpleToolName.ITEM.getLoader());
      ToolModule.LOADER.register(getResource("material_name"), MaterialToolNameModule.LOADER);
      ToolModule.LOADER.register(getResource("stat_types_name"), StatTypesToolNameModule.LOADER);
      ToolModule.LOADER.register(getResource("fixed_material_name"), FixedMaterialToolName.LOADER);
      ToolModule.LOADER.register(getResource("unique_material_name"), UniqueMaterialToolName.LOADER);
      ToolModule.LOADER.register(getResource("custom_material_name"), CustomMaterialName.LOADER);
      // tool predicates
      ToolContextPredicate.LOADER.register(getResource("has_upgrades"), ToolContextPredicate.HAS_UPGRADES.getLoader());
      ToolContextPredicate.LOADER.register(getResource("has_modifier"), HasModifierPredicate.LOADER);
      ToolContextPredicate.LOADER.register(getResource("has_material"), HasMaterialPredicate.LOADER);
      ToolContextPredicate.LOADER.register(getResource("has_stat_type"), HasStatTypePredicate.LOADER);
      ToolContextPredicate.LOADER.register(getResource("has_persistent_key"), PersistentDataPredicate.LOADER);
      ToolContextPredicate.LOADER.register(getResource("has_hook"), HasToolHookPredicate.LOADER);
      ToolStackPredicate.LOADER.register(getResource("not_broken"), ToolStackPredicate.NOT_BROKEN.getLoader());
      ToolStackPredicate.LOADER.register(getResource("stat_in_range"), StatInRangePredicate.LOADER);
      ToolStackPredicate.LOADER.register(getResource("stat_in_set"), StatInSetPredicate.LOADER);
      ToolStackPredicate.LOADER.register(getResource("has_volatile_key"), VolatileDataPredicate.LOADER);
      ToolStackPredicate.LOADER.register(getResource("variable_range"), ToolVariableRangePredicate.LOADER);
      ToolStackPredicate.LOADER.register(getResource("tool_action"), ToolActionPredicate.LOADER);
    }
  }

  public static void gatherData(FabricDataGenerator.Pack pack, ExistingFileHelper existingFileHelper) {
    pack.addProvider(ToolsRecipeProvider::new);
    pack.addProvider(MaterialRecipeProvider::new);
    MaterialDataProvider materials = pack.addProvider(MaterialDataProvider::new);
    pack.addProvider((output, registriesFuture) -> new MaterialStatsDataProvider(output, materials));
    pack.addProvider((output, registriesFuture) -> new MaterialTraitsDataProvider(output, materials));
    pack.addProvider(ToolDefinitionDataProvider::new);
    pack.addProvider(StationSlotLayoutProvider::new);
    pack.addProvider((output, registriesFuture) -> new MaterialTagProvider(output, existingFileHelper));

    TinkerMaterialSpriteProvider materialSprites = new TinkerMaterialSpriteProvider();
    TinkerPartSpriteProvider partSprites = new TinkerPartSpriteProvider();
    pack.addProvider((output, registriesFuture) -> new MaterialRenderInfoProvider(output, materialSprites));
    pack.addProvider((output, registriesFuture) -> new GeneratorPartTextureJsonGenerator(output, TConstruct.MOD_ID, partSprites));
    pack.addProvider((output, registriesFuture) -> new MaterialPartTextureGenerator(output, existingFileHelper, partSprites, materialSprites));
    pack.addProvider((output, registriesFuture) -> new TinkerSpriteSourceGenerator(output, existingFileHelper));
  }
}
