package slimeknights.tconstruct.library.client.book.content;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.ForgeI18n;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.content.PageContent;
import slimeknights.mantle.client.book.data.element.TextComponentData;
import slimeknights.mantle.client.book.data.element.TextData;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.client.screen.book.element.BookElement;
import slimeknights.mantle.client.screen.book.element.ItemElement;
import slimeknights.mantle.client.screen.book.element.TextComponentElement;
import slimeknights.mantle.client.screen.book.element.TextElement;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.mantle.util.html.HtmlElement;
import slimeknights.mantle.util.html.HtmlGroup;
import slimeknights.mantle.util.html.HtmlSerializable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.client.book.elements.TinkerItemElement;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.util.ModifierTooltip;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialFluidRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerToolParts;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Base class for material content pages.
 * TODO 1.21: move to {@link slimeknights.tconstruct.library.client.book.content.material}.
 */
public abstract class AbstractMaterialContent extends PageContent {
  /** List of fallback items for the icon if no material recipes. */
  private static final List<Supplier<? extends IMaterialItem>> FALLBACKS = new ArrayList<>();
  /** Tooltip components for the part builder craftable icon */
  public static final List<Component> PART_BUILDER = List.of(
    TConstruct.makeTranslation("book", "material.craftable"),
    TConstruct.makeTranslation("book", "material.part_builder").withStyle(ChatFormatting.GRAY)
  );
  private static final Component CASTABLE = TConstruct.makeTranslation("book", "material.castable");
  private static final String CAST_FROM = TConstruct.makeTranslationKey("book", "material.cast_from");
  private static final Component COMPOSITE = TConstruct.makeTranslation("book", "material.composite");
  private static final String COMPOSITE_FROM = TConstruct.makeTranslationKey("book", "material.composite_from");

  static final int COLUMN_MARGIN = 22;
  static final int STAT_WIDTH = BookScreen.PAGE_WIDTH / 2 - 10;

  // cached data
  private transient MaterialVariantId materialVariant;
  private transient List<ItemStack> repairStacks;
  private transient IMaterial material;

  public String title = "";
  @SerializedName("material")
  public String materialName = "";
  public boolean detailed = false;
  @SerializedName("show_all_tools")
  public boolean showAllTools = false;
  /** Additional suffix on the title from the section */
  public transient Component titleSuffix = null;

  public AbstractMaterialContent(MaterialVariantId materialVariant, boolean detailed) {
    this.materialName = materialVariant.toString();
    this.materialVariant = materialVariant;
    this.detailed = detailed;
  }

  /** Gets the page type ID */
  public abstract ResourceLocation getId();

  /** Given an index 0-3, return the stat type to show at that index */
  @Nullable
  protected abstract MaterialStatsId getStatType(int index);

  /** Gets the number of rows to display for the stats section */
  protected int getStatRows() {
    return 2;
  }

  /** Gets the suffix for the translation key */
  protected String translationSuffix() {
    // TODO 1.21: make abstract and drop the empty condition below
    return "";
  }

  /** Gets the text to display, empty if no text */
  protected String getTextKey(MaterialId material) {
    // allow both the encyclopedia and flavor keys to use a separate variant if defined
    String rootKey = "material." + material.toLanguageKey() + (detailed ? ".encyclopedia" : ".flavor");
    String suffix = translationSuffix();
    // allow the suffix to override the translation key
    if (!suffix.isEmpty()) {
      String primaryKey = rootKey + '.' + translationSuffix();
      if (Util.canTranslate(primaryKey)) {
        return primaryKey;
      }
    }
    return rootKey;
  }

  /** Returns true if this stat type is supported, anything unsupported is hidden from the tools list */
  protected abstract boolean supportsStatType(MaterialStatsId statsId);

  /** Gets the material variant for this page */
  protected MaterialVariantId getMaterialVariant() {
    if (materialVariant == null) {
      materialVariant = MaterialVariantId.parse(materialName);
    }
    return materialVariant;
  }

  /** Gets the material for this page */
  protected IMaterial getMaterial() {
    if (material == null) {
      material = MaterialRegistry.getMaterial(getMaterialVariant().getId());
    }
    return material;
  }

