package slimeknights.tconstruct.library.recipe.ingredient;

import it.unimi.dsi.fastutil.ints.IntList;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.recipe.ingredient.AbstractIngredient;

import javax.annotation.Nullable;

/** Ingredient that contains another ingredient nested inside */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class NestedIngredient extends AbstractIngredient {
  protected final Ingredient nested;


  /* Defer to nested */

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return nested.test(stack);
  }

  @Override
  public ItemStack[] getItems() {
    return nested.getItems();
  }

  @Override
  public IntList getStackingIds() {
    return nested.getStackingIds();
  }

  @Override
  public boolean isEmpty() {
    return nested.isEmpty();
  }

  @Override
  protected void invalidate() {
    super.invalidate();
  }

  @Override
  public boolean isSimple() {
    return !(nested instanceof CustomIngredient custom) || !custom.requiresTesting();
  }
}
