package slimeknights.tconstruct.smeltery.block.entity.module;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.mantle.block.entity.MantleBlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fuel module that supports multiple tanks, selecting just one for the fuel result.
 * <p>
 * The tank list and the fuel lookup both live in {@link FuelModule}. On top of that, this class is the
 * {@code Storage<FluidVariant>} that the smeltery UI and the bucket slots proxy to. Upstream got that for free by
 * implementing Forge's {@code IFluidHandler} over a map of {@code LazyOptional} handlers; Fabric has neither, so
 * each tank is looked up through {@link FluidStorage#SIDED} and the operations walk the tank list in order - which
 * is what upstream's {@code fill}/{@code drain} did.
 */
public class MultitankFuelModule extends FuelModule implements Storage<FluidVariant> {
  public MultitankFuelModule(MantleBlockEntity parent, Supplier<List<BlockPos>> tankSupplier) {
    super(parent, tankSupplier);
  }

  /** Called on structure rebuild to drop the cached display list */
  public void clearFluidListeners() {
    clearDisplayHandlers();
    lastPos = NULL_POS;
  }

  /** Called when a servant is loaded to drop the cached display list */
  public void ensureTankPresent(BlockEntity be) {
    clearDisplayHandlers();
  }


  /* Fluid storage */

  /** Gets the most recently used fluid */
  public FluidStack getLastFluid() {
    BlockPos pos;
    if (!lastPos.equals(NULL_POS)) {
      pos = lastPos;
    } else {
      List<BlockPos> positions = tankSupplier.get();
      if (positions.isEmpty()) {
        return FluidStack.EMPTY;
      }
      pos = positions.get(0);
    }
    Storage<FluidVariant> tank = FluidStorage.SIDED.find(getLevel(), pos, null);
    return tank == null ? FluidStack.EMPTY : TransferUtil.firstCopyOrEmpty(tank);
  }

  @Override
  public Storage<FluidVariant> getTankStorage() {
    return this;
  }

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    long total = 0;
    long remaining = maxAmount;
    for (BlockPos pos : tankSupplier.get()) {
      if (remaining <= 0) {
        break;
      }
      Storage<FluidVariant> tank = FluidStorage.SIDED.find(getLevel(), pos, null);
      if (tank == null) {
        continue;
      }
      long inserted = tank.insert(resource, remaining, transaction);
      total += inserted;
      remaining -= inserted;
    }
    return total;
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    long total = 0;
    long remaining = maxAmount;
    for (BlockPos pos : tankSupplier.get()) {
      if (remaining <= 0) {
        break;
      }
      Storage<FluidVariant> tank = FluidStorage.SIDED.find(getLevel(), pos, null);
      if (tank == null) {
        continue;
      }
      long extracted = tank.extract(resource, remaining, transaction);
      total += extracted;
      remaining -= extracted;
    }
    return total;
  }

  /** Views of the fuel currently in the tanks; empty when nothing is burning */
  @Override
  public Iterator<StorageView<FluidVariant>> iterator() {
    List<StorageView<FluidVariant>> views = new ArrayList<>();
    for (BlockPos pos : tankSupplier.get()) {
      Storage<FluidVariant> tank = FluidStorage.SIDED.find(getLevel(), pos, null);
      if (tank != null) {
        for (StorageView<FluidVariant> view : tank.nonEmptyViews()) {
          views.add(view);
        }
      }
    }
    return Collections.unmodifiableList(views).iterator();
  }
}