  /** Gets a list of all repair items for the given material */
  protected List<ItemStack> getRepairStacks() {
    if (repairStacks == null) {
      Level world = Minecraft.getInstance().level;
      if (world == null) {
        return Collections.emptyList();
      }
      // simply combine all items from all recipes
      MaterialVariantId material = getMaterialVariant();
      repairStacks = RecipeHelper.getUIRecipes(world.getRecipeManager(), TinkerRecipeTypes.MATERIAL.get(), MaterialRecipe.class, recipe -> material.matchesVariant(recipe.getMaterial()))
        .stream()
        // prefer 1 value 1 needed (ingots), then 1 value with higher needed (nuggets), then higher value (blocks)
        .sorted(Comparator.comparing(MaterialRecipe::getValue).thenComparing(MaterialRecipe::getNeeded))
        .flatMap(recipe -> Arrays.stream(recipe.getIngredient().getItems()))
        .collect(Collectors.toList());
      // no repair items? use the fallbacks
      if (repairStacks.isEmpty()) {
        // use the fallback stacks
        repairStacks = FALLBACKS.stream().map(Supplier::get)
          .filter(part -> part.canUseMaterial(material.getId()))
          .map(part -> part.withMaterialForDisplay(material)).toList();

        // no matching fallback? just use a repair kit
        if (repairStacks.isEmpty()) {
          TConstruct.LOG.debug("Material with id " + material + " has no representation items associated with it, using repair kit");
          // bypass the valid check, because we need to show something
          repairStacks = Collections.singletonList(TinkerToolParts.repairKit.get().withMaterialForDisplay(material));
        }
      }
    }
    return repairStacks;
  }

  /** Gets the display stacks for this page */
  public List<ItemStack> getDisplayStacks() {
    return getRepairStacks();
  }

  @Nonnull
  @Override
  public String getTitle() {
    if (title.isEmpty()) {
      return getTitleComponent().getString();
    }
    return title;
  }

  /** Gets the title of this page to display in the index */
  public Component getTitleComponent() {
    Component material = MaterialTooltipCache.getDisplayName(getMaterialVariant());
    if (titleSuffix != null) {
      return Component.translatable(TooltipUtil.KEY_FORMAT, material, titleSuffix);
    }
    return material;
  }

  @Override
  public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
    MaterialVariantId materialVariant = getMaterialVariant();
    this.addTitle(list, getTitle(), true, MaterialTooltipCache.getColor(materialVariant).getValue());

    // the cool tools to the left/right
    this.addDisplayItems(list, rightSide ? BookScreen.PAGE_WIDTH - 18 : 0, materialVariant);

    int y = getTitleHeight();
    int x = (rightSide ? 0 : COLUMN_MARGIN) + 2;

