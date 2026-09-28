package slimeknights.tconstruct.library.client.model;

import io.github.fabricators_of_create.porting_lib.event.client.TextureStitchCallback;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Logic to handle dynamic texture scans. Really just logging missing textures at this point.
 */
@Log4j2
public class DynamicTextureLoader extends ResourceValidator {
  /** Instance to register with the loader */
  private static final DynamicTextureLoader INSTANCE = new DynamicTextureLoader();

  private DynamicTextureLoader() {
    super("textures/item", "textures", ".png");
  }

  @Override
  public void onReloadSafe(ResourceManager manager) {
    // if we are logging missing textures we can use the vanilla validator instead of needing our own
    if (!Config.CLIENT.logMissingModifierTextures.get()) {
      super.onReloadSafe(manager);
    }
  }

  @Override
  public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
    return super.reload(stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor).thenRunAsync(this::clear);
  }

  /** Registers this manager */
  public static void init() {
    // clear cache on texture stitch, no longer need it then as its too late to lookup textures
    TextureStitchCallback.POST.register(e -> clearCache());
  }

  /** Checks if a texture exists */
  public static boolean textureExists(ResourceManager manager, ResourceLocation location) {
    Boolean found = EXISTING_TEXTURES.get(location);
    if (found == null) {
      found = manager.getResource(new ResourceLocation(location.getNamespace(), "textures/" + location.getPath() + ".png")).isPresent();
      EXISTING_TEXTURES.put(location, found);
    }
    return found;
  }

  /** Logs that a dynamic texture is missing, config option to disable */
  public static void logMissingTexture(ResourceLocation location) {
    if (!SKIPPED_TEXTURES.contains(location)) {
      SKIPPED_TEXTURES.add(location);
      log.debug("Skipping loading texture '{}' as it does not exist in the resource pack", location);
    }
  }

  /**
   * Gets a consumer to add textures to the given collection
   *
   * @param spriteGetter        Function mapping material names to sprites
   * @param logMissingTextures  If true, log textures that were not found
   * @return  Texture consumer
   */
  public static Predicate<Material> getTextureValidator(Function<Material,TextureAtlasSprite> spriteGetter, boolean logMissingTextures) {
    if (logMissingTextures || INSTANCE.resources.isEmpty()) {
      // this logs due to the vanilla sprite getter logging
      return mat -> !MissingTextureAtlasSprite.getLocation().equals(spriteGetter.apply(mat).contents().name());
    } else {
      return mat -> {
        // to suppress logging, need to load from our own list. We just load it for `textures/item` on the block atlas
        if (InventoryMenu.BLOCK_ATLAS.equals(mat.atlasLocation())) {
          ResourceLocation texture = mat.texture();
          if (texture.getPath().startsWith("item/")) {
            return INSTANCE.test(mat.texture());
          }
        }
        // failed preconditions? can't stop logging even if the boolean says to
        return !MissingTextureAtlasSprite.getLocation().equals(spriteGetter.apply(mat).contents().name());
      };
    }
  }
}
