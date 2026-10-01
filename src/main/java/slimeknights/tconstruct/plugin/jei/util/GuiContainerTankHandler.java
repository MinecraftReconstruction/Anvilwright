package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.AbstractContainerMenu;
import slimeknights.tconstruct.smeltery.client.screen.IScreenWithFluidTank;
import slimeknights.tconstruct.smeltery.client.screen.IScreenWithFluidTank.FluidLocation;

import java.util.Optional;

/**
 * Class to pass {@link IScreenWithFluidTank} into JEI.
 * <p>
 * Upstream built the clickable ingredient with JEI's {@code IClickableIngredientFactory}. JEI 15.20 removed that
 * factory - {@code getClickableIngredientUnderMouse} no longer receives one and has to return a
 * {@link IClickableIngredient} built from the runtime's ingredient manager instead.
 */
public class GuiContainerTankHandler<C extends AbstractContainerMenu, T extends AbstractContainerScreen<C> & IScreenWithFluidTank> implements IGuiContainerHandler<T> {
  private final IIngredientManager ingredientManager;

  public GuiContainerTankHandler(IIngredientManager ingredientManager) {
    this.ingredientManager = ingredientManager;
  }

  @Override
  public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(T containerScreen, double mouseX, double mouseY) {
    FluidLocation fluid = containerScreen.getFluidUnderMouse((int)mouseX, (int)mouseY);
    if (fluid == null || fluid.fluid().isEmpty()) {
      return Optional.empty();
    }
    return ingredientManager.createTypedIngredient(FabricTypes.FLUID_STACK, FluidIngredients.of(fluid.fluid()))
      .map(ingredient -> new ClickableFluid(ingredient, fluid.location()));
  }

  /** Clickable region for a fluid shown in a tank */
  private record ClickableFluid(ITypedIngredient<IJeiFluidIngredient> ingredient, Rect2i area) implements IClickableIngredient<IJeiFluidIngredient> {
    @Override
    public ITypedIngredient<IJeiFluidIngredient> getTypedIngredient() {
      return ingredient;
    }

    @Override
    public Rect2i getArea() {
      return area;
    }
  }
}
