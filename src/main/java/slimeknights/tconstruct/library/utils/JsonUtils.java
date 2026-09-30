package slimeknights.tconstruct.library.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;
import java.util.List;
import javax.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

/** Helpers for a few JSON related tasks */
public class JsonUtils {
  private JsonUtils() {}

  /**
   * Reads an integer with a minimum value
   * @param json  Json
   * @param key   Key to read
   * @param min   Minimum and default value
   * @return  Read int
   * @throws JsonSyntaxException if the key is not an int or below the min
   */
  public static int getIntMin(JsonObject json, String key, int min) {
    int value = GsonHelper.getAsInt(json, key, min);
    if (value < min) {
      throw new JsonSyntaxException(key + " must be at least " + min);
    }
    return value;
  }

  /**
   * Reads an integer with a minimum value
   * @param json  Json element to parse as an integer
   * @param key   Key to read
   * @param min   Minimum
   * @return  Read int
   * @throws JsonSyntaxException if the key is not an int or below the min
   */
  public static int getIntMin(JsonElement json, String key, int min) {
    int value = GsonHelper.convertToInt(json, key);
    if (value < min) {
      throw new JsonSyntaxException(key + " must be at least " + min);
    }
    return value;
  }

  /** @deprecated use {@link JsonHelper#convertToEntry(Registry, JsonElement, String)} */
  @Deprecated
  public static <T> T convertToEntry(Registry<T> registry, JsonElement element, String key) {
    return JsonHelper.convertToEntry(registry, element, key);
  }

  /** @deprecated use {@link JsonHelper#getAsEntry(Registry, JsonObject, String)} */
  @Deprecated
  public static <T> T getAsEntry(Registry<T> registry, JsonObject parent, String key) {
    return JsonHelper.getAsEntry(registry, parent, key);
  }

  /** @deprecated use {@link JsonHelper#getJson(Resource, ResourceLocation)} */
  @Deprecated
  @Nullable
  public static JsonObject getJson(Resource resource) {
    return JsonHelper.getJson(resource, new ResourceLocation(resource.sourcePackId()));
  }

  /** @deprecated use {@link JsonHelper#convertToEnum(JsonElement, String, Class)} */
  @Deprecated
  public static <T extends Enum<T>> T convertToEnum(JsonElement element, String key, Class<T> enumClass) {
    return JsonHelper.convertToEnum(element, key, enumClass);
  }

  /** @deprecated use {@link JsonHelper#getAsEnum(JsonObject, String, Class)} */
  @Deprecated
  public static <T extends Enum<T>> T getAsEnum(JsonObject json, String key, Class<T> enumClass) {
    return JsonHelper.getAsEnum(json, key, enumClass);
  }

  /** @deprecated use {@link JsonHelper#getFileInAllDomainsAndPacks(ResourceManager, String, String)} */
  @Deprecated
  public static List<JsonObject> getFileInAllDomainsAndPacks(ResourceManager manager, String path) {
    return JsonHelper.getFileInAllDomainsAndPacks(manager, path, null);
  }

  /** Called when the player logs in to send packets */
  public static void syncPackets(ServerPlayer targetedPlayer, boolean joined, ISimplePacket... packets) {
    JsonHelper.syncPackets(targetedPlayer, joined, TinkerNetwork.getInstance(), packets);
  }

  /** Creates a JSON object with the given key set to a resource location */
  public static JsonObject withLocation(String key, ResourceLocation value) {
    JsonObject json = new JsonObject();
    json.addProperty(key, value.toString());
    return json;
  }

  /** Creates a JSON object with the given type set, makes using {@link slimeknights.mantle.data.gson.GenericRegisteredSerializer} easier */
  public static JsonObject withType(ResourceLocation type) {
    return withLocation("type", type);
  }


  private static boolean allowDebugLogging;

  /** Forces the debug logging to be disabled. Used during testing since config is not loaded */
  @Internal
  @VisibleForTesting
  public static void forceDisableDebugLog() {
    allowDebugLogging = false;
  }

  /** If true, log the values of all resources loaded from data packs, for debugging pack loading */
  public static boolean debugLogResourceValues() {
    return allowDebugLogging && Config.COMMON.debugLogResourceValues.get();
  }

  /**
   * Reads the result from the given JSON
   * @param parent  Parent JSON
   * @param name    Tag name
   * @return  Item stack result
   * @throws com.google.gson.JsonSyntaxException If the syntax is invalid
   */
  public static ItemStack getAsItemStack(JsonObject parent, String name) {
    return ItemStackLoadable.OPTIONAL_STACK_NBT.getIfPresent(parent, name);
  }

  /**
   * Serializes the given result to JSON
   * @param result  Result
   * @return  JSON element
   */
  public static JsonElement serializeItemStack(ItemStack result) {
    // if the item has NBT, write both, else write just the name
    String itemName = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
    if (result.hasTag()) {
      JsonObject jsonResult = new JsonObject();
      jsonResult.addProperty("item", itemName);
      int count = result.getCount();
      if (count > 1) {
        jsonResult.addProperty("count", count);
      }
      jsonResult.addProperty("nbt", Objects.requireNonNull(result.getTag()).toString());
      return jsonResult;
    } else {
      return new JsonPrimitive(itemName);
    }
  }
}