    // material stats
    y = addAllMaterialStats(x, y, list, getStatRows(), true);
    // material description
    addDescription(x, y, list);
  }

  /** Adds the given number of rows of material info */
  protected int addAllMaterialStats(int x, int y, List<BookElement> list, int rows, boolean includeStats) {
    for (int i = 0; i < rows; i++) {
      y = Math.max(
        this.addMaterialStat(x - 3,          y, STAT_WIDTH, list, getStatType(i * 2),     includeStats),
        this.addMaterialStat(x + STAT_WIDTH, y, STAT_WIDTH, list, getStatType(i * 2 + 1), includeStats));
    }
    return y;
  }

  /** Adds the stats for a stat type */
  protected int addMaterialStat(int x, int y, int w, List<BookElement> list, @Nullable MaterialStatsId statsId, boolean includeStats) {
    if (statsId == null) {
      return y;
    }
    IMaterialRegistry registry = MaterialRegistry.getInstance();
    MaterialVariantId material = getMaterialVariant();
    Optional<IMaterialStats> stats = registry.getMaterialStats(material.getId(), statsId);
    if (stats.isEmpty()) {
      return y;
    }

    // create a list of all valid toolparts with the stats
    List<ItemStack> parts = getPartsWithMaterial(material, statsId);
    // said parts next to the name
    int textOffset = 0;
    if (!parts.isEmpty()) {
      ItemElement display = new TinkerItemElement(x, y + 1, 0.5f, parts);
      list.add(display);
      textOffset = 10;
    }

    // and the name itself
    list.add(new TextComponentElement(x + textOffset, y, w - textOffset, 10, stats.get().getLocalizedName().withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE)));
    y += 12;

    List<TextComponentData> lineData = Lists.newArrayList();
    // add lines of tool information
    List<Component> localizedDescription = stats.get().getLocalizedDescriptions();
    if (!localizedDescription.isEmpty() && (localizedDescription.size() > 1 || localizedDescription.get(0) != Component.empty())) {
      lineData.addAll(getStatLines(stats.get()));
    }
    addTraitLines(lineData, registry.getTraits(material.getId(), statsId));

    list.add(new TextComponentElement(x, y, w, BookScreen.PAGE_HEIGHT, lineData));

    // TODO: calculate actual height to properly wrap long lines?
    return y + (lineData.size() * 10) + 3;
  }

  /** Gets all stat text data for the given stat instance */
  private static void addStatLines(List<TextComponentData> lineData, IMaterialStats stats) {
    List<Component> statInfo = stats.getLocalizedInfo();
    List<Component> tooltips = stats.getLocalizedDescriptions();
    int size = Math.min(statInfo.size(), tooltips.size());
    for (int i = 0; i < size; i++) {
      // skip empty tooltips, means empty stats
      Component tooltip = tooltips.get(i);
      TextComponentData text = new TextComponentData(statInfo.get(i));
      if (tooltip.getString().isEmpty()) {
        text.tooltips = null;
      } else {
        text.tooltips = new Component[]{tooltip};
      }
      text.linebreak = true;
      lineData.add(text);
    }
  }

  /** Gets all trait text data for the given stat instance */
  protected static void addTraitLines(List<TextComponentData> lineData, List<ModifierEntry> traits) {
    for (ModifierEntry trait : traits) {
      if (!trait.isBound()) {
        continue;
      }
      Modifier mod = trait.getModifier();
      if (!mod.shouldDisplay(ModifierTooltip.BOOK)) {
        continue;
      }
      TextComponentData textComponentData = new TextComponentData(mod.getDisplayName());

      List<Component> textComponents = mod.getDescriptionList(trait.getLevel());
      textComponentData.tooltips = textComponents.toArray(new Component[0]);
      textComponentData.text = textComponentData.text.copy().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.UNDERLINE);
      textComponentData.linebreak = true;
      lineData.add(textComponentData);
    }
  }


  /** Gets the tooltip for a material category */
  protected static TinkerItemElement makeCategoryIcon(ItemStack item, ResourceLocation name) {
    TinkerItemElement element = new TinkerItemElement(item);
    name = name.withPrefix("material.category.");
    element.tooltip = List.of(
      Component.translatable(Util.makeTranslationKey("book", name)),
      Component.translatable(Util.makeTranslationKey("book", name.withSuffix(".description"))).withStyle(ChatFormatting.GRAY)
    );
    return element;
  }

  /** Adds the material category icon */
  protected void addCategory(List<ItemElement> displayTools, MaterialId material) {}

  /** If true, this section will display the part builder crafting icon. */
  protected boolean allowPartBuilder() {
    return true;
  }

  /** If true, this section will display casting and composite recipes. */
  protected boolean allowCasting() {
    return true;
  }

  /** Adds items to the display tools list for all relevant recipes */
  protected void addPrimaryDisplayItems(List<ItemElement> displayTools, MaterialVariantId materialId) {
    IMaterial material = getMaterial();
    // part builder
    if (allowPartBuilder() && material.isCraftable()) {
      ItemStack partBuilder = new ItemStack(TinkerTables.partBuilder.asItem());
      ItemElement elementItem = new TinkerItemElement(partBuilder);
      elementItem.tooltip = PART_BUILDER;
      displayTools.add(elementItem);
    }

    // regular casting recipes
    List<MaterialFluidRecipe> fluids = MaterialCastingLookup.getCastingFluids(materialId);
    if (!fluids.isEmpty()) {
      ItemElement elementItem = new TinkerItemElement(0, 0, 1, fluids.stream().flatMap(recipe -> recipe.getFluids().stream())
                                                                     .map(fluid -> new ItemStack(fluid.getFluid().getBucket()))
                                                                     .collect(Collectors.toList()));
      FluidStack firstFluid = fluids.stream()
                                    .flatMap(recipe -> recipe.getFluids().stream())
                                    .findFirst().orElse(FluidStack.EMPTY);
      elementItem.tooltip = ImmutableList.of(Component.translatable(CAST_FROM, FluidVariantAttributes.getName(firstFluid.getType())));
      displayTools.add(elementItem);
    }

    // composite casting
    List<MaterialFluidRecipe> composites = MaterialCastingLookup.getCompositeFluids(materialId);
    for (MaterialFluidRecipe composite : composites) {
      MaterialVariant input = composite.getInput();
      if (input != null) {
        MaterialVariantId inputId = input.getVariant();
        ItemElement elementItem = new TinkerItemElement(0, 0, 1, MaterialCastingLookup.getAllItemCosts().stream()
                                                                                      .map(Entry::getKey)
                                                                                      .filter(part -> part.canUseMaterial(inputId.getId()) && part.canUseMaterial(material))
                                                                                      .map(part -> part.withMaterial(inputId))
                                                                                      .collect(Collectors.toList()));
        FluidStack firstFluid = composite.getFluids().stream().findFirst().orElse(FluidStack.EMPTY);
        elementItem.tooltip = ImmutableList.of(Component.translatable(COMPOSITE_FROM, FluidVariantAttributes.getName(firstFluid.getType()), MaterialTooltipCache.getDisplayName(inputId)));
        displayTools.add(elementItem);
      }
    }
  }

  /** Adds display items to the tool sidebars */
  @SuppressWarnings("deprecation")  // its the best tag lookup
  protected void addDisplayItems(ArrayList<BookElement> list, int x, MaterialVariantId materialVariant) {
    List<ItemElement> displayTools = Lists.newArrayList();

    addCategory(displayTools, materialVariant.getId());

    // add display items
    displayTools.add(new TinkerItemElement(0, 0, 1f, getRepairStacks()));
    addPrimaryDisplayItems(displayTools, materialVariant);

    // fill in leftover space
    if (displayTools.size() < 9) {
      MaterialId materialId = materialVariant.getId();
      toolLoop:
      for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.MULTIPART_TOOL)) {
        if (item.value() instanceof IModifiable tool) {
          List<PartRequirement> requirements = tool.getToolDefinition().getData().getParts();
          // start building the tool with the given material
          MaterialNBT.Builder materials = MaterialNBT.builder();
          boolean usedMaterial = false;
          for (MaterialStatsId part : requirements) {
            // by default, give up if the tool contains any parts of another stat type. Mostly filters out ancient tools
            // but by request we can keep those visible
            boolean supported = supportsStatType(part);
            // if the stat type is not supported by the material, substitute
            if (part.canUseMaterial(materialId)) {
              materials.add(materialVariant);
              if (supported) {
                usedMaterial = true;
              }
            } else {
              materials.add(MaterialRegistry.firstWithStatType(part));
            }
          }

          // only add a stack if our material showed up
          if (usedMaterial) {
            ItemStack display = ToolBuildHandler.buildItemFromMaterials(tool, materials.build());
            displayTools.add(new TinkerItemElement(display));
            if (displayTools.size() == 9) {
              break;
            }
          }
        }
      }
    }

    // built tools
    if (!displayTools.isEmpty()) {
      int y = getTitleHeight() - 5;
      for (ItemElement element : displayTools) {
        element.x = x;
        element.y = y;
        element.scale = 1f;
        y += ItemElement.ITEM_SIZE_HARDCODED;

        list.add(element);
      }
    }
  }

  /** Adds the display text at the end of the material description */
  protected void addDescription(int x, int y, List<BookElement> list) {
    // inspirational quote, or boring description text
    String textKey = getTextKey(materialVariant.getId());
    if (I18n.exists(textKey)) {
      // using forge instead of I18n.format as that prevents % from being interpreted as a format key
      String translated = ForgeI18n.getPattern(textKey);
      if (!detailed ) {
        translated = '"' + translated + '"';
      }
      TextData flavourData = new TextData(translated);
      flavourData.italic = !detailed;
      list.add(new TextElement(x - 3, y + 5, BookScreen.PAGE_WIDTH - COLUMN_MARGIN - 5, 60, flavourData));
    }
  }


  /** Gets a list of all tool parts */
  private List<IToolPart> getToolParts() {
    return RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, TinkerTags.Items.TOOL_PARTS)
                         .filter(item -> item instanceof IToolPart)
                         .map(item -> (IToolPart) item)
                         .collect(Collectors.toList());
  }
}
