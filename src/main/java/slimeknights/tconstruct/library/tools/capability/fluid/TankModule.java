package slimeknights.tconstruct.library.tools.capability.fluid;

import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.tconstruct.library.fluid.FluidAction;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DisplayNameModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.SlotStackModifierHook;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolFluidCapability.FluidModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolNbtSnapshots;
import slimeknights.tconstruct.smeltery.item.TankItem;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Standard implementation of a tank module using the {@link ToolTankHelper}.
 * Feel free to request abstracting out an aspect of it if you wish to have less duplication in a non-standard implementation.
 * Unregistered as modifiers have no way to register new tool stats.
 */
@SuppressWarnings("deprecation")
@RequiredArgsConstructor
public class TankModule implements HookProvider, FluidModifierHook, VolatileDataModifierHook, ValidateModifierHook, ModifierRemovalHook, SlotStackModifierHook, DisplayNameModifierHook {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<TankModule>defaultHooks(ToolFluidCapability.HOOK, ModifierHooks.VOLATILE_DATA, ModifierHooks.VALIDATE, ModifierHooks.REMOVE, ModifierHooks.SLOT_STACK, ModifierHooks.DISPLAY_NAME);


  /** Helper handling updating fluids */
  private final ToolTankHelper helper;


  /* Module logic */

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT volatileData) {
    ToolFluidCapability.addTanks(modifier, volatileData, this);
  }

  @Override
  public Component getDisplayName(IToolStackView tool, ModifierEntry entry, Component name, @Nullable RegistryAccess access) {
    FluidStack fluid = helper.getFluid(tool);
    int capacity = helper.getCapacity(tool);
    if (fluid.isEmpty()) {
      // no fluid, display as: Tank Capacity: #,### mb
      return ToolTankHelper.CAPACITY_STAT.formatValue(capacity);
    } else {
      // fluid, display as: Fluid Name: #,### / #,### mb
      return fluid.getDisplayName().copy()
                  .append(": ")
                  .append(ToolTankHelper.CAPACITY_STAT.formatContents(fluid.getAmount(), capacity));
    }
  }

  @Override
  public long getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
    return helper.getCapacity(tool);
  }

  @Override
  public FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
    return helper.getFluid(tool);
  }

  @Override
  public SingleSlotStorage<FluidVariant> getSlot(IToolStackView tool, ModifierEntry modifier, int tank) {
    return new TankSlot(tool, modifier);
  }


  /* Cleanup */

  @Nullable
  @Override
  public Component validate(IToolStackView tool, ModifierEntry modifier) {
    FluidStack fluid = helper.getFluid(tool);
    int capacity = helper.getCapacity(tool);
    if (fluid.getAmount() > capacity) {
      fluid.setAmount(capacity);
      helper.setFluid(tool, fluid);
    }
    return null;
  }

  @Nullable
  @Override
  public Component onRemoved(IToolStackView tool, Modifier modifier) {
    helper.setFluid(tool, FluidStack.EMPTY);
    return null;
  }


  /* Filling and draining */

  @Override
  public long fill(ContainerItemContext context, IToolStackView tool, ModifierEntry modifier, FluidVariant resource, long maxAmount, TransactionContext tx) {
    // make sure this modifier is in charge of the tank, that is first come first serve
    if (resource.isBlank() || maxAmount <= 0) {
      return 0;
    }
    // if empty, just directly fill, setFluid will check capacity
    FluidStack current = helper.getFluid(tool);
    long capacity = helper.getCapacity(tool);
    if (current.isEmpty()) {
      long filled = Math.min(maxAmount, capacity);
      if (filled > 0) {
        ToolNbtSnapshots.updateSnapshots(tool, tx);
        helper.setFluid(tool, new FluidStack(resource, filled));
      }
      return filled;
    }
    // if the fluid matches and we have space, update
    if (current.getAmount() < capacity && current.getType().equals(resource)) {
      long filled = Math.min(maxAmount, capacity - current.getAmount());
      if (filled > 0) {
        ToolNbtSnapshots.updateSnapshots(tool, tx);
        helper.setFluid(tool, new FluidStack(current, current.getAmount() + filled));
      }
      return filled;
    }
    return 0;
  }

  @Override
  public long drain(ContainerItemContext context, IToolStackView tool, ModifierEntry modifier, FluidVariant resource, long maxAmount, TransactionContext tx) {
    if (maxAmount <= 0) {
      return 0;
    }
    // ensure we have something and it matches the request; a blank resource means drain whatever is in the tank
    FluidStack current = helper.getFluid(tool);
    if (current.isEmpty() || (!resource.isBlank() && !current.getType().equals(resource))) {
      return 0;
    }
    long drained = Math.min(current.getAmount(), maxAmount);
    if (drained > 0) {
      ToolNbtSnapshots.updateSnapshots(tool, tx);
      if (drained == current.getAmount()) {
        helper.setFluid(tool, FluidStack.EMPTY);
      } else {
        helper.setFluid(tool, new FluidStack(current, current.getAmount() - drained));
      }
    }
    return drained;
  }

  /** Live view of the module's tank, used when something needs a {@link SingleSlotStorage} instead of the tool */
  private class TankSlot implements SingleSlotStorage<FluidVariant> {
    private final IToolStackView tool;
    private final ModifierEntry modifier;

    private TankSlot(IToolStackView tool, ModifierEntry modifier) {
      this.tool = tool;
      this.modifier = modifier;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
      return fill(null, tool, modifier, resource, maxAmount, transaction);
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
      return drain(null, tool, modifier, resource, maxAmount, transaction);
    }

    @Override
    public boolean isResourceBlank() {
      return helper.getFluid(tool).getType().isBlank();
    }

    @Override
    public FluidVariant getResource() {
      return helper.getFluid(tool).getType();
    }

    @Override
    public long getAmount() {
      return helper.getFluid(tool).getAmount();
    }

    @Override
    public long getCapacity() {
      return helper.getCapacity(tool);
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
      return Collections.<StorageView<FluidVariant>>singletonList(this).iterator();
    }
  }


  /* Inventory slot stacking */

  /** Gets a tank instance for the given tool */
  private FluidTank getTank(IToolStackView tool) {
    FluidTank tank = new FluidTank(helper.getCapacity(tool));
    tank.setFluid(helper.getFluid(tool));
    return tank;
  }

  @Override
  public boolean overrideStackedOnOther(IToolStackView heldTool, ModifierEntry modifier, Slot slot, Player player) {
    ItemStack slotStack = slot.getItem();
    // must have something with possible fluid in the slot
    if (!slotStack.isEmpty() && TankItem.mayHaveFluid(slotStack)) {
      // target must be stack size 1, if not then its not safe to modify it
      if (slotStack.getCount() == 1) {
        FluidTank tank = getTank(heldTool);
        TransferResult result = FluidTransferHelper.interactWithStack(tank, slotStack, TransferDirection.REVERSE);
        // update held tank and slot item if something changed (either we have a result or the stack in the slot was shrunk)
        if (result != null) {
          if (player.level().isClientSide) {
            player.playSound(result.getSound());
          }
          helper.setFluid(heldTool, tank.getFluid());
          slot.set(FluidTransferHelper.getOrTransferFilled(player, slotStack, result.stack()));
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean overrideOtherStackedOnMe(IToolStackView slotTool, ModifierEntry modifier, ItemStack held, Slot slot, Player player, SlotAccess access) {
    // must have something with possible fluid held
    if (!held.isEmpty() && TankItem.mayHaveFluid(held)) {
      FluidTank tank = getTank(slotTool);
      TransferResult result = FluidTransferHelper.interactWithStack(tank, held, TransferDirection.AUTO);
      // update tank if something happened
      if (result != null) {
        if (player.level().isClientSide) {
          player.playSound(result.getSound());
        }
        helper.setFluid(slotTool, tank.getFluid());
        // update held item, assuming its actually held
        TankItem.updateHeldItem(player, held, result.stack());
      }

      return true;
    }
    return false;
  }
}
