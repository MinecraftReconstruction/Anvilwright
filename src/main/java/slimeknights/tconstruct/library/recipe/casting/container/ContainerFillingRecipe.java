package slimeknights.tconstruct.library.recipe.casting.container;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.casting.DisplayCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;

import java.util.List;
import java.util.Optional;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

/**
 * Casting recipe that takes an arbitrary fluid for a given amount and fills a container
 */
@RequiredArgsConstructor
public class ContainerFillingRecipe implements ICastingRecipe, IMultiRecipe<IDisplayableCastingRecipe> {
  public static final RecordLoadable<ContainerFillingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    IntLoadable.FROM_ONE.requiredField("fluid_amount", r -> r.fluidAmount),
    Loadables.ITEM.requiredField("container", r -> r.container),
    ContainerFillingRecipe::new);

  @Getter
  private final TypeAwareRecipeSerializer<?> serializer;
  @Getter
  private final ResourceLocation id;
  @Getter
  private final String group;
  private final int fluidAmount;
  private final Item container;

  @Override
  public RecipeType<?> getType() {
    return serializer.getType();
  }

  private static Optional<Storage<FluidVariant>> getFluidHandlerItem(ItemStack stack) {
    return Optional.ofNullable(FluidStorage.ITEM.find(stack, ContainerItemContext.withInitial(stack)));
  }

  @Override
  public long getFluidAmount(ICastingContainer inv) {
    FluidVariant fluid = FluidVariant.of(inv.getFluid());
    try (Transaction tx = TransferUtil.getTransaction()) {
      return getFluidHandlerItem(inv.getStack())
        .map(handler -> StorageUtil.simulateInsert(handler, fluid, this.fluidAmount, tx))
        .orElse(0L);
    }
  }

  @Override
  public boolean isConsumed() {
    return true;
  }

  @Override
  public boolean switchSlots() {
    return false;
  }

  @Override
  public int getCoolingTime(ICastingContainer inv) {
    return 5;
  }

  @Override
  public boolean matches(ICastingContainer inv, Level worldIn) {
    ItemStack stack = inv.getStack();
    FluidVariant fluid = FluidVariant.of(inv.getFluid());
    try (Transaction tx = TransferUtil.getTransaction()) {
      return stack.getItem() == this.container.asItem()
           && getFluidHandlerItem(stack)
        .filter(handler -> StorageUtil.simulateInsert(handler, fluid, this.fluidAmount, tx) > 0)
        .isPresent();
    }
  }

  /** @deprecated use {@link ICastingRecipe#assemble(Container, RegistryAccess)} */
  @Override
  @Deprecated
  public ItemStack getResultItem(RegistryAccess registryAccess) {
    return new ItemStack(this.container);
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, RegistryAccess registryAccess) {
    ItemStack stack = inv.getStack().copy();
    ContainerItemContext context = ContainerItemContext.withInitial(stack);
    return Optional.ofNullable(FluidStorage.ITEM.find(stack, context)).map(handler -> {
      try (Transaction tx = TransferUtil.getTransaction()) {
        handler.insert(FluidVariant.of(inv.getFluid(), inv.getFluidTag()), this.fluidAmount, tx);
        tx.commit();
      }
      return context.getItemVariant().toStack((int) context.getAmount());
    }).orElse(stack);
  }

  /* Display */
  /** Cache of items to display for this container */
  private List<IDisplayableCastingRecipe> displayRecipes = null;

  @Override
  public List<IDisplayableCastingRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      List<ItemStack> casts = Collections.singletonList(new ItemStack(container));
      displayRecipes = BuiltInRegistries.FLUID.stream()
                                             .filter(fluid -> fluid.getBucket() != Items.AIR && fluid.isSource(fluid.defaultFluidState()))
                                             .map(fluid -> {
                                               FluidStack fluidStack = new FluidStack(fluid, fluidAmount);
                                               ItemStack stack = new ItemStack(container);
                                               ContainerItemContext context = ContainerItemContext.withInitial(stack);
                                               stack = Optional.ofNullable(FluidStorage.ITEM.find(stack, context)).map(handler -> {
                                                 try (Transaction tx = TransferUtil.getTransaction()) {
                                                   handler.insert(fluidStack.getType(), fluidStack.getAmount(), tx);
                                                   tx.commit();
                                                 }
                                                 return context.getItemVariant().toStack((int) context.getAmount());
                                               }).orElse(stack);
                                               return new DisplayCastingRecipe(getType(), casts, Collections.singletonList(fluidStack), stack, 5, true);
                                             })
                                             .toList();
    }
    return displayRecipes;
  }
}
