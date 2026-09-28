package slimeknights.tconstruct.smeltery.network;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.util.BlockEntityHelper;

public class FluidUpdatePacket implements IThreadsafePacket {

/**
 * Packet for when the fluid changes in a block entity.
 * TODO 1.21: make record.
 */
@RequiredArgsConstructor
@ToString
public class FluidUpdatePacket implements BlockEntityPacket<IFluidPacketReceiver> {
  protected final BlockPos pos;
  protected final FluidStack fluid;

  public FluidUpdatePacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.fluid = FluidStack.readFromPacket(buffer);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    fluid.writeToPacket(buffer);
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<IFluidPacketReceiver> type() {
    return IFluidPacketReceiver.class;
  }

  @Override
  public void handleBlockEntity(Context context, IFluidPacketReceiver be) {
    be.updateFluidTo(fluid);
  }

  /** Interface to implement for anything wishing to receive fluid updates */
  public interface IFluidPacketReceiver {
    /**
     * Updates the current fluid to the specified value
     * @param fluid New fluidstack
     */
    void updateFluidTo(FluidStack fluid);
  }
}
