package slimeknights.tconstruct.library.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.Collections;
import java.util.Iterator;

/**
 * A tank that holds nothing and accepts nothing.
 * <p>
 * This is the Fabric-side stand-in for Forge's {@code EmptyFluidHandler.INSTANCE}, which Tinkers uses as the
 * "no neighbour", "no tank" and "fallback" value. It implements both shapes the port needs
 * ({@link Storage} for the Transfer API and {@link SimpleFluidTank} for the Forge-shaped call sites and
 * screens), so it can be handed to either without a cast.
 */
public class EmptyFluidStorage implements Storage<FluidVariant>, SimpleFluidTank {
  public static final EmptyFluidStorage INSTANCE = new EmptyFluidStorage();

  protected EmptyFluidStorage() {}

  /* SimpleFluidTank */

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
    // no-op, this tank holds nothing
  }

  /* Storage */

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return 0;
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return 0;
  }

  @Override
  public Iterator<StorageView<FluidVariant>> iterator() {
    return Collections.emptyIterator();
  }
}
