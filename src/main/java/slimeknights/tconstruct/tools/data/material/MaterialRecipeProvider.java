package slimeknights.tconstruct.tools.data.material;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.InstrumentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Instruments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.recipe.condition.TagCombinationCondition;
import slimeknights.mantle.recipe.condition.TagFilledCondition;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.data.BaseRecipeProvider;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.data.recipe.IMaterialRecipeHelper;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialFluidRecipeBuilder;
import slimeknights.tconstruct.library.recipe.ingredient.InstrumentIngredient;
import slimeknights.tconstruct.library.recipe.melting.MaterialMeltingRecipeBuilder;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.recipe.severing.SheepShearingRecipe;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("removal")
public class MaterialRecipeProvider extends BaseRecipeProvider implements IMaterialRecipeHelper {
  public MaterialRecipeProvider(FabricDataOutput output) {
    super(output);
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Material Recipe";
  }

  @Override
  public void buildRecipes(Consumer<FinishedRecipe> consumer) {
    addMaterialItems(consumer);
    addMaterialSmeltery(consumer);
  }

  private void addMaterialItems(Consumer<FinishedRecipe> consumer) {
    String folder = "tools/materials/";
    // tier 1
    materialRecipe(consumer, MaterialIds.wood,   Ingredient.of(Tags.Items.RODS_WOODEN), 1, 2, folder + "wood/sticks");
    // planks
    materialRecipe(consumer, MaterialIds.oak,     Ingredient.of(Items.OAK_PLANKS),      1, 1, folder + "wood/planks/oak");
    materialRecipe(consumer, MaterialIds.spruce,  Ingredient.of(Items.SPRUCE_PLANKS),   1, 1, folder + "wood/planks/spruce");
    materialRecipe(consumer, MaterialIds.birch,   Ingredient.of(Items.BIRCH_PLANKS),    1, 1, folder + "wood/planks/birch");
    materialRecipe(consumer, MaterialIds.jungle,  Ingredient.of(Items.JUNGLE_PLANKS),   1, 1, folder + "wood/planks/jungle");
    materialRecipe(consumer, MaterialIds.darkOak, Ingredient.of(Items.DARK_OAK_PLANKS), 1, 1, folder + "wood/planks/dark_oak");
    materialRecipe(consumer, MaterialIds.acacia,  Ingredient.of(Items.ACACIA_PLANKS),   1, 1, folder + "wood/planks/acacia");
    materialRecipe(consumer, MaterialIds.crimson, Ingredient.of(Items.CRIMSON_PLANKS),  1, 1, folder + "wood/planks/crimson");
    materialRecipe(consumer, MaterialIds.warped,  Ingredient.of(Items.WARPED_PLANKS),   1, 1, folder + "wood/planks/warped");
    materialRecipe(consumer, MaterialIds.mangrove,  Ingredient.of(Items.MANGROVE_PLANKS),   1, 1, folder + "wood/planks/mangrove");
    materialRecipe(consumer, MaterialIds.cherry,  Ingredient.of(Items.CHERRY_PLANKS),   1, 1, folder + "wood/planks/cherry");
    materialRecipe(consumer, MaterialIds.bamboo,  Ingredient.of(Items.BAMBOO_PLANKS),   1, 1, folder + "wood/planks/bamboo");
    materialRecipe(withCondition(consumer, TagDifferencePresentCondition.ofKeys(ItemTags.PLANKS, TinkerTags.Items.VARIANT_PLANKS)), MaterialIds.wood,
                   DefaultCustomIngredients.difference(Ingredient.of(ItemTags.PLANKS), Ingredient.of(TinkerTags.Items.VARIANT_PLANKS)), 1, 1, folder + "wood/planks/default");
    // logs
    materialRecipe(consumer, MaterialIds.oak,     Ingredient.of(ItemTags.OAK_LOGS),      4, 1, ItemOutput.fromStack(new ItemStack(Blocks.OAK_PLANKS)),      folder + "wood/logs/oak");
    materialRecipe(consumer, MaterialIds.spruce,  Ingredient.of(ItemTags.SPRUCE_LOGS),   4, 1, ItemOutput.fromStack(new ItemStack(Blocks.SPRUCE_PLANKS)),   folder + "wood/logs/spruce");
    materialRecipe(consumer, MaterialIds.birch,   Ingredient.of(ItemTags.BIRCH_LOGS),    4, 1, ItemOutput.fromStack(new ItemStack(Blocks.BIRCH_PLANKS)),    folder + "wood/logs/birch");
    materialRecipe(consumer, MaterialIds.jungle,  Ingredient.of(ItemTags.JUNGLE_LOGS),   4, 1, ItemOutput.fromStack(new ItemStack(Blocks.JUNGLE_PLANKS)),   folder + "wood/logs/jungle");
    materialRecipe(consumer, MaterialIds.darkOak, Ingredient.of(ItemTags.DARK_OAK_LOGS), 4, 1, ItemOutput.fromStack(new ItemStack(Blocks.DARK_OAK_PLANKS)), folder + "wood/logs/dark_oak");
    materialRecipe(consumer, MaterialIds.acacia,  Ingredient.of(ItemTags.ACACIA_LOGS),   4, 1, ItemOutput.fromStack(new ItemStack(Blocks.ACACIA_PLANKS)),   folder + "wood/logs/acacia");
    materialRecipe(consumer, MaterialIds.crimson, Ingredient.of(ItemTags.CRIMSON_STEMS), 4, 1, ItemOutput.fromStack(new ItemStack(Blocks.CRIMSON_PLANKS)),  folder + "wood/logs/crimson");
    materialRecipe(consumer, MaterialIds.warped,  Ingredient.of(ItemTags.WARPED_STEMS),  4, 1, ItemOutput.fromStack(new ItemStack(Blocks.WARPED_PLANKS)),   folder + "wood/logs/warped");
    materialRecipe(withCondition(consumer, TagDifferencePresentCondition.ofKeys(ItemTags.LOGS, TinkerTags.Items.VARIANT_LOGS)), MaterialIds.wood,
                   DefaultCustomIngredients.difference(Ingredient.of(ItemTags.LOGS), Ingredient.of(TinkerTags.Items.VARIANT_LOGS)), 4, 1,
                   ItemOutput.fromStack(new ItemStack(Items.STICK, 2)), folder + "wood/logs/default");
    // stone
    materialRecipe(consumer, MaterialIds.stone,      Ingredient.of(TinkerTags.Items.STONE),      1, 1, folder + "rock/stone");
    materialRecipe(consumer, MaterialIds.andesite,   Ingredient.of(TinkerTags.Items.ANDESITE),   1, 1, folder + "rock/andesite");
    materialRecipe(consumer, MaterialIds.diorite,    Ingredient.of(TinkerTags.Items.DIORITE),    1, 1, folder + "rock/diorite");
    materialRecipe(consumer, MaterialIds.granite,    Ingredient.of(TinkerTags.Items.GRANITE),    1, 1, folder + "rock/granite");
    materialRecipe(consumer, MaterialIds.blackstone, Ingredient.of(TinkerTags.Items.BLACKSTONE), 1, 1, folder + "rock/blackstone");
    materialRecipe(consumer, MaterialIds.calcite,    Ingredient.of(Blocks.CALCITE),              1, 1, folder + "rock/calcite");
    materialRecipe(consumer, MaterialIds.flint,      Ingredient.of(Items.FLINT),                 1, 1, folder + "flint/flint");
    materialRecipe(consumer, MaterialIds.basalt,     Ingredient.of(TinkerTags.Items.BASALT),     1, 1, folder + "flint/basalt");
    materialRecipe(consumer, MaterialIds.deepslate,  Ingredient.of(TinkerTags.Items.DEEPSLATE),  1, 1, folder + "flint/deepslate");
    // copper - want to include oxidized and waxed
    ItemOutput copperIngot = ItemOutput.fromTag(Tags.Items.INGOTS_COPPER);
    materialRecipe(consumer, MaterialIds.copper, Ingredient.of(TinkerTags.Items.NUGGETS_COPPER), 1, 9, folder + "copper/nugget");
    materialRecipe(consumer, MaterialIds.copper, Ingredient.of(Tags.Items.INGOTS_COPPER),        1, 1, folder + "copper/ingot");
    materialRecipe(consumer, MaterialIds.copper, CompoundIngredient.of(Ingredient.of(Tags.Items.STORAGE_BLOCKS_COPPER), Ingredient.of(Blocks.WAXED_COPPER_BLOCK)), 9, 1, copperIngot, folder + "copper/block");
    materialRecipe(consumer, MaterialIds.oxidizedCopper, Ingredient.of(Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER, Blocks.WAXED_EXPOSED_COPPER, Blocks.WAXED_WEATHERED_COPPER, Blocks.WAXED_OXIDIZED_COPPER), 9, 1, copperIngot, folder + "copper/oxidized");
    // other tier 1
    materialRecipe(consumer, MaterialIds.bone,         Ingredient.of(TinkerTags.Items.BONES),    1, 1, folder + "bone");
    materialRecipe(consumer, MaterialIds.chorus,       Ingredient.of(Items.POPPED_CHORUS_FRUIT), 1, 1, folder + "chorus_popped");
    // tier 1 binding
    materialRecipe(consumer, MaterialIds.string,  Ingredient.of(Tags.Items.STRING),  1, 4, folder + "string");
    materialRecipe(consumer, MaterialIds.leather, Ingredient.of(Tags.Items.LEATHER), 1, 1, folder + "leather");
    materialRecipe(consumer, MaterialIds.leather, Ingredient.of(Items.RABBIT_HIDE),  1, 2, folder + "rabbit_hide");
    materialRecipe(consumer, MaterialIds.vine,    Ingredient.of(Items.VINE),         1, 1, folder + "vine");
    materialRecipe(consumer, MaterialIds.cactus,  Ingredient.of(Blocks.CACTUS),      1, 1, folder + "cactus");
    materialRecipe(consumer, MaterialIds.feather, Ingredient.of(Items.FEATHER),      1, 1, folder + "feather");
    materialRecipe(consumer, MaterialIds.paper,   Ingredient.of(Items.PAPER),        1, 1, folder + "paper");
    materialRecipe(consumer, MaterialIds.leaves,  Ingredient.of(ItemTags.LEAVES),    1, 1, folder + "leaves");
    // tier 1 wool
    for (DyeColor color : DyeColor.values()) {
      String name = color.getName();
      materialRecipe(consumer, MaterialVariantId.create(MaterialIds.wool, name), Ingredient.of(SheepShearingRecipe.WOOL_BY_COLOR.get(color)), 1, 1, folder + "wool/" + name);
    }

    // tier 2
    metalMaterialRecipe(consumer, MaterialIds.iron, folder, "iron", false);
    metalMaterialRecipe(consumer, MaterialIds.gold, folder, "gold", false);
    materialRecipe(consumer, MaterialIds.searedStone,   Ingredient.of(TinkerSmeltery.searedBrick),       1, 1, folder + "seared_stone/brick");
    materialRecipe(consumer, MaterialIds.searedStone,   Ingredient.of(TinkerTags.Items.SEARED_BLOCKS),   4, 1, ItemOutput.fromItem(TinkerSmeltery.searedBrick), folder + "seared_stone/block");
    materialRecipe(consumer, MaterialIds.scorchedStone, Ingredient.of(TinkerSmeltery.scorchedBrick),     1, 1, folder + "scorched_stone/brick");
    materialRecipe(consumer, MaterialIds.scorchedStone, Ingredient.of(TinkerTags.Items.SCORCHED_BLOCKS), 4, 1, ItemOutput.fromItem(TinkerSmeltery.scorchedBrick), folder + "scorched_stone/block");
    materialRecipe(consumer, MaterialIds.venombone,     Ingredient.of(TinkerMaterials.venombone),        1, 1, folder + "venombone");
    metalMaterialRecipe(consumer, MaterialIds.roseGold, folder, "rose_gold", false);
    materialRecipe(consumer, MaterialIds.necroticBone, Ingredient.of(TinkerTags.Items.WITHER_BONES), 1, 1, folder + "necrotic_bone");
    materialRecipe(consumer, MaterialIds.endstone, Ingredient.of(Tags.Items.END_STONES), 1, 1, folder + "endstone");
    // ammo
    materialRecipe(consumer, MaterialIds.turtle,     Ingredient.of(Items.SCUTE),                 1, 1, folder + "turtle_scute");
    materialRecipe(consumer, MaterialIds.nautilus,   Ingredient.of(Items.NAUTILUS_SHELL),        4, 1, folder + "nautilus_shell");
    materialRecipe(consumer, MaterialIds.earthslime, Ingredient.of(TinkerWorld.earthGeode),      1, 1, folder + "earthslime");
    materialRecipe(consumer, MaterialIds.skyslime,   Ingredient.of(TinkerWorld.skyGeode),        1, 1, folder + "skyslime");
    materialRecipe(consumer, MaterialIds.blaze,      Ingredient.of(Tags.Items.RODS_BLAZE),       1, 1, folder + "blaze");
    materialRecipe(consumer, MaterialIds.enderPearl, Ingredient.of(Tags.Items.ENDER_PEARLS),     1, 1, folder + "ender_pearl");
    materialRecipe(consumer, MaterialIds.amethyst,   Ingredient.of(Tags.Items.GEMS_AMETHYST),    1, 1, folder + "amethyst");
    materialRecipe(consumer, MaterialIds.prismarine, Ingredient.of(Tags.Items.DUSTS_PRISMARINE), 1, 1, folder + "prismarine");
    materialRecipe(consumer, MaterialIds.glass,      Ingredient.of(Tags.Items.GLASS),            4, 1, folder + "glass");
    materialRecipe(consumer, MaterialIds.glass,      Ingredient.of(Tags.Items.GLASS_PANES),      1, 1, folder + "glass_pane");

    materialRecipe(consumer, MaterialIds.skyslimeVine, Ingredient.of(TinkerWorld.skySlimeVine), 1, 1, folder + "skyslime_vine");
    materialRecipe(consumer, MaterialIds.weepingVine,  Ingredient.of(Items.WEEPING_VINES), 1, 1, folder + "weeping_vine");
    materialRecipe(consumer, MaterialIds.twistingVine, Ingredient.of(Items.TWISTING_VINES), 1, 1, folder + "twisting_vine");
    // slimewood
    materialRecipe(consumer, MaterialIds.greenheart,  Ingredient.of(TinkerWorld.greenheart),  1, 1, folder + "slimewood/greenheart_planks");
    materialRecipe(consumer, MaterialIds.skyroot,     Ingredient.of(TinkerWorld.skyroot),     1, 1, folder + "slimewood/skyroot_planks");
    materialRecipe(consumer, MaterialIds.bloodshroom, Ingredient.of(TinkerWorld.bloodshroom), 1, 1, folder + "slimewood/bloodshroom_planks");
    materialRecipe(consumer, MaterialIds.enderbark,   Ingredient.of(TinkerWorld.enderbark),   1, 1, folder + "slimewood/enderbark_planks");
    materialRecipe(consumer, MaterialIds.greenheart,  Ingredient.of(TinkerWorld.greenheart.getLogItemTag()),  4, 1, ItemOutput.fromItem(TinkerWorld.greenheart),  folder + "slimewood/greenheart_logs");
    materialRecipe(consumer, MaterialIds.skyroot,     Ingredient.of(TinkerWorld.skyroot.getLogItemTag()),     4, 1, ItemOutput.fromItem(TinkerWorld.skyroot),     folder + "slimewood/skyroot_logs");
    materialRecipe(consumer, MaterialIds.bloodshroom, Ingredient.of(TinkerWorld.bloodshroom.getLogItemTag()), 4, 1, ItemOutput.fromItem(TinkerWorld.bloodshroom), folder + "slimewood/bloodshroom_logs");
    materialRecipe(consumer, MaterialIds.enderbark,   Ingredient.of(TinkerWorld.enderbark.getLogItemTag()),   4, 1, ItemOutput.fromItem(TinkerWorld.enderbark),   folder + "slimewood/enderbark_logs");
    // slimeball
    for (SlimeType type : SlimeType.values()) {
      String name = type.getSerializedName();
      materialRecipe(consumer, MaterialVariantId.create(MaterialIds.slimeball, name), Ingredient.of(type.getSlimeballTag()), 1, 1, folder + "slimeball/" + name);
    }
    materialRecipe(consumer, MaterialIds.magma, Ingredient.of(Items.MAGMA_CREAM),1, 1, folder + "magma");
    materialRecipe(consumer, MaterialIds.clay, Ingredient.of(Items.CLAY_BALL),   1, 1, folder + "clay_ball");
    materialRecipe(consumer, MaterialIds.clay, Ingredient.of(Blocks.CLAY),       4, 1, folder + "clay_block");

    // tier 3
    metalMaterialRecipe(consumer, MaterialIds.slimesteel, folder, "slimesteel", false);
    materialRecipe(consumer, MaterialIds.nahuatl, Ingredient.of(TinkerMaterials.nahuatl), 1, 1, folder + "nahuatl");
    metalMaterialRecipe(consumer, MaterialIds.amethystBronze, folder, "amethyst_bronze", false);
    metalMaterialRecipe(consumer, MaterialIds.pigIron, folder, "pig_iron", false);
    materialRecipe(consumer, MaterialIds.obsidian, Ingredient.of(Items.OBSIDIAN),             4, 1, folder + "obsidian");
    materialRecipe(consumer, MaterialIds.obsidian, Ingredient.of(TinkerCommons.obsidianPane), 1, 1, folder + "obsidian_pane");
    // misc
    materialRecipe(consumer, MaterialIds.ice, Ingredient.of(Blocks.ICE),        1, 9, folder + "ice/unpacked");
    materialRecipe(consumer, MaterialIds.ice, Ingredient.of(Blocks.PACKED_ICE), 1, 1, folder + "ice/packed");
    materialRecipe(consumer, MaterialIds.ice, Ingredient.of(Blocks.BLUE_ICE),   9, 1, folder + "ice/blue");
    materialRecipe(consumer, MaterialIds.ichor, Ingredient.of(TinkerWorld.ichorGeode), 1, 1, folder + "ichor");
    materialRecipe(consumer, MaterialIds.quartz, Ingredient.of(Tags.Items.GEMS_QUARTZ),           1, 1, folder + "quartz/gem");
    materialRecipe(consumer, MaterialIds.quartz, Ingredient.of(Tags.Items.STORAGE_BLOCKS_QUARTZ), 4, 1, folder + "quartz/block");
    materialRecipe(consumer, MaterialIds.glowstone, Ingredient.of(Tags.Items.DUSTS_GLOWSTONE), 1, 4, folder + "glowstone/dust");
    materialRecipe(consumer, MaterialIds.glowstone, Ingredient.of(Blocks.GLOWSTONE), 1, 1, ItemOutput.fromItem(Items.GLOWSTONE_DUST),folder + "glowstone/block");
    materialRecipe(consumer, MaterialIds.magnetite, Ingredient.of(TinkerTags.Items.STEEL_SHARD), 1, 1, folder + "magnetite");
    materialRecipe(consumer, MaterialIds.kobold, Ingredient.of(TinkerTags.Items.COBALT_SHARD), 1, 1, folder + "kobold");
    materialRecipe(consumer, MaterialIds.gunpowder, Ingredient.of(Tags.Items.GUNPOWDER), 1, 4, folder + "gunpowder");
    materialRecipe(consumer, MaterialIds.redstone, Ingredient.of(Tags.Items.DUSTS_REDSTONE), 1, 4, folder + "redstone/dust");
    materialRecipe(consumer, MaterialIds.redstone, Ingredient.of(Tags.Items.STORAGE_BLOCKS_REDSTONE), 9, 4, ItemOutput.fromItem(Items.REDSTONE, 4), folder + "redstone/block");

    // tier 3 (nether)
    metalMaterialRecipe(consumer, MaterialIds.cobalt, folder, "cobalt", false);
    metalMaterialRecipe(consumer, MaterialIds.steel,  folder, "steel",  false);
    // tier 4
    metalMaterialRecipe(consumer, MaterialIds.cinderslime, folder, "cinderslime", false);
    metalMaterialRecipe(consumer, MaterialIds.queensSlime, folder, "queens_slime", false);
    metalMaterialRecipe(consumer, MaterialIds.manyullyn, folder, "manyullyn", false);
    metalMaterialRecipe(consumer, MaterialIds.hepatizon, folder, "hepatizon", false);
    metalMaterialRecipe(consumer, MaterialIds.knightmetal, folder, "knightmetal", false);
    metalMaterialRecipe(consumer, MaterialIds.knightslime, folder, "knightslime", false);
    materialRecipe(consumer, MaterialIds.blazewood, Ingredient.of(TinkerMaterials.blazewood), 1, 1, folder + "blazewood");
    materialRecipe(consumer, MaterialIds.blazingBone, Ingredient.of(TinkerMaterials.blazingBone), 1, 1, folder + "blazing_bone");
    //registerMetalMaterial(consumer, MaterialIds.soulsteel,   "soulsteel",    false);
    // debris has no storage block, just ingots and nuggets
    materialRecipe(consumer, MaterialIds.ancient, Ingredient.of(TinkerTags.Items.INGOTS_NETHERITE_SCRAP), 1, 1, folder + "ancient/ingot");
    materialRecipe(consumer, MaterialIds.ancient, Ingredient.of(TinkerTags.Items.NUGGETS_NETHERITE_SCRAP), 1, 9, folder + "ancient/nugget");
    materialRecipe(consumer, MaterialIds.dragonScale, Ingredient.of(TinkerModifiers.dragonScale), 1, 1, folder + "dragon_scale");
    materialRecipe(consumer, MaterialIds.shulker, Ingredient.of(Items.SHULKER_SHELL), 2, 1, folder + "shulker");
    materialRecipe(consumer, MaterialIds.endRod, Ingredient.of(Items.END_ROD), 1, 1, folder + "end_rod");
    materialRecipe(consumer, MaterialIds.knightly, Ingredient.of(TinkerTags.Items.KNIGHTMETAL_SHARD), 1, 1, folder + "knightly");

    // tier 5
    materialRecipe(consumer, MaterialIds.enderslimeVine, Ingredient.of(TinkerWorld.enderSlimeVine), 1, 1, folder + "enderslime_vine");

    // tier 2 (mod compat)
    metalMaterialRecipe(consumer, MaterialIds.osmium, folder, "osmium", true);
    metalMaterialRecipe(consumer, MaterialIds.ironwood, folder, "ironwood", true);
    metalMaterialRecipe(consumer, MaterialIds.silver, folder, "silver", true);
    metalMaterialRecipe(consumer, MaterialIds.lead, folder, "lead", true);
    metalMaterialRecipe(consumer, MaterialIds.aluminum, folder, "aluminum", true);
    materialRecipe(withCondition(consumer, tagCondition("treated_wood")),  MaterialIds.treatedWood, Ingredient.of(getItemTag(COMMON, "treated_wood")), 1, 1, folder + "treated_wood");
    // no whitestone, use repair kits
    // tier 3 (mod integration)
    metalMaterialRecipe(consumer, MaterialIds.bronze, folder, "bronze", true);
    metalMaterialRecipe(consumer, MaterialIds.constantan, folder, "constantan", true);
    metalMaterialRecipe(consumer, MaterialIds.invar, folder, "invar", true);
    materialRecipe(withCondition(consumer, tagCondition("uranium_ingots")), MaterialIds.necronium, Ingredient.of(TinkerMaterials.necroniumBone), 1, 1, folder + "necronium");
    metalMaterialRecipe(consumer, MaterialIds.electrum, folder, "electrum", true);
    metalMaterialRecipe(consumer, MaterialIds.steeleaf, folder, "steeleaf", true);
    // no plated slimewood, use repair kits
    // tier 4 (mod integration)
    metalMaterialRecipe(consumer, MaterialIds.fiery, folder, "fiery", true);
    metalMaterialRecipe(consumer, MaterialIds.nicrosil, folder, "nicrosil", true);

    // slimesuit
    materialRecipe(consumer, MaterialIds.enderslime, Ingredient.of(TinkerWorld.enderGeode),     1, 1, folder + "enderslime");
    materialRecipe(consumer, MaterialIds.honey,      Ingredient.of(Items.HONEY_BOTTLE),         1, 1, folder + "honey");
    materialRecipe(consumer, MaterialIds.honey,      Ingredient.of(Blocks.HONEY_BLOCK),         4, 1, folder + "honey_block");
    materialRecipe(consumer, MaterialIds.venom,      Ingredient.of(Items.SPIDER_EYE),           1, 1, folder + "venom_eye");
    materialRecipe(consumer, MaterialIds.venom,      Ingredient.of(Items.FERMENTED_SPIDER_EYE), 2, 1, folder + "venom_fermented");
    materialRecipe(consumer, MaterialIds.phantom,    Ingredient.of(Items.PHANTOM_MEMBRANE),     1, 1, folder + "phantom_membrane");
    materialRecipe(consumer, MaterialIds.cheese,     Ingredient.of(TinkerCommons.cheeseIngot),  1, 1, folder + "cheese_ingot");
    materialRecipe(consumer, MaterialIds.cheese,     Ingredient.of(TinkerCommons.cheeseBlock),  4, 1, folder + "cheese_block");
    // goat horns
    // fallback recipe if mods add a new goat horn variant
    materialRecipe(withCondition(consumer, new TagCombinationCondition<>(List.of(InstrumentTags.GOAT_HORNS), TinkerTags.Instruments.VARIANT_HORNS)),
      MaterialIds.horn, InstrumentIngredient.of(Items.GOAT_HORN, TinkerTags.Instruments.VARIANT_HORNS), 4, 1, folder + "horn/default");
    hornMaterial(consumer, Instruments.PONDER_GOAT_HORN, folder);
    hornMaterial(consumer, Instruments.SING_GOAT_HORN,   folder);
    hornMaterial(consumer, Instruments.SEEK_GOAT_HORN,   folder);
    hornMaterial(consumer, Instruments.FEEL_GOAT_HORN,   folder);
    hornMaterial(consumer, Instruments.ADMIRE_GOAT_HORN, folder);
    hornMaterial(consumer, Instruments.CALL_GOAT_HORN,   folder);
    hornMaterial(consumer, Instruments.YEARN_GOAT_HORN,  folder);
    hornMaterial(consumer, Instruments.DREAM_GOAT_HORN,  folder);
  }

