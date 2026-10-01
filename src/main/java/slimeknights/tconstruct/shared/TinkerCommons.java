package slimeknights.tconstruct.shared;

import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TintedGlassBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.apache.logging.log4j.Logger;
import slimeknights.mantle.item.EdibleItem;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.data.model.ModelSpriteProvider;
import slimeknights.tconstruct.common.data.model.TinkerBlockStateProvider;
import slimeknights.tconstruct.common.data.model.TinkerItemModelProvider;
import slimeknights.tconstruct.common.data.model.TinkerSpriteSourceProvider;
import slimeknights.tconstruct.common.data.render.RenderFluidProvider;
import slimeknights.tconstruct.common.data.render.RenderItemProvider;
import slimeknights.tconstruct.common.json.BlockOrEntityCondition;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.common.json.TinkerConditons;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.library.json.condition.TagDifferencePresentCondition;
import slimeknights.tconstruct.library.json.condition.TagIntersectionPresentCondition;
import slimeknights.tconstruct.library.json.condition.TagNotEmptyCondition;
import slimeknights.tconstruct.library.json.loot.HasLootContextSetCondition;
import slimeknights.tconstruct.library.json.loot.TagPreferenceLootEntry;
import slimeknights.tconstruct.library.json.predicate.BlockAtFeetEntityPredicate;
import slimeknights.tconstruct.library.json.predicate.BlockVariableRangePredicate;
import slimeknights.tconstruct.library.json.predicate.EntityVariableRangePredicate;
import slimeknights.tconstruct.library.json.predicate.HarvestTierPredicate;
import slimeknights.tconstruct.library.json.predicate.HasMobEffectPredicate;
import slimeknights.tconstruct.library.json.predicate.TinkerPredicate;
import slimeknights.tconstruct.library.recipe.ingredient.BlockTagIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.InstrumentIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.NoContainerIngredient;
import slimeknights.tconstruct.library.utils.SlimeBounceHandler;
import slimeknights.tconstruct.shared.block.BetterPaneBlock;
import slimeknights.tconstruct.shared.block.ClearGlassPaneBlock;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock.GlassColor;
import slimeknights.tconstruct.shared.block.ClearStainedGlassPaneBlock;
import slimeknights.tconstruct.shared.block.GlowBlock;
import slimeknights.tconstruct.shared.block.PlatformBlock;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.shared.block.SoulGlassBlock;
import slimeknights.tconstruct.shared.block.SoulGlassPaneBlock;
import slimeknights.tconstruct.shared.block.WaxedPlatformBlock;
import slimeknights.tconstruct.shared.block.WeatheringPlatformBlock;
import slimeknights.tconstruct.shared.command.TConstructCommand;
import slimeknights.tconstruct.shared.data.CommonRecipeProvider;
import slimeknights.tconstruct.shared.data.TinkerDamageSourceProvider;
import slimeknights.tconstruct.shared.inventory.BlockContainerOpenedTrigger;
import slimeknights.tconstruct.shared.item.CheeseBlockItem;
import slimeknights.tconstruct.shared.item.CheeseItem;
import slimeknights.tconstruct.shared.item.TinkerBookItem;
import slimeknights.tconstruct.shared.item.TinkerBookItem.BookType;
import slimeknights.tconstruct.shared.particle.FluidParticleData;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerModifiers;

import static slimeknights.tconstruct.TConstruct.getResource;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Contains items and blocks and stuff that is shared by multiple modules, but might be required individually
 */
@SuppressWarnings("unused")
public final class TinkerCommons extends TinkerModule {
  /** Creative tab for general items, or those that lack another tab */
  public static final RegistryObject<CreativeModeTab> tabGeneral = CREATIVE_TABS.register(
    "general", () -> CreativeModeTab.builder().title(TConstruct.makeTranslation("itemGroup", "general"))
                                    .icon(() -> new ItemStack(TinkerCommons.materialsAndYou))
                                    .displayItems(TinkerCommons::addTabItems)
                                    .build());

