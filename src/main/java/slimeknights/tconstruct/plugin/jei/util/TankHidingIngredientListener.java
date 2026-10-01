package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IIngredientManager.IIngredientListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Handler to remove tanks when their fluid is removed from JEI. */
public record TankHidingIngredientListener(IIngredientManager manager, List<Item> tanks) implements IIngredientListener {
  /** Gets all changed tanks from the given ingredients */
  private List<ItemStack> getTanks(Collection<? extends ITypedIngredient<?>> ingredients) {
    List<ItemStack> list = new ArrayList<>();
    for (ITypedIngredient<?> ingredient : ingredients) {
      FluidStack fluid = ingredient.getIngredient(FabricTypes.FLUID_STACK).map(FluidIngredients::toStack).orElse(FluidStack.EMPTY);
      if (!fluid.isEmpty()) {
        for (Item item : tanks) {
          ItemStack tank = new ItemStack(item);
          ContainerItemContext context = ContainerItemContext.withInitial(tank);
          Storage<FluidVariant> storage = FluidStorage.ITEM.find(tank, context);
          if (storage != null && canAccept(storage, fluid)) {
            list.add(context.getItemVariant().toStack(1));
          }
        }
      }
    }
    return list;
  }

  /** Checks whether the tank item would accept the given fluid */
  private static boolean canAccept(Storage<FluidVariant> storage, FluidStack fluid) {
    try (Transaction tx = Transaction.openOuter()) {
      return storage.insert(fluid.getType(), fluid.getAmount(), tx) > 0;
    }
  }

  @Override
  public <V> void onIngredientsAdded(IIngredientHelper<V> ingredientHelper, Collection<ITypedIngredient<V>> ingredients) {
    List<ItemStack> tanks = getTanks(ingredients);
    if (!tanks.isEmpty()) {
      manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, tanks);
    }
  }

  @Override
  public <V> void onIngredientsRemoved(IIngredientHelper<V> ingredientHelper, Collection<ITypedIngredient<V>> ingredients) {
    List<ItemStack> tanks = getTanks(ingredients);
    if (!tanks.isEmpty()) {
      manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, tanks);
    }
  }
}
