package slimeknights.tconstruct.plugin.jei.melting;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IDrawableWidget;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidgetTooltipCallback;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.plugin.jei.IRecipeTooltipReplacement;
import slimeknights.tconstruct.plugin.jei.fabric.JEITypes;

import java.awt.*;
import java.util.Collections;
import java.util.List;

/** Shared logic between melting and foundry */
public abstract class AbstractMeltingCategory extends AbstractRecipeCategory<IDisplayableMeltingRecipe> {
  public static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/melting.png");
  protected static final String KEY_COOLING_TIME = TConstruct.makeTranslationKey("jei", "melting.time");
  protected static final String KEY_TEMPERATURE = TConstruct.makeTranslationKey("jei", "temperature");
  protected static final String KEY_MULTIPLIER = TConstruct.makeTranslationKey("jei", "melting.multiplier");
  protected static final Component TOOLTIP_ORE = Component.translatable(TConstruct.makeTranslationKey("jei", "melting.ore"));

  /** Tooltip for fuel display */
  public static final IRecipeTooltipReplacement FUEL_TOOLTIP = (slot, tooltip) -> {
    //noinspection SimplifyOptionalCallChains  Not for int streams
    slot.getDisplayedIngredient(FabricTypes.FLUID_STACK).ifPresent(stack -> {
      MeltingFuelHandler.getTemperature(stack.getFluid()).ifPresent(temperature -> {
        tooltip.add(Component.translatable(KEY_TEMPERATURE, temperature).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY_MULTIPLIER, temperature / 1000f).withStyle(ChatFormatting.GRAY));
      });
    });
  };

  private final IDrawable background;
  protected final IDrawableStatic tankOverlay;
  protected final IDrawableStatic plus;

  public AbstractMeltingCategory(IGuiHelper helper, RecipeType<IDisplayableMeltingRecipe> recipeType, Component title, IDrawable icon) {
    super(recipeType, title, icon, 132, 40);
    this.background = helper.createDrawable(BACKGROUND_LOC, 0, 0, 132, 40);
    this.tankOverlay = helper.createDrawable(BACKGROUND_LOC, 132, 0, 32, 32);
    this.plus = helper.drawableBuilder(BACKGROUND_LOC, 132, 32, 8, 8)
                      .addPadding(2, 2, 2, 2)
                      .build();
  }

  @Override
  public void createRecipeExtras(IRecipeExtrasBuilder builder, IDisplayableMeltingRecipe recipe, IFocusGroup focuses) {
    // includes both the static arrow background and animated foreground
    int time = recipe.getTime();
    IDrawableWidget arrow = builder.addAnimatedRecipeArrowWidget(time * 5).setPosition(56, 18);
    arrowTooltip:
    {
      if (recipe.isTimeDynamic()) {
        IRecipeSlotDrawable fluid = CategoryUtil.findSlot(builder.getRecipeSlots().getSlots(), FLUID_SLOT);
        if (fluid != null) {
          arrow.setTooltip(new MeltingArrowTooltip(recipe, fluid));
          break arrowTooltip;
        }
      }
      // not dynamic or fail to find the slot? static tooltip is fine
      arrow.setTooltip(Component.translatable(KEY_COOLING_TIME, time / 4));
    }

  @Override
  public void draw(MeltingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
    // draw the arrow
    cachedArrows.getUnchecked(recipe.getTime() * 5).draw(graphics, 56, 18);
    if (recipe.getOreType() != null) {
      plus.draw(graphics, 87, 31);
    }

    // temperature
    int temperature = recipe.getTemperature();
    Font fontRenderer = Minecraft.getInstance().font;
    String tempString = I18n.get(KEY_TEMPERATURE, temperature);
    int x = 56 - fontRenderer.width(tempString) / 2;
    graphics.drawString(fontRenderer, tempString, x, 3, Color.GRAY.getRGB(), false);
  }

  @Override
  public List<Component> getTooltipStrings(MeltingRecipe recipe, IRecipeSlotsView slots, double mouseXD, double mouseYD) {
    int mouseX = (int)mouseXD;
    int mouseY = (int)mouseYD;
    if (recipe.getOreType() != null && GuiUtil.isHovered(mouseX, mouseY, 87, 31, 16, 16)) {
      return Collections.singletonList(TOOLTIP_ORE);
    }
    // time tooltip
    if (GuiUtil.isHovered(mouseX, mouseY, 56, 18, 24, 17)) {
      return Collections.singletonList(Component.translatable(KEY_COOLING_TIME, recipe.getTime() / 4));
    }
    return Collections.emptyList();
  }

  /** Adds amounts to outputs and temperatures to fuels */
  @RequiredArgsConstructor
  public static class MeltingFluidCallback implements FluidTooltipCallback {
    public static final MeltingFluidCallback INSTANCE = new MeltingFluidCallback();

    /**
     * Adds teh tooltip for ores
     *
     * @param stack  Fluid to draw
     * @param list   Tooltip so far
     * @return true if the amount is not in buckets
     */
    protected boolean appendMaterial(FluidStack stack, List<Component> list) {
      return FluidTooltipHandler.appendMaterialNoShift(stack.getFluid(), stack.getAmount(), list);
    }

    @Override
    public void addMiddleLines(IRecipeSlotView slot, List<Component> list) {
      slot.getDisplayedIngredient(FabricTypes.FLUID_STACK).ifPresent(stack -> {
        if (appendMaterial(JEITypes.toFluidStack(stack), list)) {
          FluidTooltipHandler.appendShift(list);
        }
      });
    }
  }
}
