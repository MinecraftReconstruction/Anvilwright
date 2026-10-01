package slimeknights.tconstruct.library.fluid;

import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Whether a fluid operation should actually happen, or just be simulated.
 * <p>
 * Upstream targets Forge, where this is {@code IFluidHandler.FluidAction} and comes for free with the fluid API.
 * Fabric has no such type - the Transfer API expresses the same idea by running the operation inside a
 * {@code Transaction} and throwing it away, or by using {@code StorageUtil.simulateInsert}/{@code simulateExtract}.
 * <p>
 * This shim exists so that the ~46 files upstream added in 3.12 that take a {@code FluidAction} parameter can keep
 * their signatures while the implementations underneath translate it into whatever the Fabric side needs. It is
 * deliberately not part of Mantle: Mantle should stay a faithful port of the upstream library, and this is a
 * Tinkers-internal bridge.
 * <p>
 * The method names and semantics mirror Forge's enum, so call sites do not have to change:
 * {@link #execute()} and {@link #simulate()} are exact opposites, and {@code EXECUTE} is the "do it" case.
 *
 * @see slimeknights.tconstruct.library.fluid.FluidTransferUtil
 */
@Internal
public enum FluidAction {
  /** Perform the operation for real */
  EXECUTE(true),
  /** Only report what the operation would do */
  SIMULATE(false);

  private final boolean execute;

  FluidAction(boolean execute) {
    this.execute = execute;
  }

  /** @return true if this action should actually modify the fluid */
  public boolean execute() {
    return execute;
  }

  /** @return true if this action should only simulate */
  public boolean simulate() {
    return !execute;
  }
}
