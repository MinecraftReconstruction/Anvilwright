package slimeknights.tconstruct.smeltery.item;

import io.github.fabricators_of_create.porting_lib.item.CustomMaxCountItem;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TankItem extends BlockTooltipItem implements CustomMaxCountItem {
  private static final String KEY_FLUID = TConstruct.makeTranslationKey("block", "tank.fluid");
  private static final String KEY_MB = TConstruct.makeTranslationKey("block", "tank.mb");
  private static final String KEY_INGOTS = TConstruct.makeTranslationKey("block", "tank.ingots");
  private static final String KEY_MIXED = TConstruct.makeTranslationKey("block", "tank.mixed");

  private final boolean limitStackSize;
  public TankItem(Block blockIn, Properties builder, boolean limitStackSize) {
    super(blockIn, builder);
    this.limitStackSize = limitStackSize;
    FluidStorage.ITEM.registerForItems((itemStack, context) -> new TankItemFluidHandler(context), this);
  }

  /** Checks if the tank item is filled */
  private static boolean isFilled(ItemStack stack) {
    // has a container if not empty
    CompoundTag nbt = stack.getTag();
    return nbt != null && nbt.contains(NBTTags.TANK, Tag.TAG_COMPOUND);
  }

  @Override
  public ItemStack getRecipeRemainder(ItemStack stack) {
    return isFilled(stack) ? new ItemStack(this) : ItemStack.EMPTY;
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    if (!limitStackSize) {
      return 64;
    }
    return isFilled(stack) ? 16: 64;
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flag) {
    if (stack.hasTag()) {
      FluidTank tank = getTank(stack, 1);
      if (tank.getFluidAmount() > 0) {
        // TODO: migrate to a fluid tooltip JSON?
        tooltip.add(Component.translatable(KEY_FLUID, tank.getFluid().getDisplayName()).withStyle(ChatFormatting.GRAY));
        long amount = tank.getFluidAmount();
        TooltipKey key = SafeClientAccess.getTooltipKey();
        if (tank.getCapacity() % FluidValues.INGOT != 0 || key == TooltipKey.SHIFT) {
          tooltip.add(Component.translatable(KEY_MB, amount).withStyle(ChatFormatting.GRAY));
        } else {
          long ingots = amount / FluidValues.INGOT;
          long mb = amount % FluidValues.INGOT;
          if (mb == 0) {
            tooltip.add(Component.translatable(KEY_INGOTS, ingots).withStyle(ChatFormatting.GRAY));
          } else {
            tooltip.add(Component.translatable(KEY_MIXED, ingots, mb).withStyle(ChatFormatting.GRAY));
          }
          if (key != TooltipKey.UNKNOWN) {
            tooltip.add(FluidTooltipHandler.HOLD_SHIFT);
          }
        }
        FluidTooltipHandler.appendMaterial(fluid, tooltip);
      }
    }
    else {
      super.appendHoverText(stack, worldIn, tooltip, flag);
    }
  }

  /**
   * Sets the tank to the given stack
   * @param stack  Stack
   * @param tank   Tank instance
   * @return  Stack with tank
   */
  public static ItemStack setTank(ItemStack stack, FluidTank tank) {
    if (tank.isEmpty()) {
      removeTank(stack);
    } else {
      stack.getOrCreateTag().put(NBTTags.TANK, tank.writeToNBT(new CompoundTag()));
    }
    return stack;
  }

  /**
   * Sets the tank to the given stack
   * @param stack  Stack
   * @param fluid  Fluid
   * @return  Stack with tank
   */
  public static ItemStack setTank(ItemStack stack, FluidStack fluid) {
    if (fluid.isEmpty()) {
      removeTank(stack);
    } else {
      stack.getOrCreateTag().put(NBTTags.TANK, fluid.writeToNBT(new CompoundTag()));
    }
    return stack;
  }

  /** Creates a stack with the given fluid and amount, not validated. */
  private static ItemStack setTank(ItemLike item, ResourceLocation fluid, int amount) {
    CompoundTag tag = new CompoundTag();
    tag.putString("FluidName", fluid.toString());
    tag.putInt("Amount", amount);
    ItemStack stack = new ItemStack(item);
    stack.getOrCreateTag().put(NBTTags.TANK, tag);
    return stack;
  }

  /**
   * Gets the tank for the given stack, scaled by the stack size.
   * @param stack  Tank stack
   * @return  Tank stored in the stack
   */
  public FluidTank getTank(ItemStack stack) {
    int count = stack.getCount();
    FluidTank tank = getTank(stack, count);
    // disallow filling if the current size is larger than 16
    if (limitStackSize && count > 16) {
      tank.setValidator(NO_FILL);
    }
    return tank;
  }

  /**
   * Gets the tank for the given stack
   * @param stack  Tank stack
   * @param scale  Number of tanks in a stack, being filled or drained together.
   * @return  Tank stored in the stack
   */
  public static FluidTank getTank(ItemStack stack, int scale) {
    FluidTank tank = ScaledFluidTank.create(TankBlockEntity.getCapacity(stack.getItem()), scale);
    if (stack.hasTag()) {
      assert stack.getTag() != null;
      tank.readFromNBT(stack.getTag().getCompound(NBTTags.TANK));
    }
    return tank;
  }

  /**
   * Gets a string variant name for the given stack
   * @param stack  Stack instance to check
   * @return  String variant name
   */
  public static String getSubtype(ItemStack stack) {
    CompoundTag nbt = stack.getTag();
    if (nbt != null && nbt.contains(NBTTags.TANK, Tag.TAG_COMPOUND)) {
      return nbt.getCompound(NBTTags.TANK).getString("FluidName");
    }
    return "";
  }

  /** Adds filled variants of all standard tank items to the given consumer */
  @SuppressWarnings("deprecation")
  public static void addFilledVariants(Consumer<ItemStack> output) {
    BuiltInRegistries.FLUID.holders().filter(holder -> {
      Fluid fluid = holder.get();
      return fluid.isSource(fluid.defaultFluidState()) && !holder.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS);
    }).forEachOrdered(holder -> {
      // use an ingot variety for metals
      TankType tank, gauge;
      if (holder.is(TinkerTags.Fluids.METAL_TOOLTIPS)) {
        tank = TankType.INGOT_TANK;
        gauge = TankType.INGOT_GAUGE;
      } else {
        tank = TankType.FUEL_TANK;
        gauge = TankType.FUEL_GAUGE;
      }
      ResourceLocation fluidName = holder.key().location();
      output.accept(setTank(TinkerSmeltery.searedLantern, fluidName, FluidValues.LANTERN_CAPACITY));
      output.accept(fillTank(TinkerSmeltery.searedTank, tank, fluidName));
      output.accept(fillTank(TinkerSmeltery.searedTank, gauge, fluidName));
      output.accept(setTank(TinkerSmeltery.scorchedLantern, fluidName, FluidValues.LANTERN_CAPACITY));
      output.accept(fillTank(TinkerSmeltery.scorchedTank, tank, fluidName));
      output.accept(fillTank(TinkerSmeltery.scorchedTank, gauge, fluidName));
    });
  }

  /** Fills a tank stack with the given fluid */
  public static ItemStack fillTank(EnumObject<TankType,? extends ItemLike> tank, TankType type, Fluid fluid) {
    return setTank(new ItemStack(tank.get(type)), new FluidStack(fluid, type.getCapacity()));
  }

  /** Fills a tank stack with the given fluid */
  public static ItemStack fillTank(EnumObject<TankType,? extends ItemLike> tank, TankType type, ResourceLocation fluid) {
    return setTank(tank.get(type), fluid, type.getCapacity());
  }
}
