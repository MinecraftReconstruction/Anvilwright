package slimeknights.tconstruct.plugin.jei.modifiers;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.fabricators_of_create.porting_lib.util.ForgeI18n;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.client.model.NBTKeyModel;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.item.CreativeSlotItem;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Category for display recipes for crafting {@link ModifierEntry}. */
public class ModifierRecipeCategory extends AbstractTinkerStationCategory<IDisplayModifierRecipe> {
  protected static final ResourceLocation BACKGROUND_LOC = AbstractTinkerStationCategory.BACKGROUND_LOC;
  private static final Component TITLE = TConstruct.makeTranslation("jei", "modifiers.title");

  // translation
  private static final Component TEXT_INCREMENTAL = TConstruct.makeTranslation("jei", "modifiers.incremental");
  private static final String KEY_MIN = TConstruct.makeTranslationKey("jei", "modifiers.level.min");
  private static final String KEY_MAX = TConstruct.makeTranslationKey("jei", "modifiers.level.max");
  private static final String KEY_RANGE = TConstruct.makeTranslationKey("jei", "modifiers.level.range");
  private static final String KEY_EXACT = TConstruct.makeTranslationKey("jei", "modifiers.level.exact");

  /** Draws the slotless input icon. */
  private static final IDrawable SLOTLESS = new IDrawable() {
    @Override
    public int getWidth() {
      return SlotIngredientRenderer.INPUT.getWidth();
    }

    @Override
    public int getHeight() {
      return SlotIngredientRenderer.INPUT.getHeight();
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
      SlotIngredientRenderer.INPUT.render(graphics, null, xOffset, yOffset);
    }
  };

  private final ModifierIngredientRenderer modifierRenderer = new ModifierIngredientRenderer(124, 10);

  private final IDrawable requirements, incremental;
  public ModifierRecipeCategory(IGuiHelper helper) {
    super(helper, TConstructJEIConstants.MODIFIERS, TITLE, helper.createDrawableItemStack(CreativeSlotItem.withSlot(new ItemStack(TinkerModifiers.creativeSlotItem), SlotType.UPGRADE)));
    this.requirements = helper.createDrawable(BACKGROUND_LOC, 128, 17, 16, 16);
    this.incremental = helper.createDrawable(BACKGROUND_LOC, 128, 33, 16, 16);
  }

  @Override
  public RecipeType<IDisplayModifierRecipe> getRecipeType() {
    return TConstructJEIConstants.MODIFIERS;
  }

  @Override
  public Component getTitle() {
    return TITLE;
  }

  /** Draws a single slot icon */
  private void drawSlot(GuiGraphics graphics, IDisplayModifierRecipe recipe, int slot, int x, int y) {
    List<ItemStack> stacks = recipe.getDisplayItems(slot);
    if (stacks.isEmpty()) {
      // -1 as the item list includes the output slot, we skip that
      slotIcons[slot].draw(graphics, x + 1, y + 1);
    }
  }

  /** Gets the text describing the required modifier level, if any. */
  @Override
  @Nullable
  protected Component getVariantText(IDisplayModifierRecipe recipe) {
    Component variant = recipe.getVariant();
    if (variant != null) {
      return variant;
    }
    IntRange level = recipe.getLevel();
    int min = level.min();
    int max = level.max();
    // min being 1 means we only have a max level, we check this first as Max Level is better than exact typically
    if (min == 1) {
      if (max < ModifierEntry.VALID_LEVEL.max()) {
        return Component.translatable(KEY_MAX, max);
      }
    } else if (min == max) {
      return Component.translatable(KEY_EXACT, min);
    } else if (max == ModifierEntry.VALID_LEVEL.max()) {
      return Component.translatable(KEY_MIN, min);
    } else {
      return Component.translatable(KEY_RANGE, min, max);
    }
    return null;
  }

