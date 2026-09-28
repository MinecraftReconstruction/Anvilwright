package slimeknights.tconstruct.library.data.material;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import slimeknights.mantle.client.book.data.JsonCondition;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.recipe.condition.TagFilledCondition;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.library.json.JsonRedirect;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialManager;
import slimeknights.tconstruct.library.materials.json.MaterialJson;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extendable material provider, useful for addons
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public abstract class AbstractMaterialDataProvider extends GenericDataProvider {
  /** General purpose materials */
  public static final int ORDER_GENERAL = 0;
  /** Materials primarily used for harvest */
  public static final int ORDER_HARVEST = 1;
  /** Materials primarily used for weapons */
  public static final int ORDER_WEAPON = 2;
  /** General purpose materials */
  public static final int ORDER_SPECIAL = 3;
  /** Ranged exclusive materials */
  public static final int ORDER_RANGED = 4;
  /** Order for mod integration materials */
  public static final int ORDER_COMPAT = 5;
  /** Order for nether materials in tiers 1-3 */
  public static final int ORDER_NETHER = 10;
  /** Order for end materials in tiers 1-4 */
  public static final int ORDER_END = 15;
  /** Order for materials that are just a binding */
  public static final int ORDER_BINDING = 20;
  /** Order for materials that are just used for repair or textures */
  public static final int ORDER_REPAIR = 25;

  /** List of all added materials */
  private final Map<MaterialId, MaterialBuilder> allMaterials = new HashMap<>();

  /** Boolean just in case material stats run first */
  private boolean addMaterialsRun = false;

  public AbstractMaterialDataProvider(FabricDataOutput output) {
    super(output, MaterialManager.FOLDER, MaterialManager.GSON);
  }

  /**
   * Function to add all relevant materials
   */
  protected abstract void addMaterials();

  private void ensureAddMaterialsRun() {
    if (addMaterialsRun) {
      return;
    }
    addMaterialsRun = true;
    addMaterials();
  }

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    ensureAddMaterialsRun();
    List<CompletableFuture<?>> futures = new ArrayList<>();
    allMaterials.forEach((id, data) -> futures.add(saveThing(cache, id, convert(data))));
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
  }

  /**
   * Gets a list of all material IDs that are generated. Note this will run {@link #addMaterials()}, so generally its better to run your material data provider first
   * @return  Material ID list
   */
  public Set<MaterialId> getAllMaterials() {
    ensureAddMaterialsRun();
    // ignore any pure redirects
    return allMaterials.values().stream()
      .filter(e -> !e.isPureRedirect())
      .map(b -> b.id)
      .collect(Collectors.toSet());
  }


  /* Base methods */

  /** Adds a material to be generated with a condition and redirect data */
  protected void addMaterial(IMaterial material, @Nullable ConditionJsonProvider condition, JsonRedirect... redirect) {
    allMaterials.put(material.getIdentifier(), new DataMaterial(material, condition, redirect));
  }

  /** Adds JSON to redirect an ID to another ID */
  protected void addRedirect(MaterialId id, @Nullable ConditionJsonProvider condition, JsonRedirect... redirect) {
    allMaterials.put(id, new DataMaterial(null, condition, redirect));
  }

  /** Adds JSON to redirect an ID to another ID */
  protected void addRedirect(MaterialId id, JsonRedirect... redirect) {
    addRedirect(id, null, redirect);
  }

  /* Material helpers */

  /** Conditions on a forge tag existing */
  protected static ConditionJsonProvider tagExistsCondition(String name) {
    return DefaultResourceConditions.itemTagsPopulated(TagKey.create(Registries.ITEM, new ResourceLocation("c", name)));
  }

  /** Creates a normal material with a condition and a redirect */
  protected void addMaterial(MaterialId location, int tier, int order, boolean craftable, boolean hidden, @Nullable ConditionJsonProvider condition, JsonRedirect... redirect) {
    addMaterial(new Material(location, tier, order, craftable, hidden), condition, redirect);
  }

  /** @deprecated use {@link #material(MaterialId)} */
  @Deprecated
  protected void addMaterial(MaterialId location, int tier, int order, boolean craftable) {
    material(location).tier(tier).sort(order).craftable(craftable);
  }

  /** Creates a new compat material */
  protected void addCompatMaterial(MaterialId location, int tier, int order, String tagName, boolean craftable) {
    ConditionJsonProvider condition = DefaultResourceConditions.or(ConfigEnabledCondition.FORCE_INTEGRATION_MATERIALS, tagExistsCondition(tagName));
    addMaterial(location, tier, order, craftable, false, condition);
  }

  /** Creates a new compat material */
  protected void addCompatMetalMaterial(MaterialId location, int tier, int order, String ingotName) {
    addCompatMaterial(location, tier, order, ingotName + "_ingots", false);
  }

  /** @deprecated use {@link MaterialBuilder#compatMetal()} */
  @Deprecated
  protected void addCompatMetalMaterial(MaterialId location, int tier, int order) {
    addCompatMetalMaterial(location, tier, order, location.getPath());
  }

  /** @deprecated use {@link MaterialBuilder#compatAlloy(ICondition...)} */
  @Deprecated
  protected void addCompatAlloy(MaterialId location, int tier, int order, ICondition... alloyConditions) {
    ICondition condition = new OrCondition(
      // if forced
      ConfigEnabledCondition.FORCE_INTEGRATION_MATERIALS,
      // or we have the matching alloy ingot
      tagExistsCondition("ingots/" + location.getPath()),
      // or we allow ingotless alloys and have all alloy components
      new AndCondition(Util.prepend(alloyConditions, ConfigEnabledCondition.ALLOW_INGOTLESS_ALLOYS))
    );
    addMaterial(location, tier, order, false, false, condition);
  }

  /** @deprecated use {@link MaterialBuilder#compatAlloy(String...)} */
  @Deprecated
  protected void addCompatAlloy(MaterialId location, int tier, int order, String component) {
    addCompatAlloy(location, tier, order, tagExistsCondition("ingots/" + component));
  }


  /* Redirect helpers */

  /** Makes a conditional redirect to the given ID */
  protected JsonRedirect conditionalRedirect(MaterialId id, @Nullable ConditionJsonProvider condition) {
    return new JsonRedirect(id, condition, null);
  }

  /** @deprecated use {@link MaterialBuilder#redirect(ResourceLocation, ICondition...)} */
  @Deprecated
  protected JsonRedirect redirect(MaterialId id) {
    return conditionalRedirect(id, null);
  }


  /* Builder */

  @Accessors(fluent = true)
  @Setter
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  protected static class MaterialBuilder {
    private final List<ICondition> conditions = new ArrayList<>();
    private final List<JsonRedirect> redirects = new ArrayList<>();
    private final MaterialId id;
    private boolean craftable = false;
    private boolean hidden = false;
    private int tier = 1;
    private int sort = 100;
    private Rarity rarity = null;

    /** Makes the material craftable in the part builder */
    public MaterialBuilder craftable() {
      craftable = true;
      return this;
    }
    if (material == null) {
      return new MaterialJson(new JsonCondition(data.condition), null, null, null, null, redirect);
    }
    return new MaterialJson(new JsonCondition(data.condition), material.isCraftable(), material.getTier(), material.getSortOrder(), material.isHidden(), redirect);
  }

  private record DataMaterial(@Nullable IMaterial material, @Nullable ConditionJsonProvider condition, JsonRedirect[] redirect) {}
}
