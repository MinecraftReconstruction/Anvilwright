package slimeknights.tconstruct.smeltery.block.entity;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import slimeknights.tconstruct.library.fluid.EmptyFluidStorage;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

/** This class exists simply to allow us to have a block entity renderer for obsidian gauges. Though it is useful as a cache for the capability to render. */
public class GaugeBlockEntity extends BlockEntity {
  /** Cached neighbour tank, null until fetched, {@link EmptyFluidStorage#INSTANCE} if the neighbour has none */
  private Storage<FluidVariant> neighbor;
  public GaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public GaugeBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.gauge.get(), pos, state);
  }

  /** Gets the neighbor fluid handler. Used mainly for rendering client side */
  public Storage<FluidVariant> getTank() {
    if (level == null) {
      return EmptyFluidStorage.INSTANCE;
    }
    // if we have not fetched the neighbor, fetch it
    if (neighbor == null) {
      Direction side = getBlockState().getValue(BlockStateProperties.FACING);
      Storage<FluidVariant> found = FluidStorage.SIDED.find(level, getBlockPos().relative(side.getOpposite()), side);
      neighbor = found == null ? EmptyFluidStorage.INSTANCE : found;
    }
    // return tank or empty tank
    return neighbor;
  }
}
