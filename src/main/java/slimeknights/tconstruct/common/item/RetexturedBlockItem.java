package slimeknights.tconstruct.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.mantle.util.RetexturedHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Tinkers-side stand-in for Mantle 1.9's {@code slimeknights.mantle.item.RetexturedBlockItem}.
 * <p>
 * Mantle 1.11 dropped this class together with the old creative tab API, so the port keeps a copy here for the
 * items the Fabric fork built on it. The behaviour is the same as 1.9's: the tab is filled with one stack per
 * texture block from the tag, and the tooltip shows the texture that was picked.
 */
@SuppressWarnings("WeakerAccess")
public class RetexturedBlockItem extends BlockTooltipItem {
  /** Tag used for getting the texture */
  protected final TagKey<Item> textureTag;

  public RetexturedBlockItem(Block block, TagKey<Item> textureTag, Item.Properties builder) {
    super(block, builder);
    this.textureTag = textureTag;
  }

  /** Adds the retextured variants to the creative tab */
  public void fillItemCategory(CreativeModeTab.Output items) {
    addTagVariants(this.getBlock(), textureTag, items, true);
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
    addTooltip(stack, tooltip);
    super.appendHoverText(stack, level, tooltip, flag);
  }


  /* Utils */

  /** Gets the texture name from a stack */
  public static String getTextureName(ItemStack stack) {
    return RetexturedHelper.getTextureName(stack);
  }

  /** Gets the texture from a stack, or air if none */
  public static Block getTexture(ItemStack stack) {
    return RetexturedHelper.getBlock(getTextureName(stack));
  }

  /** Adds the texture block to the tooltip */
  public static void addTooltip(ItemStack stack, List<Component> tooltip) {
    Block block = getTexture(stack);
    if (block != Blocks.AIR) {
      tooltip.add(block.getName());
    }
  }

  /** Creates a new item stack with the given block as its texture tag */
  public static ItemStack setTexture(ItemStack stack, String name) {
    if (!name.isEmpty()) {
      RetexturedHelper.setTexture(stack, name);
    } else if (!stack.isEmpty()) {
      RetexturedHelper.setTexture(stack, name);
    }
    return stack;
  }

  /** Creates a new item stack with the given block as its texture tag */
  public static ItemStack setTexture(ItemStack stack, @Nullable Block block) {
    if (block == null || block == Blocks.AIR) {
      return setTexture(stack, "");
    }
    return setTexture(stack, Objects.requireNonNull(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block)).toString());
  }

  /**
   * Adds all blocks from the block tag to the given tab
   * @param block            Dynamic texture item instance
   * @param tag              Tag for texturing
   * @param list             Tab to add to
   * @param showAllVariants  If true, shows all variants. If false, shows just the first
   */
  public static void addTagVariants(BlockItem block, TagKey<Item> tag, CreativeModeTab.Output list, boolean showAllVariants) {
    // Mantle 1.11 expresses "add to tab" as a predicate, so wrap the output and always accept
    RetexturedHelper.addTagVariants(stack -> {
      list.accept(stack);
      return true;
    }, block, tag);
  }
}