  /** Draws the icon for the given slot type */
  private void drawSlotType(GuiGraphics graphics, @Nullable SlotType slotType, int x, int y) {
    Minecraft minecraft = Minecraft.getInstance();
    TextureAtlasSprite sprite;
    if (slotTypeSprites.containsKey(slotType)) {
      sprite = slotTypeSprites.get(slotType);
    } else {
      ModelManager modelManager = minecraft.getModelManager();
      // gets the model for the item, its a sepcial one that gives us texture info
      BakedModel model = minecraft.getItemRenderer().getItemModelShaper().getItemModel(TinkerModifiers.creativeSlotItem.get());
      if (model != null && model.getOverrides() instanceof NBTKeyModel.Overrides) {
        Material material = ((NBTKeyModel.Overrides)model.getOverrides()).getTexture(slotType == null ? "slotless" : slotType.getName());
        sprite = modelManager.getAtlas(material.atlasLocation()).getSprite(material.texture());
      } else {
        // failed to use the model, use missing texture
        sprite = modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(MissingTextureAtlasSprite.getLocation());
      }
    }
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

    graphics.blit(x, y, 0, 16, 16, sprite);
  }

  @Override
  public void draw(IDisplayModifierRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
    drawSlot(graphics, recipe, 0,  2, 32);
    drawSlot(graphics, recipe, 1, 24, 14);
    drawSlot(graphics, recipe, 2, 46, 32);
    drawSlot(graphics, recipe, 3, 42, 57);
    drawSlot(graphics, recipe, 4,  6, 57);

    // draw info icons
    if (recipe.hasRequirements()) {
      requirements.draw(graphics, 66, 58);
    }
    if (recipe.isIncremental()) {
      incremental.draw(graphics, 83, 59);
    }

    // draw max count
    Font fontRenderer = Minecraft.getInstance().font;
    int max = recipe.getMaxLevel();
    if (max > 0) {
      graphics.drawString(fontRenderer, maxPrefix + max, 66, 16, Color.GRAY.getRGB(), false);
    }

    // draw slot cost
    SlotCount slots = recipe.getSlots();
    if (slots == null) {
      drawSlotType(graphics, null, 110, 58);
    } else {
      drawSlotType(graphics, slots.getType(), 110, 58);
      String text = Integer.toString(slots.getCount());
      int x = 111 - fontRenderer.width(text);
      graphics.drawString(fontRenderer, text, x, 63, Color.GRAY.getRGB(), false);
    }
  }

  @Override
  public List<Component> getTooltipStrings(IDisplayModifierRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
    int checkX = (int) mouseX;
    int checkY = (int) mouseY;
    if (recipe.hasRequirements() && GuiUtil.isHovered(checkX, checkY, 66, 58, 16, 16)) {
      return Collections.singletonList(Component.translatable(recipe.getRequirementsError()));
    } else if (recipe.isIncremental() && GuiUtil.isHovered(checkX, checkY, 83, 59, 16, 16)) {
      return TEXT_INCREMENTAL;
    } else if (GuiUtil.isHovered(checkX, checkY, 98, 58, 24, 16)) {
      // slot tooltip over icon
      SlotCount slots = recipe.getSlots();
      if (slots != null) {
        int count = slots.getCount();
        if (count == 1) {
          return Collections.singletonList(Component.translatable(KEY_SLOT, slots.getType().getDisplayName()));
        } else if (count > 1) {
          return Collections.singletonList(Component.translatable(KEY_SLOTS, slots, slots.getType().getDisplayName()));
        }
      } else {
        return TEXT_FREE;
      }
    }

    return Collections.emptyList();
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, IDisplayModifierRecipe recipe, IFocusGroup focuses) {
    super.setRecipe(builder, recipe, focuses);

    // modifiers
    builder.addOutputSlot(3, 3)
      .setCustomRenderer(TConstructJEIConstants.MODIFIER_TYPE, modifierRenderer)
      .addIngredient(TConstructJEIConstants.MODIFIER_TYPE, recipe.getDisplayResult());

    // modifier slots
    SlotCount slots = recipe.getSlots();
    if (slots != null) {
      builder.addInputSlot(102, 58)
        .setCustomRenderer(TConstructJEIConstants.SLOT_TYPE, SlotIngredientRenderer.INPUT)
        .addIngredient(TConstructJEIConstants.SLOT_TYPE, recipe.getSlots());
    }

    // result slots is determined based on the volatile data hook. Its a bit of a heuristic, but is good enough for our usecases
    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addIngredients(TConstructJEIConstants.SLOT_TYPE, recipe.getResultSlots());
  }

  /** @deprecated never needed to be called by an addon */
  @Deprecated(forRemoval = true)
  @Internal
  public static void clearSlimeskullCache() {
    AbstractTinkerStationCategory.clearLookupCache();
  }
}
