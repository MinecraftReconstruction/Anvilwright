package slimeknights.tconstruct.library.client.model;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.client.model.ModelProperty;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;

/** Model data properties used in Tinker's Construct */
public class ModelProperties {
  /** Property for fluid stack in a fluid model */
  public static final ModelProperty<FluidStack> FLUID_STACK = new ModelProperty<>();
  /** Maximum size for a fluid tank in a tank model */
  public static final ModelProperty<Long> TANK_CAPACITY = new ModelProperty<>();
  /** Model property for a single material on a tool part. */
  public static final ModelProperty<MaterialVariantId> MATERIAL = new ModelProperty<>();
  /** Model property for the materials list on a tool. */
  public static final ModelProperty<MaterialIdNBT> MATERIALS = new ModelProperty<>();
  /** Property holding a live fluid tank instance for the tank model */
  public static final ModelProperty<FluidTank> FLUID_TANK = new ModelProperty<>();
}
