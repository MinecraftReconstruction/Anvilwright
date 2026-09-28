package slimeknights.tconstruct.smeltery.data;

import io.github.fabricators_of_create.porting_lib.data.ConditionalRecipe;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.tags.Tags;
import io.github.fabricators_of_create.porting_lib.util.TrueCondition;
import io.github.tropheusj.milk.Milk;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.recipe.condition.TagFilledCondition;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipeBuilder;
import slimeknights.mantle.recipe.data.ConsumerWrapperBuilder;
import slimeknights.mantle.recipe.data.ICommonRecipeHelper;
import slimeknights.mantle.recipe.data.ItemNameIngredient;
import slimeknights.mantle.recipe.data.ItemNameOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.recipe.ingredient.PotionDisplayIngredient;
import slimeknights.mantle.registration.deferred.PotionDeferredRegister.PotionType;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.data.BaseRecipeProvider;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.common.registration.GeodeItemObject;
import slimeknights.tconstruct.common.registration.GeodeItemObject.BudSize;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.fluids.fluids.PotionFluidType;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.library.data.recipe.ISmelteryRecipeHelper;
import slimeknights.tconstruct.library.data.recipe.SmelteryRecipeBuilder;
import slimeknights.tconstruct.library.data.recipe.SmelteryRecipeBuilder.CommonRecipe;
import slimeknights.tconstruct.library.data.recipe.SmelteryRecipeBuilder.MetalMelting;
import slimeknights.tconstruct.library.data.recipe.SmelteryRecipeBuilder.ToolItemMelting;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipeBuilder;
import slimeknights.tconstruct.library.recipe.casting.ItemCastingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.casting.PotionCastingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.casting.container.ContainerFillingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelBuilder;
import slimeknights.tconstruct.library.recipe.ingredient.BlockTagIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.NoContainerIngredient;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipeBuilder;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerEffects;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.data.material.MaterialIds;
import slimeknights.tconstruct.world.TinkerHeadType;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("removal")
public class SmelteryRecipeProvider extends BaseRecipeProvider implements ISmelteryRecipeHelper, ICommonRecipeHelper {
  public SmelteryRecipeProvider(FabricDataOutput output) {
    super(output);
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Smeltery Recipes";
  }

  @Override
  public void buildRecipes(Consumer<FinishedRecipe> consumer) {
    this.addCraftingRecipes(consumer);
    this.addSmelteryRecipes(consumer);
    this.addFoundryRecipes(consumer);
    this.addTagRecipes(consumer);
    this.addMeltingRecipes(consumer);
    this.addCastingRecipes(consumer);
    this.addAlloyRecipes(consumer);
    this.addEntityMeltingRecipes(consumer);

    this.addCompatRecipes(consumer);
  }

  private void addCraftingRecipes(Consumer<FinishedRecipe> consumer) {
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.copperCan, 3)
                       .define('c', Tags.Items.INGOTS_COPPER)
                       .pattern("c c")
                       .pattern(" c ")
                       .unlockedBy("has_item", has(Tags.Items.INGOTS_COPPER))
                       .save(consumer, prefix(TinkerSmeltery.copperCan.getRegistryName(), "smeltery/"));

