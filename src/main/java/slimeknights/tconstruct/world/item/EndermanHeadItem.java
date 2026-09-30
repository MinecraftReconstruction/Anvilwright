package slimeknights.tconstruct.world.item;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;

/** Head item for enderman head, which counts as a pumpkin on the head */
public class EndermanHeadItem extends StandingAndWallBlockItem {
  public EndermanHeadItem(Block pBlock, Block pWallBlock, Properties pProperties, Direction pAttachmentDirection) {
    super(pBlock, pWallBlock, pProperties, pAttachmentDirection);
  }

  /**
   * Forge hook that has no Porting Lib interface; kept for API compatibility.
   * Vanilla hardcodes the carved pumpkin check, so this needs a mixin to actually take effect.
   * Tracked in docs/BEHAVIOUR-DIFFERENCES.md.
   */
  public boolean isEnderMask(ItemStack stack, Player player, EnderMan endermanEntity) {
    return true;
  }
}
