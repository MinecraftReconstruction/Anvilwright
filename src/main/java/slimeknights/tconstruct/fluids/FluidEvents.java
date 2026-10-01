package slimeknights.tconstruct.fluids;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.fluids.util.ConstantFluidContainerWrapper;

/**
 * Event subscriber for modifier events
 * Note the way the subscribers are set up, technically works on anything that has the tic_modifiers tag
 * <p>
 * <b>Porting note:</b> upstream registers the powder snow bucket's fluid container through Forge's
 * {@code AttachCapabilitiesEvent<ItemStack>}; on Fabric the same thing is expressed with
 * {@code FluidStorage.ITEM.registerForItems} (the pattern already used by {@link slimeknights.tconstruct.fluids.item.ContainerFoodItem}).
 */
@SuppressWarnings("unused")
public class FluidEvents {
  /** Registers fluid related common setup. Call this once during common setup. */
  public static void init() {
    FuelRegistry.INSTANCE.add(TinkerFluids.blazingBlood.asItem(), 30000);
    // powder snow bucket is a fluid container so it can be used as a tool tank
    FluidStorage.ITEM.registerForItems(
      (stack, context) -> new ConstantFluidContainerWrapper(new FluidStack(TinkerFluids.powderedSnow.get(), FluidConstants.BUCKET), stack, context),
      Items.POWDER_SNOW_BUCKET);
  }
}