    // sand casts
    ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TinkerSmeltery.blankSandCast, 4)
                          .requires(Tags.Items.SAND_COLORLESS)
                          .unlockedBy("has_casting", has(TinkerSmeltery.searedTable))
                          .save(consumer, modResource("smeltery/sand_cast"));
    ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TinkerSmeltery.blankRedSandCast, 4)
                          .requires(Tags.Items.SAND_RED)
                          .unlockedBy("has_casting", has(TinkerSmeltery.searedTable))
                          .save(consumer, location("smeltery/red_sand_cast"));

    // pick up sand casts from the table
    MoldingRecipeBuilder.moldingTable(TinkerSmeltery.blankSandCast)
                        .setMaterial(TinkerTags.Items.SAND_CASTS)
                        .save(consumer, location("smeltery/sand_cast_pickup"));
    MoldingRecipeBuilder.moldingTable(TinkerSmeltery.blankRedSandCast)
                        .setMaterial(TinkerTags.Items.RED_SAND_CASTS)
                        .save(consumer, location("smeltery/red_sand_cast_pickup"));

    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, TinkerSmeltery.endFluidCannon)
      .define('R', Tags.Items.DUSTS_REDSTONE)
      .define('T', Items.SHULKER_SHELL)
      .define('C', TinkerMaterials.knightmetal.getIngotTag())
      .pattern(" R ")
      .pattern("CTC")
      .pattern("CCC")
      .unlockedBy("has_item", has(TinkerMaterials.knightmetal.getIngotTag()))
      .save(consumer, location("smeltery/end_fluid_cannon"));
  }

  private void addSmelteryRecipes(Consumer<FinishedRecipe> consumer) {
    String folder = "smeltery/seared/";
    // grout crafting
    ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TinkerSmeltery.grout, 2)
                          .requires(Items.CLAY_BALL)
                          .requires(ItemTags.SAND)
                          .requires(Blocks.GRAVEL)
                          .unlockedBy("has_item", has(Items.CLAY_BALL))
                          .save(consumer, prefix(TinkerSmeltery.grout.getRegistryName(), folder));
    ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TinkerSmeltery.grout, 8)
                          .requires(Blocks.CLAY)
                          .requires(ItemTags.SAND).requires(ItemTags.SAND).requires(ItemTags.SAND).requires(ItemTags.SAND)
                          .requires(Blocks.GRAVEL).requires(Blocks.GRAVEL).requires(Blocks.GRAVEL).requires(Blocks.GRAVEL)
                          .unlockedBy("has_item", has(Blocks.CLAY))
                          .save(consumer, wrap(TinkerSmeltery.grout.getRegistryName(), folder, "_multiple"));

    // seared bricks from grout
    SimpleCookingRecipeBuilder.smelting(Ingredient.of(TinkerSmeltery.grout), RecipeCategory.MISC, TinkerSmeltery.searedBrick, 0.3f, 200)
                        .unlockedBy("has_item", has(TinkerSmeltery.grout))
                        .save(consumer, prefix(TinkerSmeltery.searedBrick.getRegistryName(), folder));
    Consumer<Consumer<FinishedRecipe>> fastGrout = c ->
      SimpleCookingRecipeBuilder.blasting(Ingredient.of(TinkerSmeltery.grout), RecipeCategory.MISC, TinkerSmeltery.searedBrick, 0.3f, 100)
                          .unlockedBy("has_item", has(TinkerSmeltery.grout)).save(c);

    ConditionalRecipe.builder()
                     .addCondition(DefaultResourceConditions.allModsLoaded("ceramics"))
                     .addRecipe(c -> fastGrout.accept(ConsumerWrapperBuilder.wrap(new ResourceLocation("ceramics", "kiln")).build(c)))
                     .addCondition(TrueCondition.INSTANCE)
                     .addRecipe(fastGrout)
                     .generateAdvancement()
                     .build(consumer, wrap(TinkerSmeltery.searedBrick, folder, "_kiln"));


    // block from bricks
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedBricks)
                       .define('b', TinkerSmeltery.searedBrick)
                       .pattern("bb")
                       .pattern("bb")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, wrap(TinkerSmeltery.searedBricks.getRegistryName(), folder, "_from_brick"));
    // ladder from bricks
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedLadder, 4)
                       .define('b', TinkerSmeltery.searedBrick)
                       .define('B', TinkerTags.Items.SEARED_BRICKS)
                       .pattern("b b")
                       .pattern("b b")
                       .pattern("BBB")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, prefix(TinkerSmeltery.searedLadder.getRegistryName(), folder));

    // cobble -> stone
    SimpleCookingRecipeBuilder.smelting(Ingredient.of(TinkerSmeltery.searedCobble.get()), RecipeCategory.MISC, TinkerSmeltery.searedStone, 0.1f, 200)
                        .unlockedBy("has_item", has(TinkerSmeltery.searedCobble.get()))
                        .save(consumer, wrap(TinkerSmeltery.searedStone.getRegistryName(), folder, "_smelting"));
    // stone -> paver
    SimpleCookingRecipeBuilder.smelting(Ingredient.of(TinkerSmeltery.searedStone.get()), RecipeCategory.MISC, TinkerSmeltery.searedPaver, 0.1f, 200)
                        .unlockedBy("has_item", has(TinkerSmeltery.searedStone.get()))
                        .save(consumer, wrap(TinkerSmeltery.searedPaver.getRegistryName(), folder, "_smelting"));
    // stone -> bricks
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedBricks, 4)
                       .define('b', TinkerSmeltery.searedStone)
                       .pattern("bb")
                       .pattern("bb")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedStone))
                       .save(consumer, wrap(TinkerSmeltery.searedBricks.getRegistryName(), folder, "_crafting"));
    // bricks -> cracked
    SimpleCookingRecipeBuilder.smelting(Ingredient.of(TinkerSmeltery.searedBricks), RecipeCategory.MISC, TinkerSmeltery.searedCrackedBricks, 0.1f, 200)
                        .unlockedBy("has_item", has(TinkerSmeltery.searedBricks))
                        .save(consumer, wrap(TinkerSmeltery.searedCrackedBricks.getRegistryName(), folder, "_smelting"));
    // brick slabs -> fancy
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedFancyBricks)
                       .define('s', TinkerSmeltery.searedBricks.getSlab())
                       .pattern("s")
                       .pattern("s")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBricks.getSlab()))
                       .save(consumer, wrap(TinkerSmeltery.searedFancyBricks.getRegistryName(), folder, "_crafting"));
    // bricks or stone as input
    this.searedStonecutter(consumer, TinkerSmeltery.searedBricks, folder);
    this.searedStonecutter(consumer, TinkerSmeltery.searedFancyBricks, folder);
    this.searedStonecutter(consumer, TinkerSmeltery.searedTriangleBricks, folder);

    // seared glass
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedGlass)
                       .define('b', TinkerSmeltery.searedBrick)
                       .define('G', Tags.Items.GLASS_COLORLESS)
                       .pattern(" b ")
                       .pattern("bGb")
                       .pattern(" b ")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, prefix(TinkerSmeltery.searedGlass.getRegistryName(), folder));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedGlassPane, 16)
                       .define('#', TinkerSmeltery.searedGlass)
                       .pattern("###")
                       .pattern("###")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedGlass))
                       .save(consumer, prefix(TinkerSmeltery.searedGlassPane.getRegistryName(), folder));

    // stairs and slabs
    this.slabStairsCrafting(consumer, TinkerSmeltery.searedStone, folder, true);
    this.stairSlabWallCrafting(consumer, TinkerSmeltery.searedCobble, folder, true);
    this.slabStairsCrafting(consumer, TinkerSmeltery.searedPaver, folder, true);
    this.stairSlabWallCrafting(consumer, TinkerSmeltery.searedBricks, folder, true);

    // tanks
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedTank.get(TankType.FUEL_TANK))
                       .define('#', TinkerSmeltery.searedBrick)
                       .define('B', Tags.Items.GLASS)
                       .pattern("###")
                       .pattern("#B#")
                       .pattern("###")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, modResource(folder + "fuel_tank"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedTank.get(TankType.FUEL_GAUGE))
                       .define('#', TinkerSmeltery.searedBrick)
                       .define('B', Tags.Items.GLASS)
                       .pattern("#B#")
                       .pattern("BBB")
                       .pattern("#B#")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, modResource(folder + "fuel_gauge"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedTank.get(TankType.INGOT_TANK))
                       .define('#', TinkerSmeltery.searedBrick)
                       .define('B', Tags.Items.GLASS)
                       .pattern("#B#")
                       .pattern("#B#")
                       .pattern("#B#")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, modResource(folder + "ingot_tank"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedTank.get(TankType.INGOT_GAUGE))
                       .define('#', TinkerSmeltery.searedBrick)
                       .define('B', Tags.Items.GLASS)
                       .pattern("B#B")
                       .pattern("#B#")
                       .pattern("B#B")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, modResource(folder + "ingot_gauge"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedLantern.get(), 3)
                       .define('C', Tags.Items.INGOTS_IRON)
                       .define('B', TinkerSmeltery.searedBrick)
                       .define('P', TinkerSmeltery.searedGlassPane)
                       .pattern(" C ")
                       .pattern("PPP")
                       .pattern("BBB")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, location(folder + "lantern"));
    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, TinkerSmeltery.searedCastingTank.get())
                       .define('B', TinkerSmeltery.searedBrick)
                       .define('G', Tags.Items.GLASS)
                       .define('C', Tags.Items.INGOTS_COPPER)
                       .pattern("BGB")
                       .pattern("CGC")
                       .pattern("BGB")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, location(folder + "seared_casting_tank"));

    // fluid transfer
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedFaucet.get(), 3)
                       .define('#', TinkerSmeltery.searedBrick)
                       .pattern("# #")
                       .pattern(" # ")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, modResource(folder + "faucet"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedChannel.get(), 5)
                       .define('#', TinkerSmeltery.searedBrick)
                       .pattern("# #")
                       .pattern("###")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, location(folder + "channel"));

    // casting
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedBasin.get())
                       .define('#', TinkerSmeltery.searedBrick)
                       .pattern("# #")
                       .pattern("# #")
                       .pattern("###")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, modResource(folder + "basin"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedTable.get())
                       .define('#', TinkerSmeltery.searedBrick)
                       .pattern("###")
                       .pattern("# #")
                       .pattern("# #")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, location(folder + "table"));

    // peripherals
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedDrain)
                       .define('#', TinkerSmeltery.searedBrick)
                       .define('C', Tags.Items.INGOTS_COPPER)
                       .pattern("# #")
                       .pattern("C C")
                       .pattern("# #")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, location(folder + "drain"));
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedDrain)
                         .define('#', TinkerTags.Items.SMELTERY_BRICKS)
                         .define('C', Tags.Items.INGOTS_COPPER)
                         .pattern("C#C")
                         .unlockedBy("has_item", has(TinkerTags.Items.SMELTERY_BRICKS)))
                                 .setSource(TinkerTags.Items.SMELTERY_BRICKS)
                                 .build(consumer, modResource(folder + "drain_retextured"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedChute)
                       .define('#', TinkerSmeltery.searedBrick)
                       .define('C', Tags.Items.INGOTS_COPPER)
                       .pattern("#C#")
                       .pattern("   ")
                       .pattern("#C#")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, location(folder + "chute"));
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedChute)
                         .define('#', TinkerTags.Items.SMELTERY_BRICKS)
                         .define('C', Tags.Items.INGOTS_COPPER)
                         .pattern("C")
                         .pattern("#")
                         .pattern("C")
                         .unlockedBy("has_item", has(TinkerTags.Items.SMELTERY_BRICKS)))
                                 .setSource(TinkerTags.Items.SMELTERY_BRICKS)
                                 .build(consumer, modResource(folder + "chute_retextured"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedDuct)
                       .define('#', TinkerSmeltery.searedBrick)
                       .define('C', Tags.Items.INGOTS_GOLD)
                       .pattern("# #")
                       .pattern("C C")
                       .pattern("# #")
                       .unlockedBy("has_item", has(Tags.Items.INGOTS_GOLD))
                       .save(consumer, location(folder + "duct"));
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedDuct)
                         .define('#', TinkerTags.Items.SMELTERY_BRICKS)
                         .define('C', TinkerMaterials.cobalt.getIngotTag())
                         .pattern("C#C")
                         .unlockedBy("has_item", has(TinkerTags.Items.SMELTERY_BRICKS)))
                                 .setSource(TinkerTags.Items.SMELTERY_BRICKS)
                                 .build(consumer, modResource(folder + "duct_retextured"));

    // controllers
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedMelter)
                       .define('G', Ingredient.of(TinkerSmeltery.searedTank.get(TankType.FUEL_GAUGE), TinkerSmeltery.searedTank.get(TankType.INGOT_GAUGE)))
                       .define('B', TinkerSmeltery.searedBrick)
                       .pattern("BGB")
                       .pattern("BBB")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, modResource(folder + "melter"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.searedHeater)
                       .define('B', TinkerSmeltery.searedBrick)
                       .pattern("BBB")
                       .pattern("B B")
                       .pattern("BBB")
                       .unlockedBy("has_item", has(TinkerSmeltery.searedBrick))
                       .save(consumer, location(folder + "heater"));
    // fluid cannon
    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, TinkerSmeltery.searedFluidCannon)
      .define('R', Tags.Items.DUSTS_REDSTONE)
      .define('T', similarTanks)
      .define('C', Tags.Items.INGOTS_COPPER)
      .pattern(" R ")
      .pattern("CTC")
      .pattern("CCC")
      .unlockedBy("has_item", has(Tags.Items.INGOTS_COPPER))
      .save(consumer, location(folder + "fluid_cannon"));

    // casting
    String castingFolder = "smeltery/casting/seared/";

    ItemCastingRecipeBuilder.basinRecipe(TinkerSmeltery.searedStone)
                            .setFluidAndTime(TinkerFluids.searedStone, FluidValues.BRICK_BLOCK)
                            .save(consumer, location(castingFolder + "stone/block_from_seared"));
    this.ingotCasting(consumer, TinkerFluids.searedStone, FluidValues.BRICK, TinkerSmeltery.searedBrick, castingFolder + "brick");
    ItemCastingRecipeBuilder.basinRecipe(TinkerSmeltery.searedGlass)
                            .setFluidAndTime(TinkerFluids.searedStone, FluidValues.BRICK_BLOCK)
                            .setCast(Tags.Items.GLASS_COLORLESS, true)
                            .save(consumer, location(castingFolder + "glass"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerSmeltery.searedLamp)
      .setFluidAndTime(TinkerFluids.searedStone, FluidValues.BRICK_BLOCK)
      .setCast(Blocks.GLOWSTONE, true)
      .save(consumer, location(castingFolder + "lamp"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerSmeltery.searedSoulGlass)
                            .setFluidAndTime(TinkerFluids.searedStone, FluidValues.BRICK_BLOCK)
                            .setCast(TinkerCommons.soulGlass, true)
                            .save(consumer, location(castingFolder + "glass_soul"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerSmeltery.searedTintedGlass)
                            .setFluidAndTime(TinkerFluids.searedStone, FluidValues.BRICK_BLOCK)
                            .setCast(Tags.Items.GLASS_TINTED, true)
                            .save(consumer, location(castingFolder + "glass_tinted"));
    // discount for casting panes
    ItemCastingRecipeBuilder.tableRecipe(TinkerSmeltery.searedGlassPane)
                            .setFluidAndTime(TinkerFluids.searedStone, FluidValues.BRICK)
                            .setCast(Tags.Items.GLASS_PANES_COLORLESS, true)
                            .save(consumer, location(castingFolder + "glass_pane"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerSmeltery.searedSoulGlassPane)
                            .setFluidAndTime(TinkerFluids.searedStone, FluidValues.BRICK)
                            .setCast(TinkerCommons.soulGlassPane, true)
                            .save(consumer, location(castingFolder + "glass_pane_soul"));

    // smeltery controller
    ItemCastingRecipeBuilder.retexturedBasinRecipe(ItemOutput.fromItem(TinkerSmeltery.smelteryController))
                            .setCast(TinkerTags.Items.SMELTERY_BRICKS, true)
                            .setFluidAndTime(TinkerFluids.moltenCopper, true, FluidValues.INGOT * 4)
                            .save(consumer, prefix(TinkerSmeltery.smelteryController.getRegistryName(), castingFolder));

    // craft seared stone from clay and stone
    // button is the closest we have to a single stone brick, just go with it, better than not having the recipe
    ItemCastingRecipeBuilder.tableRecipe(TinkerSmeltery.searedBrick)
                            .setFluidAndTime(TinkerFluids.moltenClay, FluidValues.BRICK / 2)
                            .setCast(Items.FLINT, true) // if gravel works, flint makes sense to use
                            .save(consumer, location(castingFolder + "brick_composite"));
    // cobble
    searedCasting(consumer, TinkerSmeltery.searedCobble, DefaultCustomIngredients.any(Ingredient.of(Tags.Items.COBBLESTONE), Ingredient.of(Blocks.GRAVEL)), castingFolder + "cobble/block");
    searedSlabCasting(consumer, TinkerSmeltery.searedCobble.getSlab(), Ingredient.of(Blocks.COBBLESTONE_SLAB), castingFolder + "cobble/slab");
    searedCasting(consumer, TinkerSmeltery.searedCobble.getStairs(), Ingredient.of(Blocks.COBBLESTONE_STAIRS), castingFolder + "cobble/stairs");
    searedCasting(consumer, TinkerSmeltery.searedCobble.getWall(), Ingredient.of(Blocks.COBBLESTONE_WALL), castingFolder + "cobble/wall");
    // stone
    searedCasting(consumer, TinkerSmeltery.searedStone, Ingredient.of(Tags.Items.STONE), castingFolder + "stone/block_from_clay");
    searedSlabCasting(consumer, TinkerSmeltery.searedStone.getSlab(), Ingredient.of(Blocks.STONE_SLAB), castingFolder + "stone/slab");
    searedCasting(consumer, TinkerSmeltery.searedStone.getStairs(), Ingredient.of(Blocks.STONE_STAIRS), castingFolder + "stone/stairs");
    // stone bricks
    searedCasting(consumer, TinkerSmeltery.searedBricks, Ingredient.of(Blocks.STONE_BRICKS), castingFolder + "bricks/block");
    searedSlabCasting(consumer, TinkerSmeltery.searedBricks.getSlab(), Ingredient.of(Blocks.STONE_BRICK_SLAB), castingFolder + "bricks/slab");
    searedCasting(consumer, TinkerSmeltery.searedBricks.getStairs(), Ingredient.of(Blocks.STONE_BRICK_STAIRS), castingFolder + "bricks/stairs");
    searedCasting(consumer, TinkerSmeltery.searedBricks.getWall(), Ingredient.of(Blocks.STONE_BRICK_WALL), castingFolder + "bricks/wall");
    // other seared
    searedCasting(consumer, TinkerSmeltery.searedCrackedBricks, Ingredient.of(Blocks.CRACKED_STONE_BRICKS), castingFolder + "cracked");
    searedCasting(consumer, TinkerSmeltery.searedFancyBricks, Ingredient.of(Blocks.CHISELED_STONE_BRICKS), castingFolder + "chiseled");
    searedCasting(consumer, TinkerSmeltery.searedPaver, Ingredient.of(Blocks.SMOOTH_STONE), castingFolder + "paver");

    // seared blocks
    String meltingFolder = "smeltery/melting/seared/";

    // double efficiency when using smeltery for grout
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.grout), TinkerFluids.searedStone, FluidValues.BRICK * 2, 1.5f)
                        .save(consumer, location(meltingFolder + "grout"));
    // seared stone
    // stairs are here since the cheapest stair recipe is stone cutter, 1 to 1
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.any(Ingredient.of(TinkerTags.Items.SEARED_BLOCKS),
                                                       Ingredient.of(TinkerSmeltery.searedLadder, TinkerSmeltery.searedCobble.getWall(), TinkerSmeltery.searedBricks.getWall(),
                                                                     TinkerSmeltery.searedCobble.getStairs(), TinkerSmeltery.searedStone.getStairs(), TinkerSmeltery.searedBricks.getStairs(), TinkerSmeltery.searedPaver.getStairs())),
																 TinkerFluids.searedStone.get(), FluidValues.BRICK_BLOCK, 2.0f)
                        .save(consumer, modResource(meltingFolder + "block"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedCobble.getSlab(), TinkerSmeltery.searedStone.getSlab(), TinkerSmeltery.searedBricks.getSlab(), TinkerSmeltery.searedPaver.getSlab()),
																 TinkerFluids.searedStone, FluidValues.BRICK_BLOCK / 2, 1.5f)
                        .save(consumer, location(meltingFolder + "slab"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedBrick), TinkerFluids.searedStone, FluidValues.BRICK, 1.0f)
                        .save(consumer, location(meltingFolder + "brick"));

    // melt down smeltery components
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedFaucet, TinkerSmeltery.searedChannel), TinkerFluids.searedStone, FluidValues.BRICK, 1.5f)
                        .save(consumer, location(meltingFolder + "faucet"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedBasin, TinkerSmeltery.searedTable), TinkerFluids.searedStone, FluidValues.BRICK * 7, 2.5f)
                        .save(consumer, location(meltingFolder + "casting"));
    // tanks
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.searedTank.get(TankType.FUEL_TANK)), true), TinkerFluids.searedStone.get(), FluidValues.BRICK * 8, 3f)
                        .addByproduct(new FluidStack(TinkerFluids.moltenGlass.get(), FluidValues.GLASS_BLOCK))
                        .save(consumer, modResource(meltingFolder + "fuel_tank"));
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.searedTank.get(TankType.INGOT_TANK)), true), TinkerFluids.searedStone.get(), FluidValues.BRICK * 6, 2.5f)
                        .addByproduct(new FluidStack(TinkerFluids.moltenGlass.get(), FluidValues.GLASS_BLOCK * 3))
                        .save(consumer, modResource(meltingFolder + "ingot_tank"));
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.any(DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.searedTank.get(TankType.FUEL_GAUGE)), true),
                                                       DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.searedTank.get(TankType.INGOT_GAUGE)), true)),
                                 TinkerFluids.searedStone.get(), FluidValues.BRICK * 4, 2f)
                        .addByproduct(new FluidStack(TinkerFluids.moltenGlass.get(), FluidValues.GLASS_BLOCK * 5))
                        .save(consumer, modResource(meltingFolder + "gauge"));
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.searedLantern), true), TinkerFluids.searedStone.get(), FluidValues.BRICK * 2, 1.0f)
                        .addByproduct(new FluidStack(TinkerFluids.moltenGlass.get(), FluidValues.GLASS_PANE))
                        .addByproduct(new FluidStack(TinkerFluids.moltenIron.get(), FluidValues.INGOT / 3))
                        .save(consumer, modResource(meltingFolder + "lantern"));
    // glass
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedGlass), TinkerFluids.searedStone, FluidValues.BRICK * 4, 2f)
                        .addByproduct(TinkerFluids.moltenGlass.result( FluidValues.GLASS_BLOCK))
                        .save(consumer, location(meltingFolder + "glass"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedSoulGlass), TinkerFluids.searedStone, FluidValues.BRICK * 4, 2f)
                        .addByproduct(TinkerFluids.liquidSoul.result( FluidValues.GLASS_BLOCK))
                        .save(consumer, location(meltingFolder + "glass_soul"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedTintedGlass), TinkerFluids.searedStone, FluidValues.BRICK * 4, 2f)
                        .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_BLOCK))
                        .addByproduct(TinkerFluids.moltenAmethyst.result(FluidValues.GEM * 2))
                        .save(consumer, location(meltingFolder + "glass_tinted"));
    // panes
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedGlassPane), TinkerFluids.searedStone, FluidValues.BRICK, 1.0f)
                        .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_PANE))
                        .save(consumer, location(meltingFolder + "pane"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedSoulGlassPane), TinkerFluids.searedStone, FluidValues.BRICK, 1.0f)
                        .addByproduct(TinkerFluids.liquidSoul.result(FluidValues.GLASS_PANE))
                        .save(consumer, location(meltingFolder + "pane_soul"));
    // controllers
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedMelter), TinkerFluids.searedStone, FluidValues.BRICK * 9, 3.5f)
                        .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_PANE * 5))
                        .save(consumer, location(meltingFolder + "melter"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedHeater), TinkerFluids.searedStone, FluidValues.BRICK * 8, 3f)
                        .save(consumer, location(meltingFolder + "heater"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedFluidCannon), TinkerFluids.moltenCopper, FluidValues.INGOT * 5, 2.5f)
                        .addByproduct(TinkerFluids.searedStone.result(FluidValues.BRICK * 4))
                        .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_PANE * 5))
                        .save(consumer, location(meltingFolder + "fluid_cannon"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.smelteryController), TinkerFluids.moltenCopper, FluidValues.INGOT * 4, 3.5f)
                        .addByproduct(TinkerFluids.searedStone.result(FluidValues.BRICK * 4))
                        .save(consumer, location("smeltery/melting/metal/copper/smeltery_controller"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.copperGauge), TinkerFluids.moltenCopper, FluidValues.INGOT, 1f)
      .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_PANE / 5))
      .save(consumer, location("smeltery/melting/metal/copper/gauge"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedDrain, TinkerSmeltery.searedChute), TinkerFluids.moltenCopper, FluidValues.INGOT * 2, 2.5f)
                        .addByproduct(TinkerFluids.searedStone.result(FluidValues.BRICK * 4))
                        .save(consumer, location("smeltery/melting/metal/copper/smeltery_io"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.searedDuct), TinkerFluids.moltenGold, FluidValues.INGOT * 2, 2.5f)
                        .addByproduct(TinkerFluids.searedStone.result(FluidValues.BRICK * 4))
                        .save(consumer, location("smeltery/melting/metal/cobalt/seared_duct"));
  }

  private void addFoundryRecipes(Consumer<FinishedRecipe> consumer) {
    String folder = "smeltery/scorched/";
    // grout crafting
    ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TinkerSmeltery.netherGrout, 2)
                          .requires(Items.MAGMA_CREAM)
                          .requires(soulSand)
                          .requires(Blocks.GRAVEL)
                          .unlockedBy("has_item", has(Items.MAGMA_CREAM))
                          .save(consumer, prefix(TinkerSmeltery.netherGrout.getRegistryName(), folder));

    // scorched bricks from grout
    SimpleCookingRecipeBuilder.smelting(Ingredient.of(TinkerSmeltery.netherGrout), RecipeCategory.MISC, TinkerSmeltery.scorchedBrick, 0.3f, 200)
                              .unlockedBy("has_item", has(TinkerSmeltery.netherGrout))
                              .save(consumer, prefix(TinkerSmeltery.scorchedBrick.getRegistryName(), folder));
    Consumer<Consumer<FinishedRecipe>> fastGrout = c ->
      SimpleCookingRecipeBuilder.blasting(Ingredient.of(TinkerSmeltery.netherGrout), RecipeCategory.MISC, TinkerSmeltery.scorchedBrick, 0.3f, 100)
                                .unlockedBy("has_item", has(TinkerSmeltery.netherGrout)).save(c);

    ConditionalRecipe.builder()
                     .addCondition(DefaultResourceConditions.allModsLoaded("ceramics"))
                     .addRecipe(c -> fastGrout.accept(ConsumerWrapperBuilder.wrap(new ResourceLocation("ceramics", "kiln")).build(c)))
                     .addCondition(TrueCondition.INSTANCE)
                     .addRecipe(fastGrout)
                     .generateAdvancement()
                     .build(consumer, wrap(TinkerSmeltery.scorchedBrick, folder, "_kiln"));

    // block from bricks
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedBricks)
                       .define('b', TinkerSmeltery.scorchedBrick)
                       .pattern("bb")
                       .pattern("bb")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, wrap(TinkerSmeltery.scorchedBricks.getRegistryName(), folder, "_from_brick"));
    // ladder from bricks
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedLadder, 4)
                       .define('b', TinkerSmeltery.scorchedBrick)
                       .define('B', TinkerTags.Items.SCORCHED_BLOCKS)
                       .pattern("b b")
                       .pattern("b b")
                       .pattern("BBB")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, prefix(TinkerSmeltery.scorchedLadder.getRegistryName(), folder));

    // stone -> polished
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.polishedScorchedStone, 4)
                       .define('b', TinkerSmeltery.scorchedStone)
                       .pattern("bb")
                       .pattern("bb")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedStone))
                       .save(consumer, wrap(TinkerSmeltery.polishedScorchedStone.getRegistryName(), folder, "_crafting"));
    // polished -> bricks
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedBricks, 4)
                       .define('b', TinkerSmeltery.polishedScorchedStone)
                       .pattern("bb")
                       .pattern("bb")
                       .unlockedBy("has_item", has(TinkerSmeltery.polishedScorchedStone))
                       .save(consumer, wrap(TinkerSmeltery.scorchedBricks.getRegistryName(), folder, "_crafting"));
    // stone -> road
    SimpleCookingRecipeBuilder.smelting(Ingredient.of(TinkerSmeltery.scorchedStone), RecipeCategory.MISC, TinkerSmeltery.scorchedRoad, 0.1f, 200)
                        .unlockedBy("has_item", has(TinkerSmeltery.scorchedStone))
                        .save(consumer, wrap(TinkerSmeltery.scorchedRoad.getRegistryName(), folder, "_smelting"));
    // brick slabs -> chiseled
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.chiseledScorchedBricks)
                       .define('s', TinkerSmeltery.scorchedBricks.getSlab())
                       .pattern("s")
                       .pattern("s")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBricks.getSlab()))
                       .save(consumer, wrap(TinkerSmeltery.chiseledScorchedBricks.getRegistryName(), folder, "_crafting"));
    // stonecutting
    this.scorchedStonecutter(consumer, TinkerSmeltery.polishedScorchedStone, folder);
    this.scorchedStonecutter(consumer, TinkerSmeltery.scorchedBricks, folder);
    this.scorchedStonecutter(consumer, TinkerSmeltery.chiseledScorchedBricks, folder);

    // scorched glass
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedGlass)
                       .define('b', TinkerSmeltery.scorchedBrick)
                       .define('G', Tags.Items.GEMS_QUARTZ)
                       .pattern(" b ")
                       .pattern("bGb")
                       .pattern(" b ")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, prefix(TinkerSmeltery.scorchedGlass.getRegistryName(), folder));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedGlassPane, 16)
                       .define('#', TinkerSmeltery.scorchedGlass)
                       .pattern("###")
                       .pattern("###")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedGlass))
                       .save(consumer, prefix(TinkerSmeltery.scorchedGlassPane.getRegistryName(), folder));

    // stairs, slabs, and fences
    this.slabStairsCrafting(consumer, TinkerSmeltery.scorchedBricks, folder, true);
    this.slabStairsCrafting(consumer, TinkerSmeltery.scorchedRoad, folder, true);
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedBricks.getFence(), 6)
                       .define('B', TinkerSmeltery.scorchedBricks)
                       .define('b', TinkerSmeltery.scorchedBrick)
                       .pattern("BbB")
                       .pattern("BbB")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBricks))
                       .save(consumer, prefix(BuiltInRegistries.BLOCK.getKey(TinkerSmeltery.scorchedBricks.getFence()), folder));

    // tanks
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedTank.get(TankType.FUEL_TANK))
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .define('B', Tags.Items.GEMS_QUARTZ)
                       .pattern("###")
                       .pattern("#B#")
                       .pattern("###")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, modResource(folder + "fuel_tank"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedTank.get(TankType.FUEL_GAUGE))
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .define('B', Tags.Items.GEMS_QUARTZ)
                       .pattern("#B#")
                       .pattern("BBB")
                       .pattern("#B#")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, modResource(folder + "fuel_gauge"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedTank.get(TankType.INGOT_TANK))
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .define('B', Tags.Items.GEMS_QUARTZ)
                       .pattern("#B#")
                       .pattern("#B#")
                       .pattern("#B#")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, modResource(folder + "ingot_tank"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedTank.get(TankType.INGOT_GAUGE))
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .define('B', Tags.Items.GEMS_QUARTZ)
                       .pattern("B#B")
                       .pattern("#B#")
                       .pattern("B#B")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, modResource(folder + "ingot_gauge"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedLantern.get(), 3)
                       .define('C', Tags.Items.INGOTS_IRON)
                       .define('B', TinkerSmeltery.scorchedBrick)
                       .define('P', TinkerSmeltery.scorchedGlassPane)
                       .pattern(" C ")
                       .pattern("PPP")
                       .pattern("BBB")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, location(folder + "lantern"));

    // fluid transfer
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedFaucet.get(), 3)
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .pattern("# #")
                       .pattern(" # ")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, modResource(folder + "faucet"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedChannel.get(), 5)
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .pattern("# #")
                       .pattern("###")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, location(folder + "channel"));

    // casting
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedBasin.get())
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .pattern("# #")
                       .pattern("# #")
                       .pattern("###")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, modResource(folder + "basin"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedTable.get())
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .pattern("###")
                       .pattern("# #")
                       .pattern("# #")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, modResource(folder + "table"));


    // peripherals
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedDrain)
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .define('C', TinkerCommons.obsidianPane)
                       .pattern("# #")
                       .pattern("C C")
                       .pattern("# #")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, location(folder + "drain"));
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedDrain)
                         .define('#', TinkerTags.Items.FOUNDRY_BRICKS)
                         .define('C', TinkerCommons.obsidianPane)
                         .pattern("C#C")
                         .unlockedBy("has_item", has(TinkerTags.Items.FOUNDRY_BRICKS)))
                                 .setSource(TinkerTags.Items.FOUNDRY_BRICKS)
                                 .build(consumer, modResource(folder + "drain_retextured"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedChute)
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .define('C', TinkerCommons.obsidianPane)
                       .pattern("#C#")
                       .pattern("   ")
                       .pattern("#C#")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, location(folder + "chute"));
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedChute)
                         .define('#', TinkerTags.Items.FOUNDRY_BRICKS)
                         .define('C', TinkerCommons.obsidianPane)
                         .pattern("C")
                         .pattern("#")
                         .pattern("C")
                         .unlockedBy("has_item", has(TinkerTags.Items.FOUNDRY_BRICKS)))
                                 .setSource(TinkerTags.Items.FOUNDRY_BRICKS)
                                 .build(consumer, modResource(folder + "chute_retextured"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedDuct)
                       .define('#', TinkerSmeltery.scorchedBrick)
                       .define('C', Tags.Items.INGOTS_GOLD)
                       .pattern("# #")
                       .pattern("C C")
                       .pattern("# #")
                       .unlockedBy("has_item", has(Tags.Items.INGOTS_GOLD))
                       .save(consumer, location(folder + "duct"));
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedDuct)
                         .define('#', TinkerTags.Items.FOUNDRY_BRICKS)
                         .define('C', TinkerMaterials.cobalt.getIngotTag())
                         .pattern("C#C")
                         .unlockedBy("has_item", has(TinkerTags.Items.FOUNDRY_BRICKS)))
                                 .setSource(TinkerTags.Items.FOUNDRY_BRICKS)
                                 .build(consumer, modResource(folder + "duct_retextured"));

    // controllers
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerSmeltery.scorchedAlloyer)
                       .define('G', Ingredient.of(TinkerSmeltery.scorchedTank.get(TankType.INGOT_GAUGE), TinkerSmeltery.scorchedTank.get(TankType.FUEL_GAUGE)))
                       .define('B', TinkerSmeltery.scorchedBrick)
                       .pattern("BGB")
                       .pattern("BBB")
                       .unlockedBy("has_item", has(TinkerSmeltery.scorchedBrick))
                       .save(consumer, location(folder + "alloyer"));
    // fluid cannon
    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, TinkerSmeltery.scorchedFluidCannon)
      .define('R', Tags.Items.DUSTS_REDSTONE)
      .define('T', similarTanks)
      .define('C', TinkerMaterials.cobalt.getIngotTag())
      .pattern(" R ")
      .pattern("CTC")
      .pattern("CCC")
      .unlockedBy("has_item", has(TinkerMaterials.cobalt.getIngotTag()))
      .save(consumer, location(folder + "fluid_cannon"));

    // casting
    String castingFolder = "smeltery/casting/scorched/";
    ItemCastingRecipeBuilder.basinRecipe(TinkerSmeltery.scorchedStone)
                            .setFluidAndTime(TinkerFluids.scorchedStone, FluidValues.BRICK_BLOCK)
                            .save(consumer, location(castingFolder + "stone_from_scorched"));
    this.ingotCasting(consumer, TinkerFluids.scorchedStone, FluidValues.BRICK, TinkerSmeltery.scorchedBrick, castingFolder + "brick");
    ItemCastingRecipeBuilder.basinRecipe(TinkerSmeltery.scorchedGlass)
                            .setFluidAndTime(TinkerFluids.moltenQuartz, FluidValues.GEM)
                            .setCast(TinkerSmeltery.scorchedBricks, true)
                            .save(consumer, location(castingFolder + "glass"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerSmeltery.scorchedLamp)
      .setFluidAndTime(TinkerFluids.scorchedStone, FluidValues.BRICK_BLOCK)
      .setCast(Blocks.GLOWSTONE, true)
      .save(consumer, location(castingFolder + "lamp"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerSmeltery.scorchedSoulGlass)
                            .setFluidAndTime(TinkerFluids.scorchedStone, FluidValues.BRICK_BLOCK)
                            .setCast(TinkerCommons.soulGlass, true)
                            .save(consumer, location(castingFolder + "glass_soul"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerSmeltery.scorchedTintedGlass)
                            .setFluidAndTime(TinkerFluids.scorchedStone, FluidValues.BRICK_BLOCK)
                            .setCast(Tags.Items.GLASS_TINTED, true)
                            .save(consumer, location(castingFolder + "glass_tinted"));
    // discount for casting panes
    ItemCastingRecipeBuilder.tableRecipe(TinkerSmeltery.scorchedGlassPane)
                            .setFluidAndTime(TinkerFluids.moltenQuartz, FluidValues.GEM_SHARD)
                            .setCast(TinkerSmeltery.scorchedBrick, true)
                            .save(consumer, location(castingFolder + "glass_pane"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerSmeltery.scorchedSoulGlassPane)
                            .setFluidAndTime(TinkerFluids.scorchedStone, FluidValues.BRICK)
                            .setCast(TinkerCommons.soulGlassPane, true)
                            .save(consumer, location(castingFolder + "glass_pane_soul"));
    // craft scorched stone from magma and basalt
    // flint is almost a brick
    ItemCastingRecipeBuilder.tableRecipe(TinkerSmeltery.scorchedBrick)
                            .setFluidAndTime(TinkerFluids.magma, FluidValues.SLIMEBALL / 2)
                            .setCast(Items.FLINT, true)
                            .save(consumer, location(castingFolder + "brick_composite"));
    scorchedCasting(consumer, TinkerSmeltery.scorchedStone, Ingredient.of(Blocks.BASALT , Blocks.GRAVEL), castingFolder + "stone_from_magma");
    scorchedCasting(consumer, TinkerSmeltery.polishedScorchedStone, Ingredient.of(Blocks.POLISHED_BASALT), castingFolder + "polished_from_magma");
    // foundry controller
    ItemCastingRecipeBuilder.retexturedBasinRecipe(ItemOutput.fromItem(TinkerSmeltery.foundryController))
                            .setCast(TinkerTags.Items.FOUNDRY_BRICKS, true)
                            .setFluidAndTime(TinkerFluids.moltenObsidian, false, FluidValues.GLASS_BLOCK)
                            .save(consumer, prefix(TinkerSmeltery.foundryController.getRegistryName(), castingFolder));


    // melting
    String meltingFolder = "smeltery/melting/scorched/";

    // double efficiency when using smeltery for grout
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.netherGrout), TinkerFluids.scorchedStone, FluidValues.BRICK * 2, 1.5f)
                        .save(consumer, location(meltingFolder + "grout"));

    // scorched stone
    // stairs are here since the cheapest stair recipe is stone cutter, 1 to 1
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.any(Ingredient.of(TinkerTags.Items.SCORCHED_BLOCKS),
                                                       Ingredient.of(TinkerSmeltery.scorchedLadder, TinkerSmeltery.scorchedBricks.getStairs(), TinkerSmeltery.scorchedRoad.getStairs())),
																 TinkerFluids.scorchedStone.get(), FluidValues.BRICK_BLOCK, 2.0f)
                        .save(consumer, modResource(meltingFolder + "block"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedBricks.getSlab(), TinkerSmeltery.scorchedBricks.getSlab(), TinkerSmeltery.scorchedRoad.getSlab()),
																 TinkerFluids.scorchedStone, FluidValues.BRICK_BLOCK / 2, 1.5f)
                        .save(consumer, location(meltingFolder + "slab"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedBrick), TinkerFluids.scorchedStone, FluidValues.BRICK, 1.0f)
                        .save(consumer, location(meltingFolder + "brick"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedBricks.getFence()), TinkerFluids.scorchedStone, FluidValues.BRICK * 3, 1.0f)
                        .save(consumer, location(meltingFolder + "fence"));

    // melt down foundry components
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedFaucet, TinkerSmeltery.scorchedChannel), TinkerFluids.scorchedStone, FluidValues.BRICK, 1.5f)
                        .save(consumer, location(meltingFolder + "faucet"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedBasin), TinkerFluids.scorchedStone, FluidValues.BRICK * 7, 2.5f)
      .addByproduct(TinkerFluids.moltenGold.result(FluidValues.NUGGET * 10))
      .save(consumer, location(meltingFolder + "casting_basin"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedTable), TinkerFluids.scorchedStone, FluidValues.BRICK * 7, 2.5f)
      .addByproduct(TinkerFluids.moltenGold.result(FluidValues.NUGGET * 4))
      .save(consumer, location(meltingFolder + "casting_table"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedProxyTank), TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE * 2, 2.5f)
      .addByproduct(TinkerFluids.scorchedStone.result(FluidValues.BRICK * 4))
      .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM * 3))
      .save(consumer, location(meltingFolder + "proxy_tank"));

    // tanks
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.scorchedTank.get(TankType.FUEL_TANK)), true), TinkerFluids.scorchedStone.get(), FluidValues.BRICK * 8, 3f)
                        .addByproduct(new FluidStack(TinkerFluids.moltenQuartz.get(), FluidValues.GEM))
                        .save(consumer, modResource(meltingFolder + "fuel_tank"));
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.scorchedTank.get(TankType.INGOT_TANK)), true), TinkerFluids.scorchedStone.get(), FluidValues.BRICK * 6, 2.5f)
                        .addByproduct(new FluidStack(TinkerFluids.moltenQuartz.get(), FluidValues.GEM * 3))
                        .save(consumer, modResource(meltingFolder + "ingot_tank"));
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.any(DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.scorchedTank.get(TankType.FUEL_GAUGE)), true),
                                                       DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.scorchedTank.get(TankType.INGOT_GAUGE)), true)),
                                 TinkerFluids.scorchedStone.get(), FluidValues.BRICK * 4, 2f)
                        .addByproduct(new FluidStack(TinkerFluids.moltenQuartz.get(), FluidValues.GEM * 5))
                        .save(consumer, modResource(meltingFolder + "gauge"));
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.scorchedLantern), true), TinkerFluids.scorchedStone.get(), FluidValues.BRICK * 2, 1.0f)
                        .addByproduct(new FluidStack(TinkerFluids.moltenQuartz.get(), FluidValues.GEM_SHARD))
                        .addByproduct(new FluidStack(TinkerFluids.moltenIron.get(), FluidValues.NUGGET * 3))
                        .save(consumer, modResource(meltingFolder + "lantern"));
    // glass
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedGlass), TinkerFluids.scorchedStone, FluidValues.BRICK * 4, 2f)
                        .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM))
                        .save(consumer, location(meltingFolder + "glass"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedSoulGlass), TinkerFluids.scorchedStone, FluidValues.BRICK * 4, 2f)
                        .addByproduct(TinkerFluids.liquidSoul.result(FluidValues.GLASS_BLOCK))
                        .save(consumer, location(meltingFolder + "glass_soul"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedTintedGlass), TinkerFluids.scorchedStone, FluidValues.BRICK * 4, 2f)
                        .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_BLOCK))
                        .addByproduct(TinkerFluids.moltenAmethyst.result(FluidValues.GEM * 2))
                        .save(consumer, location(meltingFolder + "glass_tinted"));
    // panes
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedGlassPane), TinkerFluids.scorchedStone, FluidValues.BRICK, 1.0f)
                        .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM_SHARD))
                        .save(consumer, location(meltingFolder + "pane"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedSoulGlassPane), TinkerFluids.scorchedStone, FluidValues.BRICK, 1.0f)
                        .addByproduct(TinkerFluids.liquidSoul.result(FluidValues.GLASS_PANE))
                        .save(consumer, location(meltingFolder + "pane_soul"));
    // controllers
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedAlloyer), TinkerFluids.scorchedStone, FluidValues.BRICK * 9, 3.5f)
                        .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM * 5))
                        .save(consumer, location(meltingFolder + "melter"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedFluidCannon), TinkerFluids.moltenCobalt, FluidValues.INGOT * 5, 3.5f)
                        .addByproduct(TinkerFluids.scorchedStone.result(FluidValues.BRICK * 4))
                        .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM * 5))
                        .save(consumer, location(meltingFolder + "fluid_cannon"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.foundryController), TinkerFluids.moltenObsidian, FluidValues.GLASS_BLOCK, 3.5f)
                        .addByproduct(TinkerFluids.scorchedStone.result(FluidValues.BRICK * 4))
                        .save(consumer, location("smeltery/melting/obsidian/foundry_controller"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedDrain, TinkerSmeltery.scorchedChute), TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE * 2, 2.5f)
                        .addByproduct(TinkerFluids.scorchedStone.result(FluidValues.BRICK * 4))
                        .save(consumer, location("smeltery/melting/obsidian/foundry_io"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.obsidianGauge), TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE, 2.5f)
      .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_PANE / 5))
      .save(consumer, location("smeltery/melting/obsidian/gauge"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.scorchedDuct), TinkerFluids.moltenGold, FluidValues.INGOT * 2, 2.5f)
                        .addByproduct(TinkerFluids.scorchedStone.result(FluidValues.BRICK * 4))
                        .save(consumer, location("smeltery/melting/metal/cobalt/scorched_duct"));
  }

  private void addCastingRecipes(Consumer<FinishedRecipe> consumer) {
    // Pure Fluid Recipes
    String folder = "smeltery/casting/";

    // container filling
    ContainerFillingRecipeBuilder.tableRecipe(Items.BUCKET, FluidConstants.BUCKET)
                                 .save(consumer, modResource(folder + "filling/bucket"));
    ContainerFillingRecipeBuilder.tableRecipe(TinkerSmeltery.copperCan, FluidValues.INGOT)
                                 .save(consumer, location(folder + "filling/copper_can"));
    // potion filling
    FluidIngredient potionBottle = TinkerFluids.potion.ingredient(FluidValues.BOTTLE);
    PotionCastingRecipeBuilder.tableRecipe(Items.POTION)
                              .setBottle(Items.GLASS_BOTTLE)
                              .setFluid(potionBottle)
                              .save(consumer, location(folder + "filling/bottle"));
    PotionCastingRecipeBuilder.tableRecipe(Items.SPLASH_POTION)
                              .setBottle(MantleTags.Items.SPLASH_BOTTLE)
                              .setFluid(potionBottle)
                              .save(consumer, location(folder + "filling/lingering_bottle"));
    PotionCastingRecipeBuilder.tableRecipe(Items.LINGERING_POTION)
                              .setBottle(MantleTags.Items.LINGERING_BOTTLE)
                              .setFluid(potionBottle)
                              .save(consumer, location(folder + "filling/splash_bottle"));
    PotionCastingRecipeBuilder.tableRecipe(Items.TIPPED_ARROW)
                              .setBottle(Items.ARROW)
                              .setFluid(TinkerFluids.potion.ingredient(FluidValues.BOTTLE / 5))
                              .setCoolingTime(20)
                              .save(consumer, location(folder + "filling/tipped_arrow"));
    ItemCastingRecipeBuilder.tableRecipe(Items.ARROW)
      .setCast(PotionDisplayIngredient.of(Items.TIPPED_ARROW), true)
      .setFluid(MantleTags.Fluids.WATER, FluidValues.BOTTLE / 5)
      .setCoolingTime(1)
      .save(consumer, location(folder + "filling/tipped_arrow_clean"));
    // tank filling - seared
    ContainerFillingRecipeBuilder.basinRecipe(TinkerSmeltery.searedTank.get(TankType.INGOT_TANK), FluidValues.INGOT)
                                 .save(consumer, location(folder + "filling/seared_ingot_tank"));
    ContainerFillingRecipeBuilder.basinRecipe(TinkerSmeltery.searedTank.get(TankType.INGOT_GAUGE), FluidValues.INGOT)
                                 .save(consumer, modResource(folder + "filling/seared_ingot_gauge"));
    ContainerFillingRecipeBuilder.basinRecipe(TinkerSmeltery.searedTank.get(TankType.FUEL_TANK), FluidConstants.BUCKET / 4)
                                 .save(consumer, modResource(folder + "filling/seared_fuel_tank"));
    ContainerFillingRecipeBuilder.basinRecipe(TinkerSmeltery.searedTank.get(TankType.FUEL_GAUGE), FluidConstants.BUCKET / 4)
                                 .save(consumer, modResource(folder + "filling/seared_fuel_gauge"));
    ContainerFillingRecipeBuilder.tableRecipe(TinkerSmeltery.searedLantern, FluidValues.NUGGET)
                                 .save(consumer, modResource(folder + "filling/seared_lantern_pixel"));
    ContainerFillingRecipeBuilder.basinRecipe(TinkerSmeltery.searedLantern, FluidValues.SEARED_LANTERN_CAPACITY)
                                 .save(consumer, modResource(folder + "filling/seared_lantern_full"));
    // tank filling - scorched
    ContainerFillingRecipeBuilder.basinRecipe(TinkerSmeltery.scorchedTank.get(TankType.INGOT_TANK), FluidValues.INGOT)
                                 .save(consumer, location(folder + "filling/scorched_ingot_tank"));
    ContainerFillingRecipeBuilder.basinRecipe(TinkerSmeltery.scorchedTank.get(TankType.INGOT_GAUGE), FluidValues.INGOT)
                                 .save(consumer, modResource(folder + "filling/scorched_ingot_gauge"));
    ContainerFillingRecipeBuilder.basinRecipe(TinkerSmeltery.scorchedTank.get(TankType.FUEL_TANK), FluidConstants.BUCKET / 4)
                                 .save(consumer, modResource(folder + "filling/scorched_fuel_tank"));
    ContainerFillingRecipeBuilder.basinRecipe(TinkerSmeltery.scorchedTank.get(TankType.FUEL_GAUGE), FluidConstants.BUCKET / 4)
                                 .save(consumer, modResource(folder + "filling/scorched_fuel_gauge"));
    ContainerFillingRecipeBuilder.tableRecipe(TinkerSmeltery.scorchedLantern, FluidValues.NUGGET)
                                 .save(consumer, modResource(folder + "filling/scorched_lantern_pixel"));
    ContainerFillingRecipeBuilder.basinRecipe(TinkerSmeltery.scorchedLantern, FluidValues.SCORCHED_LANTERN_CAPACITY)
                                 .save(consumer, modResource(folder + "filling/scorched_lantern_full"));

    // Slime
    String slimeFolder = folder + "slime/";
    this.slimeCasting(consumer, TinkerFluids.earthSlime, SlimeType.EARTH, slimeFolder);
    this.slimeCasting(consumer, TinkerFluids.skySlime,   SlimeType.SKY,   slimeFolder);
    this.slimeCasting(consumer, TinkerFluids.enderSlime, SlimeType.ENDER, slimeFolder);
    this.slimeCasting(consumer, TinkerFluids.ichor,      SlimeType.ICHOR, slimeFolder);
    // magma cream
    ItemCastingRecipeBuilder.basinRecipe(Blocks.MAGMA_BLOCK)
                            .setFluidAndTime(TinkerFluids.magma, FluidValues.SLIME_CONGEALED)
                            .save(consumer, location(slimeFolder + "magma_block"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerFluids.magmaBottle)
                            .setFluid(TinkerFluids.magma.getTag(), FluidValues.SLIMEBALL)
                            .setCoolingTime(1)
                            .setCast(Items.GLASS_BOTTLE, true)
                            .save(consumer, location(slimeFolder + "magma_bottle"));

    // glass
    ItemCastingRecipeBuilder.basinRecipe(TinkerCommons.clearGlass)
                            .setFluidAndTime(TinkerFluids.moltenGlass, FluidValues.GLASS_BLOCK)
                            .save(consumer, location(folder + "glass/block"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerCommons.clearGlassPane)
                            .setFluidAndTime(TinkerFluids.moltenGlass, FluidValues.GLASS_PANE)
                            .save(consumer, location(folder + "glass/pane"));
    // soul glass
    ItemCastingRecipeBuilder.basinRecipe(TinkerCommons.soulGlass)
                            .setFluidAndTime(TinkerFluids.liquidSoul, FluidValues.GLASS_BLOCK)
                            .save(consumer, location(folder + "soul/glass"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerCommons.soulGlassPane)
                            .setFluidAndTime(TinkerFluids.liquidSoul, FluidValues.GLASS_PANE)
                            .save(consumer, location(folder + "soul/pane"));

    // clay
    ItemCastingRecipeBuilder.basinRecipe(Blocks.TERRACOTTA)
                            .setFluidAndTime(TinkerFluids.moltenClay, FluidValues.SLIME_CONGEALED)
                            .save(consumer, location(folder + "clay/block"));
    this.ingotCasting(consumer, TinkerFluids.moltenClay, FluidValues.SLIMEBALL, Items.BRICK, folder + "clay/brick");
    this.tagCasting(consumer, TinkerFluids.moltenClay, FluidValues.SLIMEBALL, TinkerSmeltery.plateCast, "plates/brick", folder + "clay/plate", true);

    // amethyst
    ItemCastingRecipeBuilder.basinRecipe(TinkerCommons.clearTintedGlass)
                            .setCast(Tags.Items.GLASS_COLORLESS, true)
                            .setFluidAndTime(TinkerFluids.moltenAmethyst, FluidValues.GEM * 2)
                            .save(consumer, location(folder + "amethyst/glass"));

    // diamond
    ItemCastingRecipeBuilder.tableDuplication()
                            .setCast(CompoundIngredient.of(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), Ingredient.of(ItemTags.TRIM_TEMPLATES)), false)
                            .setFluidAndTime(TinkerFluids.moltenDiamond, FluidValues.GEM * 5)
                            .save(consumer, location(folder + "diamond/smithing_template"));

    // ender pearls
    ItemCastingRecipeBuilder.tableRecipe(Items.ENDER_PEARL)
                            .setFluidAndTime(TinkerFluids.moltenEnder, FluidValues.SLIMEBALL)
                            .save(consumer, location(folder + "ender/pearl"));
    ItemCastingRecipeBuilder.tableRecipe(Items.ENDER_EYE)
                            .setFluidAndTime(TinkerFluids.moltenEnder, FluidValues.SLIMEBALL)
                            .setCast(Items.BLAZE_POWDER, true)
                            .save(consumer, location(folder + "ender/eye"));

    // obsidian
    ItemCastingRecipeBuilder.basinRecipe(Blocks.OBSIDIAN)
                            .setFluidAndTime(TinkerFluids.moltenObsidian, FluidValues.GLASS_BLOCK)
                            .save(consumer, location(folder + "obsidian/block"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerCommons.obsidianPane)
                            .setFluidAndTime(TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE)
                            .save(consumer, location(folder + "obsidian/pane"));
    // Molten objects with Bucket, Block, Ingot, and Nugget forms with standard values
    String metalFolder = folder + "metal/";
    this.ingotCasting(consumer, TinkerFluids.moltenDebris, Items.NETHERITE_SCRAP, metalFolder + "netherite/scrap");
    this.tagCasting(consumer, TinkerFluids.moltenDebris, FluidValues.NUGGET, TinkerSmeltery.nuggetCast, TinkerTags.Items.NUGGETS_NETHERITE_SCRAP.location().getPath(), metalFolder + "netherite/debris_nugget", false);
    // ancient is not castable, outside of repair kits
    this.castingWithCast(consumer, TinkerFluids.moltenDebris, FluidValues.INGOT * 2, TinkerSmeltery.repairKitCast, ItemOutput.fromStack(TinkerToolParts.repairKit.get().withMaterialForDisplay(MaterialIds.ancient)), metalFolder + "netherite/ancient_repair_kit");

    // water
    String waterFolder = folder + "water/";
    ItemCastingRecipeBuilder.basinRecipe(TinkerCommons.mudBricks)
                            .setFluidAndTime(new FluidStack(Fluids.WATER, FluidConstants.BUCKET / 10))
                            .setCast(Items.DIRT, true)
                            .save(consumer, prefix(TinkerCommons.mudBricks.getRegistryName(), waterFolder));
    ItemCastingRecipeBuilder.tableRecipe(ItemOutput.fromStack(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)))
                            .setFluid(MantleTags.Fluids.WATER, FluidValues.BOTTLE * 2)
                            .setCoolingTime(1)
                            .setCast(Items.GLASS_BOTTLE, true)
                            .save(consumer, location(waterFolder + "bottle"));
    ItemCastingRecipeBuilder.tableRecipe(ItemOutput.fromStack(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.WATER)))
                            .setFluid(MantleTags.Fluids.WATER, FluidValues.BOTTLE * 2)
                            .setCoolingTime(1)
                            .setCast(MantleTags.Items.SPLASH_BOTTLE, true)
                            .save(consumer, location(waterFolder + "splash"));
    ItemCastingRecipeBuilder.tableRecipe(ItemOutput.fromStack(PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), Potions.WATER)))
                            .setFluid(MantleTags.Fluids.WATER, FluidValues.BOTTLE * 2)
                            .setCoolingTime(1)
                            .setCast(MantleTags.Items.LINGERING_BOTTLE, true)
                            .save(consumer, location(waterFolder + "lingering"));
    ItemCastingRecipeBuilder.basinRecipe(Blocks.WET_SPONGE)
      .setFluid(Fluids.WATER, FluidValues.BOTTLE)
      .setCoolingTime(1)
      .setCast(Blocks.SPONGE, true)
      .save(consumer, location(waterFolder + "wet_sponge"));

    // casting concrete
    BiConsumer<Block,Block> concreteCasting = (powder, block) ->
      ItemCastingRecipeBuilder.basinRecipe(block)
                              .setFluidAndTime(new FluidStack(Fluids.WATER, FluidConstants.BUCKET / 10))
                              .setCast(powder, true)
                              .save(consumer, prefix(BuiltInRegistries.BLOCK.getKey(block), waterFolder));
    concreteCasting.accept(Blocks.WHITE_CONCRETE_POWDER,      Blocks.WHITE_CONCRETE);
    concreteCasting.accept(Blocks.ORANGE_CONCRETE_POWDER,     Blocks.ORANGE_CONCRETE);
    concreteCasting.accept(Blocks.MAGENTA_CONCRETE_POWDER,    Blocks.MAGENTA_CONCRETE);
    concreteCasting.accept(Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE);
    concreteCasting.accept(Blocks.YELLOW_CONCRETE_POWDER,     Blocks.YELLOW_CONCRETE);
    concreteCasting.accept(Blocks.LIME_CONCRETE_POWDER,       Blocks.LIME_CONCRETE);
    concreteCasting.accept(Blocks.PINK_CONCRETE_POWDER,       Blocks.PINK_CONCRETE);
    concreteCasting.accept(Blocks.GRAY_CONCRETE_POWDER,       Blocks.GRAY_CONCRETE);
    concreteCasting.accept(Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE);
    concreteCasting.accept(Blocks.CYAN_CONCRETE_POWDER,       Blocks.CYAN_CONCRETE);
    concreteCasting.accept(Blocks.PURPLE_CONCRETE_POWDER,     Blocks.PURPLE_CONCRETE);
    concreteCasting.accept(Blocks.BLUE_CONCRETE_POWDER,       Blocks.BLUE_CONCRETE);
    concreteCasting.accept(Blocks.BROWN_CONCRETE_POWDER,      Blocks.BROWN_CONCRETE);
    concreteCasting.accept(Blocks.GREEN_CONCRETE_POWDER,      Blocks.GREEN_CONCRETE);
    concreteCasting.accept(Blocks.RED_CONCRETE_POWDER,        Blocks.RED_CONCRETE);
    concreteCasting.accept(Blocks.BLACK_CONCRETE_POWDER,      Blocks.BLACK_CONCRETE);

    // misc
    ItemCastingRecipeBuilder.basinRecipe(TinkerCommons.lavawood)
                            .setFluidAndTime(new FluidStack(Fluids.LAVA, FluidConstants.BUCKET / 10))
                            .setCast(ItemTags.PLANKS, true)
                            .save(consumer, prefix(TinkerCommons.lavawood.getRegistryName(), folder));
    ItemCastingRecipeBuilder.basinRecipe(TinkerCommons.blazewood)
                            .setFluidAndTime(TinkerFluids.blazingBlood, false, FluidConstants.BUCKET / 10)
                            .setCast(DefaultCustomIngredients.all(Ingredient.of(ItemTags.PLANKS), Ingredient.of(ItemTags.NON_FLAMMABLE_WOOD)), true)
                            .save(consumer, prefix(TinkerCommons.blazewood.getRegistryName(), folder));

    // cast molten blaze into blazing stuff
    castingWithCast(consumer, TinkerFluids.blazingBlood, false, FluidConstants.BUCKET / 10, TinkerSmeltery.rodCast, Items.BLAZE_ROD, folder + "blaze/rod");
    ItemCastingRecipeBuilder.tableRecipe(Items.MAGMA_CREAM)
                            .setFluidAndTime(TinkerFluids.blazingBlood, false, FluidConstants.BUCKET / 20)
                            .setCast(Tags.Items.SLIMEBALLS, true)
                            .save(consumer, modResource(folder + "blaze/cream"));
    ItemCastingRecipeBuilder.basinRecipe(Blocks.MAGMA_BLOCK)
                            .setFluidAndTime(TinkerFluids.blazingBlood, false, FluidConstants.BUCKET / 5)
                            .setCast(TinkerTags.Items.CONGEALED_SLIME, true)
                            .save(consumer, modResource(folder + "blaze/congealed"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerMaterials.blazingBone)
                            .setFluidAndTime(TinkerFluids.blazingBlood, false, FluidConstants.BUCKET / 5)
                            .setCast(TinkerTags.Items.WITHER_BONES, true)
                            .save(consumer, location(folder + "blaze/bone"));

    // honey
    ItemCastingRecipeBuilder.tableRecipe(Items.HONEY_BOTTLE)
                            .setFluid(TinkerFluids.honey.getTag(), FluidValues.BOTTLE)
                            .setCoolingTime(1)
                            .setCast(Items.GLASS_BOTTLE, true)
                            .save(consumer, location(folder + "honey/bottle"));
    ItemCastingRecipeBuilder.basinRecipe(Items.HONEY_BLOCK)
                            .setFluidAndTime(TinkerFluids.honey, FluidValues.BOTTLE * 4)
                            .save(consumer, location(folder + "honey/block"));
    // soup
    ItemCastingRecipeBuilder.tableRecipe(Items.BEETROOT_SOUP)
                            .setFluid(TinkerFluids.beetrootSoup.getTag(), FluidValues.BOWL)
                            .setCast(Items.BOWL, true)
                            .setCoolingTime(1)
                            .save(consumer, location(folder + "soup/beetroot"));
    ItemCastingRecipeBuilder.tableRecipe(Items.MUSHROOM_STEW)
                            .setFluid(TinkerFluids.mushroomStew.getTag(), FluidValues.BOWL)
                            .setCast(Items.BOWL, true)
                            .setCoolingTime(1)
                            .save(consumer, location(folder + "soup/mushroom"));
    ItemCastingRecipeBuilder.tableRecipe(Items.RABBIT_STEW)
                            .setFluid(TinkerFluids.rabbitStew.getTag(), FluidValues.BOWL)
                            .setCast(Items.BOWL, true)
                            .setCoolingTime(1)
                            .save(consumer, location(folder + "soup/rabbit"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerFluids.meatSoupBowl)
                            .setFluid(TinkerFluids.meatSoup.getTag(), FluidValues.BOWL)
                            .setCast(Items.BOWL, true)
                            .setCoolingTime(1)
                            .save(consumer, location(folder + "soup/meat"));
    // venom
    ItemCastingRecipeBuilder.tableRecipe(TinkerFluids.venomBottle)
                            .setFluid(TinkerFluids.venom.getTag(), FluidValues.BOTTLE)
                            .setCoolingTime(1)
                            .setCast(Items.GLASS_BOTTLE, true)
                            .save(consumer, location(slimeFolder + "venom/bottle"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerMaterials.venombone)
                            .setFluidAndTime(TinkerFluids.venom, FluidValues.SLIMEBALL)
                            .setCast(Tags.Items.BONES, true)
                            .save(consumer, location(slimeFolder + "venom/bone"));

    // cheese
    ItemCastingRecipeBuilder.tableRecipe(TinkerCommons.cheeseIngot)
                            .setFluid(Tags.Fluids.MILK, FluidValues.BOTTLE)
                            .setCast(TinkerSmeltery.ingotCast.getMultiUseTag(), false)
                            .setCoolingTime(20*60*2)
                            .save(consumer, location(folder + "cheese_ingot_gold_cast"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerCommons.cheeseIngot)
                            .setFluid(Tags.Fluids.MILK, FluidValues.BOTTLE)
                            .setCast(TinkerSmeltery.ingotCast.getSingleUseTag(), true)
                            .setCoolingTime(20*60*2)
                            .save(consumer, location(folder + "cheese_ingot_sand_cast"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerCommons.cheeseBlock)
                            .setFluid(Tags.Fluids.MILK, FluidType.BUCKET_VOLUME)
                            .setCoolingTime(20*60*5)
                            .save(consumer, location(folder + "cheese_block"));
    ItemCastingRecipeBuilder.tableRecipe(Items.BONE)
      .setFluid(Tags.Fluids.MILK, FluidType.BUCKET_VOLUME / 5)
      .setCoolingTime(50)
      .setCast(TinkerTags.Items.WITHER_BONES, true)
      .save(consumer, location(folder + "bone_purifying"));


    String castFolder = "smeltery/casts/";
    this.castCreation(consumer, CompoundIngredient.of(
      // fake ingots are in the ingot tag, but you get the default "missing" ingot from that
      // so subtract it out and replace with the material version for nicer display
      DifferenceIngredient.of(Ingredient.of(Tags.Items.INGOTS), Ingredient.of(TinkerToolParts.fakeIngot)),
      MaterialIngredient.of(TinkerToolParts.fakeIngot)
    ), TinkerSmeltery.ingotCast, castFolder, "ingots");
    this.castCreation(consumer, Tags.Items.NUGGETS, TinkerSmeltery.nuggetCast, castFolder);
    this.castCreation(consumer, Tags.Items.GEMS, TinkerSmeltery.gemCast, castFolder);
    this.castCreation(consumer, Tags.Items.RODS, TinkerSmeltery.rodCast, castFolder);
    // other casts are added if needed
    this.castCreation(withCondition(consumer, tagCondition("plates")), getItemTag("c", "plates"), TinkerSmeltery.plateCast, castFolder);
    this.castCreation(withCondition(consumer, tagCondition("gears")), getItemTag("c", "gears"), TinkerSmeltery.gearCast, castFolder);
    this.castCreation(withCondition(consumer, tagCondition("coins")), getItemTag("c", "coins"), TinkerSmeltery.coinCast, castFolder);
    this.castCreation(withCondition(consumer, tagCondition("wires")), getItemTag("c", "wires"), TinkerSmeltery.wireCast, castFolder);

    // misc casting - gold
    ItemCastingRecipeBuilder.tableRecipe(TinkerCommons.goldBars)
                            .setFluidAndTime(TinkerFluids.moltenGold, FluidValues.NUGGET * 3)
                            .save(consumer, location(metalFolder + "gold/bars"));
    ItemCastingRecipeBuilder.tableRecipe(Items.GOLDEN_APPLE)
                            .setFluidAndTime(TinkerFluids.moltenGold, FluidValues.INGOT * 8)
                            .setCast(Items.APPLE, true)
                            .save(consumer, location(metalFolder + "gold/apple"));
    ItemCastingRecipeBuilder.tableRecipe(Items.GLISTERING_MELON_SLICE)
                            .setFluidAndTime(TinkerFluids.moltenGold, FluidValues.NUGGET * 8)
                            .setCast(Items.MELON_SLICE, true)
                            .save(consumer, location(metalFolder + "gold/melon"));
    ItemCastingRecipeBuilder.tableRecipe(Items.GOLDEN_CARROT)
                            .setFluidAndTime(TinkerFluids.moltenGold, FluidValues.NUGGET * 8)
                            .setCast(Items.CARROT, true)
                            .save(consumer, location(metalFolder + "gold/carrot"));
    ItemCastingRecipeBuilder.tableRecipe(Items.CLOCK)
                            .setFluidAndTime(TinkerFluids.moltenGold, FluidValues.INGOT * 4)
                            .setCast(Items.REDSTONE, true)
                            .save(consumer, location(metalFolder + "gold/clock"));
    // misc casting - iron
    ItemCastingRecipeBuilder.tableRecipe(Blocks.IRON_BARS)  // cheaper by 6mb, not a duplication as the melting recipe was adjusted too (like panes)
                            .setFluidAndTime(TinkerFluids.moltenIron, FluidValues.NUGGET * 3)
                            .save(consumer, location(metalFolder + "iron/bars"));
    ItemCastingRecipeBuilder.tableRecipe(Items.LANTERN)
                            .setFluidAndTime(TinkerFluids.moltenIron, FluidValues.NUGGET * 8)
                            .setCast(Blocks.TORCH, true)
                            .save(consumer, location(metalFolder + "iron/lantern"));
    ItemCastingRecipeBuilder.tableRecipe(Items.SOUL_LANTERN)
                            .setFluidAndTime(TinkerFluids.moltenIron, FluidValues.NUGGET * 8)
                            .setCast(Blocks.SOUL_TORCH, true)
                            .save(consumer, location(metalFolder + "iron/soul_lantern"));
    ItemCastingRecipeBuilder.tableRecipe(Items.COMPASS)
                            .setFluidAndTime(TinkerFluids.moltenIron, FluidValues.INGOT * 4)
                            .setCast(Items.REDSTONE, true)
                            .save(consumer, location(metalFolder + "iron/compass"));
    // ender chest
    ItemCastingRecipeBuilder.basinRecipe(Blocks.ENDER_CHEST)
                            .setFluidAndTime(TinkerFluids.moltenObsidian, FluidValues.GLASS_BLOCK * 8)
                            .setCast(Items.ENDER_EYE, true)
                            .save(consumer, location(folder + "obsidian/chest"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerMaterials.nahuatl)
                            .setFluidAndTime(TinkerFluids.moltenObsidian, false, FluidConstants.BUCKET)
                            .setCast(ItemTags.PLANKS, true)
                            .save(consumer, location(folder + "obsidian/nahuatl"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerMaterials.nahuatl.getSlab())
                            .setFluidAndTime(TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE / 2)
                            .setCast(ItemTags.WOODEN_SLABS, true)
                            .save(consumer, location(folder + "obsidian/nahuatl_slab"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerMaterials.nahuatl.getStairs())
                            .setFluidAndTime(TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE)
                            .setCast(ItemTags.WOODEN_STAIRS, true)
                            .save(consumer, location(folder + "obsidian/nahuatl_stairs"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerMaterials.nahuatl.getFence())
                            .setFluidAndTime(TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE)
                            .setCast(ItemTags.WOODEN_FENCES, true)
                            .save(consumer, location(folder + "obsidian/nahuatl_fence"));
    // overworld stones from quartz
    ItemCastingRecipeBuilder.basinRecipe(Blocks.ANDESITE)
                            .setFluidAndTime(TinkerFluids.moltenQuartz, FluidValues.GEM / 2)
                            .setCast(Tags.Items.COBBLESTONE, true)
                            .save(consumer, prefix(BuiltInRegistries.BLOCK.getKey(Blocks.ANDESITE), folder + "quartz/"));
    ItemCastingRecipeBuilder.basinRecipe(Blocks.DIORITE)
                            .setFluidAndTime(TinkerFluids.moltenQuartz, FluidValues.GEM / 2)
                            .setCast(Blocks.ANDESITE, true)
                            .save(consumer, prefix(BuiltInRegistries.BLOCK.getKey(Blocks.DIORITE), folder + "quartz/"));
    ItemCastingRecipeBuilder.basinRecipe(Blocks.GRANITE)
                            .setFluidAndTime(TinkerFluids.moltenQuartz, FluidValues.GEM)
                            .setCast(Blocks.DIORITE, true)
                            .save(consumer, prefix(BuiltInRegistries.BLOCK.getKey(Blocks.GRANITE), folder + "quartz/"));
  }

  private void addMeltingRecipes(Consumer<FinishedRecipe> consumer) {
    String folder = "smeltery/melting/";

    // water from ice
    MeltingRecipeBuilder.melting(Ingredient.of(Items.ICE), Fluids.WATER, FluidConstants.BUCKET, 1.0f)
                        .save(consumer, modResource(folder + "water/ice"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.PACKED_ICE), Fluids.WATER, FluidConstants.BUCKET * 9, 3.0f)
                        .save(consumer, modResource(folder + "water/packed_ice"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.BLUE_ICE), Fluids.WATER, FluidConstants.BUCKET * 81, 9.0f)
                        .save(consumer, modResource(folder + "water/blue_ice"));
    // water from snow
    MeltingRecipeBuilder.melting(Ingredient.of(Items.SNOWBALL), Fluids.WATER, FluidConstants.BUCKET / 8, 0.5f)
                        .save(consumer, modResource(folder + "water/snowball"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.SNOW_BLOCK), Fluids.WATER, FluidConstants.BUCKET / 2, 0.75f)
                        .save(consumer, modResource(folder + "water/snow_block"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.SNOW), Fluids.WATER, FluidConstants.BUCKET / 8, 0.5f)
                        .save(consumer, modResource(folder + "water/snow_layer"));

    // ores
    String metalFolder = folder + "metal/";
    MeltingRecipeBuilder.melting(Ingredient.of(Tags.Items.ORES_NETHERITE_SCRAP), TinkerFluids.moltenDebris, FluidValues.INGOT, 2.0f)
                        .setOre(OreRateType.METAL)
                        .addByproduct(TinkerFluids.moltenNetherite.result(FluidValues.NUGGET * 3))
                        .save(consumer, location(metalFolder + "molten_debris/ore"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerTags.Items.INGOTS_NETHERITE_SCRAP), TinkerFluids.moltenDebris, FluidValues.INGOT, 1.0f)
                        .save(consumer, location(metalFolder + "molten_debris/scrap"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerTags.Items.NUGGETS_NETHERITE_SCRAP), TinkerFluids.moltenDebris, FluidValues.NUGGET, 1 / 3f)
                        .save(consumer, location(metalFolder + "molten_debris/debris_nugget"));
    
    // venom
    MeltingRecipeBuilder.melting(Ingredient.of(Items.SPIDER_EYE), TinkerFluids.venom, FluidValues.BOTTLE, 1.0f)
                        .save(consumer, location(folder + "venom/eye"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.FERMENTED_SPIDER_EYE), TinkerFluids.venom, FluidValues.BOTTLE * 2, 1.0f)
                        .save(consumer, location(folder + "venom/fermented_eye"));

    // glass
    MeltingRecipeBuilder.melting(Ingredient.of(ItemTags.SMELTS_TO_GLASS), TinkerFluids.moltenGlass, FluidValues.GLASS_BLOCK, 1.5f)
                        .save(consumer, location(folder + "glass/sand"));
    MeltingRecipeBuilder.melting(Ingredient.of(Tags.Items.GLASS_SILICA), TinkerFluids.moltenGlass, FluidValues.GLASS_BLOCK, 1.0f)
                        .save(consumer, location(folder + "glass/block"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerTags.Items.GLASS_PANES_SILICA), TinkerFluids.moltenGlass, FluidValues.GLASS_PANE, 0.5f)
                        .save(consumer, location(folder + "glass/pane"));
    MeltingRecipeBuilder.melting(CompoundIngredient.of(Ingredient.of(Items.GLASS_BOTTLE), Ingredient.of(MantleTags.Items.SPLASH_BOTTLE), Ingredient.of(MantleTags.Items.LINGERING_BOTTLE)),
      TinkerFluids.moltenGlass, FluidValues.GLASS_BLOCK, 1.25f).save(consumer, location(folder + "glass/bottle"));
    // melt extra sand casts back
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.blankSandCast, TinkerSmeltery.blankRedSandCast),
                                 TinkerFluids.moltenGlass, FluidValues.GLASS_PANE, 0.75f)
                        .save(consumer, location(folder + "glass/sand_cast"));

    // liquid soul
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.SOUL_SAND, Blocks.SOUL_SOIL), TinkerFluids.liquidSoul, FluidValues.GLASS_BLOCK, 1.5f)
                        .save(consumer, location(folder + "soul/sand"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerCommons.soulGlass), TinkerFluids.liquidSoul, FluidValues.GLASS_BLOCK, 1.0f)
                        .save(consumer, location(folder + "soul/glass"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerCommons.soulGlassPane), TinkerFluids.liquidSoul, FluidValues.GLASS_PANE, 0.5f)
                        .save(consumer, location(folder + "soul/pane"));

    // clay
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.CLAY), TinkerFluids.moltenClay, FluidValues.BRICK_BLOCK, 1.0f)
                        .save(consumer, location(folder + "clay/block"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.CLAY_BALL), TinkerFluids.moltenClay, FluidValues.BRICK, 0.5f)
                        .save(consumer, location(folder + "clay/ball"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.FLOWER_POT), TinkerFluids.moltenClay, FluidValues.BRICK * 3, 2.0f)
                        .save(consumer, location(folder + "clay/pot"));
    tagMelting(consumer, TinkerFluids.moltenClay, FluidValues.BRICK, "plates/brick", 1.0f, folder + "clay/plate", true);
    // terracotta
    Ingredient terracottaBlock = Ingredient.of(
      Blocks.TERRACOTTA, Blocks.BRICKS, Blocks.BRICK_WALL, Blocks.BRICK_STAIRS,
      Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA,
      Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA,
      Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
      Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA,
      Blocks.WHITE_GLAZED_TERRACOTTA, Blocks.ORANGE_GLAZED_TERRACOTTA, Blocks.MAGENTA_GLAZED_TERRACOTTA, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
      Blocks.YELLOW_GLAZED_TERRACOTTA, Blocks.LIME_GLAZED_TERRACOTTA, Blocks.PINK_GLAZED_TERRACOTTA, Blocks.GRAY_GLAZED_TERRACOTTA,
      Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA, Blocks.CYAN_GLAZED_TERRACOTTA, Blocks.PURPLE_GLAZED_TERRACOTTA, Blocks.BLUE_GLAZED_TERRACOTTA,
      Blocks.BROWN_GLAZED_TERRACOTTA, Blocks.GREEN_GLAZED_TERRACOTTA, Blocks.RED_GLAZED_TERRACOTTA, Blocks.BLACK_GLAZED_TERRACOTTA, Blocks.DECORATED_POT);
    MeltingRecipeBuilder.melting(terracottaBlock, TinkerFluids.moltenClay, FluidValues.BRICK_BLOCK, 2.0f)
                        .save(consumer, location(folder + "clay/terracotta"));
    MeltingRecipeBuilder.melting(CompoundIngredient.of(Ingredient.of(Items.BRICK), Ingredient.of(ItemTags.DECORATED_POT_SHERDS)), TinkerFluids.moltenClay, FluidValues.BRICK, 1.0f)
                        .save(consumer, location(folder + "clay/brick"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.BRICK_SLAB),
                                 TinkerFluids.moltenClay, FluidValues.BRICK_BLOCK / 2, 1.5f)
                        .save(consumer, location(folder + "clay/brick_slab"));

    // slime
    String slimeFolder = folder + "slime/";
    slimeMelting(consumer, TinkerFluids.earthSlime, SlimeType.EARTH, slimeFolder);
    slimeMelting(consumer, TinkerFluids.skySlime,   SlimeType.SKY, slimeFolder);
    slimeMelting(consumer, TinkerFluids.enderSlime, SlimeType.ENDER, slimeFolder);
    slimeMelting(consumer, TinkerFluids.blood, SlimeType.BLOOD, slimeFolder);
    slimeMelting(consumer, TinkerFluids.ichor, SlimeType.ICHOR, slimeFolder);
    // magma cream
    MeltingRecipeBuilder.melting(Ingredient.of(Items.MAGMA_CREAM), TinkerFluids.magma, FluidValues.SLIMEBALL, 1.0f)
                        .save(consumer, location(slimeFolder + "magma/ball"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.MAGMA_BLOCK), TinkerFluids.magma, FluidValues.SLIME_CONGEALED, 3.0f)
                        .save(consumer, location(slimeFolder + "magma/block"));

    // copper cans if empty
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.nbt(new ItemStack(TinkerSmeltery.copperCan), true), TinkerFluids.moltenCopper.get(), FluidValues.INGOT, 1.0f)
                        .save(consumer, modResource(metalFolder + "copper/can"));
    // ender
    MeltingRecipeBuilder.melting(
      DefaultCustomIngredients.any(Ingredient.of(Tags.Items.ENDER_PEARLS), Ingredient.of(Items.ENDER_EYE)),
      TinkerFluids.moltenEnder.get(), FluidValues.SLIMEBALL, 1.0f)
                        .save(consumer, modResource(folder + "ender/pearl"));

    // obsidian
    MeltingRecipeBuilder.melting(Ingredient.of(Tags.Items.OBSIDIAN), TinkerFluids.moltenObsidian, FluidValues.GLASS_BLOCK, 2.0f)
                        .save(consumer, location(folder + "obsidian/block"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerCommons.obsidianPane), TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE, 1.5f)
                        .save(consumer, location(folder + "obsidian/pane"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.ENDER_CHEST), TinkerFluids.moltenObsidian, FluidValues.GLASS_BLOCK * 8, 5.0f)
                        .addByproduct(TinkerFluids.moltenEnder.result(FluidValues.SLIMEBALL))
                        .save(consumer, location(folder + "obsidian/chest"));
    tagMelting(consumer, TinkerFluids.moltenObsidian, FluidValues.GLASS_PANE, "dusts/obsidian", 1.0f, folder + "obsidian/dust", true);

    // emerald
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerModifiers.emeraldReinforcement), TinkerFluids.moltenEmerald, FluidValues.GEM_SHARD)
                        .addByproduct(TinkerFluids.moltenObsidian.result(FluidValues.GLASS_PANE))
                        .save(consumer, location(metalFolder + "emerald/reinforcement"));

    // quartz
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BRICKS, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_STAIRS, Blocks.SMOOTH_QUARTZ_STAIRS),
      TinkerFluids.moltenQuartz, FluidValues.SMALL_GEM_BLOCK, 2.0f)
                        .save(consumer, location(folder + "quartz/decorative_block"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.QUARTZ_SLAB, Blocks.SMOOTH_QUARTZ_SLAB), TinkerFluids.moltenQuartz, FluidValues.GEM * 2, 1.5f)
                        .save(consumer, location(folder + "quartz/slab"));

    // amethyst
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.AMETHYST_CLUSTER), TinkerFluids.moltenAmethyst, FluidValues.GEM * 4, 4.0f)
                        .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM * 4))
                        .setOre(OreRateType.GEM)
                        .save(consumer, location(folder + "amethyst/cluster"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.SMALL_AMETHYST_BUD), TinkerFluids.moltenAmethyst, FluidValues.GEM, 1.0f)
                        .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM))
                        .setOre(OreRateType.GEM)
                        .save(consumer, location(folder + "amethyst/bud_small"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.MEDIUM_AMETHYST_BUD), TinkerFluids.moltenAmethyst, FluidValues.GEM * 2, 2.0f)
                        .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM * 2))
                        .setOre(OreRateType.GEM)
                        .save(consumer, location(folder + "amethyst/bud_medium"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.LARGE_AMETHYST_BUD), TinkerFluids.moltenAmethyst, FluidValues.GEM * 3, 3.0f)
                        .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM * 3))
                        .setOre(OreRateType.GEM)
                        .save(consumer, location(folder + "amethyst/bud_large"));

    // iron melting - standard values
    MeltingRecipeBuilder.melting(Ingredient.of(Items.ACTIVATOR_RAIL, Items.DETECTOR_RAIL, Blocks.STONECUTTER, Blocks.PISTON, Blocks.STICKY_PISTON), TinkerFluids.moltenIron, FluidValues.INGOT)
                        .save(consumer, location(metalFolder + "iron/ingot_1"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.IRON_DOOR, Blocks.SMITHING_TABLE), TinkerFluids.moltenIron, FluidValues.INGOT * 2)
                        .save(consumer, location(metalFolder + "iron/ingot_2"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.BUCKET), TinkerFluids.moltenIron, FluidValues.INGOT * 3)
                        .save(consumer, location(metalFolder + "iron/bucket"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.COMPASS, Blocks.IRON_TRAPDOOR), TinkerFluids.moltenIron, FluidValues.INGOT * 4)
                        .save(consumer, location(metalFolder + "iron/ingot_4"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.BLAST_FURNACE, Blocks.HOPPER, Items.MINECART), TinkerFluids.moltenIron, FluidValues.INGOT * 5)
                        .save(consumer, location(metalFolder + "iron/ingot_5"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.CAULDRON), TinkerFluids.moltenIron, FluidValues.INGOT * 7)
                        .save(consumer, location(metalFolder + "iron/cauldron"));
    // non-standard
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.CHAIN), TinkerFluids.moltenIron, FluidValues.INGOT + FluidValues.NUGGET * 2)
                        .save(consumer, location(metalFolder + "iron/chain"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL), TinkerFluids.moltenIron, FluidValues.INGOT * 4 + FluidValues.METAL_BLOCK * 3)
                        .save(consumer, location(metalFolder + "iron/anvil"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.IRON_BARS, Blocks.RAIL), TinkerFluids.moltenIron, FluidValues.NUGGET * 3)
                        .save(consumer, location(metalFolder + "iron/nugget_3"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerCommons.ironPlatform), TinkerFluids.moltenIron, FluidValues.NUGGET * 10)
                        .save(consumer, location(metalFolder + "iron/platform"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.TRIPWIRE_HOOK), TinkerFluids.moltenIron, FluidValues.NUGGET * 4)
                        .save(consumer, location(metalFolder + "iron/tripwire"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.LANTERN, Blocks.SOUL_LANTERN), TinkerFluids.moltenIron, FluidValues.NUGGET * 8)
                        .save(consumer, location(metalFolder + "iron/lantern"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerModifiers.ironReinforcement), TinkerFluids.moltenIron, FluidValues.INGOT)
                        .save(consumer, location(metalFolder + "iron/reinforcement"));
    // tools
    MeltingRecipeBuilder.melting(Ingredient.of(Items.CROSSBOW), TinkerFluids.moltenIron, FluidValues.NUGGET * 13) // tripwire hook is 4 nuggets, ingot is 9 nuggets
                        .setDamagable(FluidValues.NUGGET)
                        .save(consumer, location(metalFolder + "iron/crossbow"));
    // unique melting
    MeltingRecipeBuilder.melting(Ingredient.of(Items.IRON_HORSE_ARMOR), TinkerFluids.moltenIron, FluidValues.INGOT * 7)
                        .save(consumer, location(metalFolder + "iron/horse_armor"));
    // chainmail armor to steel
    // working off the assumption that some mods out there decided to craft chainmail for an ingots worth of material at minimum, possibly a bit more if they used chains (which is nonsensical)
    final int chainIron = FluidValues.NUGGET * 6;
    final int chainSteel = FluidValues.NUGGET * 3;
    MeltingRecipeBuilder.melting(Ingredient.of(Items.CHAINMAIL_HELMET), TinkerFluids.moltenIron, chainIron * 5)
                        .addByproduct(TinkerFluids.moltenSteel.result(chainSteel * 5))
                        .setDamagable(FluidValues.NUGGET, FluidValues.NUGGET)
                        .save(consumer, location(metalFolder + "iron/chain_helmet"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.CHAINMAIL_CHESTPLATE), TinkerFluids.moltenIron, chainIron * 8)
                        .addByproduct(TinkerFluids.moltenSteel.result(chainSteel * 8))
                        .setDamagable(FluidValues.NUGGET, FluidValues.NUGGET)
                        .save(consumer, location(metalFolder + "iron/chain_chestplate"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.CHAINMAIL_LEGGINGS), TinkerFluids.moltenIron, chainIron * 7)
                        .addByproduct(TinkerFluids.moltenSteel.result(chainSteel * 7))
                        .setDamagable(FluidValues.NUGGET, FluidValues.NUGGET)
                        .save(consumer, location(metalFolder + "iron/chain_leggings"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.CHAINMAIL_BOOTS), TinkerFluids.moltenIron, chainIron * 4)
                        .addByproduct(TinkerFluids.moltenSteel.result(chainSteel * 4))
                        .setDamagable(FluidValues.NUGGET, FluidValues.NUGGET)
                        .save(consumer, location(metalFolder + "iron/chain_boots"));



    // gold melting
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerTags.Items.GOLD_CASTS), TinkerFluids.moltenGold, FluidValues.INGOT)
                        .save(consumer, location(metalFolder + "gold/cast"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.POWERED_RAIL), TinkerFluids.moltenGold, FluidValues.INGOT)
                        .save(consumer, location(metalFolder + "gold/powered_rail"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE), TinkerFluids.moltenGold, FluidValues.INGOT * 2)
                        .save(consumer, location(metalFolder + "gold/pressure_plate"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.CLOCK), TinkerFluids.moltenGold, FluidValues.INGOT * 4)
                        .save(consumer, location(metalFolder + "gold/clock"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.GOLDEN_APPLE), TinkerFluids.moltenGold, FluidValues.INGOT * 8)
                        .save(consumer, location(metalFolder + "gold/apple"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.GLISTERING_MELON_SLICE, Items.GOLDEN_CARROT), TinkerFluids.moltenGold, FluidValues.NUGGET * 8)
                        .save(consumer, location(metalFolder + "gold/produce"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerModifiers.goldReinforcement), TinkerFluids.moltenGold, FluidValues.INGOT)
                        .save(consumer, location(metalFolder + "gold/reinforcement"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerCommons.goldBars), TinkerFluids.moltenGold, FluidValues.NUGGET * 3)
                        .save(consumer, location(metalFolder + "gold/nugget_3"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerCommons.goldPlatform), TinkerFluids.moltenGold, FluidValues.NUGGET * 10)
                        .save(consumer, location(metalFolder + "gold/platform"));
    // unique melting
    MeltingRecipeBuilder.melting(Ingredient.of(Items.GOLDEN_HORSE_ARMOR), TinkerFluids.moltenGold, FluidValues.INGOT * 7)
                        .save(consumer, location(metalFolder + "gold/horse_armor"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.ENCHANTED_GOLDEN_APPLE), TinkerFluids.moltenGold, FluidValues.METAL_BLOCK * 8)
                        .save(consumer, location(metalFolder + "gold/enchanted_apple"));
    // we directly add the recipe for nether gold ore instead of doing a sparse gold ore as we want to change the byproduct
    // if you add a sparse non-nether gold ore and need it meltable, let us know and we can add support
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.NETHER_GOLD_ORE), TinkerFluids.moltenGold, FluidValues.INGOT)
                        .addByproduct(TinkerFluids.moltenCopper.result(FluidValues.INGOT))
                        .setOre(OreRateType.METAL)
                        .save(consumer, location(metalFolder + "gold/nether_gold_ore"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.GILDED_BLACKSTONE), TinkerFluids.moltenGold, FluidValues.NUGGET * 3) // bit below average, ore rate will bring you bit above average
                        .addByproduct(TinkerFluids.moltenCopper.result(FluidValues.INGOT))
                        .setOre(OreRateType.METAL)
                        .save(consumer, location(metalFolder + "gold/gilded_blackstone"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.BELL), TinkerFluids.moltenGold, FluidValues.INGOT * 4) // bit arbitrary, I am happy to change the value if someone has a better one
                        .save(consumer, location(metalFolder + "gold/bell"));


    // copper melting
    MeltingRecipeBuilder.melting(Ingredient.of(
      Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER,
      Blocks.WAXED_COPPER_BLOCK, Blocks.WAXED_EXPOSED_COPPER, Blocks.WAXED_WEATHERED_COPPER, Blocks.WAXED_OXIDIZED_COPPER),
                                 TinkerFluids.moltenCopper, FluidValues.METAL_BLOCK)
                        .save(consumer, location(metalFolder + "copper/decorative_block"));
    MeltingRecipeBuilder.melting(Ingredient.of(
                                   Blocks.CUT_COPPER, Blocks.EXPOSED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER,
                                   Blocks.CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_CUT_COPPER_STAIRS,
                                   Blocks.WAXED_CUT_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER, Blocks.WAXED_WEATHERED_CUT_COPPER, Blocks.WAXED_OXIDIZED_CUT_COPPER,
                                   Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS),
                                 TinkerFluids.moltenCopper, FluidValues.NUGGET * 20)
                        .save(consumer, location(metalFolder + "copper/cut_block"));
    MeltingRecipeBuilder.melting(Ingredient.of(
                                   Blocks.CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER_SLAB,
                                   Blocks.WAXED_CUT_COPPER_SLAB, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB),
                                 TinkerFluids.moltenCopper, FluidValues.NUGGET * 10)
                        .save(consumer, location(metalFolder + "copper/cut_slab"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.LIGHTNING_ROD), TinkerFluids.moltenCopper, FluidValues.INGOT * 3)
                        .save(consumer, location(metalFolder + "copper/lightning_rod"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerTags.Items.COPPER_PLATFORMS), TinkerFluids.moltenCopper, FluidValues.NUGGET * 10)
                        .save(consumer, location(metalFolder + "copper/platform"));

    // amethyst melting
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.TINTED_GLASS, TinkerCommons.clearTintedGlass), TinkerFluids.moltenAmethyst, FluidValues.GEM * 2)
                        .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_BLOCK / 2))
                        .save(consumer, location(folder + "amethyst/tinted_glass"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.SPYGLASS), TinkerFluids.moltenAmethyst, FluidValues.GEM)
                        .addByproduct(TinkerFluids.moltenCopper.result(FluidValues.INGOT * 2))
                        .save(consumer, location(folder + "amethyst/spyglass"));

    // diamond melting
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.JUKEBOX), TinkerFluids.moltenDiamond, FluidValues.GEM)
                        .save(consumer, location(folder + "diamond/jukebox"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.ENCHANTING_TABLE), TinkerFluids.moltenDiamond, FluidValues.GEM * 2)
                        .addByproduct(TinkerFluids.moltenObsidian.result(FluidValues.GLASS_BLOCK * 4))
                        .save(consumer, location(folder + "diamond/enchanting_table"));
    // not the full copy cost as we have a discount recipe
    MeltingRecipeBuilder.melting(CompoundIngredient.of(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), Ingredient.of(ItemTags.TRIM_TEMPLATES)), TinkerFluids.moltenDiamond, FluidValues.GEM * 5)
                        .save(consumer, location(folder + "diamond/smithing_template"));
    // unique melting
    MeltingRecipeBuilder.melting(Ingredient.of(Items.DIAMOND_HORSE_ARMOR), TinkerFluids.moltenDiamond, FluidValues.GEM * 7)
                        .save(consumer, location(folder + "diamond/horse_armor"));

    // netherite
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.LODESTONE), TinkerFluids.moltenNetherite, FluidValues.INGOT)
                        .save(consumer, location(metalFolder + "netherite/lodestone"));
    // armor
    long[] netheriteSizes = {FluidValues.NUGGET, FluidValues.GEM_SHARD};
    MeltingRecipeBuilder.melting(Ingredient.of(Items.NETHERITE_HELMET), TinkerFluids.moltenNetherite.get(), FluidValues.INGOT)
                        .setDamagable(netheriteSizes)
                        .addByproduct(TinkerFluids.moltenDiamond.result(FluidValues.GEM * 5))
                        .save(consumer, location(metalFolder + "netherite/helmet"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.NETHERITE_CHESTPLATE), TinkerFluids.moltenNetherite, FluidValues.INGOT)
                        .setDamagable(netheriteSizes)
                        .addByproduct(TinkerFluids.moltenDiamond.result(FluidValues.GEM * 8))
                        .save(consumer, location(metalFolder + "netherite/chestplate"));
    // leggings use tag for the sake of paxels from mekanism
    MeltingRecipeBuilder.melting(Ingredient.of(getItemTag(TConstruct.MOD_ID, "melting/netherite/tools_costing_" + 7)), TinkerFluids.moltenNetherite, FluidValues.INGOT)
                        .setDamagable(netheriteSizes)
                        .addByproduct(TinkerFluids.moltenDiamond.result(FluidValues.GEM * 7))
                        .save(consumer, location(metalFolder + "netherite/leggings"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.NETHERITE_BOOTS), TinkerFluids.moltenNetherite, FluidValues.INGOT)
                        .setDamagable(netheriteSizes)
                        .addByproduct(TinkerFluids.moltenDiamond.result(FluidValues.GEM * 4))
                        .save(consumer, location(metalFolder + "netherite/boots"));
    // tools
    // axes uses tag for tools complement sickle
    MeltingRecipeBuilder.melting(Ingredient.of(getItemTag(TConstruct.MOD_ID, "melting/netherite/tools_costing_" + 3)), TinkerFluids.moltenNetherite, FluidValues.INGOT)
                        .setDamagable(netheriteSizes)
                        .addByproduct(TinkerFluids.moltenDiamond.result(FluidValues.GEM * 3))
                        .save(consumer, location(metalFolder + "netherite/axes"));
    // tools costing 2 only has vanilla sword and hoe, but its easier to use the tag then not for other metals so we use it here
    MeltingRecipeBuilder.melting(Ingredient.of(getItemTag(TConstruct.MOD_ID, "melting/netherite/tools_costing_" + 2)), TinkerFluids.moltenNetherite, FluidValues.INGOT)
                        .setDamagable(netheriteSizes)
                        .addByproduct(TinkerFluids.moltenDiamond.result(FluidValues.GEM * 2))
                        .save(consumer, location(metalFolder + "netherite/sword"));
    // shovels use tags for tools complement knife
    MeltingRecipeBuilder.melting(Ingredient.of(getItemTag(TConstruct.MOD_ID, "melting/netherite/tools_costing_" + 1)), TinkerFluids.moltenNetherite, FluidValues.INGOT)
                        .setDamagable(netheriteSizes)
                        .addByproduct(TinkerFluids.moltenDiamond.result(FluidValues.GEM))
                        .save(consumer, location(metalFolder + "netherite/shovel"));
    // tools complement compat - excavators and hammers
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(new ResourceLocation("tools_complement", "netherite_excavator")), TinkerFluids.moltenNetherite, FluidValues.INGOT)
                        .setDamagable(netheriteSizes)
                        .addByproduct(TinkerFluids.moltenDiamond.result(FluidValues.GEM * 11))
                        .save(withCondition(consumer, new ItemExistsCondition("tools_complement", "netherite_excavator")), location(metalFolder + "netherite/excavator"));
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(new ResourceLocation("tools_complement", "netherite_hammer")), TinkerFluids.moltenNetherite, FluidValues.INGOT)
                        .setDamagable(netheriteSizes)
                        .addByproduct(TinkerFluids.moltenDiamond.result(FluidValues.GEM * 13))
                        .save(withCondition(consumer, new ItemExistsCondition("tools_complement", "netherite_hammer")), location(metalFolder + "netherite/hammer"));

    // quartz
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.OBSERVER, Blocks.COMPARATOR, TinkerGadgets.quartzShuriken), TinkerFluids.moltenQuartz, FluidValues.GEM)
                        .save(consumer, location(folder + "quartz/gem_1"));
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.DAYLIGHT_DETECTOR), TinkerFluids.moltenQuartz, FluidValues.GEM * 3)
                        .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_BLOCK * 3))
                        .save(consumer, location(folder + "quartz/daylight_detector"));

    // obsidian, if you are crazy i guess
    MeltingRecipeBuilder.melting(Ingredient.of(Blocks.BEACON), TinkerFluids.moltenObsidian, FluidValues.GLASS_BLOCK * 3)
                        .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_BLOCK * 5))
                        .save(consumer, location(folder + "obsidian/beacon"));

    // knightmetal recycling
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerSmeltery.endFluidCannon), TinkerFluids.moltenKnightmetal, FluidValues.INGOT * 5, 2.5f)
      .save(consumer, location(folder + "metal/knightmetal/fluid_cannon"));

    // ender
    MeltingRecipeBuilder.melting(Ingredient.of(Items.END_CRYSTAL), TinkerFluids.moltenEnder, FluidValues.SLIMEBALL)
                        .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_BLOCK * 7))
                        .save(consumer, location(folder + "ender/end_crystal"));
    // it may be silky, but its still rose gold
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerModifiers.silkyCloth), TinkerFluids.moltenRoseGold, FluidValues.INGOT)
                        .save(consumer, location(metalFolder + "rose_gold/silky_cloth"));

    // durability reinforcements
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerModifiers.slimesteelReinforcement), TinkerFluids.moltenSlimesteel, FluidValues.NUGGET * 3)
                        .addByproduct(TinkerFluids.moltenObsidian.result(FluidValues.GLASS_PANE))
                        .save(consumer, location(metalFolder + "slimesteel/reinforcement"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerModifiers.obsidianReinforcement), TinkerFluids.moltenObsidian, FluidValues.GLASS_BLOCK)
                        .save(consumer, location(metalFolder + "obsidian/reinforcement"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerModifiers.cobaltReinforcement), TinkerFluids.moltenCobalt, FluidValues.INGOT)
                        .save(consumer, location(metalFolder + "cobalt/reinforcement"));

    MeltingRecipeBuilder.melting(Ingredient.of(TinkerCommons.cobaltPlatform), TinkerFluids.moltenCobalt, FluidValues.NUGGET * 10)
                        .save(consumer, location(metalFolder + "cobalt/platform"));

    // geode stuff
    crystalMelting(consumer, TinkerWorld.earthGeode, TinkerFluids.earthSlime, slimeFolder + "earth/");
    crystalMelting(consumer, TinkerWorld.skyGeode,   TinkerFluids.skySlime,   slimeFolder + "sky/");
    crystalMelting(consumer, TinkerWorld.enderGeode, TinkerFluids.enderSlime, slimeFolder + "ender/");
    // ichor is again special, melting via byproduct
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.ichorGeode), TinkerFluids.blazingBlood, FluidValues.ICHOR_BLAZING_BLOOD, 1.0f)
                        .addByproduct(TinkerFluids.ichor.result(FluidValues.ICHOR_BYPRODUCT))
                        .save(consumer, location(slimeFolder + "ichor/crystal"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.ichorGeode.getBlock()), TinkerFluids.blazingBlood, FluidValues.ICHOR_BLAZING_BLOOD * 4, 2.0f)
                        .addByproduct(TinkerFluids.ichor.result(FluidValues.ICHOR_BYPRODUCT * 4))
                        .save(consumer, location(slimeFolder + "ichor/crystal_block"));
    for (BudSize bud : BudSize.values()) {
      int size = bud.getSize();
      MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.ichorGeode.getBud(bud)), TinkerFluids.blazingBlood, FluidValues.ICHOR_BLAZING_BLOOD * size, (size + 1) / 2f)
                          .addByproduct(TinkerFluids.ichor.result(FluidValues.ICHOR_BYPRODUCT * size))
                          .setOre(OreRateType.GEM)
                          .save(consumer, location(slimeFolder + "ichor/bud_" + bud.getName()));
    }
    // clusters
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.steelCluster), TinkerFluids.moltenSteel, FluidValues.NUGGET * 4, 5/2f)
      .save(consumer, location(folder + "metal/steel/cluster"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.cobaltCluster), TinkerFluids.moltenCobalt, FluidValues.NUGGET * 4, 5/2f)
      .save(consumer, location(folder + "metal/cobalt/cluster"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.knightmetalCluster), TinkerFluids.moltenKnightmetal, FluidValues.NUGGET * 4, 5/2f)
      .save(consumer, location(folder + "metal/knightmetal/cluster"));

    // recycle saplings
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.slimeSapling.get(FoliageType.EARTH)), TinkerFluids.earthSlime, FluidValues.SLIMEBALL)
                        .save(consumer, location(slimeFolder + "earth/sapling"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.slimeSapling.get(FoliageType.SKY)), TinkerFluids.skySlime, FluidValues.SLIMEBALL)
                        .save(consumer, location(slimeFolder + "sky/sapling"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.slimeSapling.get(FoliageType.ENDER)), TinkerFluids.enderSlime, FluidValues.SLIMEBALL)
                        .save(consumer, location(slimeFolder + "ender/sapling"));

    // honey
    MeltingRecipeBuilder.melting(Ingredient.of(Items.HONEY_BLOCK), TinkerFluids.honey, FluidValues.BOTTLE * 4)
                        .save(consumer, location(slimeFolder + "honey_block"));
    // soup
    MeltingRecipeBuilder.melting(Ingredient.of(Items.BEETROOT), TinkerFluids.beetrootSoup, FluidValues.BOTTLE / 5, 1)
                        .save(consumer, location(slimeFolder + "beetroot_soup"));
    MeltingRecipeBuilder.melting(Ingredient.of(Tags.Items.MUSHROOMS), TinkerFluids.mushroomStew, FluidValues.BOTTLE / 2, 1)
                        .save(consumer, location(slimeFolder + "mushroom_stew"));

    // fuels
    MeltingFuelBuilder.fuel(new FluidStack(Fluids.LAVA, 50 * 81), 100)
                      .save(consumer, modResource(folder + "fuel/lava"));
    MeltingFuelBuilder.fuel(new FluidStack(TinkerFluids.blazingBlood.get(), 50 * 81), 150)
                      .save(consumer, modResource(folder + "fuel/blaze"));
  }


  private void addAlloyRecipes(Consumer<FinishedRecipe> consumer) {
    String folder = "smeltery/alloys/";

    // alloy recipes are in terms of ingots

    // tier 3

    // slimesteel: 1 iron + 1 skyslime + 1 seared brick = 2
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenSlimesteel.get(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenIron.getForgeTag(), FluidValues.INGOT)
                      .addInput(TinkerFluids.skySlime.getLocalTag(), FluidValues.SLIMEBALL)
                      .addInput(TinkerFluids.searedStone.getLocalTag(), FluidValues.BRICK)
                      .save(consumer, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenSlimesteel.get()), folder));

    // amethyst bronze: 1 copper + 1 amethyst = 1
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenAmethystBronze.get(), FluidValues.INGOT)
                      .addInput(TinkerFluids.moltenCopper.getForgeTag(), FluidValues.INGOT)
                      .addInput(TinkerFluids.moltenAmethyst.getLocalTag(), FluidValues.GEM)
                      .save(consumer, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenAmethystBronze.get()), folder));

    // rose gold: 1 copper + 1 gold = 2
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenRoseGold.get(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenCopper.getForgeTag(), FluidValues.INGOT)
                      .addInput(TinkerFluids.moltenGold.getForgeTag(), FluidValues.INGOT)
                      .save(consumer, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenRoseGold.get()), folder));
    // pig iron: 1 iron + 2 blood + 1 honey = 2
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenPigIron.get(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenIron.getForgeTag(), FluidValues.INGOT)
                      .addInput(TinkerFluids.blood.getLocalTag(), FluidValues.SLIMEBALL * 2)
                      .addInput(TinkerFluids.honey.getForgeTag(), FluidValues.BOTTLE)
                      .save(consumer, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenPigIron.get()), folder));
    // obsidian: 1 water + 1 lava = 2
    // note this is not a progression break, as the same tier lets you combine glass and copper for same mining level
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenObsidian.get(), FluidValues.GLASS_BLOCK / 10)
                      .addInput(Fluids.WATER, FluidConstants.BUCKET / 20)
                      .addInput(Fluids.LAVA, FluidConstants.BUCKET / 10)
                      .save(consumer, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenObsidian.get()), folder));
    // nether obsidian recipe: when water is rare, use the soup
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenObsidian.get(), FluidValues.GLASS_BLOCK / 4)
                      .addInput(TinkerFluids.mushroomStew.getForgeTag(), FluidValues.BOWL)
                      .addInput(Fluids.LAVA, FluidConstants.BUCKET / 4)
                      .save(consumer, wrap(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenObsidian.get()), folder, "_from_soup"));

    // tier 4

    // cinderslime: 1 gold + 1 ichor + 1 scorched stone = 2
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenCinderslime, FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenGold.ingredient(FluidValues.INGOT))
                      .addInput(TinkerFluids.ichor.ingredient(FluidValues.SLIMEBALL))
                      .addInput(TinkerFluids.scorchedStone.ingredient(FluidValues.BRICK))
                      .save(consumer, prefix(TinkerFluids.moltenCinderslime, folder));

    // queens slime: 1 cobalt + 1 gold + 1 magma cream = 2
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenQueensSlime.get(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenCobalt.getForgeTag(), FluidValues.INGOT)
                      .addInput(TinkerFluids.moltenGold.getForgeTag(), FluidValues.INGOT)
                      .addInput(TinkerFluids.magma.getForgeTag(), FluidValues.SLIMEBALL)
                      .save(consumer, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenQueensSlime.get()), folder));

    // manyullyn: 3 cobalt + 1 debris = 3
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenManyullyn.get(), FluidValues.INGOT * 4)
                      .addInput(TinkerFluids.moltenCobalt.getForgeTag(), FluidValues.INGOT * 3)
                      .addInput(TinkerFluids.moltenDebris.getLocalTag(), FluidValues.INGOT)
                      .save(consumer, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenManyullyn.get()), folder));

    // heptazion: 2 copper + 1 cobalt + 1/4 obsidian = 2
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenHepatizon.get(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenCopper.getForgeTag(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenCobalt.getForgeTag(), FluidValues.INGOT)
                      .addInput(TinkerFluids.moltenQuartz.getLocalTag(), FluidValues.GEM * 4)
                      .save(consumer, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenHepatizon.get()), folder));

    // netherite: 4 debris + 4 gold = 1 (why is this so dense vanilla?)
    ConditionalRecipe.builder()
                     .addCondition(ConfigEnabledCondition.CHEAPER_NETHERITE_ALLOY)
                     .addRecipe(
                       AlloyRecipeBuilder.alloy(TinkerFluids.moltenNetherite, FluidValues.NUGGET)
                                         .addInput(TinkerFluids.moltenDebris.ingredient(FluidValues.NUGGET * 4))
                                         .addInput(TinkerFluids.moltenGold.ingredient(FluidValues.NUGGET * 2))::save)
                     .addCondition(TrueCondition.INSTANCE) // fallback
                     .addRecipe(
                       AlloyRecipeBuilder.alloy(TinkerFluids.moltenNetherite.get(), FluidValues.NUGGET)
                                         .addInput(TinkerFluids.moltenDebris.getLocalTag(), FluidValues.NUGGET * 4)
                                         .addInput(TinkerFluids.moltenGold.getForgeTag(), FluidValues.NUGGET * 4)::save)
                     .build(consumer, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenNetherite.get()), folder));

    // knightslime: 1 cobalt + 1 enderslime + 1 obsidian = 2
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenKnightslime, FluidValues.INGOT * 2)
      .addInput(TinkerFluids.moltenCobalt.ingredient(FluidValues.INGOT))
      .addInput(TinkerFluids.enderSlime.ingredient(FluidValues.SLIMEBALL))
      .addInput(TinkerFluids.moltenObsidian.ingredient(FluidValues.BRICK))
      .save(consumer, prefix(TinkerFluids.moltenKnightslime, folder));


    // tier 3 compat
    Consumer<FinishedRecipe> wrapped;

    // bronze
    wrapped = withCondition(consumer, tagCondition("bronze_ingots"), tagCondition("tin_ingots"));
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenBronze.get(), FluidValues.INGOT * 4)
                      .addInput(TinkerFluids.moltenCopper.getForgeTag(), FluidValues.INGOT * 3)
                      .addInput(TinkerFluids.moltenTin.getForgeTag(), FluidValues.INGOT)
                      .save(wrapped, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenBronze.get()), folder));

    // brass
    wrapped = withCondition(consumer, tagCondition("brass_ingots"), tagCondition("zinc_ingots"));
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenBrass.get(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenCopper.getForgeTag(), FluidValues.INGOT)
                      .addInput(TinkerFluids.moltenZinc.getForgeTag(), FluidValues.INGOT)
                      .save(wrapped, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenBrass.get()), folder));

    // electrum
    wrapped = withCondition(consumer, tagCondition("electrum_ingots"), tagCondition("silver_ingots"));
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenElectrum.get(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenGold.getForgeTag(), FluidValues.INGOT)
                      .addInput(TinkerFluids.moltenSilver.getForgeTag(), FluidValues.INGOT)
                      .save(wrapped, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenElectrum.get()), folder));

    // invar
    wrapped = withCondition(consumer, tagCondition("invar_ingots"), tagCondition("nickel_ingots"));
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenInvar.get(), FluidValues.INGOT * 3)
                      .addInput(TinkerFluids.moltenIron.getForgeTag(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenNickel.getForgeTag(), FluidValues.INGOT)
                      .save(wrapped, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenInvar.get()), folder));

    // constantan
    wrapped = withCondition(consumer, tagCondition("constantan_ingots"), tagCondition("nickel_ingots"));
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenConstantan.get(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenCopper.getForgeTag(), FluidValues.INGOT)
                      .addInput(TinkerFluids.moltenNickel.getForgeTag(), FluidValues.INGOT)
                      .save(wrapped, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenConstantan.get()), folder));

    // pewter
    wrapped = withCondition(consumer, tagCondition("pewter_ingots"), tagCondition("lead_ingots"));
    ConditionalRecipe.builder()
                     // when available, alloy pewter with tin
                     // we mainly add it to support Edilon which uses iron to reduce ores, but the author thinks tin is fine balance wise
                     .addCondition(tagCondition("tin_ingots"))
                     .addRecipe(
                       // ratio from Allomancy mod
                       AlloyRecipeBuilder.alloy(TinkerFluids.moltenPewter.get(), FluidValues.INGOT * 3)
                                         .addInput(TinkerFluids.moltenTin.getForgeTag(), FluidValues.INGOT * 2)
                                         .addInput(TinkerFluids.moltenLead.getForgeTag(), FluidValues.INGOT)::save)
                     .addCondition(TrueCondition.INSTANCE) // fallback
                     .addRecipe(
                       // ratio from Edilon mod
                       AlloyRecipeBuilder.alloy(TinkerFluids.moltenPewter.get(), FluidValues.INGOT * 2)
                                         .addInput(TinkerFluids.moltenIron.getForgeTag(), FluidValues.INGOT)
                                         .addInput(TinkerFluids.moltenLead.getForgeTag(), FluidValues.INGOT)::save)
                     .build(wrapped, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenPewter.get()), folder));

    // thermal alloys
    Function<String,ConditionJsonProvider> fluidTagLoaded = name -> DefaultResourceConditions.fluidTagsPopulated(TagKey.create(Registries.FLUID, new ResourceLocation("c", name)));
    Function<String,TagKey<Fluid>> fluidTag = name -> TagKey.create(Registries.FLUID, new ResourceLocation("c", name));
    // enderium
    wrapped = withCondition(consumer, tagCondition("enderium_ingots"), tagCondition("lead_ingots"));
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenEnderium.get(), FluidValues.INGOT * 2)
                      .addInput(TinkerFluids.moltenLead.getForgeTag(), FluidValues.INGOT * 3)
                      .addInput(TinkerFluids.moltenDiamond.getLocalTag(), FluidValues.GEM)
                      .addInput(TinkerFluids.moltenEnder.getForgeTag(), FluidValues.SLIMEBALL * 2)
                      .save(wrapped, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenEnderium.get()), folder));
    // lumium
    wrapped = withCondition(consumer, tagCondition("lumium_ingots"), tagCondition("tin_ingots"), tagCondition("silver_ingots"), fluidTagLoaded.apply("glowstone"));
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenLumium.get(), FluidValues.INGOT * 4)
                      .addInput(TinkerFluids.moltenTin.getForgeTag(), FluidValues.INGOT * 3)
                      .addInput(TinkerFluids.moltenSilver.getForgeTag(), FluidValues.INGOT)
                      .addInput(FluidIngredient.of(fluidTag.apply("glowstone"), FluidValues.SLIMEBALL * 2))
                      .save(wrapped, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenLumium.get()), folder));
    // signalum
    wrapped = withCondition(consumer, tagCondition("signalum_ingots"), tagCondition("copper_ingots"), tagCondition("silver_ingots"), fluidTagLoaded.apply("redstone"));
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenSignalum.get(), FluidValues.INGOT * 4)
                      .addInput(TinkerFluids.moltenCopper.getForgeTag(), FluidValues.INGOT * 3)
                      .addInput(TinkerFluids.moltenSilver.getForgeTag(), FluidValues.INGOT)
                      .addInput(FluidIngredient.of(fluidTag.apply("redstone"), 400))
                      .save(wrapped, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenSignalum.get()), folder));

    // refined obsidian, note glowstone is done as a composite
    wrapped = withCondition(consumer, tagCondition("refined_obsidian_ingots"), tagCondition("osmium_ingots"));
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenRefinedObsidian.get(), FluidValues.INGOT)
                      .addInput(TinkerFluids.moltenObsidian.getLocalTag(), FluidValues.GLASS_PANE)
                      .addInput(TinkerFluids.moltenDiamond.getLocalTag(), FluidValues.GEM)
                      .addInput(TinkerFluids.moltenOsmium.getForgeTag(), FluidValues.INGOT)
                      .save(wrapped, prefix(BuiltInRegistries.FLUID.getKey(TinkerFluids.moltenRefinedObsidian.get()), folder));
  }

  private void addEntityMeltingRecipes(Consumer<FinishedRecipe> consumer) {
    String folder = "smeltery/entity_melting/";
    String headFolder = "smeltery/entity_melting/heads/";

    // zombies give less blood, they lost a lot already
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOGLIN, EntityType.ZOMBIE_HORSE),
                                       new FluidStack(TinkerFluids.blood.get(), FluidValues.SLIMEBALL / 10), 2)
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ZOMBIE), folder));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.ZOMBIE_HEAD, TinkerWorld.heads.get(TinkerHeadType.HUSK), Blocks.PIGLIN_HEAD, TinkerWorld.heads.get(TinkerHeadType.PIGLIN_BRUTE), TinkerWorld.heads.get(TinkerHeadType.ZOMBIFIED_PIGLIN)), TinkerFluids.blood.get(), FluidValues.SLIMEBALL * 2)
                        .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ZOMBIE), headFolder));
    // drowned are weird, there is water flowing through their veins
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.DROWNED),
                                       new FluidStack(Fluids.WATER, FluidConstants.BUCKET / 50), 2)
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.DROWNED), folder));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.heads.get(TinkerHeadType.DROWNED)), Fluids.WATER, FluidConstants.BUCKET / 4)
                        .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.DROWNED), headFolder));
    // melt spiders into venom
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.SPIDER, EntityType.CAVE_SPIDER),
                                       new FluidStack(TinkerFluids.venom.get(), FluidValues.BOTTLE / 10), 2)
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.SPIDER), folder));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.heads.get(TinkerHeadType.SPIDER), TinkerWorld.heads.get(TinkerHeadType.CAVE_SPIDER)), TinkerFluids.venom.get(), FluidValues.SLIMEBALL * 2)
                        .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.SPIDER), headFolder));

    // creepers are based on explosives, tnt is explosive, tnt is made from sand, sand melts into glass. therefore, creepers melt into glass
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.CREEPER),
                                       new FluidStack(TinkerFluids.moltenGlass.get(), FluidValues.GLASS_BLOCK / 20), 2)
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.CREEPER), folder));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.CREEPER_HEAD), TinkerFluids.moltenGlass.get(), FluidConstants.BUCKET / 4)
                        .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.CREEPER), headFolder));

    // melt skeletons to get the milk out
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityIngredient.of(EntityTypeTags.SKELETONS), EntityIngredient.of(EntityType.SKELETON_HORSE)),
                                       new FluidStack(Milk.STILL_MILK, FluidConstants.BUCKET / 10))
                              .save(consumer, modResource(folder + "skeletons"));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, TinkerWorld.heads.get(TinkerHeadType.STRAY)), Milk.STILL_MILK, FluidConstants.BUCKET / 4)
                        .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.SKELETON), headFolder));

    // slimes melt into slime, shocker
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.SLIME, TinkerWorld.earthSlimeEntity.get()), new FluidStack(TinkerFluids.earthSlime.get(), FluidValues.SLIMEBALL / 10))
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.SLIME), folder));
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(TinkerWorld.skySlimeEntity.get()), new FluidStack(TinkerFluids.skySlime.get(), FluidValues.SLIMEBALL / 10))
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(TinkerWorld.skySlimeEntity.get()), folder));
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(TinkerWorld.enderSlimeEntity.get()), new FluidStack(TinkerFluids.enderSlime.get(), FluidValues.SLIMEBALL / 10))
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(TinkerWorld.enderSlimeEntity.get()), folder));
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(TinkerWorld.terracubeEntity.get()), new FluidStack(TinkerFluids.moltenClay.get(), FluidValues.SLIMEBALL / 10))
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(TinkerWorld.terracubeEntity.get()), folder));
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.MAGMA_CUBE), new FluidStack(TinkerFluids.magma.get(), FluidValues.SLIMEBALL / 10))
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.MAGMA_CUBE), folder));
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.BEE), new FluidStack(TinkerFluids.honey.get(), FluidValues.BOTTLE / 10))
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BEE), folder));

    // iron golems can be healed using an iron ingot 25 health
    // 4 * 9 gives 36, which is larger
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.IRON_GOLEM), new FluidStack(TinkerFluids.moltenIron.get(), FluidValues.NUGGET), 4)
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.IRON_GOLEM), folder));
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.SNOW_GOLEM), new FluidStack(Fluids.WATER, FluidConstants.BUCKET / 10))
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.SNOW_GOLEM), folder));

    // "melt" blazes to get fuel
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.BLAZE), new FluidStack(TinkerFluids.blazingBlood.get(), FluidConstants.BUCKET / 50), 2)
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BLAZE), folder));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.heads.get(TinkerHeadType.BLAZE)), new FluidStack(TinkerFluids.blazingBlood.get(), FluidConstants.BUCKET / 10), 1000, IMeltingRecipe.calcTime(1500, 1.0f))
                        .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BLAZE), headFolder));

    // guardians are rock, seared stone is rock, don't think about it too hard
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN), new FluidStack(TinkerFluids.searedStone.get(), FluidValues.BRICK / 5), 4)
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.GUARDIAN), folder));
    // silverfish also seem like rock, sorta?
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.SILVERFISH), new FluidStack(TinkerFluids.searedStone.get(), FluidValues.BRICK / 5), 2)
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.SILVERFISH), folder));

    // villagers melt into emerald, but they die quite quick
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(TinkerTags.EntityTypes.VILLAGERS),
                                       new FluidStack(TinkerFluids.moltenEmerald.get(), FluidValues.GEM_SHARD), 5)
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.VILLAGER), folder));
    // illagers are more resistant, they resist the villager culture afterall
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(TinkerTags.EntityTypes.ILLAGERS), TinkerFluids.moltenEmerald.result(FluidValues.GEM_SHARD), 2)
                              .save(consumer, location(folder + "illager"));

    // melt ender for the molten ender
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.ENDER_DRAGON),
                                       new FluidStack(TinkerFluids.moltenEnder.get(), FluidValues.SLIMEBALL / 10), 2)
                              .save(consumer, modResource(folder + "ender"));
    MeltingRecipeBuilder.melting(Ingredient.of(TinkerWorld.heads.get(TinkerHeadType.ENDERMAN)), TinkerFluids.moltenEnder.get(), FluidValues.SLIMEBALL * 2)
                        .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ENDERMAN), headFolder));
    MeltingRecipeBuilder.melting(Ingredient.of(Items.DRAGON_HEAD), TinkerFluids.moltenEnder.get(), FluidValues.SLIMEBALL * 4)
                        .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ENDER_DRAGON), headFolder));

    // if you can get him to stay, wither is a source of free liquid soul
    EntityMeltingRecipeBuilder.melting(EntityIngredient.of(EntityType.WITHER),
                                       new FluidStack(TinkerFluids.liquidSoul.get(), FluidValues.GLASS_BLOCK / 20), 2)
                              .save(consumer, prefix(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.WITHER), folder));
  }

  private void addCompatRecipes(Consumer<FinishedRecipe> consumer) {
    String folder = "compat/";
    // create - cast andesite alloy
    ItemOutput andesiteAlloy = ItemNameOutput.fromName(new ResourceLocation("create", "andesite_alloy"));
    Consumer<FinishedRecipe> createConsumer = withCondition(consumer, DefaultResourceConditions.allModsLoaded("create"));
    ItemCastingRecipeBuilder.basinRecipe(andesiteAlloy)
                            .setCast(Blocks.ANDESITE, true)
                            .setFluidAndTime(TinkerFluids.moltenIron, FluidValues.NUGGET)
                            .save(createConsumer, location(folder + "create/andesite_alloy_iron"));
    ItemCastingRecipeBuilder.basinRecipe(andesiteAlloy)
                            .setCast(Blocks.ANDESITE, true)
                            .setFluidAndTime(TinkerFluids.moltenZinc, FluidValues.NUGGET)
                            .save(createConsumer, location(folder + "create/andesite_alloy_zinc"));

    // immersive engineering - casting treated wood
    String treatedWood = "treated_wood";
    TagKey<Fluid> creosote = getFluidTag(COMMON, "creosote");
    ItemCastingRecipeBuilder.basinRecipe(ItemOutput.fromTag(getItemTag(COMMON, treatedWood)))
                            .setCast(ItemTags.PLANKS, true)
                            .setFluid(TagKey.create(Registries.FLUID, new ResourceLocation("c", "creosote")), 125)
                            .setCoolingTime(100)
                            .save(withCondition(consumer, DefaultResourceConditions.allModsLoaded("immersiveengineering")), modResource(folder + "immersiveengineering/treated_wood"));

    // ceramics compat: a lot of melting and some casting

    // ceramics constants //
    // normally its 1/8 of a bucket per lava (125mb), but we give a small discount on casting to make the slab math work out nicer (100 is divisible by 2)
    int lavaPerBlock = FluidType.BUCKET_VOLUME / 10;
    // normally a quarter of a glass pane, but thats 62.5, so round down to 50 for a nice number
    int gaugeGlass = FluidValues.GLASS_PANE / 5;
    // normally its 1 ingot per 8, we do 1 nugget giving a small discount
    int goldPerBlock = FluidValues.NUGGET;

    // ID helpers
    String ceramics = "ceramics";
    String ceramicsFolder = folder + ceramics + "/";
    Function<String,ResourceLocation> ceramicsId = name -> new ResourceLocation(ceramics, name);
    Function<String,TagKey<Item>> ceramicsTag = name -> TagKey.create(Registries.ITEM, new ResourceLocation(ceramics, name));
    Consumer<FinishedRecipe> ceramicsConsumer = withCondition(consumer, DefaultResourceConditions.allModsLoaded(ceramics));

    // fill clay and cracked clay buckets
    ContainerFillingRecipeBuilder.tableRecipe(ceramicsId.apply("clay_bucket"), FluidConstants.BUCKET)
                                 .save(ceramicsConsumer, modResource(ceramicsFolder + "filling_clay_bucket"));
    ContainerFillingRecipeBuilder.tableRecipe(ceramicsId.apply("cracked_clay_bucket"), FluidConstants.BUCKET)
                                 .save(ceramicsConsumer, modResource(ceramicsFolder + "filling_cracked_clay_bucket"));

    // porcelain for ceramics
    AlloyRecipeBuilder.alloy(TinkerFluids.moltenPorcelain, FluidValues.BRICK * 4)
                      .addInput(TinkerFluids.moltenClay.getTag(), FluidValues.BRICK * 3)
                      .addInput(TinkerFluids.moltenQuartz.getTag(), FluidValues.GEM)
                      .save(ceramicsConsumer, location(ceramicsFolder + "alloy_porcelain"));

    // melting clay
    String clayFolder = ceramicsFolder + "clay/";

    // unfired clay
    MeltingRecipeBuilder.melting(ceramicsItem.apply("unfired_clay_plate"), TinkerFluids.moltenClay, FluidValues.BRICK, 0.5f)
      .save(ceramicsConsumer, location(clayFolder + "clay_1"));
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(ceramicsId.apply("clay_faucet"), ceramicsId.apply("clay_channel")), TinkerFluids.moltenClay, FluidValues.BRICK * 2, 0.65f)
      .save(ceramicsConsumer, location(clayFolder + "clay_2"));
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(ceramicsId.apply("unfired_clay_bucket"), ceramicsId.apply("clay_cistern")), TinkerFluids.moltenClay, FluidValues.BRICK * 3, 0.9f)
      .save(ceramicsConsumer, location(clayFolder + "clay_3"));

    // 2 bricks
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(
        ceramicsId.apply("dark_bricks_slab"), ceramicsId.apply("dragon_bricks_slab"),
        ceramicsId.apply("terracotta_faucet"), ceramicsId.apply("terracotta_channel")
      ), TinkerFluids.moltenClay, FluidValues.BRICK * 2, 1.33f)
      .save(ceramicsConsumer, location(clayFolder + "bricks_2"));
    // 3 bricks
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.any(
      Ingredient.of(ceramicsTag.apply("terracotta_cisterns")),
      NBTNameIngredient.from(ceramicsId.apply("clay_bucket")),
      NBTNameIngredient.from(ceramicsId.apply("cracked_clay_bucket"))),
                                 new FluidStack(TinkerFluids.moltenClay.get(), FluidValues.SLIMEBALL * 3), 1.67f)
                        .save(ceramicsConsumer, modResource(clayFolder + "bricks_3"));
    // 4 bricks
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(
      ceramicsId.apply("dark_bricks"), ceramicsId.apply("dark_bricks_stairs"), ceramicsId.apply("dark_bricks_wall"),
      ceramicsId.apply("dragon_bricks"), ceramicsId.apply("dragon_bricks_stairs"), ceramicsId.apply("dragon_bricks_wall")
    ), TinkerFluids.moltenClay, FluidValues.BRICK * 4, 2.0f)
      .save(ceramicsConsumer, location(clayFolder + "block"));
    MeltingRecipeBuilder.melting(ceramicsItem.apply("kiln"), TinkerFluids.moltenClay, FluidValues.BRICK_BLOCK * 3 + FluidValues.BRICK * 5, 4.0f)
      .save(ceramicsConsumer, location(clayFolder + "kiln"));

    // lava bricks, lava byproduct
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(ceramicsId.apply("lava_bricks_slab")), new FluidStack(TinkerFluids.moltenClay.get(), FluidValues.SLIMEBALL * 2), 1.33f)
                        .addByproduct(new FluidStack(Fluids.LAVA, FluidConstants.BUCKET / 20))
                        .save(ceramicsConsumer, modResource(clayFolder + "lava_bricks_slab"));
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(
      ceramicsId.apply("lava_bricks"), ceramicsId.apply("lava_bricks_stairs"), ceramicsId.apply("lava_bricks_wall")
    ), new FluidStack(TinkerFluids.moltenClay.get(), FluidValues.SLIMEBALL * 4), 2f)
                        .addByproduct(new FluidStack(Fluids.LAVA, FluidConstants.BUCKET / 10))
                        .save(ceramicsConsumer, modResource(clayFolder + "lava_bricks_block"));
    // gauge, partially glass
    MeltingRecipeBuilder.melting(ceramicsItem.apply("terracotta_gauge"), TinkerFluids.moltenClay, FluidValues.BRICK, 1f)
      .addByproduct(TinkerFluids.moltenGlass.result(gaugeGlass))
      .save(ceramicsConsumer, location(clayFolder + "gauge"));

    // clay armor
    int brickPart = FluidValues.BRICK / 5;
    MeltingRecipeBuilder.melting(ceramicsItem.apply("clay_helmet"), TinkerFluids.moltenClay, FluidValues.BRICK * 5, 2.25f)
      .setDamagable(brickPart)
      .save(ceramicsConsumer, location(clayFolder + "clay_helmet"));
    MeltingRecipeBuilder.melting(ceramicsItem.apply("clay_chestplate"), TinkerFluids.moltenClay, FluidValues.BRICK * 8, 3f)
      .setDamagable(brickPart)
      .save(ceramicsConsumer, location(clayFolder + "clay_chestplate"));
    MeltingRecipeBuilder.melting(ceramicsItem.apply("clay_leggings"), TinkerFluids.moltenClay, FluidValues.BRICK * 7, 2.75f)
      .setDamagable(brickPart)
      .save(ceramicsConsumer, location(clayFolder + "clay_leggings"));
    MeltingRecipeBuilder.melting(ceramicsItem.apply("clay_boots"), TinkerFluids.moltenClay, FluidValues.BRICK * 4, 2f)
      .setDamagable(brickPart)
      .save(ceramicsConsumer, location(clayFolder + "clay_boots"));

    // melting porcelain
    String porcelainFolder = ceramicsFolder + "porcelain/";
    // unfired
    MeltingRecipeBuilder.melting(ceramicsItem.apply("unfired_porcelain"), TinkerFluids.moltenPorcelain, FluidValues.BRICK, 0.5f)
      .save(ceramicsConsumer, location(porcelainFolder + "unfired_1"));
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(ceramicsId.apply("unfired_faucet"), ceramicsId.apply("unfired_channel")), TinkerFluids.moltenPorcelain, FluidValues.BRICK * 2, 0.65f)
      .save(ceramicsConsumer, location(porcelainFolder + "unfired_2"));
    MeltingRecipeBuilder.melting(ceramicsItem.apply("unfired_cistern"), TinkerFluids.moltenPorcelain, FluidValues.BRICK * 3, 0.9f)
      .save(ceramicsConsumer, location(porcelainFolder + "unfired_3"));
    MeltingRecipeBuilder.melting(ceramicsItem.apply("unfired_porcelain_block"), TinkerFluids.moltenPorcelain, FluidValues.BRICK_BLOCK, 1f)
      .save(ceramicsConsumer, location(porcelainFolder + "unfired_4"));

    // 1 brick
    MeltingRecipeBuilder.melting(ceramicsItem.apply("porcelain_brick"), TinkerFluids.moltenPorcelain, FluidValues.BRICK, 1f)
      .save(ceramicsConsumer, location(porcelainFolder + "bricks_1"));
    // 2 bricks
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(
      ceramicsId.apply("porcelain_bricks_slab"), ceramicsId.apply("monochrome_bricks_slab"), ceramicsId.apply("marine_bricks_slab"), ceramicsId.apply("rainbow_bricks_slab"),
      ceramicsId.apply("porcelain_faucet"), ceramicsId.apply("porcelain_channel")
    ), TinkerFluids.moltenPorcelain, FluidValues.BRICK * 2, 1.33f)
      .save(ceramicsConsumer, location(porcelainFolder + "bricks_2"));
    // 3 bricks
    MeltingRecipeBuilder.melting(ceramicsTag.apply("porcelain_cisterns"), TinkerFluids.moltenPorcelain, FluidValues.BRICK * 3, 1.67f)
      .save(ceramicsConsumer, location(porcelainFolder + "bricks_3"));
    // 4 bricks
    MeltingRecipeBuilder.melting(DefaultCustomIngredients.any(
      Ingredient.of(ceramicsTag.apply("porcelain_block")),
      Ingredient.of(ceramicsTag.apply("rainbow_porcelain")),
      ItemNameIngredient.from(
        ceramicsId.apply("porcelain_bricks"), ceramicsId.apply("porcelain_bricks_stairs"), ceramicsId.apply("porcelain_bricks_wall"),
        ceramicsId.apply("monochrome_bricks"), ceramicsId.apply("monochrome_bricks_stairs"), ceramicsId.apply("monochrome_bricks_wall"),
        ceramicsId.apply("marine_bricks"), ceramicsId.apply("marine_bricks_stairs"), ceramicsId.apply("marine_bricks_wall"),
        ceramicsId.apply("rainbow_bricks"), ceramicsId.apply("rainbow_bricks_stairs"), ceramicsId.apply("rainbow_bricks_wall")
      )), TinkerFluids.moltenPorcelain, FluidValues.BRICK * 4, 2.0f)
      .save(ceramicsConsumer, location(porcelainFolder + "blocks"));

    // gold bricks
    MeltingRecipeBuilder.melting(ceramicsItem.apply("golden_bricks_slab"), TinkerFluids.moltenPorcelain, FluidValues.BRICK * 2, 1.33f)
      .addByproduct(TinkerFluids.moltenGold.result(goldPerBlock / 2))
      .save(ceramicsConsumer, location(porcelainFolder + "golden_bricks_slab"));
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(
      ceramicsId.apply("golden_bricks"), ceramicsId.apply("golden_bricks_stairs"), ceramicsId.apply("golden_bricks_wall")
    ), TinkerFluids.moltenPorcelain, FluidValues.BRICK * 4, 2f)
      .addByproduct(TinkerFluids.moltenGold.result(goldPerBlock))
      .save(ceramicsConsumer, location(porcelainFolder + "golden_bricks_block"));

    // gauge, partially glass
    MeltingRecipeBuilder.melting(ceramicsItem.apply("porcelain_gauge"), TinkerFluids.moltenPorcelain, FluidValues.BRICK, 1f)
      .addByproduct(TinkerFluids.moltenGlass.result(gaugeGlass))
      .save(ceramicsConsumer, location(porcelainFolder + "gauge"));

    // casting bricks
    String castingFolder = ceramicsFolder + "casting/";
    castingWithCast(ceramicsConsumer, TinkerFluids.moltenPorcelain, FluidValues.BRICK, TinkerSmeltery.ingotCast, ceramicsOutput.apply("porcelain_brick"), castingFolder + "porcelain_brick");
    ItemCastingRecipeBuilder.basinRecipe(ItemNameOutput.fromName(ceramicsId.apply("white_porcelain")))
      .setFluidAndTime(TinkerFluids.moltenPorcelain, FluidValues.SLIME_CONGEALED)
      .save(ceramicsConsumer, location(castingFolder + "porcelain"));

    // lava bricks
    ItemCastingRecipeBuilder.basinRecipe(ItemNameOutput.fromName(ceramicsId.apply("lava_bricks")))
                            .setCast(Blocks.BRICKS, true)
                            .setFluidAndTime(new FluidStack(Fluids.LAVA, FluidConstants.BUCKET / 10))
                            .save(ceramicsConsumer, modResource(castingFolder + "lava_bricks"));
    ItemCastingRecipeBuilder.basinRecipe(ItemNameOutput.fromName(ceramicsId.apply("lava_bricks_slab")))
                            .setCast(Blocks.BRICK_SLAB, true)
                            .setFluidAndTime(new FluidStack(Fluids.LAVA, FluidConstants.BUCKET / 20))
                            .save(ceramicsConsumer, modResource(castingFolder + "lava_bricks_slab"));
    ItemCastingRecipeBuilder.basinRecipe(ItemNameOutput.fromName(ceramicsId.apply("lava_bricks_stairs")))
                            .setCast(Blocks.BRICK_STAIRS, true)
                            .setFluidAndTime(new FluidStack(Fluids.LAVA, FluidConstants.BUCKET / 10))
                            .save(ceramicsConsumer, modResource(castingFolder + "lava_bricks_stairs"));
    ItemCastingRecipeBuilder.basinRecipe(ItemNameOutput.fromName(ceramicsId.apply("lava_bricks_wall")))
                            .setCast(Blocks.BRICK_WALL, true)
                            .setFluidAndTime(new FluidStack(Fluids.LAVA, FluidConstants.BUCKET / 10))
                            .save(ceramicsConsumer, modResource(castingFolder + "lava_bricks_wall"));

    // golden bricks
    ItemCastingRecipeBuilder.basinRecipe(ceramicsOutput.apply("golden_bricks"))
      .setCast(ceramicsItem.apply("porcelain_bricks"), true)
      .setFluidAndTime(TinkerFluids.moltenGold, goldPerBlock)
      .save(ceramicsConsumer, location(castingFolder + "golden_bricks"));
    ItemCastingRecipeBuilder.basinRecipe(ceramicsOutput.apply("golden_bricks_slab"))
      .setCast(ceramicsItem.apply("porcelain_bricks_slab"), true)
      .setFluidAndTime(TinkerFluids.moltenGold, goldPerBlock / 2)
      .save(ceramicsConsumer, location(castingFolder + "golden_bricks_slab"));
    ItemCastingRecipeBuilder.basinRecipe(ceramicsOutput.apply("golden_bricks_stairs"))
      .setCast(ceramicsItem.apply("porcelain_bricks_stairs"), true)
      .setFluidAndTime(TinkerFluids.moltenGold, goldPerBlock)
      .save(ceramicsConsumer, location(castingFolder + "golden_bricks_stairs"));
    ItemCastingRecipeBuilder.basinRecipe(ceramicsOutput.apply("golden_bricks_wall"))
      .setCast(ceramicsItem.apply("porcelain_bricks_wall"), true)
      .setFluidAndTime(TinkerFluids.moltenGold, goldPerBlock)
      .save(ceramicsConsumer, location(castingFolder + "golden_bricks_wall"));

    // refined glowstone composite
    Consumer<FinishedRecipe> wrapped = withCondition(consumer, tagCondition("refined_glowstone_ingots"), tagCondition("osmium_ingots"));
    ItemCastingRecipeBuilder.tableRecipe(ItemOutput.fromTag(getItemTag("c", "refined_glowstone_ingots"), 1))
                            .setCast(Tags.Items.DUSTS_GLOWSTONE, true)
                            .setFluidAndTime(TinkerFluids.moltenOsmium, FluidValues.INGOT)
                            .save(wrapped, modResource(folder + "refined_glowstone_ingot"));
    wrapped = withCondition(consumer, tagCondition("refined_obsidian_ingots"), tagCondition("osmium_ingots"));
    ItemCastingRecipeBuilder.tableRecipe(ItemOutput.fromTag(getItemTag("c", "refined_obsidian_ingots"), 1))
                            .setCast(getItemTag("c", "refined_obsidian_dusts"), true)
                            .setFluidAndTime(TinkerFluids.moltenOsmium, FluidValues.INGOT)
                            .save(wrapped, location(folder + "refined_obsidian_ingot"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerMaterials.necroniumBone)
                            .setFluidAndTime(TinkerFluids.moltenUranium, FluidValues.INGOT)
                            .setCast(TinkerTags.Items.WITHER_BONES, true)
                            .save(withCondition(consumer, tagCondition("uranium_ingots")), modResource(folder + "necronium_bone"));
  }


  /* Seared casting */

  /**
   * Adds a stonecutting recipe with automatic name and criteria
   * @param consumer  Recipe consumer
   * @param output    Recipe output
   * @param folder    Recipe folder path
   */
  private void searedStonecutter(Consumer<FinishedRecipe> consumer, ItemLike output, String folder) {
    SingleItemRecipeBuilder.stonecutting(
      DefaultCustomIngredients.any(
        Ingredient.of(TinkerSmeltery.searedStone),
        DefaultCustomIngredients.difference(Ingredient.of(TinkerTags.Items.SEARED_BRICKS), Ingredient.of(output))), RecipeCategory.MISC, output, 1)
                           .unlockedBy("has_stone", has(TinkerSmeltery.searedStone))
                           .unlockedBy("has_bricks", has(TinkerTags.Items.SEARED_BRICKS))
                           .save(consumer, wrap(id(output), folder, "_stonecutting"));
  }

  /**
   * Adds a recipe to create the given seared block using molten clay on stone
   * @param consumer  Recipe consumer
   * @param block     Output block
   * @param cast      Cast item
   * @param location  Recipe location
   */
  private void searedCasting(Consumer<FinishedRecipe> consumer, ItemLike block, Ingredient cast, String location) {
    searedCasting(consumer, block, cast, FluidValues.SLIMEBALL * 2, location);
  }

  /**
   * Adds a recipe to create the given seared slab block using molten clay on stone
   * @param consumer  Recipe consumer
   * @param block     Output block
   * @param cast      Cast item
   * @param location  Recipe location
   */
  private void searedSlabCasting(Consumer<FinishedRecipe> consumer, ItemLike block, Ingredient cast, String location) {
    searedCasting(consumer, block, cast, FluidValues.SLIMEBALL, location);
  }

  /**
   * Adds a recipe to create the given seared block using molten clay on stone
   * @param consumer  Recipe consumer
   * @param block     Output block
   * @param cast      Cast item
   * @param amount    Amount of fluid needed
   * @param location  Recipe location
   */
  private void searedCasting(Consumer<FinishedRecipe> consumer, ItemLike block, Ingredient cast, int amount, String location) {
    ItemCastingRecipeBuilder.basinRecipe(block)
                            .setFluidAndTime(TinkerFluids.moltenClay, amount)
                            .setCast(cast, true)
                            .save(consumer, location(location));
  }


  /* Scorched casting */

  /**
   * Adds a stonecutting recipe with automatic name and criteria
   * @param consumer  Recipe consumer
   * @param output    Recipe output
   * @param folder    Recipe folder path
   */
  private void scorchedStonecutter(Consumer<FinishedRecipe> consumer, ItemLike output, String folder) {
    SingleItemRecipeBuilder.stonecutting(DefaultCustomIngredients.difference(Ingredient.of(TinkerTags.Items.SCORCHED_BLOCKS), Ingredient.of(output)), RecipeCategory.MISC, output, 1)
                           .unlockedBy("has_block", has(TinkerTags.Items.SCORCHED_BLOCKS))
                           .save(consumer, wrap(id(output), folder, "_stonecutting"));
  }

  /**
   * Adds a recipe to create the given seared block using molten clay on stone
   * @param consumer  Recipe consumer
   * @param block     Output block
   * @param cast      Cast item
   * @param location  Recipe location
   */
  private void scorchedCasting(Consumer<FinishedRecipe> consumer, ItemLike block, Ingredient cast, String location) {
    scorchedCasting(consumer, block, cast, FluidValues.SLIMEBALL * 2, location);
  }

  /**
   * Adds a recipe to create the given seared block using molten clay on stone
   * @param consumer  Recipe consumer
   * @param block     Output block
   * @param cast      Cast item
   * @param amount    Amount of fluid needed
   * @param location  Recipe location
   */
  private void scorchedCasting(Consumer<FinishedRecipe> consumer, ItemLike block, Ingredient cast, int amount, String location) {
    ItemCastingRecipeBuilder.basinRecipe(block)
                            .setFluidAndTime(TinkerFluids.magma, amount)
                            .setCast(cast, true)
                            .save(consumer, location(location));
  }


  /* Casting */

  /**
   * Adds melting recipes for slime
   * @param consumer  Consumer
   * @param fluid     Fluid
   * @param type      Slime type
   * @param folder    Output folder
   */
  private void slimeMelting(Consumer<FinishedRecipe> consumer, FluidObject<?> fluid, SlimeType type, String folder) {
    String slimeFolder = folder + type.getSerializedName() + "/";
    MeltingRecipeBuilder.melting(Ingredient.of(type.getSlimeballTag()), fluid, FluidValues.SLIMEBALL, 1.0f)
                        .save(consumer, location(slimeFolder + "ball"));
    ItemLike item = TinkerWorld.congealedSlime.get(type);
    MeltingRecipeBuilder.melting(Ingredient.of(item), fluid, FluidValues.SLIME_CONGEALED, 2.0f)
                        .save(consumer, location(slimeFolder + "congealed"));
    item = TinkerWorld.slime.get(type);
    MeltingRecipeBuilder.melting(Ingredient.of(item), fluid, FluidValues.SLIME_BLOCK, 3.0f)
                        .save(consumer, location(slimeFolder + "block"));
  }

  /**
   * Adds slime related casting recipes
   * @param consumer    Recipe consumer
   * @param fluid       Fluid matching the slime type
   * @param slimeType   SlimeType for this recipe
   * @param folder      Output folder
   */
  private void slimeCasting(Consumer<FinishedRecipe> consumer, FluidObject<?> fluid, SlimeType slimeType, String folder) {
    String colorFolder = folder + slimeType.getSerializedName() + "/";
    ItemCastingRecipeBuilder.basinRecipe(TinkerWorld.congealedSlime.get(slimeType))
                            .setFluidAndTime(fluid, FluidValues.SLIME_CONGEALED)
                            .save(consumer, location(colorFolder + "congealed"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerWorld.slimyEnderbarkRoots.get(slimeType))
                            .setFluidAndTime(fluid, FluidValues.SLIME_CONGEALED)
                            .setCast(TinkerWorld.enderbarkRoots, true)
                            .save(consumer, location(colorFolder + "roots"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerWorld.slime.get(slimeType))
                            .setFluidAndTime(fluid, FluidValues.SLIME_BLOCK - FluidValues.SLIME_CONGEALED)
                            .setCast(TinkerWorld.congealedSlime.get(slimeType), true)
                            .save(consumer, location(colorFolder + "block"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerCommons.slimeball.get(slimeType))
                            .setFluidAndTime(fluid, FluidValues.SLIMEBALL)
                            .save(consumer, location(colorFolder + "slimeball"));
    ItemCastingRecipeBuilder.tableRecipe(TinkerFluids.slimeBottle.get(slimeType))
                            .setFluid(fluid.ingredient(FluidValues.SLIMEBALL))
                            .setCoolingTime(1)
                            .setCast(Items.GLASS_BOTTLE, true)
                            .save(consumer, location(colorFolder + "bottle"));
    ItemCastingRecipeBuilder.basinRecipe(TinkerWorld.slimeDirt.get(slimeType.asDirt()))
                            .setFluidAndTime(fluid, FluidValues.SLIMEBALL * 2)
                            .setCast(Blocks.DIRT, true)
                            .save(consumer, location(colorFolder + "dirt"));
  }

  /** Adds recipes for melting slime crystals */
  private void crystalMelting(Consumer<FinishedRecipe> consumer, GeodeItemObject geode, FluidObject<?> fluid, String folder) {
    MeltingRecipeBuilder.melting(Ingredient.of(geode), fluid, FluidValues.SLIMEBALL, 1.0f).save(consumer, location(folder + "crystal"));
    MeltingRecipeBuilder.melting(Ingredient.of(geode.getBlock()), fluid, FluidValues.SLIMEBALL * 4, 2.0f).save(consumer, location(folder + "crystal_block"));
    for (BudSize bud : BudSize.values()) {
      int size = bud.getSize();
      MeltingRecipeBuilder.melting(Ingredient.of(geode.getBud(bud)), fluid, FluidValues.SLIMEBALL * size, (size + 1) / 2f)
                          .setOre(OreRateType.GEM)
                          .save(consumer, location(folder + "bud_" + bud.getName()));
    }
  }
}
