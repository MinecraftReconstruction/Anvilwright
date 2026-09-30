package slimeknights.tconstruct.smeltery.block.entity.tank;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.inventory.SingleItemHandler;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.fluid.IFluidTankUpdater;

import javax.annotation.Nullable;

/**
 * Fluid storage that proxies to the tank inside the stored item stack (a bucket, a tank item, ...).
 * <p>
 * Upstream implements Forge's {@code IFluidHandlerItem} on the item stack's capability; Fabric models the same
 * thing as a {@code Storage<FluidVariant>} looked up with a {@link ContainerItemContext}. The context is created
 * per operation because filling can replace the item (bucket to empty bucket) and the new stack has to be written
 * back into the slot - that write-back is what the upstream {@code getContainer()} call did.
 */
public class ProxyItemTank<T extends MantleBlockEntity & IFluidTankUpdater> extends SingleItemHandler<T> implements Storage<FluidVariant> {
  public ProxyItemTank(T parent) {
    super(parent, 1);
  }

  @SuppressWarnings("deprecation")
  @Override
  protected boolean isItemValid(ItemVariant variant) {
    // can only store items that are fluid handlers, though allow blacklist in case something is really broken
    // blacklist is mostly used for items that don't support incremental filling, as this block really isn't good at working with them
    // we check the container item so we don't have to put every bucket in the tag. Not bothering with complex container items; odds are item stack sensitive just returns the same item
    ItemStack stack = variant.toStack();
    Item craftRemainingItem = stack.getItem().getCraftingRemainingItem();
    return !stack.is(TinkerTags.Items.PROXY_TANK_BLACKLIST)
      && (craftRemainingItem == null || !RegistryHelper.contains(TinkerTags.Items.PROXY_TANK_BLACKLIST, craftRemainingItem))
      && FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
  }

  /** Used by the fluid handler logic to sync changes as we directly mutate the internal stack */
  private void setStack(ItemStack newStack, boolean syncSame) {
    // if swapping to an empty stack, switch to the empty stack instance
    // prevents accidently having a 0 stack size capability
    if (newStack.isEmpty()) {
      newStack = ItemStack.EMPTY;
    }
    // update stack
    ItemStack oldStack = getStack();
    super.setStack(newStack);

    // server side may need to sync
    Level world = parent.getLevel();
    boolean needsUpdate = world != null && !world.isClientSide;
    if (oldStack != newStack) {
      // if the stack instance changed, discard cached cap and sync
      if (needsUpdate) {
        // both stacks being empty means our stack shrunk by 1 and is being replaced with ItemStack.EMPTY
        needsUpdate = (oldStack.isEmpty() && newStack.isEmpty()) || !ItemStack.isSameItemSameTags(oldStack, newStack);
      }
    } else if (needsUpdate) {
      needsUpdate = syncSame;
    }
    // sync changes
    if (needsUpdate) {
      parent.onTankContentsChanged();
      BlockPos pos = parent.getBlockPos();
      TinkerNetwork.getInstance().sendToClientsAround(new InventorySlotSyncPacket(newStack, 0, pos), world, pos);
    }
  }

  @Override
  public void setStack(ItemStack newStack) {
    setStack(newStack, false);
  }

  /** Gets the fluid storage of the stored item, or null if the item has no tank */
  @Nullable
  private Storage<FluidVariant> findItemTank(ContainerItemContext context) {
    ItemStack stack = getStack();
    if (stack.isEmpty()) {
      return null;
    }
    return FluidStorage.ITEM.find(stack, context);
  }

  /** Copies the (possibly replaced) container stack from the context back into the slot */
  private void syncContainer(ContainerItemContext context) {
    setStack(context.getItemVariant().toStack((int)context.getAmount()), true);
  }

  /** The fluid currently held by the item, for rendering and tooltips */
  public FluidStack getFluid() {
    Storage<FluidVariant> tank = findItemTank(ContainerItemContext.withConstant(getStack()));
    return tank == null ? FluidStack.EMPTY : TransferUtil.firstCopyOrEmpty(tank);
  }

  /** The capacity of the item's tank, for rendering and tooltips */
  public long getCapacity() {
    Storage<FluidVariant> tank = findItemTank(ContainerItemContext.withConstant(getStack()));
    return tank == null ? 0 : TransferUtil.firstCapacity(tank);
  }

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    ContainerItemContext context = ContainerItemContext.withInitial(getStack());
    Storage<FluidVariant> tank = findItemTank(context);
    if (tank == null) {
      return 0;
    }
    long inserted;
    try (Transaction nested = Transaction.openNested(transaction)) {
      inserted = tank.insert(resource, maxAmount, nested);
      if (inserted > 0) {
        nested.commit();
      }
    }
    // force a sync of the item stack; the container may have been replaced (bucket to empty bucket)
    if (inserted > 0) {
      syncContainer(context);
    }
    return inserted;
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    ContainerItemContext context = ContainerItemContext.withInitial(getStack());
    Storage<FluidVariant> tank = findItemTank(context);
    if (tank == null) {
      return 0;
    }
    long extracted;
    try (Transaction nested = Transaction.openNested(transaction)) {
      extracted = tank.extract(resource, maxAmount, nested);
      if (extracted > 0) {
        nested.commit();
      }
    }
    if (extracted > 0) {
      syncContainer(context);
    }
    return extracted;
  }
}
