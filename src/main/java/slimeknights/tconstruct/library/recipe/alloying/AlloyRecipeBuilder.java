package slimeknights.tconstruct.library.recipe.alloying;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe.AlloyIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe.getTemperature;

/** Builder for alloy recipes */
@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "alloy")
public class AlloyRecipeBuilder extends AbstractRecipeBuilder<AlloyRecipeBuilder> {
  private final FluidOutput output;
  private final int temperature;
  private final List<AlloyIngredient> inputs = new ArrayList<>();

  /**
   * Creates a new recipe producing the given fluid
   * @param fluid    fluid to alloy
   * @param amount   fluid amount
   * @return  Builder instance
   */
  public static AlloyRecipeBuilder alloy(FluidObject<?> fluid, int amount) {
    return alloy(fluid.result(amount), getTemperature(fluid));
  }

  /**
   * Creates a new recipe producing the given fluid
   * @param fluid   Fluid output
   * @return  Builder instance
   */
  public static AlloyRecipeBuilder alloy(FluidStack fluid) {
    return alloy(fluid, FluidVariantAttributes.getTemperature(fluid.getType()) - 300);
  }

  /**
   * Creates a new recipe producing the given fluid
   * @param fluid   Fluid output
   * @param amount  Output amount
   * @return  Builder instance
   */
  public static AlloyRecipeBuilder alloy(Fluid fluid, long amount) {
    return alloy(new FluidStack(fluid, amount));
  }


  /* Inputs */

  /**
   * Adds an input
   * @param input  Input ingredient
   * @return  Builder instance
   */
  public AlloyRecipeBuilder addInput(FluidIngredient input) {
    inputs.add(new AlloyIngredient(input, false));
    return this;
  }

  /**
   * Adds an input that is not consumed
   * @param input  Catalyst ingredient
   * @return  Builder instance
   */
  public AlloyRecipeBuilder addCatalyst(FluidIngredient input) {
    inputs.add(new AlloyIngredient(input, true));
    return this;
  }

  /**
   * Adds an input
   * @param input  Input fluid
   * @return  Builder instance
   */
  public AlloyRecipeBuilder addInput(FluidStack input) {
    return addInput(FluidIngredient.of(input));
  }

  /**
   * Adds an input
   * @param fluid   Input fluid
   * @param amount  Input amount
   * @return  Builder instance
   */
  public AlloyRecipeBuilder addInput(Fluid fluid, long amount) {
    return addInput(FluidIngredient.of(new FluidStack(fluid, amount)));
  }

  /**
   * Adds an input
   * @param tag     Input tag
   * @param amount  Input amount
   * @return  Builder instance
   */
  public AlloyRecipeBuilder addInput(TagKey<Fluid> tag, long amount) {
    return addInput(FluidIngredient.of(tag, amount));
  }


  /* Building */

  @Override
  public void save(Consumer<FinishedRecipe> consumer) {
    save(consumer, Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(output.getFluid())));
  }

  @Override
  public void save(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
    if (inputs.size() < 2) {
      throw new IllegalStateException("Invalid alloying recipe " + id + ", must have at least two inputs");
    }
    consumer.accept(new LoadableFinishedRecipe<>(
      new AlloyRecipe(id, inputs, output, temperature),
      AlloyRecipe.LOADER,
      this.buildOptionalAdvancement(id, "alloys")
    ));
  }
}
