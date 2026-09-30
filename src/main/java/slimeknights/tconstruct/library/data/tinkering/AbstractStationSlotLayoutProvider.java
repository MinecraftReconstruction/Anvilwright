package slimeknights.tconstruct.library.data.tinkering;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import java.util.Collections;

/** Base data generator to generate station slot layouts */
@SuppressWarnings("deprecation")  // just let me get item keys forge
public abstract class AbstractStationSlotLayoutProvider extends GenericDataProvider {

  // TODO 1.21: rework these to have a bit more space between numbers
  /** Sort index for weapons */
  protected static final int SORT_WEAPON = 3;
  /** Sort index for harvest */
  protected static final int SORT_HARVEST = 6;
  /** Sort index for ammo */
  protected static final int SORT_AMMO = 7;
  /** Sort index for ranged */
  protected static final int SORT_RANGED = 8;
  /** Index for large tools, add to either weapon or harvest */
  protected static final int SORT_LARGE = 6;
  /** Index for armor */
  protected static final int SORT_ARMOR = 15;

  private final Map<ResourceLocation,SerializeLayout> allLayouts = new HashMap<>();

  public AbstractStationSlotLayoutProvider(FabricDataOutput output) {
    super(output, Target.DATA_PACK, StationSlotLayoutLoader.FOLDER, StationSlotLayoutLoader.GSON);
  }

  /**
   * Function to add all relevant layouts
   */
  protected abstract void addLayouts();

  /** Defines the given ID as a general layout */
  protected StationSlotLayout.Builder define(ResourceLocation id) {
    return allLayouts.computeIfAbsent(id, i -> new SerializeLayout()).builder;
  }

  /** Defines the given ID as a general layout with conditions. */
  protected StationSlotLayout.Builder define(ResourceLocation id, ConditionJsonProvider... conditions) {
    SerializeLayout layout = allLayouts.computeIfAbsent(id, i -> new SerializeLayout());
    Collections.addAll(layout.conditions, conditions);
    return layout.builder;
  }

  /** Defines the given ID as a item layout */
  protected StationSlotLayout.Builder define(ItemLike item) {
    return define(BuiltInRegistries.ITEM.getKey(item.asItem()));
  }

  /** Defines the given ID as a tool layout, sets icon and name */
  protected StationSlotLayout.Builder defineModifiable(IModifiableDisplay item) {
    return define(BuiltInRegistries.ITEM.getKey(item.asItem()))
      .translationKey(item.asItem().getDescriptionId())
      .icon(item.getRenderTool());
  }

  /** Defines the given ID as a tool layout, sets icon and name */
  protected StationSlotLayout.Builder defineModifiable(Supplier<? extends IModifiableDisplay> item) {
    return defineModifiable(item.get());
  }

  /** Defines the given ID as a layout using the pattern as both the id and the icon */
  protected StationSlotLayout.Builder definePattern(Pattern id) {
    return define(id).icon(id);
  }

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    addLayouts();
    List<CompletableFuture<?>> futures = new ArrayList<>();
    allLayouts.forEach((id, layout) -> futures.add(saveJson(cache, id, layout.serialize())));
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
  }

  /** Stores the pair of conditions and builder for one layout. */
  private record SerializeLayout(StationSlotLayout.Builder builder, List<ConditionJsonProvider> conditions) {
    public SerializeLayout() {
      this(StationSlotLayout.builder(), new ArrayList<>());
    }

    /** Serializes the given builder to JSON, including any conditions */
    public JsonObject serialize() {
      JsonObject json = StationSlotLayoutLoader.GSON.toJsonTree(builder.build()).getAsJsonObject();
      if (!conditions.isEmpty()) {
        // Forge stored an array of ICondition here; Fabric conditions are written with ConditionJsonProvider
        ConditionJsonProvider.write(json, conditions.toArray(ConditionJsonProvider[]::new));
      }
      return json;
    }
  }
}
