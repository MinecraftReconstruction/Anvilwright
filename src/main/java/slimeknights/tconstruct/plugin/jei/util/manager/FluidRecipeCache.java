package slimeknights.tconstruct.plugin.jei.util.manager;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.tconstruct.plugin.jei.util.FluidIngredients;
import slimeknights.tconstruct.library.recipe.display.FilteredFluidRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Cache for searching for recipes from a list of {@link FilteredFluidRecipe}. */
public class FluidRecipeCache<T extends FilteredFluidRecipe> extends AbstractRecipeCache<T, IJeiFluidIngredient> {
  private final boolean output;
  public FluidRecipeCache(IIngredientHelper<IJeiFluidIngredient> helper, List<? extends T> recipes, boolean output) {
    super(helper, recipes);
    this.output = output;
  }

  @Override
  protected boolean matches(T recipe, Predicate<IJeiFluidIngredient> predicate) {
    // the recipe side is typed on Tinkers' fluid stack, so adapt the JEI predicate instead of the recipe
    return recipe.matchesFluid(stack -> predicate.test(FluidIngredients.of(stack)), this.output);
  }

  @Override
  public List<T> filterRecipes(IJeiFluidIngredient focus) {
    List<T> recipes = matchingRecipes(focus);
    if (recipes.isEmpty()) {
      return List.of();
    }
    FluidStack focusStack = FluidIngredients.toStack(focus);
    List<T> filtered = new ArrayList<>(recipes.size());
    for (T recipe : recipes) {
      if (recipe.isVisibleFromFluid(focusStack, output)) {
        filtered.add(recipe);
      }
    }
    return filtered;
  }
}
