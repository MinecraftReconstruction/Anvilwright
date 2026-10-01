package slimeknights.tconstruct.plugin.jei;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.RecipeManagerAccessor;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.category.extensions.IExtendableRecipeCategory;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IIngredientVisibility;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.item.RetexturedBlockItem;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.fluids.fluids.PotionFluidType;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.display.FilteredRecipe;
import slimeknights.tconstruct.library.recipe.display.VanillaFilteredRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.material.IDisplayMaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapelessMaterialsRecipe;
import slimeknights.tconstruct.library.recipe.melting.IDisplayableMeltingRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.OverslimeCraftingTableRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayCraftingTinkering;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayToolTinkering;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.plugin.jei.casting.CastingBasinCategory;
import slimeknights.tconstruct.plugin.jei.casting.CastingRecipeManager;
import slimeknights.tconstruct.plugin.jei.casting.CastingTableCategory;
import slimeknights.tconstruct.plugin.jei.entity.DefaultEntityMeltingRecipe;
import slimeknights.tconstruct.plugin.jei.entity.EntityMeltingRecipeCategory;
import slimeknights.tconstruct.plugin.jei.entity.SeveringCategory;
import slimeknights.tconstruct.plugin.jei.fabric.JEITypes;
import slimeknights.tconstruct.plugin.jei.melting.FoundryCategory;
import slimeknights.tconstruct.plugin.jei.melting.FuelCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierBookmarkIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierIngredientHelper;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierRecipeCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierWorktableCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.SlotIngredientHelper;
import slimeknights.tconstruct.plugin.jei.modifiers.SlotIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringExtension;
import slimeknights.tconstruct.plugin.jei.partbuilder.MaterialItemList;
import slimeknights.tconstruct.plugin.jei.partbuilder.PartBuilderCategory;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientHelper;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.transfer.CraftingStationTransferInfo;
import slimeknights.tconstruct.plugin.jei.transfer.TinkerStationTransferInfo;
import slimeknights.tconstruct.plugin.jei.transfer.ToolInventoryTransferInfo;
import slimeknights.tconstruct.plugin.jei.util.GuiContainerTankHandler;
import slimeknights.tconstruct.plugin.jei.util.PotionSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.TankHidingIngredientListener;
import slimeknights.tconstruct.plugin.jei.util.ToolPartSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.ToolSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.manager.SimpleItemRecipeManager;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.client.screen.AlloyerScreen;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.MelterScreen;
import slimeknights.tconstruct.smeltery.data.SmelteryCompat;
import slimeknights.tconstruct.smeltery.item.CopperCanItem;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.recipe.CraftingTableRepairKitRecipe;
import slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.client.ToolContainerScreen;
import slimeknights.tconstruct.tools.item.CreativeSlotItem;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static slimeknights.mantle.util.RetexturedHelper.addTagVariants;
import slimeknights.tconstruct.common.registration.CastItemObject;
import slimeknights.tconstruct.smeltery.client.screen.IScreenWithFluidTank;
import net.minecraft.core.RegistryAccess;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
  /** Recipes that are meant as jokes and tend to confuse players, so are hidden */
  private static final ResourceLocation[] EASTER_EGG_RECIPES = {
    TConstruct.getResource("tables/tinkers_forge"),
    TConstruct.getResource("tables/scorched_forge"),
    TConstruct.getResource("tables/seared_forge_material"),
    TConstruct.getResource("tables/scorched_forge_material")
  };
  /** @deprecated no longer used */
  @Deprecated(forRemoval = true)
  public static IModIdHelper modIdHelper;

  @Override
  public ResourceLocation getPluginUid() {
    return TConstructJEIConstants.PLUGIN;
  }

  @Override
  public void registerCategories(IRecipeCategoryRegistration registry) {
    final IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
    final IPlatformFluidHelper<IJeiFluidIngredient> fluidHelper = (IPlatformFluidHelper<IJeiFluidIngredient>) registry.getJeiHelpers().getPlatformFluidHelper();
    // casting
    registry.addRecipeCategories(new CastingBasinCategory(guiHelper));
    registry.addRecipeCategories(new CastingTableCategory(guiHelper));
    registry.addRecipeCategories(new MoldingRecipeCategory(guiHelper));
    // melting and casting
    registry.addRecipeCategories(new MeltingCategory(guiHelper));
    registry.addRecipeCategories(new AlloyRecipeCategory(guiHelper));
    registry.addRecipeCategories(new EntityMeltingRecipeCategory(guiHelper));
    registry.addRecipeCategories(new FoundryCategory(guiHelper));
    registry.addRecipeCategories(new FuelCategory(guiHelper));
    // tinker station
    registry.addRecipeCategories(new ModifierRecipeCategory(guiHelper));
    registry.addRecipeCategories(new SeveringCategory(guiHelper));
    registry.addRecipeCategories(new ToolBuildingCategory(guiHelper));
    registry.addRecipeCategories(new ToolTinkeringCategory(guiHelper));
    ToolTinkeringExtension.prepareDrawables(guiHelper);
    // part builder
    registry.addRecipeCategories(new MaterialCategory(guiHelper));
    registry.addRecipeCategories(new PartBuilderCategory(guiHelper));
    // modifier worktable
    registry.addRecipeCategories(new ModifierWorktableCategory(guiHelper));
  }

  @Override
  public void registerIngredients(IModIngredientRegistration registration) {
    List<ModifierEntry> modifiers = Collections.emptyList();
    if (Config.CLIENT.showModifiersInJEI.get()) {
      modifiers = ModifierRecipeLookup.getRecipeModifierList();
    }
    registration.register(TConstructJEIConstants.MODIFIER_TYPE, modifiers, new ModifierIngredientHelper(), ModifierBookmarkIngredientRenderer.INSTANCE);
    registration.register(TConstructJEIConstants.MATERIAL_TYPE, List.of(), new MaterialIngredientHelper(), MaterialIconIngredientRenderer.INSTANCE);
    registration.register(TConstructJEIConstants.PATTERN_TYPE, List.of(), new PatternIngredientHelper(), PatternIngredientRenderer.INSTANCE);
    List<SlotCount> slots = SlotType.getAllSlotTypes().stream().map(type -> new SlotCount(type, 1)).toList();
    SlotIngredientRenderer.clearCache();
    registration.register(TConstructJEIConstants.SLOT_TYPE, slots, new SlotIngredientHelper(), SlotIngredientRenderer.INGREDIENT);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registry) {
    IExtendableRecipeCategory<CraftingRecipe, ICraftingCategoryExtension> craftingCategory = registry.getCraftingCategory();
    craftingCategory.addCategoryExtension(ShapedMaterialRecipe.class, ShapedMaterialExtension::new);
    craftingCategory.addCategoryExtension(ShapedMaterialsRecipe.class, ShapedMaterialsExtension::create);
    craftingCategory.addCategoryExtension(ShapelessMaterialsRecipe.class, ShapelessMaterialsExtension::shapeless);
    craftingCategory.addCategoryExtension(OverslimeCraftingTableRecipe.class, OverslimeRecipeExtension::new);
    craftingCategory.addCategoryExtension(IDisplayCraftingTinkering.class, ToolTinkeringExtension::new);
  }

  /** Gets a list of unfiltered casting recipes to give to JEI. */
  private static List<IDisplayableCastingRecipe> getCastingRecipes(RegistryAccess access, RecipeManager manager, Supplier<? extends RecipeType<ICastingRecipe>> recipeType) {
    return FilteredRecipe.unfiltered(RecipeHelper.getJEIRecipes(access, manager, recipeType.get(), IDisplayableCastingRecipe.class));
  }

  /** Finds the first recipe of the given type */
  @Nullable
  private static <T, C extends Container, R extends Recipe<C>> T findFirst(RecipeManager manager, RecipeType<R> type, Class<T> clazz) {
    return manager.byType(type).values().stream().filter(clazz::isInstance).map(clazz::cast).findFirst().orElse(null);
  }

  @Override
  public void registerRecipes(IRecipeRegistration register) {
    Level level = Minecraft.getInstance().level;
    assert level != null;
    RegistryAccess access = level.registryAccess();
    RecipeManager manager = level.getRecipeManager();
    // casting
    register.addRecipes(TConstructJEIConstants.CASTING_BASIN, getCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_BASIN));
    register.addRecipes(TConstructJEIConstants.CASTING_TABLE, getCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_TABLE));

    // melting
    // need to register the fuels before the categories so they are available
    MeltingFuelHandler.registerSolidFuels(register.getIngredientManager());
    List<MeltingFuel> fuels = RecipeHelper.getRecipes(manager, TinkerRecipeTypes.FUEL.get(), MeltingFuel.class);
    MeltingFuelHandler.setMeltngFuels(fuels);
    register.addRecipes(TConstructJEIConstants.FUEL, fuels);
    // register melting recipes
    List<IDisplayableMeltingRecipe> meltingRecipes = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MELTING.get(), IDisplayableMeltingRecipe.class);
    register.addRecipes(TConstructJEIConstants.MELTING, meltingRecipes);
    register.addRecipes(TConstructJEIConstants.FOUNDRY, meltingRecipes);

    // entity melting
    List<EntityMeltingRecipe> entityMeltingRecipes = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.ENTITY_MELTING.get(), EntityMeltingRecipe.class);
    // generate a "default" recipe for all other entity types
    entityMeltingRecipes.add(new DefaultEntityMeltingRecipe(entityMeltingRecipes));
    register.addRecipes(TConstructJEIConstants.ENTITY_MELTING, entityMeltingRecipes);

    // alloying
    register.addRecipes(TConstructJEIConstants.ALLOY, RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.ALLOYING.get(), AlloyRecipe.class));

    // molding
    List<MoldingRecipe> moldingRecipes = new ArrayList<>();
    moldingRecipes.addAll(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MOLDING_TABLE.get(), MoldingRecipe.class));
    moldingRecipes.addAll(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MOLDING_BASIN.get(), MoldingRecipe.class));
    register.addRecipes(TConstructJEIConstants.MOLDING, moldingRecipes);

    // modifiers
    List<IDisplayModifierRecipe> modifierRecipes = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayModifierRecipe.class)
                                                               .stream()
                                                               .sorted((r1, r2) -> {
                                                                 SlotType t1 = r1.getSlotType();
                                                                 SlotType t2 = r2.getSlotType();
                                                                 String n1 = t1 == null ? "zzzzzzzzzz" : t1.getName();
                                                                 String n2 = t2 == null ? "zzzzzzzzzz" : t2.getName();
                                                                 return n1.compareTo(n2);
                                                               }).collect(Collectors.toList());
    register.addRecipes(TConstructJEIConstants.MODIFIERS, modifierRecipes);
    register.addRecipes(TConstructJEIConstants.TOOL_MODIFICATION, FilteredRecipe.unfiltered(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayToolTinkering.class)));

    // beheading
    register.addRecipes(TConstructJEIConstants.SEVERING, RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.SEVERING.get(), SeveringRecipe.class));

    // tool building
    List<ToolBuildingRecipe> toolBuilding = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), ToolBuildingRecipe.class)
      .stream()
      .sorted(Comparator.comparingInt(r -> StationSlotLayoutLoader.getInstance().get(r.getLayoutSlotId()).getSortIndex()))
      .toList();
    register.addRecipes(TConstructJEIConstants.TOOL_BUILDING, toolBuilding);

    // materials
    register.addRecipes(TConstructJEIConstants.MATERIALS, Stream.<IDisplayMaterialRecipe>concat(
        MaterialRecipeCache.getSortedRecipes().stream(),
        Stream.concat(MaterialCastingLookup.getSortedCastingFluids().stream(), MaterialCastingLookup.getSortedCompositeFluids().stream()))
      .sorted(Comparator.comparing(IDisplayMaterialRecipe::getMaterial)).toList());

    // part builder
    MaterialItemList.setRecipes(List.of()); // list of recipes is ignored as this whole class is getting ditched in 1.21; it just clears cache right now
    register.addRecipes(TConstructJEIConstants.PART_BUILDER, RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.PART_BUILDER.get(), IDisplayPartBuilderRecipe.class));

    // modifier worktable
    register.addRecipes(TConstructJEIConstants.MODIFIER_WORKTABLE, RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MODIFIER_WORKTABLE.get(), IModifierWorktableRecipe.class)
      .stream().filter(recipe -> {
        if (recipe.getModifierOptions(null).isEmpty()) {
          TConstruct.LOG.debug("Hiding Modifier Worktable recipe {} as it has no valid modifiers", recipe.getId());
          return false;
        }
        return true;
      }).toList());

    // copy tinker station repair recipes to the crafting table
    CraftingTableRepairKitRecipe craftingRepair = findFirst(manager, RecipeType.CRAFTING, CraftingTableRepairKitRecipe.class);
    TinkerStationRepairRecipe tinkerRepair = findFirst(manager, TinkerRecipeTypes.TINKER_STATION.get(), TinkerStationRepairRecipe.class);
    if (craftingRepair != null && tinkerRepair != null) {
      ResourceLocation id = craftingRepair.getId();
      List<IDisplayCraftingTinkering> recipes = tinkerRepair.getRecipes(access);
      List<CraftingRecipe> newRecipes = new ArrayList<>();
      TinkerStationRepairRecipe.setCraftingId(recipes, id);
      for (IDisplayCraftingTinkering recipe : recipes) {
        if (!recipe.isFiltered()) {
          newRecipes.add(recipe);
        }
      }
      if (!recipes.isEmpty()) {
        register.addRecipes(RecipeTypes.CRAFTING, newRecipes);
      }
    }

    // add an ingredient listener to hide tanks when fluids are hidden
    IIngredientManager ingredientManager = register.getIngredientManager();
    ingredientManager.registerIngredientListener(new TankHidingIngredientListener(ingredientManager, Stream.concat(
      Stream.of(TinkerSmeltery.copperCan, TinkerSmeltery.searedLantern, TinkerSmeltery.scorchedLantern),
      Stream.concat(TinkerSmeltery.searedTank.values().stream(), TinkerSmeltery.scorchedTank.values().stream())
    ).map(ItemLike::asItem).toList()));
  }

  /** Gets a list of filtered casting recipes to give to JEI. */
  private static List<IDisplayableCastingRecipe> getFilteredCastingRecipes(RegistryAccess access, RecipeManager manager, Supplier<? extends RecipeType<ICastingRecipe>> recipeType) {
    return FilteredRecipe.filtered(RecipeHelper.getJEIRecipes(access, manager, recipeType.get(), IDisplayableCastingRecipe.class));
  }

  @Override
  public void registerAdvanced(IAdvancedRegistration registration) {
    Level level = Minecraft.getInstance().level;
    assert level != null;
    RegistryAccess access = level.registryAccess();
    RecipeManager manager = level.getRecipeManager();

    IIngredientManager ingredientManager = registration.getJeiHelpers().getIngredientManager();
    registration.addTypedRecipeManagerPlugin(TConstructJEIConstants.CASTING_BASIN, new CastingRecipeManager(ingredientManager, getFilteredCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_BASIN)));
    registration.addTypedRecipeManagerPlugin(TConstructJEIConstants.CASTING_TABLE, new CastingRecipeManager(ingredientManager, getFilteredCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_TABLE)));
    registration.addTypedRecipeManagerPlugin(TConstructJEIConstants.TOOL_MODIFICATION, new SimpleItemRecipeManager<>(ingredientManager, FilteredRecipe.filtered(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayToolTinkering.class))));

    // copy tinker station repair recipes to the crafting table
    List<IDisplayCraftingTinkering> craftingRecipes = VanillaFilteredRecipe.getRecipes(access, manager, RecipeType.CRAFTING, IDisplayCraftingTinkering.class);
    // copy tinker repair recipes to the crafting table
    TinkerStationRepairRecipe tinkerRepair = findFirst(manager, TinkerRecipeTypes.TINKER_STATION.get(), TinkerStationRepairRecipe.class);
    if (tinkerRepair != null) {
      List<IDisplayCraftingTinkering> recipes = tinkerRepair.getRecipes(access);
      for (IDisplayCraftingTinkering recipe : recipes) {
        if (recipe.isFiltered()) {
          craftingRecipes.add(recipe);
        }
      }
    }
    // add the plugin if we found anything
    if (!craftingRecipes.isEmpty()) {
      registration.addTypedRecipeManagerPlugin(RecipeTypes.CRAFTING, SimpleItemRecipeManager.createCrafting(ingredientManager, craftingRecipes));
    }
  }

  /** Adds a table as a catalys */
  private static void addTableCatalyst(IRecipeCatalystRegistration registry, ItemLike table, TagKey<Item> tag, boolean addDefault, mezz.jei.api.recipe.RecipeType<?>... types) {
    List<ItemStack> list = new ArrayList<>();
    // add default variant
    if (addDefault) list.add(new ItemStack(table));
    RetexturedHelper.addTagVariants(stack -> {
      list.add(stack);
      return false;
    }, table, tag);
    Consumer<IIngredientAcceptor<?>> ingredientAdder = acceptor -> acceptor.addItemStacks(list);
    for (mezz.jei.api.recipe.RecipeType<?> type : types) {
      registry.addRecipeCatalyst(type, ingredientAdder);
    }
  }

  /**
   * Adds an item as a casting catalyst, and as a molding catalyst if it has molding recipes
   * @param registry     Catalyst registry
   * @param item         Item to add
   * @param ownCategory  Category to always add
   * @param type         Molding recipe type
   */
  private static void addCastingCatalyst(IRecipeCatalystRegistration registry, ItemLike item, mezz.jei.api.recipe.RecipeType<IDisplayableCastingRecipe> ownCategory, RecipeType<MoldingRecipe> type) {
    registry.addRecipeCatalyst(item, ownCategory);
    assert Minecraft.getInstance().level != null;
    if (!((RecipeManagerAccessor)Minecraft.getInstance().level.getRecipeManager()).port_lib$byType(type).isEmpty()) {
      registry.addRecipeCatalyst(stack, TConstructJEIConstants.MOLDING);
    }
  }

  @Override
  public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
    // tables
    addTableCatalyst(registry, TinkerTables.craftingStation, ItemTags.LOGS, true, RecipeTypes.CRAFTING);
    addTableCatalyst(registry, TinkerTables.partBuilder, ItemTags.PLANKS, true, TConstructJEIConstants.PART_BUILDER);
    addTableCatalyst(registry, TinkerTables.tinkerStation, ItemTags.PLANKS, true, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.tinkersAnvil, TinkerTags.Items.ANVIL_METAL, false, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.scorchedAnvil, TinkerTags.Items.ANVIL_METAL, false, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.modifierWorktable, TinkerTags.Items.WORKSTATION_ROCK, true, TConstructJEIConstants.MODIFIER_WORKTABLE);

    // smeltery
    registry.addRecipeCatalyst(TinkerSmeltery.searedMelter, TConstructJEIConstants.MELTING, TConstructJEIConstants.FUEL);
    registry.addRecipeCatalyst(TinkerSmeltery.searedHeater, RecipeTypes.FUELING, TConstructJEIConstants.FUEL);
    addCastingCatalyst(registry, TinkerSmeltery.searedTable, TConstructJEIConstants.CASTING_TABLE, TinkerRecipeTypes.MOLDING_TABLE.get());
    addCastingCatalyst(registry, TinkerSmeltery.searedBasin, TConstructJEIConstants.CASTING_BASIN, TinkerRecipeTypes.MOLDING_BASIN.get());
    addTableCatalyst(registry, TinkerSmeltery.smelteryController, TinkerTags.Items.SEARED_BLOCKS, false, TConstructJEIConstants.MELTING, TConstructJEIConstants.ALLOY, TConstructJEIConstants.ENTITY_MELTING, TConstructJEIConstants.FUEL);

    // foundry
    registry.addRecipeCatalyst(TinkerSmeltery.scorchedAlloyer, TConstructJEIConstants.ALLOY, TConstructJEIConstants.FUEL);
    addCastingCatalyst(registry, TinkerSmeltery.scorchedTable, TConstructJEIConstants.CASTING_TABLE, TinkerRecipeTypes.MOLDING_TABLE.get());
    addCastingCatalyst(registry, TinkerSmeltery.scorchedBasin, TConstructJEIConstants.CASTING_BASIN, TinkerRecipeTypes.MOLDING_BASIN.get());
    addTableCatalyst(registry, TinkerSmeltery.foundryController, TinkerTags.Items.SCORCHED_BLOCKS, false, TConstructJEIConstants.FOUNDRY, TConstructJEIConstants.FUEL);

    // modifiers
    for (Holder<Item> item : Objects.requireNonNull(BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.MELEE))) {
      // add any tools with a severing trait
      if (item instanceof IModifiable modifiable && modifiable.getToolDefinition().getData().getTraits().stream().anyMatch(entry -> entry.matches(TinkerModifiers.severing.getId()))) {
        registry.addRecipeCatalyst(IModifiableDisplay.getDisplayStack(item.value()), TConstructJEIConstants.SEVERING);
      }
    }
  }

  @Override
  public void registerItemSubtypes(ISubtypeRegistration registry) {
    // retexturable blocks
    IIngredientSubtypeInterpreter<ItemStack> tables = (stack, context) -> {
      if (context == UidContext.Ingredient) {
        return RetexturedHelper.getTextureName(stack);
      }
      return IIngredientSubtypeInterpreter.NONE;
    };
    registry.registerSubtypeInterpreter(TinkerTables.craftingStation.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.partBuilder.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.tinkerStation.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.modifierWorktable.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.smelteryController.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedDrain.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedDuct.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedChute.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.foundryController.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedDrain.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedDuct.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedChute.asItem(), tables);

    // anvils have both texture and material blocks
    IIngredientSubtypeInterpreter<ItemStack> anvils = (stack, context) -> {
      if (context == UidContext.Ingredient) {
        String name = RetexturedHelper.getTextureName(stack);
        if (!name.isEmpty()) {
          return '#' + name;
        }
        return ToolPartSubtypeInterpreter.INSTANCE.apply(stack, UidContext.Ingredient);
      }
      return IIngredientSubtypeInterpreter.NONE;
    };
    registry.registerSubtypeInterpreter(TinkerTables.tinkersAnvil.asItem(), anvils);
    registry.registerSubtypeInterpreter(TinkerTables.scorchedAnvil.asItem(), anvils);

    // potions
    registry.registerSubtypeInterpreter(TinkerFluids.potion.asItem(), (PotionSubtypeInterpreter<ItemStack>)ItemStack::getTag);
    registry.registerSubtypeInterpreter(ForgeTypes.FLUID_STACK, TinkerFluids.potion.get(), (PotionSubtypeInterpreter<FluidStack>)FluidStack::getTag);

    // parts
    for (Holder<Item> item : getTag(TinkerTags.Items.TOOL_PARTS)) {
      registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item.value(), toolPartInterpreter);
    }

    // tools
    Item slimeskull = TinkerTools.slimesuit.get(ArmorSlotType.HELMET);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, slimeskull, ToolSubtypeInterpreter.ALWAYS);
    for (Holder<Item> item : getTag(TinkerTags.Items.MULTIPART_TOOL)) {
      if (item.value() != slimeskull) {
        registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item.value(), ToolSubtypeInterpreter.INGREDIENT);
      }
    }

    // fluid containers have types based on fluid, don't bother with different sizes
    registry.registerSubtypeInterpreter(TinkerSmeltery.copperCan.get(), (stack, context) -> CopperCanItem.getSubtype(stack));
    IIngredientSubtypeInterpreter<ItemStack> tankInterpreter = (stack, context) -> TankItem.getSubtype(stack);
    for (TankType type : TankType.values()) {
      registry.registerSubtypeInterpreter(TinkerSmeltery.searedTank.get(type).asItem(), tankInterpreter);
      registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedTank.get(type).asItem(), tankInterpreter);
    }
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedLantern.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedLantern.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedFluidCannon.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedFluidCannon.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(TinkerSmeltery.endFluidCannon.asItem(), tankInterpreter);

    registry.registerSubtypeInterpreter(TinkerModifiers.creativeSlotItem.get(), (stack, context) -> {
      SlotType slotType = CreativeSlotItem.getSlot(stack);
      return slotType != null ? slotType.getName() : "";
    });
    registry.registerSubtypeInterpreter(TinkerModifiers.modifierCrystal.get(), (stack, context) -> {
      ModifierId id = ModifierCrystalItem.getModifier(stack);
      return id == null ? "" : id.toString();
    });
  }

  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addGenericGuiContainerHandler(MelterScreen.class, new GuiContainerTankHandler<>());
    registration.addGenericGuiContainerHandler(AlloyerScreen.class, new GuiContainerTankHandler<>());
    registration.addGenericGuiContainerHandler(HeatingStructureScreen.class, new GuiContainerTankHandler<>());
    registration.addGenericGuiContainerHandler(ToolContainerScreen.class, new GuiContainerTankHandler<>());
  }

  @Override
  public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
    registration.addRecipeTransferHandler(new CraftingStationTransferInfo());
    IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
    registration.addRecipeTransferHandler(new TinkerStationTransferInfo<>(TConstructJEIConstants.MODIFIERS, helper), TConstructJEIConstants.MODIFIERS);
    registration.addRecipeTransferHandler(new TinkerStationTransferInfo<>(TConstructJEIConstants.TOOL_BUILDING, helper), TConstructJEIConstants.TOOL_BUILDING);
    registration.addRecipeTransferHandler(new ToolInventoryTransferInfo(helper), RecipeTypes.CRAFTING);
  }

  /**
   * Removes a fluid from JEI
   * @param manager  Manager
   * @param fluidHelper Platform specific fluid helper
   * @param fluid    Fluid to remove
   * @param bucket   Fluid bucket to remove
   */
  private static void removeFluid(IIngredientManager manager, IPlatformFluidHelper<IJeiFluidIngredient> fluidHelper, Fluid fluid, Item bucket) {
    manager.removeIngredientsAtRuntime(FabricTypes.FLUID_STACK, Collections.singleton(fluidHelper.create(fluid, fluidHelper.bucketVolume())));
    manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, Collections.singleton(new ItemStack(bucket)));
  }

  /** Helper to get an item tag */
  private static Iterable<Holder<Item>> getTag(ResourceLocation name) {
    return getTag(TagKey.create(Registries.ITEM, name));
  }

  /** Helper to get an item tag */
  private static Iterable<Holder<Item>> getTag(TagKey<Item> name) {
    return Objects.requireNonNull(BuiltInRegistries.ITEM.getTagOrEmpty(name));
  }

  /**
   * Hides an item if the related tag is empty
   * @param manager  Ingredient manager
   * @param item     Cast instance
   * @param tagName  Tag to check
   */
  @SuppressWarnings("SameParameterValue")
  private static void optionalItem(IIngredientManager manager, ItemLike item, String tagName) {
    Iterable<Holder<Item>> tag = getTag(new ResourceLocation("c", tagName));
    if (Iterables.isEmpty(tag)) {
      manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, Collections.singletonList(new ItemStack(item)));
    }
  }

  /**
   * Hides casts if the related tag is empty
   * @param manager  Ingredient manager
   * @param cast     Cast instance
   */
  private static void optionalCast(IIngredientManager manager, CastItemObject cast) {
    Iterable<Holder<Item>> tag = getTag(new ResourceLocation("c", cast.getName().getPath() + "_blocks"));
    if (Iterables.isEmpty(tag)) {
      manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, cast.values().stream().map(ItemStack::new).collect(Collectors.toList()));
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
    IIngredientManager manager = jeiRuntime.getIngredientManager();
    IPlatformFluidHelper<IJeiFluidIngredient> fluidHelper = (IPlatformFluidHelper<IJeiFluidIngredient>) jeiRuntime.getJeiHelpers().getPlatformFluidHelper();

    // ingredient runtime
    List<ItemStack> removeItems = new ArrayList<>();
    Consumer<ItemStack> removeItem = removeItems::add;
    List<ItemStack> addItems = new ArrayList<>();
    Consumer<ItemStack> addItem = addItems::add;
    // ingredient visibility
    List<ItemStack> hideItems = new ArrayList<>();
    Consumer<ItemStack> hideItem = hideItems::add;
    List<ItemStack> showItems = new ArrayList<>();
    Consumer<ItemStack> showItem = showItems::add;
    // shown via the modifiers
    NonNullList<ItemStack> modifierCrystals = NonNullList.create();
//    TinkerModifiers.modifierCrystal.get().fillItemCategory(CreativeModeTab.TAB_SEARCH, modifierCrystals); TODO: PORT?
    if (!modifierCrystals.isEmpty()) {
      manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, modifierCrystals);
    }

    // hide knightslime and slimesteel until implemented
    removeFluid(manager, fluidHelper, TinkerFluids.moltenSoulsteel.get(), TinkerFluids.moltenSoulsteel.asItem());
    removeFluid(manager, fluidHelper, TinkerFluids.moltenKnightslime.get(), TinkerFluids.moltenKnightslime.asItem());
    // hide compat that is not present
    List<FluidStack> removeFluids = new ArrayList<>();
    for (SmelteryCompat compat : SmelteryCompat.values()) {
      Iterable<Holder<Item>> ingot = getTag(new ResourceLocation("c", compat.getName() + "_ingots"));
      if (Iterables.isEmpty(ingot)) {
        removeFluid(manager, fluidHelper, compat.getFluid().get(), compat.getBucket());
      }
    }
    if (!FabricLoader.getInstance().isModLoaded("ceramics")) {
      removeFluid(manager, fluidHelper, TinkerFluids.moltenPorcelain.get(), TinkerFluids.moltenPorcelain.asItem());
    }
    optionalCast(manager, TinkerSmeltery.plateCast);
    optionalCast(manager, TinkerSmeltery.gearCast);
    optionalCast(manager, TinkerSmeltery.coinCast);
    optionalCast(manager, TinkerSmeltery.wireCast);
    optionalItem(manager, TinkerMaterials.necroniumBone, "uranium_ingots");
    modIdHelper = jeiRuntime.getJeiHelpers().getModIdHelper();
  }

  /** Class to pass {@link IScreenWithFluidTank} into JEI */
  public static class GuiContainerTankHandler<C extends AbstractContainerMenu, T extends AbstractContainerScreen<C> & IScreenWithFluidTank> implements IGuiContainerHandler<T> {
//    @Override TODO: PORT
    @Nullable
    public Object getIngredientUnderMouse(T containerScreen, double mouseX, double mouseY) {
      return containerScreen.getIngredientUnderMouse(mouseX, mouseY);
    }
  }

  /** Subtype interpreter for tools, treats the tool as unique in ingredient list, generic in recipes */
  public enum ToolSubtypeInterpreter implements IIngredientSubtypeInterpreter<ItemStack> {
    ALWAYS, INGREDIENT;

    @Override
    public String apply(ItemStack itemStack, UidContext context) {
      if (this == ALWAYS || context == UidContext.Ingredient) {
        StringBuilder builder = new StringBuilder();
        List<MaterialVariantId> materialList = MaterialIdNBT.from(itemStack).getMaterials();
        if (!materialList.isEmpty()) {
          // append first entry without a comma
          builder.append(materialList.get(0));
          for (int i = 1; i < materialList.size(); i++) {
            builder.append(',');
            builder.append(materialList.get(i).getId());
          }
        }
        return builder.toString();
      }
      return NONE;
    }
  }
}
