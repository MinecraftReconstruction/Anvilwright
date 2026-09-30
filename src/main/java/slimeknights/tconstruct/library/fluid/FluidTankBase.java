package slimeknights.tconstruct.library.fluid;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.minecraft.world.level.Level;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import javax.annotation.Nonnull;

public class FluidTankBase<T extends MantleBlockEntity> extends FluidTank implements SimpleFluidTank {

  protected T parent;

  public FluidTankBase(long capacity, T parent) {
    super(capacity);
    this.parent = parent;
  }

  /**
   * Both {@link SimpleFluidTank} and Porting Lib's {@code SingleSlotStorage} (via {@link FluidTank}) declare a default
   * {@code iterator()}, so the inherited pair is ambiguous. Porting Lib's tank is a real transactional implementation,
   * so defer to it instead of the interface default.
   */
  @Override
  public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant>> iterator() {
    return super.iterator();
  }

  /*
   * The fill/drain overrides below replace the interface defaults for one reason: they must fire
   * {@link #onContentsChanged()} at the exact moment the fluid changes, which is what drives the light
   * update and the FluidUpdatePacket. Porting Lib's FluidTank only notifies from onFinalCommit(), which
   * does not run for these direct tank style calls. The bodies otherwise mirror Forge's FluidTank, with
   * the fork's fix kept: the "how much did we fill" value is tracked in a local so that it stays correct
   * even if onContentsChanged() mutates the tank.
   */
  @Override
  public long fill(FluidStack resource, FluidAction action) {
    if (resource.isEmpty() || !isFluidValid(resource)) {
      return 0;
    }
    FluidStack fluid = getFluid();
    long capacity = getCapacity();
    if (action.simulate()) {
      if (fluid.isEmpty()) {
        return Math.min(capacity, resource.getAmount());
      }
      if (!fluid.isFluidEqual(resource)) {
        return 0;
      }
      return Math.min(capacity - fluid.getAmount(), resource.getAmount());
    }
    if (fluid.isEmpty()) {
      long filled = Math.min(capacity, resource.getAmount());
      setFluid(new FluidStack(resource, filled));
      onContentsChanged();
      return filled;
    }
    if (!fluid.isFluidEqual(resource)) {
      return 0;
    }
    long filled = Math.min(capacity - fluid.getAmount(), resource.getAmount());
    if (filled > 0) {
      fluid.grow(filled);
      setFluid(fluid);
      onContentsChanged();
    }
    return filled;
  }

  @Nonnull
  @Override
  public FluidStack drain(FluidStack resource, FluidAction action) {
    if (resource.isEmpty()) {
      return FluidStack.EMPTY;
    }
    FluidStack fluid = getFluid();
    if (fluid.isEmpty() || !fluid.isFluidEqual(resource)) {
      return FluidStack.EMPTY;
    }
    return drain(resource.getAmount(), action);
  }

  @Nonnull
  @Override
  public FluidStack drain(long maxDrain, FluidAction action) {
    if (maxDrain <= 0) {
      return FluidStack.EMPTY;
    }
    FluidStack fluid = getFluid();
    if (fluid.isEmpty()) {
      return FluidStack.EMPTY;
    }
    long drained = Math.min(maxDrain, fluid.getAmount());
    FluidStack result = new FluidStack(fluid, drained);
    if (action.execute() && drained > 0) {
      fluid.shrink(drained);
      setFluid(fluid.isEmpty() ? FluidStack.EMPTY : fluid);
      onContentsChanged();
    }
    return result;
  }

  @Override
  public void onContentsChanged() {
    if (parent instanceof IFluidTankUpdater) {
      ((IFluidTankUpdater) parent).onTankContentsChanged();
    }

    parent.setChanged();
    Level level = parent.getLevel();
    if(level != null && !level.isClientSide) {
      TinkerNetwork.getInstance().sendToClientsAround(new FluidUpdatePacket(parent.getBlockPos(), this.getFluid()), level, parent.getBlockPos());
    }
  }
}
