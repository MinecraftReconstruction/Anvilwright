package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;

/**
 * Provides block items to things that place blocks, such as the Exchanging modifier or place-block fluid effects like
 * Ichor. Providers are encouraged to use a single instance for all objects with the same logic, as the stack and more
 * context are provided in the relevant methods.
 * <p>
 * <b>Porting note:</b> upstream this is a Forge capability attached to {@link ItemStack}. Fabric has no equivalent -
 * there is no per-stack capability registry, and Fabric's {@code ItemApiLookup} would need the default to be
 * registered for every single block item individually, which is not practical. Upstream ships exactly one
 * implementation, the default one for stacks holding a {@link BlockItem}, so this port resolves the provider with a
 * direct type check and drops the registry and attach events. The consequence is a documented behaviour difference:
 * addons cannot inject their own provider for arbitrary stacks.
 */
public interface BlockItemProviderCapability {

  /** Capability ID */
  ResourceLocation ID = TConstruct.getResource("block_provider");

  /** Kept for call-site compatibility; there is nothing to register on Fabric */
  @ApiStatus.Internal
  static void register() {}

  /**
   * Gets the block provider for the given stack, or null if this stack cannot provide block items.
   */
  @Nullable
  static BlockItemProviderCapability getBlockProvider(ItemStack stack) {
    return stack.getItem() instanceof BlockItem ? SimpleBlockItem.INSTANCE : null;
  }

  /**
   * Utility to verify that a given stack does indeed contain a BlockItem
   * @param stack The stack to check
   * @param blockProvider The provider that provided this item, used in case it fails as debugging information
   * @return the contained BlockItem, or null if it was not a BlockItem
   */
  @Nullable
  static BlockItem verifyBlockItem(ItemStack stack, BlockItemProviderCapability blockProvider) {
    if (stack.getItem() instanceof BlockItem bItem) {
      return bItem;
    } else {
      TConstruct.LOG.warn("BlockItemProviderCapability implementation tried to return a non-empty, non-blockitem stack! Cap: {}, Cap Class: {}, Provided Item: {}", blockProvider, blockProvider.getClass().getName(), Loadables.ITEM.getKey(stack.getItem()));
      return null;
    }
  }

  /**
   * Get a {@link BlockItem} to provide, wrapped as an ItemStack with any required placement NBT data. Can be randomised, if desired.
   * <br>
   * <br>
   * <b>The returned stack must have {@link ItemStack#getItem} return an instance of {@link BlockItem}, or be {@link ItemStack#EMPTY}!</b>
   * @param stack The {@link ItemStack} that this provider was fetched for.
   * @param entity The {@link LivingEntity} (usually a {@link Player}) that is requesting a block.
   * @return the {@link ItemStack} that this provides, or {@link ItemStack#EMPTY} if this cannot provide more block items (for example if the stack has been depleted)
   */
  ItemStack getBlockItemStack(ItemStack stack, @Nullable LivingEntity entity);

  /**
   * Consume one item from this provider.
   * @param stack The {@link ItemStack} that this provider was fetched for.
   * @param backingStack The stack returned by {@link #getBlockItemStack} that was placed and is now being consumed. It is unmodified and the same instance so can use == for comparisons.
   * @param entity The {@link LivingEntity} (usually a {@link Player}) that has just consumed a block.
   */
  void consume(ItemStack stack, ItemStack backingStack, @Nullable LivingEntity entity);

  /**
   * A simple implementation of {@link BlockItemProviderCapability} that provides from an ItemStack holding a BlockItem
   */
  final class SimpleBlockItem implements BlockItemProviderCapability {
    public static final SimpleBlockItem INSTANCE = new SimpleBlockItem();

    private SimpleBlockItem() {}

    @Override
    public ItemStack getBlockItemStack(ItemStack capStack, @Nullable LivingEntity entity) {
      return capStack.isEmpty() ? ItemStack.EMPTY : capStack;
    }

    @Override
    public void consume(ItemStack capStack, ItemStack backingStack, @Nullable LivingEntity entity) {
      capStack.shrink(1);
    }
  }
}
