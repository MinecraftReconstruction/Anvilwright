package slimeknights.tconstruct.library.client.modifiers.model;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.client.modifiers.IBakedModifierModel;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.function.Function;

/** Represents a model defined for the given tool */
public interface ModifierModel extends IBakedModifierModel, IHaveLoader {
  ModifierModel EMPTY = SingletonLoader.singleton(loader -> new ModifierModel() {
    @Override
    public Mesh getQuads(IToolStackView tool, ModifierEntry modifier, Function<Material, TextureAtlasSprite> spriteGetter, Transformation transforms, boolean isLarge, int startTintIndex, @Nullable ItemLayerPixels pixels) {
      return EMPTY_MESH;
    }

    @Nullable
    @Override
    public Object getCacheKey(IToolStackView tool, ModifierEntry modifier) {
      return null;
    }

    @Override
    public RecordLoadable<? extends ModifierModel> getLoader() {
      return loader;
    }

    @Override
    public void validate(Function<Material, TextureAtlasSprite> spriteGetter) {}
  });

  /**
   * Loader for registering modifier models.
   * @see ModifierModelLoadable
   */
  GenericLoaderRegistry<ModifierModel> LOADER = new GenericLoaderRegistry<>("Modifier Model", EMPTY, false);
  /** Loadable for reading materials, a common feature of modifier models */
  StringLoadable<Material> MATERIAL_LOADABLE = Loadables.RESOURCE_LOCATION.flatXmap(ModifierModel::blockAtlas, Material::texture);

  @Override
  RecordLoadable<? extends ModifierModel> getLoader();

  /** Validates that all textures in this model exist. */
  void validate(Function<Material, TextureAtlasSprite> spriteGetter);

  static Material blockAtlas(ResourceLocation path) {
    return new Material(InventoryMenu.BLOCK_ATLAS, path);
  }
}
