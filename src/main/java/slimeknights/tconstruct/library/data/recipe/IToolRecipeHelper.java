package slimeknights.tconstruct.library.data.recipe;

import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.registration.CastItemObject;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.recipe.casting.material.CompositeCastingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;
import slimeknights.tconstruct.library.recipe.partbuilder.ItemPartRecipeBuilder;
import slimeknights.tconstruct.library.recipe.partbuilder.PartRecipeBuilder;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipeBuilder;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tools.data.material.MaterialIds;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.world.level.ItemLike;
import static slimeknights.mantle.Mantle.commonResource;
import slimeknights.mantle.recipe.helper.ItemOutput;

/**
 * Interface for tool and part crafting recipes
 */
public interface IToolRecipeHelper extends ICastCreationHelper {

  /**
   * Registers recipe for tool building
   * @param consumer Recipe consumer
   * @param tool     Tool
   * @param folder   Folder for recipe
   */
  default void toolBuilding(Consumer<FinishedRecipe> consumer, IModifiable tool, String folder) {
    ToolBuildingRecipeBuilder.toolBuildingRecipe(tool)
                             .save(consumer, commonResource(folder + Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(tool.asItem())).getPath()));
  }

  /**
   * Registers recipe for tool building
   * @param consumer Recipe consumer
   * @param tool     Tool supplier
   * @param folder   Folder for recipe
   */
  default void toolBuilding(Consumer<FinishedRecipe> consumer, Supplier<? extends IModifiable> tool, String folder) {
    toolBuilding(consumer, tool.get(), folder);
  }

  /**
   * Registers recipe for tool building using a custom layout slot
   * @param consumer    Recipe consumer
   * @param tool        Tool
   * @param folder      Folder for recipe
   * @param layoutSlot  Slot in the tinker station this tool is built in
   */
  default void toolBuilding(Consumer<FinishedRecipe> consumer, IModifiable tool, String folder, ResourceLocation layoutSlot) {
    ToolBuildingRecipeBuilder.toolBuildingRecipe(tool)
                             .layoutSlot(layoutSlot)
                             .save(consumer, commonResource(folder + Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(tool.asItem())).getPath()));
  }

  /**
   * Adds part casting recipes for using a part. Does not add part builder recipes or cast creation recipes
   * @param consumer Recipe consumer
   * @param part     Part to be crafted
   * @param cast     Part cast
   * @param cost     Part cost
   * @param partFolder   Folder for recipes
   */
  default void partCasting(Consumer<FinishedRecipe> consumer, IMaterialItem part, CastItemObject cast, int cost, String partFolder) {
    String name = id(part).getPath();
    String castingFolder = partFolder + "casting/";
    MaterialCastingRecipeBuilder.tableRecipe(part)
                                .setItemCost(cost)
                                .setCast(cast.getMultiUseTag(), false)
                                .save(consumer, location(castingFolder + name + "_gold_cast"));
    MaterialCastingRecipeBuilder.tableRecipe(part)
                                .setItemCost(cost)
                                .setCast(cast.getSingleUseTag(), true)
                                .save(consumer, location(castingFolder + name + "_sand_cast"));
    CompositeCastingRecipeBuilder.table(part, cost)
                                 .save(consumer, location(castingFolder + name + "_composite"));
  }

  /**
   * Adds cast creation and casting recipes for using a part. Does not add part builder recipes.
   * @param consumer Recipe consumer
   * @param part     Part to be crafted
   * @param cast     Part cast
   * @param cost     Part cost
   * @param partFolder   Folder for recipes
   * @param castFolder   Folder for cast creation recipes
   */
  default void partCasting(Consumer<FinishedRecipe> consumer, IMaterialItem part, CastItemObject cast, int cost, String partFolder, String castFolder) {
    // Material Casting
    partCasting(consumer, part, cast, cost, partFolder);
    // Cast Casting
    castCreation(consumer, MaterialIngredient.of(part), cast, castFolder, id(part).getPath());
  }

  /**
   * Adds recipes for a part with no cast item
   * @param consumer             Recipe consumer
   * @param part                 Part to be crafted
   * @param cost                 Part cost
   * @param castingStatConflict  If nonnull, disallows casting if a material matching this fluid and stat type can be casted. Prevents conflicts with tool casting
   * @param partFolder   Folder for recipes
   */
  default void uncastablePart(Consumer<FinishedRecipe> consumer, IMaterialItem part, int cost, @Nullable MaterialStatsId castingStatConflict, String partFolder) {
    ResourceLocation id = id(part);
    PartRecipeBuilder.partRecipe(part)
                     .setPattern(id)
                     .setPatternItem(Ingredient.of(TinkerTags.Items.DEFAULT_PATTERNS))
                     .setCost(cost)
                     .save(consumer, location(partFolder + "builder/" + id.getPath()));
    CompositeCastingRecipeBuilder.table(part, cost)
                                 .castingStatConflict(castingStatConflict)
                                 .save(consumer, location(partFolder + "casting/" + id.getPath() + "_composite"));
  }

  /**
   * Adds cast creation and casting recipes for using a part with a dummy part for the part builder
   * @param consumer   Recipe consumer
   * @param part       Part to be crafted
   * @param dummyPart  Dummy item used for creating casts, made in the part builder
   * @param cast       Part cast
   * @param cost       Part cost
   * @param partFolder   Folder for recipes
   * @param castFolder   Folder for cast creation recipes
   */
  default void partWithDummy(Consumer<FinishedRecipe> consumer, IMaterialItem part, ItemLike dummyPart, CastItemObject cast, int cost, String partFolder, String castFolder) {
    // Material Casting
    partCasting(consumer, part, cast, cost, partFolder);
    // Cast Casting
    castCreation(consumer, DefaultCustomIngredients.all(Ingredient.of(dummyPart), MaterialIngredient.of(part)), cast, castFolder, id(part).getPath());
    // dummy part builder recipe
    ItemPartRecipeBuilder.item(cast.getName(), ItemOutput.fromItem(dummyPart))
                         .material(MaterialIds.rock, cost)
                         .setPatternItem(DefaultCustomIngredients.all(Ingredient.of(TinkerTags.Items.DEFAULT_PATTERNS), Ingredient.of(cast.get())))
                         .save(consumer, location(partFolder + "builder/" + cast.getName().getPath()));
  }

  /**
   * Adds cast creation, casting, and part builder recipes for a material item
   * @param consumer Recipe consumer
   * @param part     Part to be crafted
   * @param cast     Part cast
   * @param cost     Part cost
   * @param partFolder   Folder for recipes
   */
  default void partRecipes(Consumer<FinishedRecipe> consumer, IMaterialItem part, CastItemObject cast, int cost, String partFolder, String castFolder) {
    String name = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(part.asItem())).getPath();

    // Part Builder
    PartRecipeBuilder.partRecipe(part)
                     .setPattern(commonResource(name))
                     .setPatternItem(DefaultCustomIngredients.any(Ingredient.of(TinkerTags.Items.DEFAULT_PATTERNS), Ingredient.of(cast.get())))
                     .setCost(cost)
                     .save(consumer, commonResource(partFolder + "builder/" + name));

    // Material Casting
    String castingFolder = partFolder + "casting/";
    MaterialCastingRecipeBuilder.tableRecipe(part)
                                .setItemCost(cost)
                                .setCast(cast.getMultiUseTag(), false)
                                .save(consumer, commonResource(castingFolder + name + "_gold_cast"));
    MaterialCastingRecipeBuilder.tableRecipe(part)
                                .setItemCost(cost)
                                .setCast(cast.getSingleUseTag(), true)
                                .save(consumer, commonResource(castingFolder + name + "_sand_cast"));
    CompositeCastingRecipeBuilder.table(part, cost)
                                 .save(consumer, commonResource(castingFolder + name + "_composite"));

    // Cast Casting
    MaterialIngredient ingredient = MaterialIngredient.of(part, MaterialPredicate.ANY);
    castCreation(consumer, ingredient, cast, castFolder, Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(part.asItem())).getPath());
  }

  /**
   * Adds a recipe to craft a material item
   * @param consumer Recipe consumer
   * @param part     Part to be crafted
   * @param cast     Part cast
   * @param cost     Part cost
   * @param partFolder   Folder for recipes
   */
  default void partRecipes(Consumer<FinishedRecipe> consumer, Supplier<? extends IMaterialItem> part, CastItemObject cast, int cost, String partFolder, String castFolder) {
    partRecipes(consumer, part.get(), cast, cost, partFolder, castFolder);
  }
}
