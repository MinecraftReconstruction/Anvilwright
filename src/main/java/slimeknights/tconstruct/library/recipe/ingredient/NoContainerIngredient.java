package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.utils.JsonUtils;

import javax.annotation.Nullable;
import slimeknights.tconstruct.library.utils.Util;

/** Ingredient matching an item with no container item, used to ensure NBT fluid items are empty */
public class NoContainerIngredient extends NestedIngredient {
  public static final ResourceLocation ID = TConstruct.getResource("no_container");

  protected NoContainerIngredient(Ingredient nested) {
    super(nested);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && super.test(stack) && !Util.hasCraftingRemainingItem(stack);
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public JsonElement toJson() {
    JsonElement nestedElement = nested.toJson();
    // if we are a vanilla ingredient, and not an array ingredient, serialize into the ingredient directly
    if (!(nested instanceof CustomIngredient) && nestedElement.isJsonObject()) {
      JsonObject nestedObject = nestedElement.getAsJsonObject();
      nestedObject.addProperty("fabric:type", ID.toString());
      return nestedObject;
    }
    // if we have an array or a type, then serialize nested
    // NOTE(porting): Fabric reads custom ingredients from the "fabric:type" key, not Forge's "type", so this form has
    // to write that key or the recipe loader treats the object as a vanilla ingredient and fails on it
    JsonObject json = new JsonObject();
    json.addProperty("fabric:type", ID.toString());
    json.add("match", nestedElement);
    return json;
  }

  @Override
  public CustomIngredientSerializer<?> getSerializer() {
    return Serializer.INSTANCE;
  }

  public enum Serializer implements CustomIngredientSerializer<NoContainerIngredient> {
    INSTANCE;

    @Override
    public NoContainerIngredient read(JsonObject json) {
      // strip our own fabric:type first, otherwise Ingredient.fromJson dispatches straight back into this
      // serializer and recurses forever on the inline (no "match") form
      if (json.has("fabric:type")) {
        json.remove("fabric:type");
      }
      // if we have match, parse as a nested object. Without match, just parse the object as vanilla
      Ingredient ingredient;
      if (json.has("match")) {
        ingredient = Ingredient.fromJson(json.get("match"), false);
      } else {
        ingredient = Ingredient.fromJson(json);
      }
      return new NoContainerIngredient(ingredient);
    }

    @Override
    public NoContainerIngredient read(FriendlyByteBuf buffer) {
      return new NoContainerIngredient(Ingredient.fromNetwork(buffer));
    }

    @Override
    public void write(FriendlyByteBuf buffer, NoContainerIngredient ingredient) {
      ingredient.nested.toNetwork(buffer);
    }

    @Override
    public ResourceLocation getIdentifier() {
      return ID;
    }

    @Override
    public void write(JsonObject parent, NoContainerIngredient ingredient) {
      parent.add("match", ingredient.nested.toJson());
    }
  }


  /* Static constructors */

  /** Creates an instance from the given nested ingredient */
  public static NoContainerIngredient of(Ingredient ingredient) {
    return new NoContainerIngredient(ingredient);
  }

  /** Creates an instance from the given items */
  public static NoContainerIngredient of(ItemLike... items) {
    return of(Ingredient.of(items));
  }

  /** Creates an instance from the given stacks */
  public static NoContainerIngredient of(ItemStack... stacks) {
    return of(Ingredient.of(stacks));
  }

  /** Creates an instance from the given tag */
  public static NoContainerIngredient of(TagKey<Item> tag) {
    return of(Ingredient.of(tag));
  }
}
