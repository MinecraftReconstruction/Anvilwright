package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.BlockPos;
import slimeknights.mantle.block.entity.MantleBlockEntity;

import java.util.Collections;

/**
 * Fuel module variant that supports both item and fluid fuels. Only supports a single fluid position which should
 * not change.
 * <p>
 * Upstream kept its own pair of {@code LazyOptional} capability fields here; in this port the tank list lives in
 * {@link FuelModule} (which already tries fluid storage, then item storage, at every position it is given), so all
 * this class has to add is the single position and the "item fuel has no fluid to show" case for the UI.
 */
public class SolidFuelModule extends FuelModule {
  /** Location of the fuel tank */
  private final BlockPos fuelPos;

  public SolidFuelModule(MantleBlockEntity parent, BlockPos fuelPos) {
    super(parent, () -> Collections.singletonList(fuelPos));
    this.fuelPos = fuelPos;
  }

  /** Location of the fuel tank */
  public BlockPos getFuelPos() {
    return fuelPos;
  }

  @Override
  public FuelInfo getFuelInfo() {
    FuelInfo info = super.getFuelInfo();
    if (info.isEmpty() && itemHandler != null) {
      return FuelInfo.ITEM;
    }
    return info;
  }
}
