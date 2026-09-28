package slimeknights.tconstruct.library.recipe.casting;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Recipe for casting a fluid onto an item, copying the fluid NBT to the item.
 * TODO 1.21: move to {@link slimeknights.tconstruct.library.recipe.casting.potion}
 */
public class PotionCastingRecipe implements ICastingRecipe, IMultiRecipe<IDisplayableCastingRecipe> {
  protected static final LoadableField<FluidIngredient, PotionCastingRecipe> FLUID_FIELD = FluidIngredient.LOADABLE.requiredField("fluid", r -> r.fluid);
  protected static final LoadableField<Integer, PotionCastingRecipe> COOLING_TIME_FIELD = IntLoadable.FROM_ONE.defaultField("cooling_time", 5, r -> r.coolingTime);
  public static final RecordLoadable<PotionCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("bottle", r -> r.bottle),
    FLUID_FIELD,
    Loadables.ITEM.requiredField("result", r -> r.result),
    COOLING_TIME_FIELD,
    PotionCastingRecipe::new);

  @Getter
  protected final TypeAwareRecipeSerializer<?> serializer;
  @Getter
  protected final ResourceLocation id;
  @Getter
  protected final String group;
  /** Input on the casting table, always consumed */
  protected final Ingredient bottle;
  /** Potion ingredient, typically just the potion tag */
  protected final FluidIngredient fluid;
  /** Potion item result, will be given the proper NBT */
  protected final Item result;
  /** Cooling time for this recipe, used for tipped arrows */
  protected final int coolingTime;

  public PotionCastingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, Ingredient bottle, FluidIngredient fluid, Item result, int coolingTime) {
    this.serializer = serializer;
    this.id = id;
    this.group = group;
    this.bottle = bottle;
    this.fluid = fluid;
    this.result = result;
    this.coolingTime = coolingTime;
    CastingRecipeLookup.registerCastable(result);
  }

  @Override
  public RecipeType<?> getType() {
    return serializer.getType();
  }

  @Override
  public boolean matches(ICastingContainer inv, Level level) {
    return bottle.test(inv.getStack()) && fluid.test(inv.getFluid());
  }

  @Override
  public long getFluidAmount(ICastingContainer inv) {
    return fluid.getAmount(inv.getFluid());
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
    return coolingTime;
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, RegistryAccess registryAccess) {
    ItemStack result = new ItemStack(this.result);
    result.setTag(inv.getFluidTag());
    return result;
  }


  /* JEI */
  // TODO 1.21: consider making this a display recipe instead of a multirecipe
  protected List<IDisplayableCastingRecipe> displayRecipes = null;

  @Override
  public List<IDisplayableCastingRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      // create a subrecipe for every potion variant
      List<ItemStack> bottles = List.of(bottle.getItems());
      displayRecipes = BuiltInRegistries.POTION.stream()
        .map(potion -> {
          ItemStack result = PotionUtils.setPotion(new ItemStack(this.result), potion);
          return new DisplayCastingRecipe(type, bottles, fluid.getFluids().stream()
                                                              .map(fluid -> new FluidStack(fluid.getFluid(), fluid.getAmount(), result.getTag()))
                                                              .toList(),
                                          result, coolingTime, true);
        }).toList();
    }
    return displayRecipes;
  }


  /* Recipe interface methods */

  @Override
  public NonNullList<Ingredient> getIngredients() {
    return NonNullList.of(Ingredient.EMPTY, bottle);
  }

  /** @deprecated use {@link #assemble(Container, RegistryAccess)} */
  @Deprecated
  @Override
  public ItemStack getResultItem(RegistryAccess registryAccess) {
    return new ItemStack(this.result);
  }


    @Override
    public PotionCastingRecipe fromJson(ResourceLocation id, JsonObject json) {
      String group = GsonHelper.getAsString(json, "group", "");
      Ingredient bottle = Ingredient.fromJson(JsonHelper.getElement(json, "bottle"));
      FluidIngredient fluid = FluidIngredient.deserialize(json, "fluid");
      Item result = JsonHelper.getAsEntry(BuiltInRegistries.ITEM, json, "result");
      int coolingTime = GsonHelper.getAsInt(json, "cooling_time");
      return new PotionCastingRecipe(type.get(), this, id, group, bottle, fluid, result, coolingTime);
    }

    @Nullable
    @Override
    protected PotionCastingRecipe fromNetworkSafe(ResourceLocation id, FriendlyByteBuf buffer) {
      String group = buffer.readUtf(Short.MAX_VALUE);
      Ingredient bottle = Ingredient.fromNetwork(buffer);
      FluidIngredient fluid = FluidIngredient.read(buffer);
      Item result = BuiltInRegistries.ITEM.get(buffer.readResourceLocation());
      int coolingTime = buffer.readVarInt();
      return new PotionCastingRecipe(type.get(), this, id, group, bottle, fluid, result, coolingTime);
    }

    @Override
    protected void toNetworkSafe(FriendlyByteBuf buffer, PotionCastingRecipe recipe) {
      buffer.writeUtf(recipe.group);
      recipe.bottle.toNetwork(buffer);
      recipe.fluid.write(buffer);
      buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(recipe.result));
      buffer.writeVarInt(recipe.coolingTime);
    }
  }
}
