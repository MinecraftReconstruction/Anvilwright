package slimeknights.tconstruct.library.fluid;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.world.item.ItemStack;

/**
 * Fluid handler for an item stack that has no tank: accepts and returns nothing, but still carries the
 * container stack. This is the Fabric-side stand-in for Forge's {@code EmptyFluidHandler.INSTANCE} plus
 * {@code IFluidHandlerItem} - the container is what makes it usable as the fallback of
 * {@link slimeknights.tconstruct.smeltery.block.entity.tank.ProxyItemTank}.
 */
@RequiredArgsConstructor
public class EmptyFluidHandlerItem implements SimpleFluidTank {
  public static final EmptyFluidHandlerItem INSTANCE = new EmptyFluidHandlerItem(ItemStack.EMPTY);

  /** Container reference */
  @Getter
  private final ItemStack container;

  @Override
  public FluidStack getFluid() {
    return FluidStack.EMPTY;
  }

  @Override
  public long getCapacity() {
    return 0;
  }

  @Override
  public void setFluid(FluidStack fluid) {
    // no-op, this handler holds nothing
  }
}
