package slimeknights.tconstruct.library;

import net.minecraft.world.item.ItemDisplayContext;

/**
 * Custom transform types used for tinkers item rendering.
 * <p>
 * <b>Porting note:</b> upstream registers its own {@link ItemDisplayContext} values through Forge's
 * {@code DISPLAY_CONTEXTS} registry. Vanilla's display context is an enum that cannot be extended and Fabric has no
 * such registry, so each entry falls back to the vanilla context upstream declares as its fallback. Item positions in
 * the melter, tables, fluid cannon and throwing are therefore the vanilla ones - see docs/BEHAVIOUR-DIFFERENCES.md.
 */
public class TinkerItemDisplays {
  private TinkerItemDisplays() {}

  /** Kept for call-site compatibility; there is nothing to register on Fabric */
  public static void init() {}

  /** Used by the melter and smeltery for display of items its melting */
  public static final ItemDisplayContext MELTER = ItemDisplayContext.NONE;
  /** Used by the part builder, crafting station, tinkers station, and tinker anvil */
  public static final ItemDisplayContext TABLE = ItemDisplayContext.NONE;
  /** Used by the casting table for item rendering */
  public static final ItemDisplayContext CASTING_TABLE = ItemDisplayContext.FIXED;
  /** Used by the casting basin for item rendering */
  public static final ItemDisplayContext CASTING_BASIN = ItemDisplayContext.NONE;
  /** Used by the fluid cannon for display of the item in front */
  public static final ItemDisplayContext FLUID_CANNON = ItemDisplayContext.FIXED;
  /** Used by throwing to allow adjusting the tool position */
  public static final ItemDisplayContext THROWN = ItemDisplayContext.FIXED;
}
