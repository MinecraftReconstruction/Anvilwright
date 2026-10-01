package slimeknights.tconstruct.common.util;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * Tinkers-side stand-in for Mantle 1.9's {@code slimeknights.mantle.util.SupplierCreativeTab}: a creative tab whose
 * icon comes from an item supplier. Mantle 1.11 dropped it along with the old tab API, and Fabric's
 * {@link FabricItemGroup#builder()} provides the same builder the fork code expects (minus Forge's
 * {@code withTabsBefore}, which has no Fabric equivalent).
 */
public class SupplierCreativeTab {
  /** Creates a new item group with an item-supplied icon */
  public static CreativeModeTab.Builder create(String modId, String name, Supplier<ItemStack> supplier) {
    return FabricItemGroup.builder()
      .title(Component.translatable(String.format("itemGroup.%s.%s", modId, name)))
      .icon(supplier);
  }
}