  /*
   * Blocks
   */
  public static final RegistryObject<GlowBlock> glow = BLOCKS.registerNoItem("glow", () -> new GlowBlock(builder(SoundType.WOOL).forceSolidOff().pushReaction(PushReaction.DESTROY).replaceable().strength(0.0F).lightLevel(s -> 14).noOcclusion()));
  public static final BuildingBlockObject mudBricks = BLOCKS.registerBuilding("mud_bricks", builder(MapColor.DIRT, SoundType.GRAVEL).requiresCorrectToolForDrops().strength(2.0F), GENERAL_BLOCK_ITEM);
  // glass
  public static final ItemObject<GlassBlock> clearGlass = BLOCKS.register("clear_glass", () -> new GlassBlock(glassBuilder(MapColor.NONE)), GENERAL_BLOCK_ITEM);
  public static final ItemObject<TintedGlassBlock> clearTintedGlass = BLOCKS.register("clear_tinted_glass", () -> new TintedGlassBlock(glassBuilder(MapColor.COLOR_GRAY).mapColor(MapColor.COLOR_GRAY).noOcclusion().isValidSpawn(Blocks::never).isRedstoneConductor(Blocks::never).isSuffocating(Blocks::never).isViewBlocking(Blocks::never)), GENERAL_BLOCK_ITEM);
  public static final ItemObject<ClearGlassPaneBlock> clearGlassPane = BLOCKS.register("clear_glass_pane", () -> new ClearGlassPaneBlock(glassBuilder(MapColor.NONE)), GENERAL_BLOCK_ITEM);
  public static final EnumObject<GlassColor,ClearStainedGlassBlock> clearStainedGlass = BLOCKS.registerEnum(GlassColor.values(), "clear_stained_glass", (color) -> new ClearStainedGlassBlock(glassBuilder(color.getDye().getMapColor()), color), GENERAL_BLOCK_ITEM);
  public static final EnumObject<GlassColor,ClearStainedGlassPaneBlock> clearStainedGlassPane = BLOCKS.registerEnum(GlassColor.values(), "clear_stained_glass_pane", (color) -> new ClearStainedGlassPaneBlock(glassBuilder(color.getDye().getMapColor()), color), GENERAL_BLOCK_ITEM);
  public static final ItemObject<GlassBlock> soulGlass = BLOCKS.register("soul_glass", () -> new GlassBlock(glassBuilder(MapColor.COLOR_BROWN)), GENERAL_BLOCK_ITEM);
  public static final ItemObject<ClearGlassPaneBlock> soulGlassPane = BLOCKS.register("soul_glass_pane", () -> new ClearGlassPaneBlock(glassBuilder(MapColor.COLOR_BROWN)), GENERAL_BLOCK_ITEM);
  // wood
  public static final BuildingBlockObject lavawood = BLOCKS.registerBuilding("lavawood", woodBuilder(MapColor.COLOR_ORANGE).lightLevel(s -> 7), GENERAL_BLOCK_ITEM);
  public static final BuildingBlockObject blazewood = BLOCKS.registerBuilding("blazewood", woodBuilder(MapColor.TERRACOTTA_RED).lightLevel(s -> 7), GENERAL_BLOCK_ITEM);
  // panes
  public static final ItemObject<IronBarsBlock> goldBars = BLOCKS.register("gold_bars", () -> new IronBarsBlock(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion()), GENERAL_TOOLTIP_BLOCK_ITEM);
  public static final ItemObject<Block> obsidianPane = BLOCKS.register("obsidian_pane", () -> new BetterPaneBlock(builder(MapColor.PODZOL, SoundType.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().noOcclusion().strength(25.0F, 400.0F)), GENERAL_BLOCK_ITEM);
  // platforms
  public static final ItemObject<PlatformBlock> goldPlatform = BLOCKS.register("gold_platform", () -> new PlatformBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()), GENERAL_TOOLTIP_BLOCK_ITEM);
  public static final ItemObject<PlatformBlock> ironPlatform = BLOCKS.register("iron_platform", () -> new PlatformBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM);
  public static final ItemObject<PlatformBlock> cobaltPlatform = BLOCKS.register("cobalt_platform", () -> new PlatformBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).requiresCorrectToolForDrops().strength(5.0f).sound(SoundType.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM);
  public static final EnumObject<WeatherState,PlatformBlock> copperPlatform = new EnumObject.Builder<WeatherState,PlatformBlock>(WeatherState.class)
    .put(WeatherState.UNAFFECTED, BLOCKS.register("copper_platform",           () -> new WeatheringPlatformBlock(WeatherState.UNAFFECTED, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
    .put(WeatherState.EXPOSED,    BLOCKS.register("exposed_copper_platform",   () -> new WeatheringPlatformBlock(WeatherState.EXPOSED,    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
    .put(WeatherState.WEATHERED,  BLOCKS.register("weathered_copper_platform", () -> new WeatheringPlatformBlock(WeatherState.WEATHERED,  BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_STEM).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
    .put(WeatherState.OXIDIZED,   BLOCKS.register("oxidized_copper_platform",  () -> new WeatheringPlatformBlock(WeatherState.OXIDIZED,   BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_NYLIUM).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
    .build();
  public static final EnumObject<WeatherState,PlatformBlock> waxedCopperPlatform = new EnumObject.Builder<WeatherState,PlatformBlock>(WeatherState.class)
    .put(WeatherState.UNAFFECTED, BLOCKS.register("waxed_copper_platform",           () -> new WaxedPlatformBlock(WeatherState.UNAFFECTED, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
    .put(WeatherState.EXPOSED,    BLOCKS.register("waxed_exposed_copper_platform",   () -> new WaxedPlatformBlock(WeatherState.EXPOSED,    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
    .put(WeatherState.WEATHERED,  BLOCKS.register("waxed_weathered_copper_platform", () -> new WaxedPlatformBlock(WeatherState.WEATHERED,  BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_STEM).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
    .put(WeatherState.OXIDIZED,   BLOCKS.register("waxed_oxidized_copper_platform",  () -> new WaxedPlatformBlock(WeatherState.OXIDIZED,   BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_NYLIUM).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
    .build();


  /*
   * Items
   */
  public static final ItemObject<EdibleItem> bacon = ITEMS.register("bacon", () -> new EdibleItem(TinkerFood.BACON));
  public static final ItemObject<EdibleItem> jeweledApple = ITEMS.register("jeweled_apple", () -> new EdibleItem(TinkerFood.JEWELED_APPLE));

  private static final Item.Properties BOOK = new Item.Properties().stacksTo(1);
  public static final ItemObject<TinkerBookItem> materialsAndYou = ITEMS.register("materials_and_you", () -> new TinkerBookItem(BOOK, BookType.MATERIALS_AND_YOU));
  public static final ItemObject<TinkerBookItem> punySmelting = ITEMS.register("puny_smelting", () -> new TinkerBookItem(BOOK, BookType.PUNY_SMELTING));
  public static final ItemObject<TinkerBookItem> mightySmelting = ITEMS.register("mighty_smelting", () -> new TinkerBookItem(BOOK, BookType.MIGHTY_SMELTING));
  public static final ItemObject<TinkerBookItem> tinkersGadgetry = ITEMS.register("tinkers_gadgetry", () -> new TinkerBookItem(BOOK, BookType.TINKERS_GADGETRY));
  public static final ItemObject<TinkerBookItem> fantasticFoundry = ITEMS.register("fantastic_foundry", () -> new TinkerBookItem(BOOK, BookType.FANTASTIC_FOUNDRY));
  public static final ItemObject<TinkerBookItem> encyclopedia = ITEMS.register("encyclopedia", () -> new TinkerBookItem(BOOK, BookType.ENCYCLOPEDIA));

  public static final RegistryObject<ParticleType<FluidParticleData>> fluidParticle = PARTICLE_TYPES.register("fluid", FluidParticleData.Type::new);

  /* Loot conditions */
  public static final RegistryObject<LootItemConditionType> lootConfig = LOOT_CONDITIONS.register(ConfigEnabledCondition.ID.getPath(), () -> new LootItemConditionType(ConfigEnabledCondition.SERIALIZER));
  public static final RegistryObject<LootItemConditionType> lootBlockOrEntity = LOOT_CONDITIONS.register("block_or_entity", () -> new LootItemConditionType(new BlockOrEntityCondition.ConditionSerializer()));
  public static final RegistryObject<LootItemConditionType> hasLootContextSet = LOOT_CONDITIONS.register("has_context_set", () -> new LootItemConditionType(new HasLootContextSetCondition.Serializer()));
  /** @deprecated use {@link slimeknights.mantle.loot.MantleLoot#TAG_FILLED} */
  @SuppressWarnings("removal")
  @Deprecated(forRemoval = true)
  public static final RegistryObject<LootItemConditionType> lootTagNotEmptyCondition = LOOT_CONDITIONS.register("tag_not_empty", () -> new LootItemConditionType(new TagNotEmptyCondition.ConditionSerializer()));
  /** @deprecated use {@link slimeknights.mantle.loot.MantleLoot#TAG_PREFERENCE} */
  @SuppressWarnings("removal")
  @Deprecated(forRemoval = true)
  public static final RegistryObject<LootPoolEntryType> lootTagPreference = LOOT_ENTRIES.register("tag_preference", () -> new LootPoolEntryType(new TagPreferenceLootEntry.Serializer()));

  /* Slime Balls are edible, believe it or not */
  public static final EnumObject<SlimeType, Item> slimeball = new EnumObject.Builder<SlimeType, Item>(SlimeType.class)
    .put(SlimeType.EARTH, () -> Items.SLIME_BALL)
    .putAll(ITEMS.registerEnum(SlimeType.TINKER, "slime_ball", type -> new Item(GENERAL_PROPS)))
    .build();

  public static final BlockContainerOpenedTrigger CONTAINER_OPENED_TRIGGER = new BlockContainerOpenedTrigger();

  public TinkerCommons() {
    RecipeCacheInvalidator.onReloadListenerReload();
    commonSetupEvent();
  }

  void commonSetupEvent() {
    TConstructCommand.init();
    SlimeBounceHandler.init();
    registerRecipeSerializers();
  }

  void registerRecipeSerializers() {
    ResourceConditions.register(ConfigEnabledCondition.ID, ConfigEnabledCondition::test);
    CriteriaTriggers.register(CONTAINER_OPENED_TRIGGER);


    ResourceConditions.register(TagIntersectionPresentCondition.NAME, TinkerConditons::tagIntersectionPresentPredicate);
    ResourceConditions.register(TagDifferencePresentCondition.NAME, TinkerConditons::tagDifferencePresentPredicate);
    // block predicates
    BlockPredicate.LOADER.register(TConstruct.getResource("and"), BlockPredicate.AND);
    BlockPredicate.LOADER.register(TConstruct.getResource("or"), BlockPredicate.OR);
    BlockPredicate.LOADER.register(TConstruct.getResource("inverted"), BlockPredicate.INVERTED);
    BlockPredicate.LOADER.register(TConstruct.getResource("requires_tool"), BlockPredicate.REQUIRES_TOOL.getLoader());
    // entity predicates
    LivingEntityPredicate.LOADER.register(TConstruct.getResource("and"), LivingEntityPredicate.AND);
    LivingEntityPredicate.LOADER.register(TConstruct.getResource("or"), LivingEntityPredicate.OR);
    LivingEntityPredicate.LOADER.register(TConstruct.getResource("inverted"), LivingEntityPredicate.INVERTED);
    LivingEntityPredicate.LOADER.register(TConstruct.getResource("any"), LivingEntityPredicate.ANY.getLoader());
    LivingEntityPredicate.LOADER.register(TConstruct.getResource("fire_immune"), LivingEntityPredicate.FIRE_IMMUNE.getLoader());
    LivingEntityPredicate.LOADER.register(TConstruct.getResource("water_sensitive"), LivingEntityPredicate.WATER_SENSITIVE.getLoader());
    LivingEntityPredicate.LOADER.register(TConstruct.getResource("on_fire"), LivingEntityPredicate.ON_FIRE.getLoader());
    LivingEntityPredicate.LOADER.register(TConstruct.getResource("mob_type"), MobTypePredicate.LOADER);
    // register mob types
    MobTypePredicate.MOB_TYPES.register(new ResourceLocation("undefined"), MobType.UNDEFINED);
    MobTypePredicate.MOB_TYPES.register(new ResourceLocation("undead"), MobType.UNDEAD);
    MobTypePredicate.MOB_TYPES.register(new ResourceLocation("arthropod"), MobType.ARTHROPOD);
    MobTypePredicate.MOB_TYPES.register(new ResourceLocation("illager"), MobType.ILLAGER);
    MobTypePredicate.MOB_TYPES.register(new ResourceLocation("water"), MobType.WATER);
  }

  public static void gatherData(final FabricDataGenerator.Pack pack) {
    pack.addProvider(CommonRecipeProvider::new);
    pack.addProvider(TinkerDamageSourceProvider::new);
  }
}
