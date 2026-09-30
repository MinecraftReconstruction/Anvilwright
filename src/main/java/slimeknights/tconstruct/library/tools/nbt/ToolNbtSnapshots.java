package slimeknights.tconstruct.library.tools.nbt;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper to make NBT backed tool state transactional.
 * <p>
 * The Fabric Transfer API can only roll a change back if something copied the previous state, so any modifier that
 * writes into a tool's persistent NBT as part of a transaction should call {@link #updateSnapshots} right before the
 * change. Upstream Forge code never needed this: on Forge the same data is exposed through capabilities whose
 * simulate/execute pair is done by the caller, while on Fabric the writes must be undone when the transaction aborts.
 * <p>
 * This mirrors the snapshot handling the pre-3.12 port had in {@code library/modifiers/impl/InventoryModifier};
 * the list is indexed by transaction nesting depth and, like that class, shared between all tools (it is only
 * touched on the server thread).
 */
public final class ToolNbtSnapshots {
  private ToolNbtSnapshots() {}

  /** Snapshots of the persistent data, indexed by transaction nesting depth */
  private static final List<CompoundTag> SNAPSHOTS = new ArrayList<>();

  /**
   * Copies the tool data for the given transaction, so it can be restored if the transaction aborts.
   * Call this every time the tool data is about to change as part of a transaction.
   * @param tool         Tool being changed
   * @param transaction  Transaction the change happens in, may be null (then nothing needs to be rolled back)
   */
  public static void updateSnapshots(IToolStackView tool, TransactionContext transaction) {
    if (transaction == null) {
      return;
    }
    // make sure we have enough storage for snapshots
    while (SNAPSHOTS.size() <= transaction.nestingDepth()) {
      SNAPSHOTS.add(null);
    }
    // if the snapshot is null, we need to create it, and we need to register a callback
    if (SNAPSHOTS.get(transaction.nestingDepth()) == null) {
      SNAPSHOTS.set(transaction.nestingDepth(), tool.getPersistentData().getCopy());
      transaction.addCloseCallback(new SnapshotCallback(tool));
    }
  }

  /** Restores the tool data if the transaction that changed it gets aborted */
  private record SnapshotCallback(IToolStackView tool) implements TransactionContext.CloseCallback {
    @Override
    public void onClose(TransactionContext transaction, TransactionContext.Result result) {
      // get and remove the relevant snapshot
      CompoundTag snapshot = SNAPSHOTS.set(transaction.nestingDepth(), null);
      if (result.wasAborted()) {
        // if the transaction was aborted, revert to the state of the snapshot
        if (snapshot != null) {
          tool.getPersistentData().copyFrom(snapshot);
        }
      } else if (transaction.nestingDepth() > 0 && SNAPSHOTS.get(transaction.nestingDepth() - 1) == null) {
        // no snapshot yet at the parent level, so move this one up and listen to the parent transaction instead
        SNAPSHOTS.set(transaction.nestingDepth() - 1, snapshot);
        transaction.getOpenTransaction(transaction.nestingDepth() - 1).addCloseCallback(this);
      }
    }
  }
}
