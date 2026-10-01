package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicateField;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.crafting.Ingredient.Value;
import slimeknights.tconstruct.library.materials.definition.MaterialId;

/**
 * Extension of the vanilla ingredient to display materials on items and support matching by materials
 */
public class MaterialIngredient extends NestedIngredient {
  private final IJsonPredicate<MaterialVariantId> material;
  @Nullable
  private Value[] values;
  @Nullable
  private ItemStack[] materialStacks;
  protected MaterialIngredient(Ingredient nested, IJsonPredicate<MaterialVariantId> material) {
    super(nested);
    this.material = material;
  }

  /** @deprecated use {@link #MaterialIngredient(Ingredient, IJsonPredicate)} */
  @Deprecated(forRemoval = true)
  protected MaterialIngredient(Ingredient nested, MaterialVariantId material, @Nullable TagKey<IMaterial> tag) {
    this(nested, makePredicate(material, tag));
  }

  /** Converts the legacy material and tag into a predicate */
  private static IJsonPredicate<MaterialVariantId> makePredicate(MaterialVariantId material, @Nullable TagKey<IMaterial> tag) {
    // UNKNOWN is the legacy way to express any material
    IJsonPredicate<MaterialVariantId> predicate = material.equals(IMaterial.UNKNOWN.getIdentifier()) ? MaterialPredicate.ANY : MaterialPredicate.variant(material);
    if (tag != null) {
      IJsonPredicate<MaterialVariantId> tagPredicate = MaterialPredicate.tag(tag);
      if (predicate == MaterialPredicate.ANY) {
        predicate = tagPredicate;
      } else {
        predicate = MaterialPredicate.and(predicate, tagPredicate);
      }
    }
    return predicate;
  }

  /** Creates an ingredient matching the given materials */
  public static MaterialIngredient of(Ingredient ingredient, IJsonPredicate<MaterialVariantId> material) {
    return new MaterialIngredient(ingredient, material);
  }

  /** Creates an ingredient matching the given materials */
  public static MaterialIngredient of(ItemLike item, IJsonPredicate<MaterialVariantId> material) {
    return of(Ingredient.of(item), material);
  }

  /** Creates an ingredient matching a specific material */
  public static MaterialIngredient of(Ingredient ingredient) {
    return new MaterialIngredient(ingredient, MaterialPredicate.ANY);
  }

  /** Creates an ingredient matching a single material */
  public static MaterialIngredient of(Ingredient ingredient, MaterialVariantId material) {
    return of(ingredient, MaterialPredicate.variant(material));
  }

  /** Creates an ingredient matching a material tag */
  public static MaterialIngredient of(Ingredient ingredient, TagKey<IMaterial> tag) {
    return of(ingredient, MaterialPredicate.tag(tag));
  }

  /**
   * Creates a new instance from an item with a fixed material
   * @param item      Material item
   * @param material  Material ID
   * @return  Material ingredient instance
   */
  public static MaterialIngredient of(ItemLike item, MaterialVariantId material) {
    return of(Ingredient.of(item), material);
  }

  /**
   * Creates a new instance from an item with a tagged material
   * @param item      Material item
   * @param tag   Material tag
   * @return  Material ingredient instance
   */
  public static MaterialIngredient of(ItemLike item, TagKey<IMaterial> tag) {
    return of(Ingredient.of(item), tag);
  }

  /**
   * Creates a new ingredient matching any material from items
   * @param item  Material item
   * @return  Material ingredient instance
   */
  public static MaterialIngredient of(ItemLike item) {
    return of(Ingredient.of(item));
  }

  /**
   * Creates a new ingredient from a tag
   * @param tag       Tag instance
   * @param material  Material value
   * @return  Material with tag
   */
  public static MaterialIngredient of(TagKey<Item> tag, MaterialVariantId material) {
    return of(Ingredient.of(tag), material);
  }

  /**
   * Creates a new ingredient matching any material from a tag
   * @param tag       Tag instance
   * @return  Material with tag
   */
  public static MaterialIngredient of(TagKey<Item> tag) {
    return of(Ingredient.of(tag));
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    // check super first, should be faster
    if (stack == null || stack.isEmpty() || !super.test(stack)) {
      return false;
    }
    // no need to read material NBT if the material is the any predicate
    if (material != MaterialPredicate.ANY) {
      return material.matches(IMaterialItem.getMaterialFromStack(stack));
    }
    return true;
  }

