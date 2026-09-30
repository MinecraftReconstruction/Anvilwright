package slimeknights.tconstruct.plugin.jei.util;

import com.mojang.datafixers.util.Either;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/** Helper for working with fluid tooltips */
@SuppressWarnings("removal")
@FunctionalInterface
public interface FluidTooltipCallback extends mezz.jei.api.gui.ingredient.IRecipeSlotTooltipCallback, IRecipeSlotRichTooltipCallback {
  String AMOUNT_KEY = "jei.tooltip.liquid.amount";

  /** Default instance, simply replaces mb units with our unit handler. */
  FluidTooltipCallback UNITS = (fluid, recipeSlotView, tooltip) -> FluidTooltipHandler.appendMaterial(fluid, tooltip);

  /** Instance that removes the amount from fluid tooltips. */
  FluidTooltipCallback NO_AMOUNT = (fluid, recipeSlotView, tooltip) -> {};

  @Override
  default void onTooltip(IRecipeSlotView recipeSlotView, List<Component> tooltip) {
    ListIterator<Component> listIterator = tooltip.listIterator();
    while (listIterator.hasNext()) {
      Component component = listIterator.next();
      if (component.getContents() instanceof TranslatableContents translatable && AMOUNT_KEY.equals(translatable.getKey())) {
        listIterator.remove();
        FluidStack fluid = recipeSlotView.getDisplayedIngredient(FabricTypes.FLUID_STACK).map(FluidIngredients::toStack).orElse(FluidStack.EMPTY);
        List<Component> newTooltip = new ArrayList<>();
        onFluidTooltip(fluid, recipeSlotView, newTooltip);
        tooltip.addAll(listIterator.nextIndex(), newTooltip);
        return;
      }
    }
    // failed to find the tooltip to replace, so just append our stuff at the end
    FluidStack fluid = recipeSlotView.getDisplayedIngredient(FabricTypes.FLUID_STACK).map(FluidIngredients::toStack).orElse(FluidStack.EMPTY);
    onFluidTooltip(fluid, recipeSlotView, tooltip);
  }

  @Override
  default void onRichTooltip(IRecipeSlotView recipeSlotView, ITooltipBuilder tooltip) {
    // JEI 15.20 does not expose the lines of the rich tooltip, so the vanilla amount line cannot be replaced;
    // the fluid info is appended instead (upstream replaces the line in place)
    recipeSlotView.getDisplayedIngredient(FabricTypes.FLUID_STACK)
      .map(FluidIngredients::toStack)
      .ifPresent(fluid -> onFluidTooltip(fluid, recipeSlotView, tooltip));
  }

  /** Adds rich information about the fluid to the tooltip. */
  default void onFluidTooltip(FluidStack fluid, IRecipeSlotView recipeSlotView, ITooltipBuilder tooltip) {
    List<Component> newTooltip = new ArrayList<>();
    onFluidTooltip(fluid, recipeSlotView, newTooltip);
    tooltip.addAll(newTooltip);
  }

  /** Adds information about the fluid to the tooltip */
  void onFluidTooltip(FluidStack fluid, IRecipeSlotView recipeSlotView, List<Component> tooltip);
}
