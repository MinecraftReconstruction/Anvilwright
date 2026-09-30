package slimeknights.tconstruct.plugin.jei.casting;

import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.world.item.ItemStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.display.FilteredRecipe;
import slimeknights.tconstruct.plugin.jei.util.manager.FluidRecipeCache;
import slimeknights.tconstruct.plugin.jei.util.manager.ItemRecipeCache;

import java.util.List;
import java.util.Optional;

/** Plugin handling filterable casting recipes. */
public class CastingRecipeManager implements ISimpleRecipeManagerPlugin<IDisplayableCastingRecipe> {
  @Getter
  private final List<IDisplayableCastingRecipe> allRecipes;
  private final ItemRecipeCache<IDisplayableCastingRecipe> inputItemCache, outputItemCache;
  private final FluidRecipeCache<IDisplayableCastingRecipe> fluidCache;

  public CastingRecipeManager(IIngredientManager ingredientManager, List<IDisplayableCastingRecipe> recipes) {
    this.allRecipes = FilteredRecipe.alwaysVisible(recipes);
    IIngredientHelper<ItemStack> itemHelper = ingredientManager.getIngredientHelper(VanillaTypes.ITEM_STACK);
    IIngredientHelper<FluidStack> fluidHelper = ingredientManager.getIngredientHelper(FabricTypes.FLUID_STACK);
    inputItemCache = new ItemRecipeCache<>(itemHelper, recipes, false);
    outputItemCache = new ItemRecipeCache<>(itemHelper, recipes, true);
    fluidCache = new FluidRecipeCache<>(fluidHelper, recipes, false);
  }

  @Override
  public boolean isHandledInput(ITypedIngredient<?> input) {
    IIngredientType<?> type = input.getType();
    return type == VanillaTypes.ITEM_STACK || type == FabricTypes.FLUID_STACK;
  }

  @Override
  public boolean isHandledOutput(ITypedIngredient<?> output) {
    return output.getType() == VanillaTypes.ITEM_STACK;
  }

  @Override
  public List<IDisplayableCastingRecipe> getRecipesForInput(ITypedIngredient<?> input) {
    Optional<ItemStack> itemOpt = input.getIngredient(VanillaTypes.ITEM_STACK);
    if (itemOpt.isPresent()) {
      return inputItemCache.filterRecipes(itemOpt.get());
    }
    Optional<FluidStack> fluidOpt = input.getIngredient(FabricTypes.FLUID_STACK);
    if (fluidOpt.isPresent()) {
      return fluidCache.filterRecipes(fluidOpt.get());
    }
    return List.of();
  }

  @Override
  public List<IDisplayableCastingRecipe> getRecipesForOutput(ITypedIngredient<?> output) {
    Optional<ItemStack> itemOpt = output.getIngredient(VanillaTypes.ITEM_STACK);
    if (itemOpt.isPresent()) {
      return outputItemCache.filterRecipes(itemOpt.get());
    }
    return List.of();
  }
}
