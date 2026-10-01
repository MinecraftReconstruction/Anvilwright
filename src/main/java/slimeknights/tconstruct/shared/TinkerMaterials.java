package slimeknights.tconstruct.shared;

import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import slimeknights.mantle.registration.object.FenceBuildingBlockObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.registration.object.MetalItemObject;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.library.json.predicate.material.MaterialDefinitionPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialHasPartPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialIdPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialStatTypePredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialTierPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialVariantPredicate;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient;
import slimeknights.tconstruct.shared.block.KnightMetalBlock;
import slimeknights.tconstruct.shared.block.OrientableBlock;
import slimeknights.tconstruct.shared.block.SlimesteelBlock;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.data.material.MaterialIds;

import java.util.function.Consumer;

import static slimeknights.tconstruct.TConstruct.getResource;
import slimeknights.tconstruct.library.recipe.ingredient.BlockTagIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.InstrumentIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.NoContainerIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.ToolHookIngredient;

/**
 * Contains bommon blocks and items used in crafting materials
 */
@SuppressWarnings("unused")
public final class TinkerMaterials extends TinkerModule {
  // ores
  public static final MetalItemObject cobalt = BLOCKS.registerMetal("cobalt", metalBuilder(MapColor.COLOR_BLUE), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  public static final MetalItemObject steel = BLOCKS.registerMetal("steel", metalBuilder(MapColor.STONE), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  // tier 3
  public static final MetalItemObject slimesteel     = BLOCKS.registerMetal("slimesteel", () -> new SlimesteelBlock(metalBuilder(MapColor.WARPED_WART_BLOCK).noOcclusion()), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  public static final MetalItemObject amethystBronze = BLOCKS.registerMetal("amethyst_bronze", metalBuilder(MapColor.COLOR_PURPLE), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  public static final MetalItemObject roseGold       = BLOCKS.registerMetal("rose_gold", metalBuilder(MapColor.TERRACOTTA_WHITE), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  public static final MetalItemObject pigIron        = BLOCKS.registerMetal("pig_iron", () -> new OrientableBlock(metalBuilder(MapColor.COLOR_PINK)), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  // tier 4
  public static final MetalItemObject queensSlime = BLOCKS.registerMetal("queens_slime", metalBuilder(MapColor.COLOR_GREEN), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  public static final MetalItemObject cinderslime = BLOCKS.registerMetal("cinderslime", () -> new SlimesteelBlock(metalBuilder(MapColor.COLOR_ORANGE).sound(SoundType.NETHERITE_BLOCK).noOcclusion()), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  public static final MetalItemObject manyullyn   = BLOCKS.registerMetal("manyullyn", metalBuilder(MapColor.COLOR_PURPLE), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  public static final MetalItemObject hepatizon   = BLOCKS.registerMetal("hepatizon", metalBuilder(MapColor.TERRACOTTA_BLUE), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  public static final MetalItemObject knightmetal = BLOCKS.registerMetal("knightmetal", () -> new KnightMetalBlock(metalBuilder(MapColor.GRASS).sound(SoundType.NETHERITE_BLOCK).noOcclusion()), TOOLTIP_BLOCK_ITEM, ITEM_PROPS);
  public static final MetalItemObject soulsteel   = BLOCKS.registerMetal("soulsteel", metalBuilder(MapColor.COLOR_BROWN).noOcclusion(), BLOCK_ITEM, ITEM_PROPS);
  public static final ItemObject<Item> copperNugget = ITEMS.register("copper_nugget", ITEM_PROPS);
  public static final ItemObject<Item> netheriteNugget = ITEMS.register("netherite_nugget", ITEM_PROPS);
  public static final ItemObject<Item> debrisNugget = ITEMS.register("debris_nugget", TOOLTIP_ITEM);
  // tier 5
  public static final MetalItemObject knightslime = BLOCKS.registerMetal("knightslime", metalBuilder(MapColor.COLOR_MAGENTA), BLOCK_ITEM, ITEM_PROPS);

  // non-metal
  public static final ItemObject<Item> necroticBone = ITEMS.register("necrotic_bone", TOOLTIP_ITEM);
  public static final ItemObject<Item> venombone = ITEMS.register("venombone", TOOLTIP_ITEM);
  public static final ItemObject<Item> blazingBone = ITEMS.register("blazing_bone", TOOLTIP_ITEM);
  public static final ItemObject<Item> necroniumBone = ITEMS.register("necronium_bone", TOOLTIP_ITEM);
  public static final FenceBuildingBlockObject nahuatl = BLOCKS.registerFenceBuilding("nahuatl", builder(MapColor.PODZOL, SoundType.WOOD).requiresCorrectToolForDrops().strength(25f, 300f), BLOCK_ITEM);
  public static final FenceBuildingBlockObject blazewood = BLOCKS.registerFenceBuilding("blazewood", woodBuilder(MapColor.TERRACOTTA_RED).requiresCorrectToolForDrops().strength(25f, 300f).lightLevel(s -> 7), BLOCK_ITEM);

  /*
   * Serializers
   */
  void registerSerializers() {
    CustomIngredientSerializer.register(MaterialIngredient.Serializer.INSTANCE);
    // Upstream splits these across TinkerCommons/TinkerMaterials/TinkerTools (one CraftingHelper.register each);
    // Fabric's API is a single registry, so they are all registered here instead.
    CustomIngredientSerializer.register(MaterialValueIngredient.Serializer.INSTANCE);
    CustomIngredientSerializer.register(NoContainerIngredient.Serializer.INSTANCE);
    CustomIngredientSerializer.register(BlockTagIngredient.Serializer.INSTANCE);
    CustomIngredientSerializer.register(InstrumentIngredient.SERIALIZER);
    CustomIngredientSerializer.register(ToolHookIngredient.Serializer.INSTANCE);
  }

  public TinkerMaterials() {
    registerSerializers();
  }
}
