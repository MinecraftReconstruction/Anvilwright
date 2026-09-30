package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Widget that optionally draws an {@link IDrawable} and shows a tooltip while the mouse is over it.
 * <p>
 * JEI 15.20 (what Fabric ships for 1.20.1) has no {@code addDrawableWidget(...).setTooltip(...)} /
 * {@code addTooltipArea(...)} helpers; both are hand rolled here from {@link IRecipeWidget}. Upstream
 * targets a newer JEI where those helpers exist, so this is the port's replacement for them.
 */
public class TooltipWidget implements IRecipeWidget {
  /** Drawable to render, or null to only show the tooltip */
  @Nullable
  private final IDrawable drawable;
  private final int x, y, width, height;
  /** Tooltip to show while hovered, null to show nothing */
  @Nullable
  private final List<Component> tooltip;

  public TooltipWidget(@Nullable IDrawable drawable, int x, int y, int width, int height, @Nullable List<Component> tooltip) {
    this.drawable = drawable;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.tooltip = tooltip;
  }

  /** Adds a drawable plus its tooltip at the given position */
  public static void add(IRecipeExtrasBuilder builder, IDrawable drawable, int x, int y, @Nullable List<Component> tooltip) {
    builder.addDrawable(drawable, x, y);
    builder.addWidget(new TooltipWidget(null, x, y, drawable.getWidth(), drawable.getHeight(), tooltip));
  }

  /** Adds a tooltip area with no drawable */
  public static void addArea(IRecipeExtrasBuilder builder, int x, int y, int width, int height, @Nullable List<Component> tooltip) {
    builder.addWidget(new TooltipWidget(null, x, y, width, height, tooltip));
  }

  @Override
  public ScreenPosition getPosition() {
    return new ScreenPosition(x, y);
  }

  @Override
  public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
    if (drawable != null) {
      drawable.draw(graphics, x, y);
    }
  }

  @Override
  public void getTooltip(ITooltipBuilder tooltipBuilder, double mouseX, double mouseY) {
    if (tooltip != null && !tooltip.isEmpty() && mouseX >= 0 && mouseX < width && mouseY >= 0 && mouseY < height) {
      tooltipBuilder.addAll(tooltip);
    }
  }
}
