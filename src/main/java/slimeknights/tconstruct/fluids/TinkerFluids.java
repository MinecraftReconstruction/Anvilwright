package slimeknights.tconstruct.fluids;

import net.minecraft.network.syncher.EntityDataSerializers;
import io.github.fabricators_of_create.porting_lib.brewing.BrewingRecipe;
import io.github.fabricators_of_create.porting_lib.brewing.BrewingRecipeRegistry;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import io.github.tropheusj.milk.Milk;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import slimeknights.mantle.fluid.UnplaceableFluid;
import slimeknights.mantle.fluid.attributes.FluidAttributes;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.util.SimpleFlowingFluid;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.fluids.fluids.DirectionalSlimeFluid;
import slimeknights.tconstruct.fluids.fluids.PotionFluidAttributes;
import slimeknights.tconstruct.fluids.fluids.SlimeFluid;
import slimeknights.tconstruct.fluids.fluids.SlimeFluidType;
import slimeknights.tconstruct.fluids.item.BottleItem;
import slimeknights.tconstruct.fluids.item.ContainerFoodItem;
import slimeknights.tconstruct.fluids.item.ContainerFoodItem.FluidContainerFoodItem;
import slimeknights.tconstruct.fluids.item.MagmaBottleItem;
import slimeknights.tconstruct.fluids.item.PotionBucketItem;
import slimeknights.tconstruct.fluids.util.BottleBrewingRecipe;
import slimeknights.tconstruct.fluids.util.EmptyBottleIntoEmpty;
import slimeknights.tconstruct.fluids.util.EmptyBottleIntoWater;
import slimeknights.tconstruct.fluids.util.FillBottle;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.shared.TinkerEffects;
import slimeknights.tconstruct.shared.TinkerFood;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.item.CopperCanItem;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.data.material.MaterialIds;
import slimeknights.tconstruct.tools.network.FluidDataSerializer;
import slimeknights.tconstruct.world.TinkerWorld;

import static slimeknights.mantle.Mantle.commonResource;
import static slimeknights.tconstruct.fluids.block.BurningLiquidBlock.createBurning;
import static slimeknights.tconstruct.fluids.block.MobEffectLiquidBlock.createEffect;
import java.util.Map;
import net.minecraft.network.syncher.EntityDataSerializer;

/**
 * Contains all fluids used throughout the mod
 */
@SuppressWarnings("removal")
public final class TinkerFluids extends TinkerModule {
  public TinkerFluids() {
    Milk.enableMilkFluid();
  }

  /** Creative tab for general items, or those that lack another tab */
  public static final RegistryObject<CreativeModeTab> tabFluids = CREATIVE_TABS.register(
    "fluids", () -> CreativeModeTab.builder().title(TConstruct.makeTranslation("itemGroup", "fluids"))
                                   .icon(() -> TankItem.fillTank(TinkerSmeltery.searedTank, TankType.FUEL_GAUGE, TinkerFluids.moltenCobalt.get()))
                                   .displayItems(TinkerFluids::addFilledContainers)
                                   .withTabsBefore(TinkerTables.tabTables.getId())
                                   .withSearchBar()
                                   .build());