  @Override
  public ItemStack[] getItems() {
    if (materialStacks == null) {
      if (!MaterialRegistry.isFullyLoaded()) {
        return nested.getItems();
      }
      // expand every matching stack into one stack per registry material, then filter by the predicate
      Stream<ItemStack> items = getMatchingStacks().stream()
        .flatMap(stack -> MaterialRegistry.getMaterials().stream()
          .map(mat -> IMaterialItem.withMaterial(stack, mat.getIdentifier()))
          .filter(ItemStack::hasTag));
      if (material != MaterialPredicate.ANY) {
        items = items.filter(this::test);
      }
      materialStacks = items.distinct().toArray(ItemStack[]::new);
    }
    return materialStacks;
  }

  @Override
  public CustomIngredientSerializer<FabricMaterialIngredient> getSerializer() {
    return Serializer.INSTANCE;
  }

  @Override
  public JsonElement toJson() {
    JsonElement parent = nested.toJson();
    JsonObject result;
    if (parent.isJsonObject()) {
      result = parent.getAsJsonObject();
    } else {
      result = new JsonObject();
      result.add("match", parent);
    }
    JsonObject object = parent.getAsJsonObject();
    object.addProperty("fabric:type", Serializer.ID.toString());
    if (material != MaterialPredicate.ANY) {
      object.addProperty("material", material.toString());
    }
    return object;
  }

  @Override
  public FabricMaterialIngredient getCustomIngredient() {
    return new FabricMaterialIngredient(this);
  }

  public static class FabricMaterialIngredient implements CustomIngredient {
    private final MaterialIngredient ingredient;

    public FabricMaterialIngredient(Stream<? extends Ingredient.Value> itemLists, MaterialVariantId material) {
      this.ingredient = new MaterialIngredient(Ingredient.fromValues(itemLists), MaterialPredicate.variant(material));
    }

    public FabricMaterialIngredient(MaterialIngredient ingredient) {
      this.ingredient = ingredient;
    }

    @Override
    public boolean test(ItemStack stack) {
      return ingredient.test(stack);
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
      return List.of(ingredient.getItems());
    }

    @Override
    public boolean requiresTesting() {
      return true;
    }

    @Override
    public CustomIngredientSerializer<FabricMaterialIngredient> getSerializer() {
      return Serializer.INSTANCE;
    }

    @Override
    public MaterialIngredient toVanilla() {
      return ingredient;
    }
  }

  /**
   * Serializer instance
   */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Serializer implements CustomIngredientSerializer<FabricMaterialIngredient> {
    public static final Serializer INSTANCE = new Serializer();
    public static final ResourceLocation ID = TConstruct.getResource("material");
    private static final LoadableField<IJsonPredicate<MaterialVariantId>,MaterialIngredient> MATERIAL_FIELD = new MaterialPredicateField<>("material", i -> i.material);

    @Override
    public ResourceLocation getIdentifier() {
      return ID;
    }

    @Override
    public FabricMaterialIngredient read(JsonObject json) {
      // strip our own fabric:type first, otherwise Ingredient.fromJson would dispatch straight back into this
      // serializer and recurse forever when the material is written inline (no "match" field)
      if (json.has("fabric:type")) {
        json.remove("fabric:type");
      }
      // if we have match, parse as a nested object. Without match, just parse the object as vanilla
      Ingredient ingredient = json.has("match") ? Ingredient.fromJson(json.get("match")) : Ingredient.fromJson(json);
      // the predicate is one of the material predicates, defaulting to any material
      IJsonPredicate<MaterialVariantId> material = MATERIAL_FIELD.get(json);
      return new FabricMaterialIngredient(new MaterialIngredient(ingredient, material));
    }

    @Override
    public void write(JsonObject parent, FabricMaterialIngredient ingredient) {
      parent.addProperty("fabric:type", Serializer.ID.toString());
      MATERIAL_FIELD.serialize(ingredient.ingredient, parent);
    }

    @Override
    public FabricMaterialIngredient read(FriendlyByteBuf buffer) {
      IJsonPredicate<MaterialVariantId> material = MATERIAL_FIELD.decode(buffer);
      int size = buffer.readVarInt();
      return new FabricMaterialIngredient(new MaterialIngredient(Ingredient.fromValues(Stream.generate(() -> new ItemValue(buffer.readItem())).limit(size)), material));
    }

    @Override
    public void write(FriendlyByteBuf buffer, FabricMaterialIngredient ingredient) {
      buffer.writeResourceLocation(Serializer.ID);
      // write first as the order of the stream is uncertain
      MATERIAL_FIELD.encode(buffer, ingredient.toVanilla());
      // write stacks
      ItemStack[] items = ingredient.toVanilla().getMatchingStacks().toArray(new ItemStack[0]);
      buffer.writeVarInt(items.length);
      for (ItemStack stack : items) {
        buffer.writeItem(stack);
      }
    }
  }
}
