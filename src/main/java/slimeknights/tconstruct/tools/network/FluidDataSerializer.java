package slimeknights.tconstruct.tools.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

/** Serializer for fluid stack data in entities */
public class FluidDataSerializer implements EntityDataSerializer<FluidStack> {
  @Override
  public void write(FriendlyByteBuf buffer, FluidStack stack) {
    stack.writeToPacket(buffer);
  }

  @Override
  public FluidStack read(FriendlyByteBuf buffer) {
    return FluidStack.readFromPacket(buffer);
  }

  @Override
  public FluidStack copy(FluidStack stack) {
    return stack.copy();
  }
}
