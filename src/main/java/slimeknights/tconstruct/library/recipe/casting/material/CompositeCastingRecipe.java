package slimeknights.tconstruct.library.recipe.casting.material;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.recipe.casting.DisplayCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;

/**
 * Casting recipe taking a part of a material and a fluid and outputting the part with a new material
 */
public class CompositeCastingRecipe extends MaterialCastingRecipe implements IMultiRecipe<IDisplayableCastingRecipe> {
  public static final RecordLoadable<CompositeCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(),
    LoadableRecipeSerializer.RECIPE_GROUP, ITEM_COST_FIELD, RESULT_FIELD, MATERIALS_FIELD,
    MaterialStatsId.PARSER.nullableField("casting_stat_conflict", r -> r.castingStatConflict),
    CompositeCastingRecipe::new);

  @Nullable
  private final MaterialStatsId castingStatConflict;

  public CompositeCastingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, int itemCost, IMaterialItem result, IJsonPredicate<MaterialVariantId> materials, @Nullable MaterialStatsId castingStatConflict) {
    super(serializer, id, group, Ingredient.of(result), itemCost, result, materials, true, false);
    this.castingStatConflict = castingStatConflict;
  }

  /** @deprecated use {@link #CompositeCastingRecipe(TypeAwareRecipeSerializer, ResourceLocation, String, int, IMaterialItem, IJsonPredicate, MaterialStatsId)} */
  @Deprecated(forRemoval = true)
  public CompositeCastingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, IMaterialItem result, int itemCost, @Nullable MaterialStatsId castingStatConflict) {
    this(serializer, id, group, itemCost, result, MaterialPredicate.ANY, castingStatConflict);
  }

  @Override
  protected MaterialFluidRecipe getFluidRecipe(ICastingContainer inv) {
    Fluid fluid = inv.getFluid();
    if (castingStatConflict != null) {
      // if we have casting recipe that matches our fluid and is valid for the result, return no match
      // used to prevent conflicts between tool casting and composite part casting
      MaterialFluidRecipe recipe = MaterialCastingLookup.getCastingFluid(fluid); // TODO: does this need a filter?
      if (recipe != MaterialFluidRecipe.EMPTY && castingStatConflict.canUseMaterial(recipe.getOutput().getId())) {
        return MaterialFluidRecipe.EMPTY;
      }
    }
    // find a composite match, requires fetching the material ID but not a huge deal as we already validated the cast (won't be calling this for multiple fluids)
    return MaterialCastingLookup.getCompositeFluid(fluid, IMaterialItem.getMaterialFromStack(inv.getStack()), materials);
  }

  /* JEI */

  /** Grows the given list to the new size by repeating elements modulo */
  private static <T> List<T> growList(List<T> list, int newSize) {
    List<T> newList = new ArrayList<>(newSize);
    newList.addAll(list);
    int oldSize = list.size();
    for (int i = oldSize; i < newSize; i++) {
      newList.add(list.get(i % oldSize));
    }
    return newList;
  }

  @Override
  public List<IDisplayableCastingRecipe> getRecipes(RegistryAccess access) {
    if (multiRecipes == null) {
      RecipeType<?> type = getType();
      multiRecipes = MaterialCastingLookup
        .getAllCompositeFluids().stream()
        .filter(recipe -> {
          MaterialVariant output = recipe.getOutput();
          MaterialVariant input = recipe.getInput();
          return !output.isUnknown() && input != null && !input.isUnknown()
            && !output.get().isHidden() && !input.get().isHidden() && result.canUseMaterial(output.getId()) && result.canUseMaterial(input.getId());
        })
        .map(recipe -> {
          List<FluidStack> fluids = resizeFluids(recipe.getFluids());
          long fluidAmount = fluids.stream().mapToLong(FluidStack::getAmount).max().orElse(0);
          return new DisplayCastingRecipe(type, Collections.singletonList(result.withMaterial(Objects.requireNonNull(recipe.getInput()).getVariant())), fluids, result.withMaterial(recipe.getOutput().getVariant()),
                                          ICastingRecipe.calcCoolingTime(recipe.getTemperature(), itemCost * fluidAmount), consumed);
        })
        .collect(Collectors.toList());
    }
    return multiRecipes;
  }
}
