package slimeknights.tconstruct.library.recipe.tinkerstation.building;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;

public class ToolBuildingRecipeSerializer implements LoggingRecipeSerializer<ToolBuildingRecipe> {

  @Override
  public ToolBuildingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
    String group = GsonHelper.getAsString(json, "group", "");
    // output fetch as a modifiable item, its an error if it does not implement that interface or does not have parts
    ResourceLocation itemName = new ResourceLocation(GsonHelper.getAsString(json, "result"));
    if (!BuiltInRegistries.ITEM.containsKey(itemName) || !(BuiltInRegistries.ITEM.get(itemName) instanceof IModifiable item)) {
      throw new JsonSyntaxException("Result must be a modifiable item with tool parts");
    }
    if (!item.getToolDefinition().hasMaterials()) {
      throw new JsonSyntaxException("Modifiable item must have tool parts to get a tool building recipe");
    }
    int resultCount = GsonHelper.getAsInt(json, "result_count", 1);
    List<Ingredient> extraRequirements = Collections.emptyList();
    if (json.has("extra_requirements")) {
      extraRequirements = JsonHelper.parseList(json, "extra_requirements", jsonObject -> Ingredient.fromJson(jsonObject));
    }
    return new ToolBuildingRecipe(recipeId, group, item, resultCount, null, extraRequirements);
  }

  @Nullable
  @Override
  public ToolBuildingRecipe fromNetworkSafe(ResourceLocation recipeId, FriendlyByteBuf buffer) {
    String group = buffer.readUtf(Short.MAX_VALUE);
    IModifiable result = RecipeHelper.readItem(buffer, IModifiable.class);
    int resultSize = buffer.readVarInt();
    int size = buffer.readVarInt();
    ImmutableList.Builder<Ingredient> ingredients = ImmutableList.builder();
    for (int i = 0; i < size; i++) {
      ingredients.add(Ingredient.fromNetwork(buffer));
    }
    return new ToolBuildingRecipe(recipeId, group, result, resultSize, null, ingredients.build());
  }

  @Override
  public void toNetworkSafe(FriendlyByteBuf buffer, ToolBuildingRecipe recipe) {
    buffer.writeUtf(recipe.group);
    RecipeHelper.writeItem(buffer, recipe.output);
    buffer.writeVarInt(recipe.outputCount);
    buffer.writeVarInt(recipe.ingredients.size());
    for (Ingredient ingredient : recipe.ingredients) {
      ingredient.toNetwork(buffer);
    }
  }
}
