package slimeknights.tconstruct.library.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import javax.annotation.Nonnull;

/**
 * Stand-in for Forge's {@code IFluidTank} + {@code IFluidHandler} pair, for a single tank.
 * <p>
 * Fabric has no equivalent types: the Transfer API models the same concept as a
 * {@code Storage<FluidVariant>}, which has a different shape (variants + transactions instead of
 * "one fluid stack"). Tinkers' code, its screens and its JSON all want the Forge shape, so this
 * interface keeps it and {@link FluidTankBase} implements it on top of Porting Lib's {@code FluidTank}.
 * <p>
 * Amounts are {@code long} throughout, matching Porting Lib's {@code FluidStack}/{@code FluidTank}.
 * The default {@link #fill}/{@link #drain} bodies mirror Forge's {@code FluidTank} semantics, including
 * honouring {@link FluidAction}.
 */
public interface SimpleFluidTank {
  @Nonnull
  FluidStack getFluid();

  /** Maximum amount of fluid this tank can hold */
  long getCapacity();

  /** Called to set the fluid, used by the default fill/drain logic */
  void setFluid(FluidStack fluid);

  /** Amount of fluid currently held */
  default long getFluidAmount() {
    return getFluid().getAmount();
  }

  /** If true, this tank is empty */
  default boolean isEmpty() {
    return getFluid().isEmpty();
  }

  /** If true, this tank accepts the given fluid */
  default boolean isFluidValid(FluidStack stack) {
    return true;
  }

  /** The tank count, always 1 */
  default int getTanks() {
    return 1;
  }

  /**
   * Used by {@link #fill(FluidStack, FluidAction)}, {@link #drain(long, FluidAction)}, and {@link #drain(FluidStack, FluidAction)} to update the fluid result.
   * Allows updating the fluid without needing to call {@link #getFluid()} again, in case it has a cost.
   * @param updated  New fluid stack
   * @param change   Amount the fluid grew or shrunk by.
   */
  default void updateFluid(FluidStack updated, long change) {
    if (change != 0) {
      setFluid(updated);
    }
  }


  /* Redirect duplicate methods */

  @Nonnull
  default FluidStack getFluidInTank(int tank) {
    return getFluid();
  }

  default long getTankCapacity(int tank) {
    return getCapacity();
  }

  default boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
    return isFluidValid(stack);
  }


  /* Filling and draining */

  default long fill(FluidStack resource, FluidAction action) {
    // if nothing to fill, do nothing
    if (resource.isEmpty() || !isFluidValid(resource)) {
      return 0;
    }
    FluidStack fluid = getFluid();

    // if we have nothing, fill as much as possible
    if (fluid.isEmpty()) {
      long amount = Math.min(getCapacity(), resource.getAmount());
      if (action.execute()) {
        updateFluid(new FluidStack(resource, amount), amount);
      }
      return amount;
    }

    // if unable to fill, nothing more to do
    if (!fluid.isFluidEqual(resource)) {
      return 0;
    }

    long capacity = getCapacity();
    long filled = Math.min(capacity - fluid.getAmount(), resource.getAmount());
    if (action.execute()) {
      fluid.grow(filled);
      updateFluid(fluid, filled);
    }
    return filled;
  }

  /** Common logic between both drain methods */
  private FluidStack drain(FluidStack fluid, long maxDrain, FluidAction action) {
    // preconditions: fluid is not empty, maxDrain > 0
    // limit max drain to current fluid
    long drained = maxDrain;
    if (fluid.getAmount() < drained) {
      drained = fluid.getAmount();
    }
    // build the result
    FluidStack result = new FluidStack(fluid, drained);
    if (action.execute()) {
      fluid.shrink(drained);
      updateFluid(fluid, -drained);
    }
    return result;
  }

  @Nonnull
  default FluidStack drain(FluidStack resource, FluidAction action) {
    if (resource.isEmpty()) {
      return FluidStack.EMPTY;
    }
    FluidStack fluid = getFluid();
    if (fluid.isEmpty() || !fluid.isFluidEqual(resource)) {
      return FluidStack.EMPTY;
    }
    return drain(fluid, resource.getAmount(), action);
  }

  @Nonnull
  default FluidStack drain(long maxDrain, FluidAction action) {
    if (maxDrain <= 0) {
      return FluidStack.EMPTY;
    }
    FluidStack fluid = getFluid();
    if (fluid.isEmpty()) {
      return FluidStack.EMPTY;
    }
    return drain(fluid, maxDrain, action);
  }
}