  // basic
  public static final FluidObject<SimpleFlowingFluid> blood = FLUIDS.register("blood", builder().density(1200).viscosity(1200).temperature(336), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 0);
  public static final FluidObject<SimpleFlowingFluid> venom = FLUIDS.register("venom", builder().density(1400).viscosity(1300).temperature(310), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 0);
  public static final ItemObject<Item> venomBottle = ITEMS.register("venom_bottle", () -> new FluidContainerFoodItem(
    new Item.Properties().food(new FoodProperties.Builder().alwaysEat()
                                 .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800), 1.0f)
                                 .effect(new MobEffectInstance(MobEffects.POISON, 450), 1.0f)
                                 .build()).stacksTo(1).craftRemainder(Items.GLASS_BOTTLE),
    () -> new FluidStack(venom.get(), FluidValues.BOTTLE))
  );

  // slime -  note second name parameter is forge tag name
  public static final FluidObject<SimpleFlowingFluid> earthSlime = FLUIDS.register("earth_slime", "slime",  builder().density(1400).viscosity(1400).temperature(350), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), SlimeFluid.Source::new, SlimeFluid.Flowing::new, 0);
  public static final FluidObject<SimpleFlowingFluid> skySlime   = FLUIDS.register("sky_slime",             builder().density(1500).viscosity(1500).temperature(310), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), SlimeFluid.Source::new, SlimeFluid.Flowing::new, 0);
  public static final FluidObject<SimpleFlowingFluid> enderSlime = FLUIDS.register("ender_slime",           builder().density(1600).viscosity(1600).temperature(370), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), SlimeFluid.Source::new, SlimeFluid.Flowing::new, 0);
  public static final FluidObject<SimpleFlowingFluid> magma      = FLUIDS.register("magma",                 builder().density(1900).viscosity(1900).temperature(600), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), SlimeFluid.Source::new, SlimeFluid.Flowing::new, 3);
  public static final FluidObject<DirectionalSlimeFluid> ichor    = FLUIDS.registerUpsideDown("ichor",       builder().density(-1200).viscosity(1900).temperature(1000).gaseous(), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), DirectionalSlimeFluid.Source::new, DirectionalSlimeFluid.Flowing::new, 3);
  public static final Map<SlimeType, FluidObject<SimpleFlowingFluid>> slime;
  static {
    slime = new EnumMap<>(SlimeType.class);
    slime.put(SlimeType.EARTH, earthSlime);
    slime.put(SlimeType.SKY, skySlime);
    slime.put(SlimeType.ENDER, enderSlime);
    slime.put(SlimeType.BLOOD, blood);
  }
  // bottles of slime
  public static final EnumObject<SlimeType, Item> slimeBottle = new EnumObject.Builder<SlimeType,Item>(SlimeType.class)
    .put(SlimeType.EARTH, ITEMS.register("earth_slime_bottle", () -> new FluidContainerFoodItem(
      new Item.Properties().food(new FoodProperties.Builder().alwaysEat()
                                   .effect(new MobEffectInstance(MobEffects.LUCK, 1500), 1.0f)
                                   .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 900), 1.0f)
                                   .build()).stacksTo(1).craftRemainder(Items.GLASS_BOTTLE),
      () -> new FluidStack(earthSlime.get(), FluidValues.BOTTLE))))
    .put(SlimeType.SKY, ITEMS.register("sky_slime_bottle", () -> new FluidContainerFoodItem(
      new Item.Properties().food(new FoodProperties.Builder().alwaysEat()
                                   .effect(new MobEffectInstance(MobEffects.JUMP, 1800), 1.0f)
                                   .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 900), 1.0f)
                                   .build()).stacksTo(1).craftRemainder(Items.GLASS_BOTTLE),
      () -> new FluidStack(skySlime.get(), FluidValues.BOTTLE))))
    .put(SlimeType.ENDER, ITEMS.register("ender_slime_bottle", () -> new FluidContainerFoodItem(
      new Item.Properties().food(new FoodProperties.Builder().alwaysEat()
                                   .effect(new MobEffectInstance(MobEffects.LEVITATION, 450), 1.0f)
                                   .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 900), 1.0f)
                                   .build()).stacksTo(1).craftRemainder(Items.GLASS_BOTTLE),
      () -> new FluidStack(enderSlime.get(), FluidValues.BOTTLE))))
    .put(SlimeType.ICHOR, ITEMS.register("ichor_bottle", () -> new ContainerFoodItem(
      new Item.Properties().food(new FoodProperties.Builder().alwaysEat()
                                   .effect(new MobEffectInstance(MobEffects.ABSORPTION, 500), 1.0f)
                                   .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 900), 1.0f)
                                   .build()).stacksTo(1).craftRemainder(Items.GLASS_BOTTLE))))
    .put(SlimeType.BLOOD, ITEMS.register("blood_bottle", () -> new FluidContainerFoodItem(
      new Item.Properties().food(new FoodProperties.Builder()
                                   .nutrition(6).saturationMod(0.1F)
                                   .effect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 600), 0.8f)
                                   .build()).stacksTo(16).craftRemainder(Items.GLASS_BOTTLE),
      () -> new FluidStack(blood.get(), FluidValues.BOTTLE))))
    .build();
  public static final ItemObject<Item> magmaBottle = ITEMS.register("magma_bottle", () -> new FluidContainerFoodItem(
    new Item.Properties().food(new FoodProperties.Builder().alwaysEat()
                                 .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600), 1.0f)
                                 .build()).stacksTo(1).craftRemainder(Items.GLASS_BOTTLE),
    () -> new FluidStack(magma.get(), FluidValues.BOTTLE)));

  // foods
  public static FluidObject<SimpleFlowingFluid> honey        = FLUIDS.register("honey",         builder().temperature(301), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 0);
  public static FluidObject<SimpleFlowingFluid> beetrootSoup = FLUIDS.register("beetroot_soup", builder().temperature(400), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 0);
  public static FluidObject<SimpleFlowingFluid> mushroomStew = FLUIDS.register("mushroom_stew", builder().temperature(400), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 0);
  public static FluidObject<SimpleFlowingFluid> rabbitStew   = FLUIDS.register("rabbit_stew",   builder().temperature(400), properties -> properties.mapColor(MapColor.WATER).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 0);

  // potion
  public static final FluidObject<UnplaceableFluid> potion = FLUIDS.register("potion").type(() -> new PotionFluidType(cool().descriptionId("item.minecraft.potion.effect.empty").density(1100).viscosity(1100).temperature(315).sound(SoundActions.BUCKET_FILL, SoundEvents.BOTTLE_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BOTTLE_EMPTY))).bucket(fluid -> new PotionBucketItem(fluid, RegistrationHelper.BUCKET_PROPS)).commonTag().unplacable();
  public static final ItemObject<Item> splashBottle = ITEMS.register("splash_bottle", () -> new BottleItem(Items.SPLASH_POTION, ITEM_PROPS));
  public static final ItemObject<Item> lingeringBottle = ITEMS.register("lingering_bottle", () -> new BottleItem(Items.LINGERING_POTION, ITEM_PROPS));

  // base molten fluids
  public static final FluidObject<SimpleFlowingFluid> searedStone   = FLUIDS.register("seared_stone",   builder().temperature( 900), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  6);
  public static final FluidObject<SimpleFlowingFluid> scorchedStone = FLUIDS.register("scorched_stone", builder().temperature( 800), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  4);
  public static final FluidObject<SimpleFlowingFluid> moltenClay    = FLUIDS.register("molten_clay",    builder().temperature( 750), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  3);
  public static final FluidObject<SimpleFlowingFluid> moltenGlass   = FLUIDS.register("molten_glass",   builder().temperature(1050), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  1);
  public static final FluidObject<SimpleFlowingFluid> liquidSoul    = FLUIDS.register("liquid_soul",    builder().temperature( 700), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  2);
  // ceramics compat
  public static final FluidObject<SimpleFlowingFluid> moltenPorcelain = FLUIDS.register("molten_porcelain", builder().temperature(1000), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 2);
  // fancy molten fluids
  public static final FluidObject<SimpleFlowingFluid> moltenObsidian = FLUIDS.register("molten_obsidian", builder().temperature(1300), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 3);
  public static final FluidObject<SimpleFlowingFluid> moltenEnder    = FLUIDS.register("molten_ender", "ender", builder().temperature( 777), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 5);
  public static final FluidObject<SimpleFlowingFluid> blazingBlood   = FLUIDS.register("blazing_blood",   builder().temperature(1800).density(3500), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 15);

  // ores
  public static final FluidObject<SimpleFlowingFluid> moltenEmerald  = FLUIDS.register("molten_emerald",  builder().temperature(1234), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  9);
  public static final FluidObject<SimpleFlowingFluid> moltenQuartz   = FLUIDS.register("molten_quartz",   builder().temperature( 937), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  6);
  public static final FluidObject<SimpleFlowingFluid> moltenAmethyst = FLUIDS.register("molten_amethyst", builder().temperature(1250), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 11);
  public static final FluidObject<SimpleFlowingFluid> moltenDiamond  = FLUIDS.register("molten_diamond",  builder().temperature(1750), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 13);
  public static final FluidObject<SimpleFlowingFluid> moltenDebris   = FLUIDS.register("molten_debris",   builder().temperature(1475), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 14);
  // metal ores
  public static final FluidObject<SimpleFlowingFluid> moltenIron   = FLUIDS.register("molten_iron",   builder().temperature(1100), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenGold   = FLUIDS.register("molten_gold",   builder().temperature(1000), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenCopper = FLUIDS.register("molten_copper", builder().temperature( 800), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenCobalt = FLUIDS.register("molten_cobalt", builder().temperature(1250), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  8);
  // alloys
  public static final FluidObject<SimpleFlowingFluid> moltenSlimesteel     = FLUIDS.register("molten_slimesteel",      builder().temperature(1200), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 10);
  public static final FluidObject<SimpleFlowingFluid> moltenAmethystBronze = FLUIDS.register("molten_amethyst_bronze", builder().temperature(1120), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenRoseGold       = FLUIDS.register("molten_rose_gold",       builder().temperature( 850), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenPigIron        = FLUIDS.register("molten_pig_iron",        builder().temperature(1111), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 10);

  public static final FluidObject<SimpleFlowingFluid> moltenManyullyn   = FLUIDS.register("molten_manyullyn",    builder().temperature(1500), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 11);
  public static final FluidObject<SimpleFlowingFluid> moltenHepatizon   = FLUIDS.register("molten_hepatizon",    builder().temperature(1700), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  8);
  public static final FluidObject<SimpleFlowingFluid> moltenQueensSlime = FLUIDS.register("molten_queens_slime", builder().temperature(1450), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  9);
  public static final FluidObject<SimpleFlowingFluid> moltenSoulsteel   = FLUIDS.register("molten_soulsteel",    builder().temperature(1500), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  6);
  public static final FluidObject<SimpleFlowingFluid> moltenNetherite   = FLUIDS.register("molten_netherite",    builder().temperature(1550), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 14);
  public static final FluidObject<SimpleFlowingFluid> moltenKnightslime = FLUIDS.register("molten_knightslime",  builder().temperature(1425), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);

  // compat ores
  public static final FluidObject<SimpleFlowingFluid> moltenTin      = FLUIDS.register("molten_tin",      builder().temperature( 525), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenAluminum = FLUIDS.register("molten_aluminum", builder().temperature( 725), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenLead     = FLUIDS.register("molten_lead",     builder().temperature( 630), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenSilver   = FLUIDS.register("molten_silver",   builder().temperature(1090), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenNickel   = FLUIDS.register("molten_nickel",   builder().temperature(1250), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenZinc     = FLUIDS.register("molten_zinc",     builder().temperature( 720), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenPlatinum = FLUIDS.register("molten_platinum", builder().temperature(1270), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenTungsten = FLUIDS.register("molten_tungsten", builder().temperature(1250), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenOsmium   = FLUIDS.register("molten_osmium",   builder().temperature(1275), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  4);
  public static final FluidObject<SimpleFlowingFluid> moltenUranium  = FLUIDS.register("molten_uranium",  builder().temperature(1130), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 15);

  // compat alloys
  public static final FluidObject<SimpleFlowingFluid> moltenBronze     = FLUIDS.register("molten_bronze",     builder().temperature(1000), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 10);
  public static final FluidObject<SimpleFlowingFluid> moltenBrass      = FLUIDS.register("molten_brass",      builder().temperature( 905), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 10);
  public static final FluidObject<SimpleFlowingFluid> moltenElectrum   = FLUIDS.register("molten_electrum",   builder().temperature(1060), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 10);
  public static final FluidObject<SimpleFlowingFluid> moltenInvar      = FLUIDS.register("molten_invar",      builder().temperature(1200), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 10);
  public static final FluidObject<SimpleFlowingFluid> moltenConstantan = FLUIDS.register("molten_constantan", builder().temperature(1220), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 10);
  public static final FluidObject<SimpleFlowingFluid> moltenPewter     = FLUIDS.register("molten_pewter",     builder().temperature( 700), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 10);
  public static final FluidObject<SimpleFlowingFluid> moltenSteel      = FLUIDS.register("molten_steel",      builder().temperature(1250), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 13);

  // mod-specific compat
  // thermal
  public static final FluidObject<SimpleFlowingFluid> moltenEnderium = FLUIDS.register("molten_enderium", builder().temperature(1650), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 12);
  public static final FluidObject<SimpleFlowingFluid> moltenLumium   = FLUIDS.register("molten_lumium",   builder().temperature(1350), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 15);
  public static final FluidObject<SimpleFlowingFluid> moltenSignalum = FLUIDS.register("molten_signalum", builder().temperature(1425), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 13);
  // mekanism
  public static final FluidObject<SimpleFlowingFluid> moltenRefinedGlowstone = FLUIDS.register("molten_refined_glowstone", builder().temperature(1125), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(), 15);
  public static final FluidObject<SimpleFlowingFluid> moltenRefinedObsidian  = FLUIDS.register("molten_refined_obsidian",  builder().temperature(1775), properties -> properties.mapColor(MapColor.FIRE).replaceable().pushReaction(PushReaction.DESTROY).liquid(),  7);

  // fluid data serializer
  public static final FluidDataSerializer FLUID_DATA_SERIALIZER = new FluidDataSerializer();
  // Forge put entity data serializers in a registry; vanilla keeps a static list, so register directly
  public static void registerSerializers() {
    EntityDataSerializers.registerSerializer(FLUID_DATA_SERIALIZER);
  }

  /** Creates a builder for a cool fluid with sounds */
  private static FluidType.Properties cool() {
    return FluidType.Properties.create()
                               .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                               .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                               .motionScale(0.0023333333333333335D)
                               .canExtinguish(true);

  }

  /** Creates a builder for a cool fluid with sounds and description */
  private static FluidType.Properties cool(String name) {
    return cool().descriptionId(TConstruct.makeDescriptionId("fluid", name))
                 .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                 .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
  }

  /** Creates a builder for a cool fluid with sounds and description */
  private static FluidType.Properties slime(String name) {
    return cool(name).density(1600).viscosity(1600);
  }

  /** Creates a builder for a cool fluid with sounds and description */
  @SuppressWarnings("SameParameterValue")
  private static FluidType.Properties powder(String name) {
    return FluidType.Properties.create().descriptionId(TConstruct.makeDescriptionId("fluid", name))
                               .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_POWDER_SNOW)
                               .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_POWDER_SNOW);
  }

  /** Creates a builder for a hot with sounds and description */
  private static FluidType.Properties hot(String name) {
    return FluidType.Properties.create().density(2000).viscosity(10000).temperature(1000)
      .descriptionId(TConstruct.makeDescriptionId("fluid", name))
      .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
      .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
      // from forge lava type
      .motionScale(0.0023333333333333335D)
      .canSwim(false).canDrown(false)
      .pathType(BlockPathTypes.LAVA).adjacentPathType(null);
  }

  public static void gatherData(final FabricDataGenerator.Pack pack) {
    pack.addProvider(FluidTooltipProvider::new);
  }

  public static void commonSetup() {
    CauldronInteraction.WATER.put(splashBottle.get(), new FillBottle(Items.SPLASH_POTION));
    CauldronInteraction.WATER.put(lingeringBottle.get(), new FillBottle(Items.LINGERING_POTION));
    CauldronInteraction.WATER.put(Items.SPLASH_POTION,    new EmptyBottleIntoWater(splashBottle,    CauldronInteraction.WATER.get(Items.SPLASH_POTION)));
    CauldronInteraction.WATER.put(Items.LINGERING_POTION, new EmptyBottleIntoWater(lingeringBottle, CauldronInteraction.WATER.get(Items.LINGERING_POTION)));
    CauldronInteraction.EMPTY.put(Items.SPLASH_POTION,    new EmptyBottleIntoEmpty(splashBottle,    CauldronInteraction.EMPTY.get(Items.SPLASH_POTION)));
    CauldronInteraction.EMPTY.put(Items.LINGERING_POTION, new EmptyBottleIntoEmpty(lingeringBottle, CauldronInteraction.EMPTY.get(Items.LINGERING_POTION)));
    // brew bottles into each other, bit weird but feels better than shapeless
    BrewingRecipeRegistry.addRecipe(new BottleBrewingRecipe(Ingredient.of(Items.GLASS_BOTTLE), Items.POTION, Items.SPLASH_POTION, new ItemStack(splashBottle)));
    BrewingRecipeRegistry.addRecipe(new BottleBrewingRecipe(Ingredient.of(TinkerTags.Items.SPLASH_BOTTLE), Items.SPLASH_POTION, Items.LINGERING_POTION, new ItemStack(lingeringBottle)));

    // dispense buckets
    DispenseItemBehavior dispenseBucket = new DefaultDispenseItemBehavior() {
      private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

      @Override
      public ItemStack execute(BlockSource source, ItemStack stack) {
        DispensibleContainerItem container = (DispensibleContainerItem)stack.getItem();
        BlockPos blockpos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
        Level level = source.getLevel();
        if (container.emptyContents(null, level, blockpos, null, stack)) {
          container.checkExtraContent(null, level, stack, blockpos);
          return new ItemStack(Items.BUCKET);
        } else {
          return this.defaultDispenseItemBehavior.dispense(source, stack);
        }
      }
    };
    // slime
    DispenserBlock.registerBehavior(blood, dispenseBucket);
    DispenserBlock.registerBehavior(venom, dispenseBucket);
    DispenserBlock.registerBehavior(earthSlime, dispenseBucket);
    DispenserBlock.registerBehavior(skySlime, dispenseBucket);
    DispenserBlock.registerBehavior(enderSlime, dispenseBucket);
    DispenserBlock.registerBehavior(magma, dispenseBucket);
    // foods
    DispenserBlock.registerBehavior(honey, dispenseBucket);
    DispenserBlock.registerBehavior(beetrootSoup, dispenseBucket);
    DispenserBlock.registerBehavior(mushroomStew, dispenseBucket);
    DispenserBlock.registerBehavior(rabbitStew, dispenseBucket);
    // base molten fluids
    DispenserBlock.registerBehavior(searedStone, dispenseBucket);
    DispenserBlock.registerBehavior(scorchedStone, dispenseBucket);
    DispenserBlock.registerBehavior(moltenClay, dispenseBucket);
    DispenserBlock.registerBehavior(moltenGlass, dispenseBucket);
    DispenserBlock.registerBehavior(liquidSoul, dispenseBucket);
    DispenserBlock.registerBehavior(moltenPorcelain, dispenseBucket);
    DispenserBlock.registerBehavior(moltenObsidian, dispenseBucket);
    DispenserBlock.registerBehavior(moltenEnder, dispenseBucket);
    DispenserBlock.registerBehavior(blazingBlood, dispenseBucket);
    // ores
    DispenserBlock.registerBehavior(moltenEmerald, dispenseBucket);
    DispenserBlock.registerBehavior(moltenQuartz, dispenseBucket);
    DispenserBlock.registerBehavior(moltenAmethyst, dispenseBucket);
    DispenserBlock.registerBehavior(moltenDiamond, dispenseBucket);
    DispenserBlock.registerBehavior(moltenDebris, dispenseBucket);
    // metal ores
    DispenserBlock.registerBehavior(moltenIron, dispenseBucket);
    DispenserBlock.registerBehavior(moltenGold, dispenseBucket);
    DispenserBlock.registerBehavior(moltenCopper, dispenseBucket);
    DispenserBlock.registerBehavior(moltenCobalt, dispenseBucket);
    // alloys
    DispenserBlock.registerBehavior(moltenSlimesteel, dispenseBucket);
    DispenserBlock.registerBehavior(moltenAmethystBronze, dispenseBucket);
    DispenserBlock.registerBehavior(moltenRoseGold, dispenseBucket);
    DispenserBlock.registerBehavior(moltenPigIron, dispenseBucket);
    DispenserBlock.registerBehavior(moltenManyullyn, dispenseBucket);
    DispenserBlock.registerBehavior(moltenHepatizon, dispenseBucket);
    DispenserBlock.registerBehavior(moltenQueensSlime, dispenseBucket);
    DispenserBlock.registerBehavior(moltenSoulsteel, dispenseBucket);
    DispenserBlock.registerBehavior(moltenNetherite, dispenseBucket);
    DispenserBlock.registerBehavior(moltenKnightslime, dispenseBucket);
    // compat ores
    DispenserBlock.registerBehavior(moltenTin, dispenseBucket);
    DispenserBlock.registerBehavior(moltenAluminum, dispenseBucket);
    DispenserBlock.registerBehavior(moltenLead, dispenseBucket);
    DispenserBlock.registerBehavior(moltenSilver, dispenseBucket);
    DispenserBlock.registerBehavior(moltenNickel, dispenseBucket);
    DispenserBlock.registerBehavior(moltenZinc, dispenseBucket);
    DispenserBlock.registerBehavior(moltenPlatinum, dispenseBucket);
    DispenserBlock.registerBehavior(moltenTungsten, dispenseBucket);
    DispenserBlock.registerBehavior(moltenOsmium, dispenseBucket);
    DispenserBlock.registerBehavior(moltenUranium, dispenseBucket);
    // compat alloys
    DispenserBlock.registerBehavior(moltenBronze, dispenseBucket);
    DispenserBlock.registerBehavior(moltenBrass, dispenseBucket);
    DispenserBlock.registerBehavior(moltenElectrum, dispenseBucket);
    DispenserBlock.registerBehavior(moltenInvar, dispenseBucket);
    DispenserBlock.registerBehavior(moltenConstantan, dispenseBucket);
    DispenserBlock.registerBehavior(moltenPewter, dispenseBucket);
    DispenserBlock.registerBehavior(moltenSteel, dispenseBucket);
    // mod-specific compat alloys
    DispenserBlock.registerBehavior(moltenEnderium, dispenseBucket);
    DispenserBlock.registerBehavior(moltenLumium, dispenseBucket);
    DispenserBlock.registerBehavior(moltenSignalum, dispenseBucket);
    DispenserBlock.registerBehavior(moltenRefinedGlowstone, dispenseBucket);
    DispenserBlock.registerBehavior(moltenRefinedObsidian, dispenseBucket);

    // brew congealed slime into bottles to get slime bottles, easy melting
    for (SlimeType slime : SlimeType.values()) {
      BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(Items.GLASS_BOTTLE), Ingredient.of(TinkerWorld.congealedSlime.get(slime)), new ItemStack(TinkerFluids.slimeBottle.get(slime))));
    }
    BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(Items.GLASS_BOTTLE), Ingredient.of(Blocks.MAGMA_BLOCK), new ItemStack(TinkerFluids.magmaBottle)));
  }

  /** Adds all relevant items to the creative tab, called by smeltery */
  @SuppressWarnings("deprecation")
  public static void addTabItems(ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
    // containers
    output.accept(splashBottle);
    output.accept(lingeringBottle);
    // slime
    output.accept(earthSlime);
    output.accept(skySlime);
    output.accept(ichor);
    output.accept(enderSlime);
    accept(output, slimeBottle);
    output.accept(magma);
    output.accept(magmaBottle);
    output.accept(venom);
    output.accept(venomBottle);

    // food
    output.accept(honey);
    output.accept(beetrootSoup);
    output.accept(mushroomStew);
    output.accept(rabbitStew);
    output.accept(meatSoup);
    output.accept(meatSoupBowl);

    // stone
    output.accept(searedStone);
    output.accept(scorchedStone);
    output.accept(moltenClay);
    if (ModList.get().isLoaded("ceramics")) {
      output.accept(moltenPorcelain);
    }
    output.accept(moltenGlass);
    output.accept(moltenObsidian);
    output.accept(liquidSoul);
    output.accept(moltenEnder);
    output.accept(blazingBlood);

    // ores
    output.accept(moltenEmerald);
    output.accept(moltenQuartz);
    output.accept(moltenAmethyst);
    output.accept(moltenDiamond);
    output.accept(moltenDebris);
    // metal ores
    output.accept(moltenCopper);
    output.accept(moltenIron);
    output.accept(moltenGold);
    output.accept(moltenCobalt);
    output.accept(moltenSteel);

    // overworld alloys
    output.accept(moltenSlimesteel);
    output.accept(moltenAmethystBronze);
    output.accept(moltenRoseGold);
    output.accept(moltenPigIron);
    // nether alloys
    output.accept(moltenCinderslime);
    output.accept(moltenQueensSlime);
    output.accept(moltenManyullyn);
    output.accept(moltenHepatizon);
    output.accept(moltenNetherite);
    output.accept(moltenKnightmetal);
    output.accept(moltenKnightslime);
    // future: soulsteel

    // compat ores
    acceptMolten(output, moltenTin);
    acceptCompat(output, moltenAluminum, MaterialIds.aluminum);
    acceptCompat(output, moltenLead, MaterialIds.lead);
    acceptCompat(output, moltenSilver, MaterialIds.silver);
    acceptMolten(output, moltenNickel);
    acceptMolten(output, moltenZinc);
    acceptMolten(output, moltenPlatinum);
    acceptMolten(output, moltenTungsten);
    acceptCompat(output, moltenOsmium, MaterialIds.osmium);
    acceptMolten(output, moltenUranium, MaterialIds.necronium);
    acceptMolten(output, moltenChromium);
    acceptMolten(output, moltenCadmium);
    // compat alloys
    acceptCompat(output, moltenBronze, MaterialIds.bronze);
    acceptMolten(output, moltenBrass, MaterialIds.platedSlimewood);
    acceptCompat(output, moltenElectrum, MaterialIds.electrum);
    acceptCompat(output, moltenInvar, MaterialIds.invar);
    acceptCompat(output, moltenConstantan, MaterialIds.constantan);
    acceptCompat(output, moltenPewter, MaterialIds.pewter);
    acceptMolten(output, moltenNicrosil, MaterialIds.nicrosil);
    acceptMolten(output, moltenEnderium);
    acceptMolten(output, moltenLumium);
    acceptMolten(output, moltenSignalum);
    acceptMolten(output, moltenRefinedGlowstone);
    acceptMolten(output, moltenRefinedObsidian);
    acceptMolten(output, moltenDuralumin);
    acceptMolten(output, moltenBendalloy);
    acceptCompat(output, moltenSteeleaf, MaterialIds.steeleaf);
    acceptCompat(output, fieryLiquid, "fiery", MaterialIds.fiery);
    // potion buckets
    BuiltInRegistries.POTION.holders().filter(holder -> {
      Potion potion = holder.get();
      return potion != Potions.EMPTY && potion != Potions.WATER;
    }).forEachOrdered(holder ->
      output.accept(PotionFluidType.potionBucket(holder.key())));
  }

  /** Adds all filled containers to the fluids tab. */
  private static void addFilledContainers(ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
    // add copper cans, tanks, and lanterns for all the fluids
    CopperCanItem.addFilledVariants(output::accept);
    TankItem.addFilledVariants(output::accept);
  }

  /**
   * Accepts the given item if the passed ingot is present
   */
  private static void acceptCompat(Output output, ItemLike item, String ingot) {
    acceptIfTag(output, item, ItemTags.create(commonResource("ingots/" + ingot)));
  }

  /** Accepts the given item if the passed ingot or material is present */
  private static void acceptCompat(CreativeModeTab.Output output, ItemLike item, String ingot, MaterialId material) {
    if (!acceptIfMaterial(output, item, material)) {
      acceptCompat(output, item, ingot);
    }
  }

  /** Accepts the given item if the passed material or same named ingot is present */
  private static void acceptCompat(CreativeModeTab.Output output, ItemLike item, MaterialId material) {
    acceptCompat(output, item, material.getPath(), material);
  }

  /** Accepts the given item if the ingot named after the fluid is present */
  private static void acceptMolten(CreativeModeTab.Output output, FluidObject<?> fluid) {
    acceptCompat(output, fluid, withoutMolten(fluid));
  }

  /** Accepts the given item if the ingot named after the fluid or the material is present */
  private static void acceptMolten(CreativeModeTab.Output output, FluidObject<?> fluid, MaterialId material) {
    acceptCompat(output, fluid, withoutMolten(fluid), material);
  }

  /** Length of the molten prefix */
  private static final int MOLTEN_LENGTH = "molten_".length();

  /** Removes the "molten_" prefix from the fluids ID */
  public static String withoutMolten(FluidObject<?> fluid) {
    return fluid.getId().getPath().substring(MOLTEN_LENGTH);
  }
}
