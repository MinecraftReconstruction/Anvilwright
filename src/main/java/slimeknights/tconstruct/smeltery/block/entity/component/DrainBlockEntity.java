package slimeknights.tconstruct.smeltery.block.entity.component;

import io.github.fabricators_of_create.porting_lib.block.CustomDataPacketHandlingBlockEntity;
import io.github.fabricators_of_create.porting_lib.block.CustomUpdateTagHandlingBlockEntity;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryInputOutputBlockEntity.SmelteryFluidIO;
import slimeknights.tconstruct.smeltery.block.entity.tank.IDisplayFluidListener;

import javax.annotation.Nonnull;
import slimeknights.mantle.client.model.ModelData;
import java.util.Objects;

/**
 * Fluid IO extension to display controller fluid
 */
public class DrainBlockEntity extends SmelteryFluidIO implements IDisplayFluidListener, CustomUpdateTagHandlingBlockEntity, CustomDataPacketHandlingBlockEntity {
  @Getter
  private FluidStack displayFluid = FluidStack.EMPTY;

  public DrainBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.drain.get(), pos, state);
  }

  protected DrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Nonnull
  @Override
  public ModelData getModelData() {
    return RetexturedHelper.getModelDataBuilder(getTexture()).with(ModelProperties.FLUID_STACK, displayFluid).build();
  }

  @Override
  public void notifyDisplayFluidUpdated(FluidStack fluid) {
    if (!fluid.isFluidEqual(displayFluid)) {
      // no need to copy as the fluid was copied by the caller
      displayFluid = fluid;
      assert level != null;
      BlockState state = getBlockState();
      level.sendBlockUpdated(worldPosition, state, state, 48);
    }
  }

  @Override
  public BlockPos getListenerPos() {
    return worldPosition;
  }


  /* Updating */

  // override instead of writeSynced to avoid writing master to the main tag twice
  @Override
  public CompoundTag getUpdateTag() {
    CompoundTag nbt = super.getUpdateTag();
    writeMaster(nbt);
    return nbt;
  }

  @Override
  public void handleUpdateTag(CompoundTag tag) {
    CustomUpdateTagHandlingBlockEntity.super.handleUpdateTag(tag);
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }
}
