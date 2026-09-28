package slimeknights.tconstruct.tools.recipe;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.recipe.ITinkerableContainer;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierSalvage;
import slimeknights.tconstruct.library.recipe.modifiers.adding.ModifierRecipe;
import slimeknights.tconstruct.library.recipe.worktable.AbstractWorktableRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class ModifierRemovalRecipe extends AbstractWorktableRecipe {
  public static final String BASE_KEY = TConstruct.makeTranslationKey("recipe", "remove_modifier");
  private static final Component DESCRIPTION = TConstruct.makeTranslation("recipe", "remove_modifier.description");
  private static final Component NO_MODIFIERS = TConstruct.makeTranslation("recipe", "remove_modifier.no_modifiers");
  public static final SizedIngredient DEFAULT_TOOLS = SizedIngredient.of(AbstractWorktableRecipe.DEFAULT_TOOLS);

  protected static final LoadableField<String,ModifierRemovalRecipe> NAME_FIELD = StringLoadable.DEFAULT.defaultField("name", "modifiers", true, r -> r.name);
  protected static final LoadableField<SizedIngredient,ModifierRemovalRecipe> TOOLS_FIELD = SizedIngredient.LOADABLE.defaultField("tools", DEFAULT_TOOLS, true, r -> r.sizedTool);
  protected static final LoadableField<List<ItemStack>,ModifierRemovalRecipe> LEFTOVERS_FIELD = ItemStackLoadable.REQUIRED_STACK_NBT.list(0).defaultField("leftovers", List.of(), r -> r.leftovers);
  protected static final LoadableField<IJsonPredicate<ModifierId>,ModifierRemovalRecipe> MODIFIER_PREDICATE_FIELD = ModifierPredicate.LOADER.defaultField("modifier_predicate", false, r -> r.modifierPredicate);

  /** Recipe loadable */
  public static final RecordLoadable<ModifierRemovalRecipe> LOADER = RecordLoadable.create(ContextKey.ID.requiredField(), NAME_FIELD, TOOLS_FIELD, INPUTS_FIELD, LEFTOVERS_FIELD, MODIFIER_PREDICATE_FIELD, ModifierRemovalRecipe::new);

  private final String name;
  @Getter
  private final Component title;
  private final SizedIngredient sizedTool;
  private final List<ItemStack> leftovers;
  private final IJsonPredicate<ModifierId> modifierPredicate;

  protected final Predicate<ModifierEntry> entryPredicate;
  private List<ModifierEntry> displayModifiers;

  public ModifierRemovalRecipe(ResourceLocation id, String name, SizedIngredient toolRequirement, List<SizedIngredient> inputs, List<ItemStack> leftovers, IJsonPredicate<ModifierId> modifierPredicate) {
    super(id, toolRequirement.getIngredient(), inputs);
    this.name = name;
    this.title = Component.translatable(getBaseKey() + "." + name);
    this.sizedTool = toolRequirement;
    this.leftovers = leftovers;
    this.modifierPredicate = modifierPredicate;
    this.entryPredicate = mod -> modifierPredicate.matches(mod.getId());
  }

  /** Gets the base key for the title translation */
  protected String getBaseKey() {
    return BASE_KEY;
  }

  @Override
  public boolean matches(ITinkerableContainer inv, Level world) {
    if (!sizedTool.test(inv.getTinkerableStack())) {
      return false;
    }
    return ModifierRecipe.checkMatch(inv, inputs);
  }

  /** Filters the given modifier list */
  protected List<ModifierEntry> filter(@Nullable IToolStackView tool, List<ModifierEntry> modifiers) {
    if (modifierPredicate != ModifierPredicate.ANY) {
      return modifiers.stream().filter(entryPredicate).toList();
    }
    return modifiers;
  }

  @Override
  public List<ModifierEntry> getModifierOptions(@Nullable ITinkerableContainer inv) {
    if (inv == null) {
      if (displayModifiers == null) {
        displayModifiers = filter(null, ModifierRecipeLookup.getRecipeModifierList());
      }
      return displayModifiers;
    }
    return filter(inv.getTinkerable(), inv.getTinkerable().getUpgrades().getModifiers());
  }

  @Override
  public Component getDescription(@Nullable ITinkerableContainer inv) {
    if (inv != null && inv.getTinkerable().getUpgrades().getModifiers().stream().noneMatch(entryPredicate)) {
      return NO_MODIFIERS;
    }
    return DESCRIPTION;
  }

  @Override
  public RecipeResult<LazyToolStack> getResult(ITinkerableContainer inv, ModifierEntry entry) {
    ToolStack original = inv.getTinkerable();

    // salvage
    ToolStack tool = original.copy();
    ModifierId modifierId = entry.getId();
    ItemStack originalStack = inv.getTinkerableStack();
    ModifierSalvage salvage = ModifierRecipeLookup.getSalvage(originalStack, tool, modifierId, entry.getLevel());

    // restore the slots
    if (salvage != null) {
      salvage.updateTool(tool);
    }

    // first remove hook, primarily for removing raw NBT which is highly discouraged using
    int newLevel = tool.getModifierLevel(modifierId) - 1;
    Modifier modifier = entry.getModifier();
    if (newLevel <= 0) {
      modifier.getHook(ModifierHooks.RAW_DATA).removeRawData(tool, modifier, tool.getRestrictedNBT());
    }

    // remove the actual modifier
    tool.removeModifier(modifierId, 1);

    // ensure the tool is still valid
    Component error = tool.tryValidate();
    if (error != null) {
      return RecipeResult.failure(error);
    }
    error = ModifierRemovalHook.onRemoved(original, tool);
    if (error != null) {
      return RecipeResult.failure(error);
    }
    // successfully removed
    return LazyToolStack.successCopy(tool, originalStack);
  }

  @Override
  public void updateInputs(LazyToolStack result, ITinkerableContainer.Mutable inv, ModifierEntry selected, boolean isServer) {
    super.updateInputs(result, inv, selected, isServer);
    if (isServer) {
      for (ItemStack stack : leftovers) {
        inv.giveItem(stack.copy());
      }
    }
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerModifiers.removeModifierSerializer.get();
  }



  /* JEI */

  /** Gets a list of tools to display */
  @Override
  public List<ItemStack> getInputTools() {
    if (tools == null) {
      tools = sizedTool.getMatchingStacks().stream().map(stack -> {
        ItemStack tool = IModifiableDisplay.getDisplayStack(stack.getItem());
        if (stack.getCount() > 1) {
          tool = ItemHandlerHelper.copyStackWithSize(tool, stack.getCount());
        }
        return tool;
      }).toList();
    }
    return tools;
  }

  /** @deprecated use {@link Factory} */
  @FunctionalInterface
  public interface ModifierRemovalRecipeFactory extends Factory {
    ModifierRemovalRecipe create(ResourceLocation id, List<SizedIngredient> inputs, List<ItemStack> leftovers, IJsonPredicate<ModifierId> modifierPredicate);

    @Override
    default ModifierRemovalRecipe create(ResourceLocation id, SizedIngredient toolRequirement, List<SizedIngredient> inputs, List<ItemStack> leftovers, IJsonPredicate<ModifierId> modifierPredicate) {
      return create(id, inputs, leftovers, modifierPredicate);
    }
  }

  /** Factory interface for modifier removal recipes */
  @FunctionalInterface
  public interface Factory {
    ModifierRemovalRecipe create(ResourceLocation id, SizedIngredient toolRequirement, List<SizedIngredient> inputs, List<ItemStack> leftovers, IJsonPredicate<ModifierId> modifierPredicate);
  }

  @RequiredArgsConstructor
  public static class Serializer extends LoggingRecipeSerializer<ModifierRemovalRecipe> {
    private final Factory factory;

    /** @deprecated use {@link #Serializer(Factory)} */
    @Deprecated
    public Serializer(ModifierRemovalRecipeFactory factory) {
      this((Factory)factory);
    }

    @Override
    public ModifierRemovalRecipe fromJson(ResourceLocation id, JsonObject json) {
      SizedIngredient tool;
      if (json.has("tools")) {
        tool = SizedIngredient.deserialize(GsonHelper.getAsJsonObject(json, "tools"));
      } else {
        tool = SizedIngredient.fromTag(TinkerTags.Items.MODIFIABLE);
      }
      List<SizedIngredient> ingredients = JsonHelper.parseList(json, "inputs", SizedIngredient::deserialize);
      List<ItemStack> leftovers = Collections.emptyList();
      if (json.has("leftovers")) {
        leftovers = JsonHelper.parseList(json, "leftovers", JsonUtils::convertToItemStack);
      }
      IJsonPredicate<ModifierId> modifierPredicate = ModifierPredicate.ALWAYS;
      if (json.has("modifier_predicate")) {
        modifierPredicate = ModifierPredicate.LOADER.getAndDeserialize(json, "modifier_predicate");
      }
      return factory.create(id, tool, ingredients, leftovers, modifierPredicate);
    }

    @Nullable
    @Override
    public ModifierRemovalRecipe fromNetworkSafe(ResourceLocation id, FriendlyByteBuf buffer) {
      SizedIngredient tool = SizedIngredient.read(buffer);
      int size = buffer.readVarInt();
      ImmutableList.Builder<SizedIngredient> ingredients = ImmutableList.builder();
      for (int i = 0; i < size; i++) {
        ingredients.add(SizedIngredient.read(buffer));
      }
      size = buffer.readVarInt();
      ImmutableList.Builder<ItemStack> leftovers = ImmutableList.builder();
      for (int i = 0; i < size; i++) {
        leftovers.add(buffer.readItem());
      }
      IJsonPredicate<ModifierId> modifierPredicate = ModifierPredicate.LOADER.fromNetwork(buffer);
      return factory.create(id, tool, ingredients.build(), leftovers.build(), modifierPredicate);
    }

    @Override
    public void toNetworkSafe(FriendlyByteBuf buffer, ModifierRemovalRecipe recipe) {
      recipe.sizedTool.write(buffer);
      buffer.writeVarInt(recipe.inputs.size());
      for (SizedIngredient ingredient : recipe.inputs) {
        ingredient.write(buffer);
      }
      buffer.writeVarInt(recipe.leftovers.size());
      for (ItemStack itemStack : recipe.leftovers) {
        buffer.writeItem(itemStack);
      }
      ModifierPredicate.LOADER.toNetwork(recipe.modifierPredicate, buffer);
    }
  }

  @RequiredArgsConstructor(staticName = "removal")
  public static class Builder extends AbstractSizedIngredientRecipeBuilder<Builder> {
    private final RecipeSerializer<? extends ModifierRemovalRecipe> serializer;
    private final List<ItemStack> leftovers = new ArrayList<>();
    private SizedIngredient tools = SizedIngredient.EMPTY;
    @Setter @Accessors(fluent = true)
    private IJsonPredicate<ModifierId> modifierPredicate = ModifierPredicate.ALWAYS;

    public static Builder removal() {
      return removal(TinkerModifiers.removeModifierSerializer.get());
    }

    /** Sets the tool requirement for this recipe */
    public Builder setTools(SizedIngredient ingredient) {
      this.tools = ingredient;
      return this;
    }

    /** Sets the tool requirement for this recipe */
    public Builder setTools(Ingredient ingredient) {
      return setTools(SizedIngredient.of(ingredient));
    }

    /** Adds a leftover stack to the recipe */
    public Builder addLeftover(ItemStack stack) {
      leftovers.add(stack);
      return this;
    }

    /** Adds a leftover stack to the recipe */
    public Builder addLeftover(ItemLike item) {
      return addLeftover(new ItemStack(item));
    }

    @Override
    public void save(Consumer<FinishedRecipe> consumer) {
      save(consumer, BuiltInRegistries.ITEM.getKey(leftovers.get(0).getItem()));
    }

    @Override
    public void save(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
      if (inputs.isEmpty()) {
        throw new IllegalStateException("Must have at least one input");
      }
      ResourceLocation advancementId = buildOptionalAdvancement(id, "modifiers");
      consumer.accept(new Finished(id, advancementId));
    }

    private class Finished extends SizedFinishedRecipe {
      public Finished(ResourceLocation ID, @Nullable ResourceLocation advancementID) {
        super(ID, advancementID);
      }

      @Override
      public void serializeRecipeData(JsonObject json) {
        super.serializeRecipeData(json);
        SizedIngredient ingredient = tools;
        if (ingredient == SizedIngredient.EMPTY) {
          ingredient = SizedIngredient.fromTag(TinkerTags.Items.MODIFIABLE);
        }
        json.add("tools", ingredient.serialize());
        if (!leftovers.isEmpty()) {
          JsonArray array = new JsonArray();
          for (ItemStack stack : leftovers) {
            array.add(JsonUtils.serializeItemStack(stack));
          }
          json.add("leftovers", array);
        }
        json.add("modifier_predicate", ModifierPredicate.LOADER.serialize(modifierPredicate));
      }

      @Override
      public RecipeSerializer<?> getType() {
        return serializer;
      }
    }
  }
}
