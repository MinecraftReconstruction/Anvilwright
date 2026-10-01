package slimeknights.tconstruct.library.recipe.casting.material;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.casting.CastingRecipeLookup;
import slimeknights.tconstruct.library.recipe.casting.DisplayCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import java.util.ArrayList;
import java.util.List;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.loadable.field.ContextKey;
import java.util.Collection;

/**
 * Casting recipe that takes an arbitrary fluid of a given amount and set the material on the output based on that fluid
 */
public class MaterialCastingRecipe extends AbstractMaterialCastingRecipe implements IMultiRecipe<IDisplayableCastingRecipe> {
  protected static final LoadableField<IMaterialItem,MaterialCastingRecipe> RESULT_FIELD = TinkerLoadables.MATERIAL_ITEM.requiredField("result", r -> r.result);
  public static final RecordLoadable<MaterialCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(),
    ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP, CAST_FIELD,
    ITEM_COST_FIELD, RESULT_FIELD, MATERIALS_FIELD, CAST_CONSUMED_FIELD, SWITCH_SLOTS_FIELD,
    MaterialCastingRecipe::new);

  protected final IMaterialItem result;

  public MaterialCastingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, Ingredient cast, int itemCost, IMaterialItem result, IJsonPredicate<MaterialVariantId> materials, boolean consumed, boolean switchSlots) {
    super(serializer, id, group, cast, itemCost, consumed, switchSlots, materials);
    this.result = result;
    CastingRecipeLookup.registerCastable(result);
    MaterialCastingLookup.registerItemCost(result, itemCost);
  }

  /** @deprecated use {@link #MaterialCastingRecipe(TypeAwareRecipeSerializer, ResourceLocation, String, Ingredient, int, IMaterialItem, IJsonPredicate, boolean, boolean)} */
  @Deprecated(forRemoval = true)
  public MaterialCastingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, Ingredient cast, int itemCost, IMaterialItem result, boolean consumed, boolean switchSlots) {
    this(serializer, id, group, cast, itemCost, result, MaterialPredicate.ANY, consumed, switchSlots);
  }

  @Override
  public boolean matches(ICastingContainer inv, Level worldIn) {
    if (!this.getCast().test(inv.getStack())) {
      return false;
    }
    MaterialFluidRecipe fluid = getFluidRecipe(inv);
    return fluid != MaterialFluidRecipe.EMPTY && result.canUseMaterial(fluid.getOutput().getId());
  }

  @Override
  public int getCoolingTime(ICastingContainer inv) {
    return getCachedMaterialFluid(inv)
      .map(recipe -> ICastingRecipe.calcCoolingTime(recipe.getTemperature(), recipe.getFluidAmount(inv.getFluid()) * itemCost))
      .orElse(1);
  }

  @Override
  public long getFluidAmount(ICastingContainer inv) {
    return getCachedMaterialFluid(inv)
             .map(recipe -> recipe.getFluidAmount(inv.getFluid()))
             .orElse(1L) * this.itemCost;
  }

  @Override
  public ItemStack getResultItem(RegistryAccess registryAccess) {
    return new ItemStack(result);
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, RegistryAccess registryAccess) {
    MaterialVariant material = getCachedMaterialFluid(inv).map(MaterialFluidRecipe::getOutput).orElse(MaterialVariant.UNKNOWN);
    return result.withMaterial(material.getVariant());
  }


  /* JEI */
  protected List<IDisplayableCastingRecipe> multiRecipes;

  @Override
  public List<IDisplayableCastingRecipe> getRecipes(RegistryAccess access) {
    if (multiRecipes == null) {
      // expand the fluid list, one result per fluid
      Collection<MaterialFluidRecipe> recipes = MaterialCastingLookup.getAllCastingFluids();
      List<ItemStack> results = new ArrayList<>();
      List<FluidStack> fluids = new ArrayList<>();
      int maxTime = 0;
      for (MaterialFluidRecipe recipe : recipes) {
        // must support this material
        MaterialVariant output = recipe.getOutput();
        MaterialVariantId outputId = output.getVariant();
        if (!result.canUseMaterial(output.getId()) || !this.materials.matches(outputId)) {
          continue;
        }

        // add all fluids to our builders
        List<FluidStack> newFluids = resizeFluids(recipe.getFluids());
        fluids.addAll(newFluids);
        ItemStack resultStack = this.result.withMaterial(outputId);
        for (FluidStack fluid : newFluids) {
          // add one copy of result per fluid
          results.add(resultStack);
          // use the maximum time for cooling time. Will be recomputed dynamically but need a fallback
          int time = ICastingRecipe.calcCoolingTime(recipe.getTemperature(), fluid.getAmount() * itemCost);
          if (time > maxTime) {
            maxTime = time;
          }
        }
      }
      if (fluids.isEmpty()) {
        multiRecipes = List.of();
      } else {
        multiRecipes = List.of(DisplayCastingRecipe.from(this)
          .cast(getCast()).consumed(isConsumed())
          .fluids(List.copyOf(fluids))
          .results(List.copyOf(results))
          .coolingTime(maxTime).materialCasting(true)
          .build());
      }
    }
    return multiRecipes;
  }
}
