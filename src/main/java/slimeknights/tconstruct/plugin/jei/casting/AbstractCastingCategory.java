package slimeknights.tconstruct.plugin.jei.casting;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.plugin.jei.util.IRecipeTooltipReplacement;
import slimeknights.tconstruct.plugin.jei.fabric.JEITypes;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated.StartDirection;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import slimeknights.tconstruct.library.recipe.display.RecipeSlot;
import slimeknights.tconstruct.plugin.jei.util.CategoryUtil;
import slimeknights.tconstruct.plugin.jei.util.FluidTooltipCallback;
import slimeknights.tconstruct.plugin.jei.util.RecipeSlotWrapper;
import javax.annotation.Nullable;
import java.util.ArrayList;

/** Shared base logic for the two casting recipe types */
public abstract class AbstractCastingCategory extends AbstractRecipeCategory<IDisplayableCastingRecipe> implements IRecipeTooltipReplacement {
  private static final String KEY_COOLING_TIME = TConstruct.makeTranslationKey("jei", "time");
  private static final String KEY_CAST_KEPT = TConstruct.makeTranslationKey("jei", "casting.cast_kept");
  private static final String KEY_CAST_CONSUMED = TConstruct.makeTranslationKey("jei", "casting.cast_consumed");
  protected static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/casting.png");

  @Getter
  private final IDrawable background;
  private final IDrawable tankOverlay;
  private final IDrawable castConsumed;
  private final IDrawable castKept;
  private final IDrawable block;
  private final LoadingCache<Integer,IDrawableAnimated> cachedArrows;

  protected AbstractCastingCategory(IGuiHelper guiHelper, mezz.jei.api.recipe.RecipeType<IDisplayableCastingRecipe> recipeType, Component title, Block icon, IDrawable block) {
    super(recipeType, title, guiHelper.createDrawableItemLike(icon), 117, 54);
    this.background = guiHelper.createDrawable(BACKGROUND_LOC, 0, 0, 117, 54);
    this.tankOverlay = guiHelper.createDrawable(BACKGROUND_LOC, 133, 0, 32, 32);
    this.castConsumed = guiHelper.createDrawable(BACKGROUND_LOC, 141, 32, 13, 11);
    this.castKept = guiHelper.createDrawable(BACKGROUND_LOC, 141, 43, 13, 11);
    this.block = block;
    this.cachedArrows = CacheBuilder.newBuilder().maximumSize(25L).build(new CacheLoader<>() {
      @Override
      public IDrawableAnimated load(Integer coolingTime) {
        return guiHelper.drawableBuilder(BACKGROUND_LOC, 117, 32, 24, 17).buildAnimated(coolingTime, IDrawableAnimated.StartDirection.LEFT, false);
      }
    });
  }

  @Override
  public boolean isHandled(IDisplayableCastingRecipe recipe) {
    return true;
  }

  @Override
  public void draw(IDisplayableCastingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
    cachedArrows.getUnchecked(Math.max(1, recipe.getCoolingTime())).draw(graphics, 58, 18);
    block.draw(graphics, 38, 35);
    if (recipe.hasCast()) {
      (recipe.isConsumed() ? castConsumed : castKept).draw(graphics, 63, 39);
    }

    int coolingTime = recipe.getCoolingTime() / 20;
    String coolingString = I18n.get(KEY_COOLING_TIME, coolingTime);
    Font fontRenderer = Minecraft.getInstance().font;
    int x = 72 - fontRenderer.width(coolingString) / 2;
    graphics.drawString(fontRenderer, coolingString, x, 2, Color.GRAY.getRGB(), false);
  }

  @Override
  public List<Component> getTooltipStrings(IDisplayableCastingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
    if (recipe.hasCast() && GuiUtil.isHovered((int)mouseX, (int)mouseY, 63, 39, 13, 11)) {
      return Collections.singletonList(Component.translatable(recipe.isConsumed() ? KEY_CAST_CONSUMED : KEY_CAST_KEPT));
    }
    return Collections.emptyList();
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, IDisplayableCastingRecipe recipe, IFocusGroup focuses) {
    // items
    List<ItemStack> casts = recipe.getCastItems();
    if (!casts.isEmpty()) {
      builder.addSlot(recipe.isConsumed() ? RecipeIngredientRole.INPUT : RecipeIngredientRole.CATALYST, 38, 19).addItemStacks(casts);
    }
    builder.addSlot(RecipeIngredientRole.OUTPUT, 93, 18).addItemStack(recipe.getOutput());

    // fluids
    // tank fluids
    long capacity = FluidValues.METAL_BLOCK;
    builder.addSlot(RecipeIngredientRole.INPUT, 3, 3)
           .addTooltipCallback(this)
           .setFluidRenderer(capacity, false, 32, 32)
           .setOverlay(tankOverlay, 0, 0)
           .addIngredients(FabricTypes.FLUID_STACK, JEITypes.toJEI(recipe.getFluids()));
    // pouring fluid
    int h = 11;
    if (!recipe.hasCast()) {
      h += 16;
    }
    builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 43, 8)
           .addTooltipCallback(this)
           .setFluidRenderer(1L, false, 6, h)
           .addIngredients(FabricTypes.FLUID_STACK, JEITypes.toJEI(recipe.getFluids()));
  }

  @Override
  public void addMiddleLines(IRecipeSlotView slot, List<Component> list) {
    slot.getDisplayedIngredient(FabricTypes.FLUID_STACK).ifPresent(stack -> FluidTooltipHandler.appendMaterial(JEITypes.toFluidStack(stack), list));
  }
}
