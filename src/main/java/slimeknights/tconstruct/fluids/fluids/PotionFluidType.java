package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.fluids.TinkerFluids;

import java.util.Objects;

public class PotionFluidType extends FluidType {
  public PotionFluidType(Properties properties) {
    super(properties);
  }

  @Override
  public String getDescriptionId(FluidStack stack) {
    return PotionUtils.getPotion(stack.getTag()).getName("item.minecraft.potion.effect.");
  }

  @Override
  public ItemStack getBucket(FluidStack fluidStack) {
    ItemStack itemStack = new ItemStack(fluidStack.getFluid().getBucket());
    itemStack.setTag(fluidStack.getTag());
    return itemStack;
  }

  // NOTE(porting): upstream supplies the potion fluid's client texture and tint through Forge's
  //  IClientFluidTypeExtensions here. Fabric has no such extension hook, so this port keeps its own
  //  PotionFluidAttributes + ClientFluidAttributeRegistry pair for the client side (see TinkerFluids), which
  //  implements the same CustomPotionColor/potion-colour logic. See docs/BEHAVIOUR-DIFFERENCES.md.

  /** Creates the potion tag */
  private static CompoundTag potionTag(ResourceLocation location) {
    CompoundTag tag = new CompoundTag();
    tag.putString("Potion", location.toString());
    return tag;
  }

  /** Creates a fluid stack for the given potion */
  public static FluidStack potionFluid(ResourceKey<Potion> potion, int size) {
    CompoundTag tag = null;
    if (potion != Potions.EMPTY_ID) {
      tag = potionTag(potion.location());
    }
    return new FluidStack(TinkerFluids.potion.get(), size, tag);
  }

  /** Creates a fluid stack for the given potion */
  @SuppressWarnings("deprecation")  // forge registries have nullable keys, like why would you want that?
  public static FluidStack potionFluid(Potion potion, int size) {
    CompoundTag tag = null;
    if (potion != Potions.EMPTY) {
      tag = potionTag(BuiltInRegistries.POTION.getKey(potion));
    }
    return new FluidStack(TinkerFluids.potion.get(), size, tag);
  }

  /** Creates a fluid output for the given potion */
  @SuppressWarnings("deprecation")  // forge registries have nullable keys, like why would you want that?
  public static FluidOutput potionResult(Potion potion, long size) {
    CompoundTag tag = null;
    if (potion != Potions.EMPTY) {
      tag = potionTag(BuiltInRegistries.POTION.getKey(potion));
    }
    return FluidOutput.fromTag(Objects.requireNonNull(TinkerFluids.potion.getCommonTag()), size, tag);
  }

  /** Creates a potion bucket for the given potion */
  public static ItemStack potionBucket(ResourceKey<Potion> potion) {
    ItemStack stack = new ItemStack(TinkerFluids.potion);
    if (potion != Potions.EMPTY_ID) {
      stack.setTag(potionTag(potion.location()));
    }
    return stack;
  }

  /** Creates a potion bucket for the given potion */
  @SuppressWarnings("deprecation")  // forge registries have nullable keys, like why would you want that?
  public static ItemStack potionBucket(Potion potion) {
    ItemStack stack = new ItemStack(TinkerFluids.potion);
    if (potion != Potions.EMPTY) {
      stack.setTag(potionTag(BuiltInRegistries.POTION.getKey(potion)));
    }
    return stack;
  }
}
