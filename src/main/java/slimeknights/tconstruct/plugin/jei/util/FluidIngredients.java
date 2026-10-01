package slimeknights.tconstruct.plugin.jei.util;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.fabric.ingredients.fluids.JeiFluidIngredient;

/**
 * Bridges Tinkers' {@link FluidStack} and JEI's fluid ingredient.
 * <p>
 * Upstream JEI for Forge used {@code mezz.jei.api.forge.FabricTypes.FLUID_STACK}, whose ingredient type <em>is</em>
 * Forge's {@code FluidStack}, so no conversion was ever needed. JEI for Fabric has its own ingredient interface
 * ({@link IJeiFluidIngredient}) that Porting Lib's stack does not implement, so every crossing between the plugin
 * and the rest of the mod goes through here.
 */
public final class FluidIngredients {
  private FluidIngredients() {}

  /** Converts a Tinkers fluid stack to JEI's fluid ingredient */
  public static IJeiFluidIngredient of(FluidStack stack) {
    return new JeiFluidIngredient(stack.getFluid(), stack.getAmount(), stack.getTag());
  }

  /** Converts JEI's fluid ingredient back to a Tinkers fluid stack */
  public static FluidStack toStack(IJeiFluidIngredient ingredient) {
    return new FluidStack(ingredient.getFluid(), ingredient.getAmount(), ingredient.getTag().orElse(null));
  }

  /** Client side safe conversion that falls back to the empty stack */
  public static FluidStack toStackOrEmpty(Object ingredient) {
    return ingredient instanceof IJeiFluidIngredient fluid ? toStack(fluid) : FluidStack.EMPTY;
  }
}
