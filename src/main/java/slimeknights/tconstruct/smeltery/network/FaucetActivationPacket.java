package slimeknights.tconstruct.smeltery.network;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import slimeknights.mantle.network.packet.BlockEntityPacket;

/**
 * Sent to clients to activate the faucet animation clientside.
 * TODO 1.21: make record.
 */
@RequiredArgsConstructor
@ToString(callSuper = true)
public class FaucetActivationPacket implements BlockEntityPacket<FaucetBlockEntity> {
  protected final BlockPos pos;
  protected final FluidStack fluid;
  private final boolean isPouring;

  public FaucetActivationPacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.fluid = buffer.readFluidStack();
    this.isPouring = buffer.readBoolean();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    fluid.writeToPacket(buffer);
    buffer.writeBoolean(isPouring);
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<FaucetBlockEntity> type() {
    return FaucetBlockEntity.class;
  }

  @Override
  public void handleBlockEntity(Context context, FaucetBlockEntity be) {
    be.onActivationPacket(fluid, isPouring);
  }
}