  private void addMaterialSmeltery(Consumer<FinishedRecipe> consumer) {
    String folder = "tools/materials/";

    // melting and casting
    // tier 2
    materialMeltingCasting(consumer, MaterialIds.iron,          TinkerFluids.moltenIron,    folder);
    materialMeltingCasting(consumer, MaterialIds.copper,        TinkerFluids.moltenCopper,  folder);
    materialMeltingCasting(consumer, MaterialIds.gold,          TinkerFluids.moltenGold,    folder);
    materialMeltingCasting(consumer, MaterialIds.searedStone,   TinkerFluids.searedStone,   FluidValues.BRICK, folder);
    materialMeltingCasting(consumer, MaterialIds.scorchedStone, TinkerFluids.scorchedStone, FluidValues.BRICK, folder);
    // half a clay is 1 seared brick per grout amounts
    materialComposite(consumer, MaterialIds.rock, MaterialIds.searedStone,        TinkerFluids.moltenClay, FluidValues.BRICK / 2, folder);
    materialComposite(consumer, MaterialIds.flint, MaterialIds.scorchedStone,     TinkerFluids.magma,      FluidValues.SLIMEBALL / 2, folder);
    materialComposite(consumer, MaterialIds.wood,    MaterialIds.slimewoodComposite, TinkerFluids.earthSlime, FluidValues.SLIMEBALL, folder);
    materialComposite(consumer, MaterialIds.bone, MaterialIds.venombone,          TinkerFluids.venom,      FluidValues.SLIMEBALL, folder);
    // oxidize copper and iron via water, it does not rust iron because magic
    MaterialFluidRecipeBuilder.material(MaterialIds.oxidizedIron)
                              .setInputId(MaterialIds.iron)
                              .setFluid(MantleTags.Fluids.WATER, FluidValues.BOTTLE)
                              .setTemperature(1)
                              .save(consumer, location(folder + "composite/iron_oxidized"));
    MaterialFluidRecipeBuilder.material(MaterialIds.oxidizedCopper)
                              .setInputId(MaterialIds.copper)
                              .setFluid(MantleTags.Fluids.WATER, FluidValues.BOTTLE)
                              .setTemperature(1)
                              .save(consumer, location(folder + "composite/copper_oxidized"));
    // slimeskin
    String slimeskinFolder = folder + "slimeskin/";
    materialComposite(consumer, MaterialIds.leather,   MaterialIds.slimeskin,      TinkerFluids.earthSlime, FluidValues.SLIMEBALL, slimeskinFolder, "earth");
    materialComposite(consumer, MaterialIds.leather,   MaterialIds.skyslimeskin,   TinkerFluids.skySlime,   FluidValues.SLIMEBALL, slimeskinFolder, "sky");
    materialComposite(consumer, MaterialIds.leather,   MaterialIds.ichorskin,      TinkerFluids.ichor,      FluidValues.SLIMEBALL, slimeskinFolder, "ichor");
    materialComposite(consumer, MaterialIds.leather,   MaterialIds.enderslimeskin, TinkerFluids.enderSlime, FluidValues.SLIMEBALL, slimeskinFolder, "ender");
    venomCleaning(consumer, MaterialIds.slimeskin,      slimeskinFolder, "earth_cleaning");
    venomCleaning(consumer, MaterialIds.skyslimeskin,   slimeskinFolder, "sky_cleaning");
    venomCleaning(consumer, MaterialIds.ichorskin,      slimeskinFolder, "ichor_cleaning");
    venomCleaning(consumer, MaterialIds.enderslimeskin, slimeskinFolder, "ender_cleaning");

    // tier 3
    materialMeltingCasting(consumer, MaterialIds.slimesteel,     TinkerFluids.moltenSlimesteel, folder);
    materialMeltingCasting(consumer, MaterialIds.amethystBronze, TinkerFluids.moltenAmethystBronze, folder);
    materialMeltingCasting(consumer, MaterialIds.roseGold,       TinkerFluids.moltenRoseGold, folder);
    materialMeltingCasting(consumer, MaterialIds.pigIron,        TinkerFluids.moltenPigIron, folder);
    materialMeltingCasting(consumer, MaterialIds.cobalt,         TinkerFluids.moltenCobalt, folder);
    materialMeltingCasting(consumer, MaterialIds.steel,          TinkerFluids.moltenSteel, folder);
    materialMeltingCasting(consumer, MaterialIds.obsidian,       TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE, folder);
    // allow rose gold as a bowstring by string composite, means we also get a redundant binding recipe, but thats fine
    materialComposite(consumer,        MaterialIds.string, MaterialIds.roseGold,   TinkerFluids.moltenRoseGold, FluidValues.INGOT, folder);
    materialMeltingComposite(consumer, MaterialIds.wood,   MaterialIds.nahuatl,    TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE, folder);
    materialMeltingComposite(consumer, MaterialIds.string, MaterialIds.darkthread, TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE, folder);
    MaterialMeltingRecipeBuilder.material(MaterialIds.ice, 10, FluidOutput.fromFluid(Fluids.WATER, FluidType.BUCKET_VOLUME * 9))
      .save(consumer, location(folder + "melting/ice"));
    materialComposite(consumer, MaterialIds.blaze, MaterialIds.jadeite, TinkerFluids.moltenEmerald, FluidValues.GEM, folder);

    // tier 4
    materialMeltingCasting(consumer, MaterialIds.queensSlime, TinkerFluids.moltenQueensSlime, false, folder);
    materialMeltingCasting(consumer, MaterialIds.hepatizon,   TinkerFluids.moltenHepatizon,   true,  folder);
    materialMeltingCasting(consumer, MaterialIds.manyullyn,   TinkerFluids.moltenManyullyn,   true,  folder);
    materialComposite(consumer, MaterialIds.necroticBone, MaterialIds.blazingBone, TinkerFluids.blazingBlood, false, FluidConstants.BUCKET / 5, folder);
    materialMeltingComposite(consumer, MaterialIds.leather, MaterialIds.ancientHide, TinkerFluids.moltenDebris, false, FluidValues.INGOT, folder);

    // tier 2 compat
    compatMeltingCasting(consumer, MaterialIds.osmium,   TinkerFluids.moltenOsmium,   folder);
    compatMeltingCasting(consumer, MaterialIds.silver,   TinkerFluids.moltenSilver,   folder);
    compatMeltingCasting(consumer, MaterialIds.lead,     TinkerFluids.moltenLead,     folder);
    compatMeltingCasting(consumer, MaterialIds.aluminum, TinkerFluids.moltenAluminum, folder);
    materialComposite(withCondition(consumer, tagCondition("aluminum_ingots")), MaterialIds.rock, MaterialIds.whitestone, TinkerFluids.moltenAluminum, true, FluidValues.INGOT, folder, "whitestone_from_aluminum");
    materialComposite(withCondition(consumer, tagCondition("tin_ingots")),      MaterialIds.rock, MaterialIds.whitestone, TinkerFluids.moltenTin,      true, FluidValues.INGOT, folder, "whitestone_from_tin");
    materialComposite(withCondition(consumer, tagCondition("zinc_ingots")),     MaterialIds.rock, MaterialIds.whitestone, TinkerFluids.moltenZinc,     true, FluidValues.INGOT, folder, "whitestone_from_zinc");
    // tier 3 compat
    compatMeltingCasting(consumer, MaterialIds.steel,          TinkerFluids.moltenSteel,      folder);
    compatMeltingCasting(consumer, MaterialIds.constantan,     TinkerFluids.moltenConstantan, folder);
    compatMeltingCasting(consumer, MaterialIds.invar,          TinkerFluids.moltenInvar,      folder);
    compatMeltingCasting(consumer, MaterialIds.electrum,       TinkerFluids.moltenElectrum,   folder);
    compatMeltingCasting(consumer, MaterialIds.bronze,         TinkerFluids.moltenBronze,     folder);
    materialMeltingComposite(withCondition(consumer, tagCondition("uranium_ingots")), MaterialIds.necroticBone, MaterialIds.necronium,       TinkerFluids.moltenUranium, true, FluidValues.INGOT, folder);
    materialMeltingComposite(withCondition(consumer, tagCondition("brass_ingots")),   MaterialIds.slimewood,    MaterialIds.platedSlimewood, TinkerFluids.moltenBrass,   true, FluidValues.INGOT, folder);

    // slimesuit - slime
    materialMeltingCasting(consumer, MaterialIds.earthslime, TinkerFluids.earthSlime, FluidValues.SLIMEBALL, folder);
    materialMeltingCasting(consumer, MaterialIds.skyslime,   TinkerFluids.skySlime,   FluidValues.SLIMEBALL, folder);
    materialMeltingCasting(consumer, MaterialIds.ichor,      TinkerFluids.ichor,      FluidValues.SLIMEBALL, folder);
    materialMeltingCasting(consumer, MaterialIds.enderslime, TinkerFluids.enderSlime, FluidValues.SLIMEBALL, folder);
    //materialMeltingCasting(consumer, MaterialIds.venom, TinkerFluids.venom, FluidConstants.BUCKET / 4, folder);
  }
}
