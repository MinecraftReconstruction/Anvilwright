package slimeknights.tconstruct.common.data.tags;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.concurrent.CompletableFuture;

public class BlockEntityTypeTagProvider extends FabricTagProvider<BlockEntityType<?>> {
  @SuppressWarnings("deprecation")
  public BlockEntityTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, Registries.BLOCK_ENTITY_TYPE, registriesFuture);
  }

  /** Creates a RL for iron chests */
  private static void ironchest(FabricTagBuilder appender, String name) {
    ResourceLocation chest = new ResourceLocation("ironchest", name + "_chest");
    appender.addOptional(chest).addOptional(chest.withPrefix("trapped_"));
    if (!"dirt".equals(name)) {
      appender.addOptional(new ResourceLocation("ironshulkerbox", name + "_shulker_box"));
    }
  }

  @Override
  protected void addTags(HolderLookup.Provider provider) {
    FabricTagBuilder sideInventories = this.getOrCreateTagBuilder(TinkerTags.TileEntityTypes.SIDE_INVENTORIES);
    sideInventories.add(
      BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST, BlockEntityType.BARREL, BlockEntityType.SHULKER_BOX,
      BlockEntityType.DISPENSER, BlockEntityType.DROPPER, BlockEntityType.HOPPER);
    // TODO 1.21: verify if BlockEntityType.CHISELED_BOOKSHELF has fixed the bug where setItem(ItemStack.EMPTY) doesn't work so it can be whitelisted.
    sideInventories.addOptional(new ResourceLocation("immersiveengineering", "woodencrate"));
    ironchest(sideInventories, "iron");
    ironchest(sideInventories, "gold");
    ironchest(sideInventories, "diamond");
    ironchest(sideInventories, "copper");
    ironchest(sideInventories, "crystal");
    ironchest(sideInventories, "obsidian");
    ironchest(sideInventories, "dirt");

    // these block entities don't fully sync the fluid to client, so show simplified information
    this.getOrCreateTagBuilder(MantleTags.BlockEntities.HIDES_GAUGE_AMOUNT).add(TinkerSmeltery.faucet.get(), TinkerSmeltery.channel.get());
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Block Entity Type Tags";
  }
}
