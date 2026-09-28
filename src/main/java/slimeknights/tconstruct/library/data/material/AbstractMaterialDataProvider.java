package slimeknights.tconstruct.library.data.material;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Rarity;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.GenericDataProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.library.json.JsonRedirect;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialManager;
import slimeknights.tconstruct.library.materials.json.MaterialJson;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

  public AbstractMaterialDataProvider(PackOutput packOutput) {
    super(packOutput, Target.DATA_PACK, MaterialManager.FOLDER, MaterialManager.GSON);
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
    return allOf(allMaterials.entrySet().stream().map(entry -> saveJson(cache, entry.getKey(), entry.getValue().convert())));
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

  /** Creates a new material builder */
  protected MaterialBuilder material(MaterialId id) {
    return allMaterials.computeIfAbsent(id, MaterialBuilder::new);
  }

  /** Conditions on a tag existing; Mantle's TagFilledCondition is a loot condition under Fabric, so use the resource condition instead */
  protected static ConditionJsonProvider tagExistsCondition(String name) {
    return DefaultResourceConditions.itemTagsPopulated(TagKey.create(Registries.ITEM, Mantle.commonResource(name)));
  }


  /* Material helpers */

  /** @deprecated use {@link #material(MaterialId)} */
  @Deprecated
  protected void addMaterial(IMaterial material, @Nullable ConditionJsonProvider condition, JsonRedirect... redirect) {
    MaterialId id = material.getIdentifier();
    if (allMaterials.containsKey(id)) {
      throw new IllegalArgumentException("Duplicate material with ID: " + material.getIdentifier());
    }
    MaterialBuilder builder = material(id)
      .craftable(material.isCraftable())
      .hidden(material.isHidden())
      .tier(material.getTier())
      .sort(material.getSortOrder());
    // skip rarity as that method was never designed to set it
    if (condition != null) {
      builder.condition(condition);
    }
    for (JsonRedirect r : redirect) {
      builder.redirect(r);
    }
  }

  /** @deprecated use {@link #material(MaterialId)} */
  @Deprecated
  protected void addMaterial(MaterialId location, int tier, int order, boolean craftable, boolean hidden, @Nullable ConditionJsonProvider condition, JsonRedirect... redirect) {
    MaterialBuilder builder = material(location).tier(tier).sort(order).craftable(craftable).hidden(hidden);
    if (condition != null) {
      builder.condition(condition);
    }
    for (JsonRedirect r : redirect) {
      builder.redirect(r);
    }
  }

  /** @deprecated use {@link #material(MaterialId)} */
  @Deprecated
  protected void addMaterial(MaterialId location, int tier, int order, boolean craftable) {
    material(location).tier(tier).sort(order).craftable(craftable);
  }

  /** @deprecated {@link MaterialBuilder#compat(String...)} */
  @Deprecated
  protected void addCompatMaterial(MaterialId location, int tier, int order, boolean craftable, String... tagNames) {
    ConditionJsonProvider condition = DefaultResourceConditions.or(Stream.concat(
      Stream.of(ConfigEnabledCondition.FORCE_INTEGRATION_MATERIALS),
      Arrays.stream(tagNames).map(AbstractMaterialDataProvider::tagExistsCondition)
    ).toArray(ConditionJsonProvider[]::new));
    addMaterial(location, tier, order, craftable, false, condition);
  }

  /** @deprecated {@link MaterialBuilder#compatMetal(String)} or {@link MaterialBuilder#compatAlloy(String...)} */
  @Deprecated(forRemoval = true)
  protected void addCompatMetalMaterial(MaterialId location, int tier, int order, String... ingotNames) {
    addCompatMaterial(location, tier, order, false, Arrays.stream(ingotNames).map(name -> "ingots/" + name).toArray(String[]::new));
  }

  /** @deprecated use {@link MaterialBuilder#compatMetal()} */
  @Deprecated
  protected void addCompatMetalMaterial(MaterialId location, int tier, int order) {
    addCompatMetalMaterial(location, tier, order, location.getPath());
  }

  /** @deprecated use {@link MaterialBuilder#compatAlloy(ConditionJsonProvider...)} */
  @Deprecated
  protected void addCompatAlloy(MaterialId location, int tier, int order, ConditionJsonProvider... alloyConditions) {
    ConditionJsonProvider condition = DefaultResourceConditions.or(
      // if forced
      ConfigEnabledCondition.FORCE_INTEGRATION_MATERIALS,
      // or we have the matching alloy ingot
      tagExistsCondition("ingots/" + location.getPath()),
      // or we allow ingotless alloys and have all alloy components
      DefaultResourceConditions.and(Util.prepend(alloyConditions, ConfigEnabledCondition.ALLOW_INGOTLESS_ALLOYS))
    );
    addMaterial(location, tier, order, false, false, condition);
  }

  /** @deprecated use {@link MaterialBuilder#compatAlloy(String...)} */
  @Deprecated
  protected void addCompatAlloy(MaterialId location, int tier, int order, String component) {
    addCompatAlloy(location, tier, order, tagExistsCondition("ingots/" + component));
  }


  /* Redirect helpers */

  /** @deprecated use {@link MaterialBuilder#redirect(ResourceLocation, ConditionJsonProvider...)} */
  @Deprecated
  protected void addRedirect(MaterialId id, @Nullable ConditionJsonProvider condition, JsonRedirect... redirect) {
    MaterialBuilder builder = material(id);
    if (condition != null) {
      builder.condition(condition);
    }
    for (JsonRedirect r : redirect) {
      builder.redirect(r);
    }
  }

  /** @deprecated use {@link MaterialBuilder#redirect(ResourceLocation, ConditionJsonProvider...)} */
  @Deprecated
  protected void addRedirect(MaterialId id, JsonRedirect... redirect) {
    addRedirect(id, null, redirect);
  }

  /** @deprecated use {@link MaterialBuilder#redirect(ResourceLocation, ConditionJsonProvider...)} */
  @Deprecated
  protected JsonRedirect conditionalRedirect(MaterialId id, @Nullable ConditionJsonProvider condition) {
    return new JsonRedirect(id, condition);
  }

  /** @deprecated use {@link MaterialBuilder#redirect(ResourceLocation, ConditionJsonProvider...)} */
  @Deprecated
  protected JsonRedirect redirect(MaterialId id) {
    return conditionalRedirect(id, null);
  }


  /* Builder */

  @Accessors(fluent = true)
  @Setter
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  protected static class MaterialBuilder {
    private final List<ConditionJsonProvider> conditions = new ArrayList<>();
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

    /** Makes the material hidden in recipe viewers and the book */
    public MaterialBuilder hidden() {
      hidden = true;
      return this;
    }


    /* Conditions */

    /** Adds a condition to the builder */
    public MaterialBuilder condition(@Nullable ConditionJsonProvider condition, ConditionJsonProvider... conditions) {
      this.conditions.add(condition);
      Collections.addAll(this.conditions, conditions);
      return this;
    }

    /** Adds a tag exists condition */
    public MaterialBuilder ifTagExists(String name) {
      return condition(tagExistsCondition(name));
    }

    /** Adds the compat material conditions */
    private MaterialBuilder compat(Stream<ConditionJsonProvider> conditions) {
      return condition(DefaultResourceConditions.or(Stream.concat(Stream.of(ConfigEnabledCondition.FORCE_INTEGRATION_MATERIALS), conditions).toArray(ConditionJsonProvider[]::new)));
    }

    /** Adds the compat material conditions */
    public MaterialBuilder compat(ConditionJsonProvider... conditions) {
      return compat(Arrays.stream(conditions));
    }

    /** Adds the compat material conditions */
    public MaterialBuilder compat(String... tagNames) {
      return compat(Arrays.stream(tagNames).map(AbstractMaterialDataProvider::tagExistsCondition));
    }

    /** Adds the compat material condition for a single ingot */
    public MaterialBuilder compatMetal(String ingotName) {
      return compat("ingots/" + ingotName);
    }

    /** Adds the compat material condition for a single ingot name from the material ID */
    public MaterialBuilder compatMetal() {
      return compatMetal(id.getPath());
    }

    /** Adds the compat material condition for the given nested conditions */
    public MaterialBuilder compatAlloy(ConditionJsonProvider... alloyConditions) {
      return condition(DefaultResourceConditions.or(
        // if forced
        ConfigEnabledCondition.FORCE_INTEGRATION_MATERIALS,
        // or we have the matching alloy ingot
        tagExistsCondition("ingots/" + id.getPath()),
        // or we allow ingotless alloys and have all alloy components
        DefaultResourceConditions.and(Util.prepend(alloyConditions, ConfigEnabledCondition.ALLOW_INGOTLESS_ALLOYS))
      ));
    }

    /** Adds the compat material condition for a list of ingot options, alloy is enabled if any of them are enabled */
    public MaterialBuilder compatAlloy(String... ingotNames) {
      ConditionJsonProvider[] conditions = Arrays.stream(ingotNames).map(name -> tagExistsCondition("ingots/" + name)).toArray(ConditionJsonProvider[]::new);
      if (conditions.length == 1) {
        return compatAlloy(conditions[0]);
      }
      return compatAlloy(DefaultResourceConditions.or(conditions));
    }


    /* Redirects */

    /** Checks if this builder is a pure redirect, having no path that will use the rest of the JSON */
    private boolean isPureRedirect() {
      return !redirects.isEmpty() && redirects.get(redirects.size() - 1).getCondition() == null;
    }

    /** Adds a redirect to the builder */
    public MaterialBuilder redirect(JsonRedirect redirect) {
      if (isPureRedirect()) {
        throw new IllegalStateException("Material is already a pure redirect, cannot add another redirect");
      }
      redirects.add(redirect);
      return this;
    }

    /** Adds a redirect to the builder */
    public MaterialBuilder redirect(ResourceLocation id, ConditionJsonProvider... conditions) {
      ConditionJsonProvider combined = null;
      if (conditions.length == 1) {
        combined = conditions[0];
      } else if (conditions.length > 1) {
        combined = DefaultResourceConditions.and(conditions);
      }
      return redirect(new JsonRedirect(id, combined));
    }


    /* Build */

    /** Creates a material JSON */
    private MaterialJson convert() {
      ConditionJsonProvider condition = null;
      int count = conditions.size();
      if (count == 1) {
        condition = conditions.get(0);
      } else if (count > 1) {
        condition = DefaultResourceConditions.and(conditions.toArray(new ConditionJsonProvider[0]));
      }
      JsonRedirect[] redirects = null;
      if (!this.redirects.isEmpty()) {
        redirects = this.redirects.toArray(new JsonRedirect[0]);
      }
      // if the array ends in a null condition, skip serializing all material data
      if (isPureRedirect()) {
        return new MaterialJson(condition, null, null, null, null, null, redirects);
      }
      return new MaterialJson(condition, craftable, tier, sort, rarity, hidden, redirects);
    }
  }
}
