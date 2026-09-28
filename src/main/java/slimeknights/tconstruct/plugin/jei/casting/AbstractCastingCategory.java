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
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated.StartDirection;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IDrawableWidget;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidgetTooltipCallback;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.plugin.jei.IRecipeTooltipReplacement;
import slimeknights.tconstruct.plugin.jei.fabric.JEITypes;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** Shared base logic for the two casting recipe types */
public abstract class AbstractCastingCategory extends AbstractRecipeCategory<IDisplayableCastingRecipe> {
  private static final String KEY_COOLING_TIME = TConstruct.makeTranslationKey("jei", "casting.time");
  private static final Component CAST_KEPT = TConstruct.makeTranslation("jei", "casting.cast_kept");
  private static final Component CAST_CONSUMED = TConstruct.makeTranslation("jei", "casting.cast_consumed");
  protected static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/casting.png");
  private static final String CAST_SLOT = "cast";
  private static final String RESULT_SLOT = "result";
  private static final String FLUID_SLOT = "fluid";
  private static final String FAUCET_SLOT = "faucet";

  private final IDrawable background;
  private final IDrawable tankOverlay;
  private final IDrawable castConsumed;
  private final IDrawable castKept;
  private final IDrawable block;
  private final IGuiHelper guiHelper;

  protected AbstractCastingCategory(IGuiHelper guiHelper, RecipeType<IDisplayableCastingRecipe> recipeType, Component title, Block icon, IDrawable block) {
    super(recipeType, title, guiHelper.createDrawableItemLike(icon), 117, 54);
    this.background = guiHelper.createDrawable(BACKGROUND_LOC, 0, 0, 117, 54);
    this.tankOverlay = guiHelper.createDrawable(BACKGROUND_LOC, 133, 0, 32, 32);
    this.castConsumed = guiHelper.createDrawable(BACKGROUND_LOC, 141, 32, 13, 11);
    this.castKept = guiHelper.createDrawable(BACKGROUND_LOC, 141, 43, 13, 11);
    this.block = block;
    this.guiHelper = guiHelper;
  }

  @Override
  public void createRecipeExtras(IRecipeExtrasBuilder builder, IDisplayableCastingRecipe recipe, IFocusGroup focuses) {
    builder.addDrawableWidget(block).setPosition(38, 35);
    int coolingTime = recipe.getCoolingTime();
    IDrawable arrow = guiHelper.drawableBuilder(BACKGROUND_LOC, 117, 32, 24, 17)
                                  .buildAnimated(Math.max(5, coolingTime), StartDirection.LEFT, false);
    IDrawableWidget arrowWidget = builder.addDrawableWidget(arrow).setPosition(58, 18);
    arrowTooltip:
    {
      if (recipe.isCoolingTimeDynamic()) {
        IRecipeSlotDrawable fluid = CategoryUtil.findSlot(builder.getRecipeSlots().getSlots(), FLUID_SLOT);
        if (fluid != null) {
          arrowWidget.setTooltip(new CoolingArrowTooltip(recipe, fluid));
          break arrowTooltip;
        }
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
    // fetch focus data
    IFocus<ItemStack> focus = focuses.getItemStackFocuses().findFirst().orElse(null);
    ItemStack focusStack = ItemStack.EMPTY;
    boolean focusOutput = false;
    if (focus != null) {
      focusStack = focus.getTypedValue().getIngredient();
      focusOutput = focus.getRole() == RecipeIngredientRole.OUTPUT;
    }

    List<ItemStack> outputs = recipe.getOutputs(focusStack, focusOutput);
    IRecipeSlotBuilder output = builder.addOutputSlot(93, 18).addItemStacks(outputs).setSlotName(RESULT_SLOT);
    List<IRecipeSlotBuilder> linked = new ArrayList<>(4);
    int outputSize = outputs.size();
    if (outputSize > 1) {
      linked.add(output);
    }

    // items
    List<ItemStack> casts = recipe.getCastItems(focusStack, focusOutput);
    if (!casts.isEmpty()) {
      IRecipeSlotBuilder cast = builder.addSlot(recipe.isConsumed() ? RecipeIngredientRole.INPUT : RecipeIngredientRole.CATALYST, 38, 19).addItemStacks(casts).setSlotName(CAST_SLOT);
      // if the same size, tie a focus link to the output and cast; means we have material variants on both
      if (recipe.linkCastToOutput() && !linked.isEmpty() && casts.size() == outputSize) {
        linked.add(cast);
      }
    }

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
