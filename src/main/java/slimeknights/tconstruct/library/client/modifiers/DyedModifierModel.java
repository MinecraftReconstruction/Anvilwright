package slimeknights.tconstruct.library.client.modifiers;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.client.modifiers.model.SimpleModifierModel;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Modifier model that copies dye from a key.
 * TODO 1.21: move to {@link slimeknights.tconstruct.library.modifiers.modules}
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class DyedModifierModel implements SimpleModifierModel {
  public static final RecordLoadable<DyedModifierModel> LOADER = SimpleModifierModel.loader(DyedModifierModel::new);
  /** @deprecated legacy system, use {@link #LOADER} */
  @Deprecated
  public static final IUnbakedModifierModel UNBAKED_INSTANCE = (smallGetter, largeGetter) -> {
    Material smallTexture = smallGetter.apply("");
    Material largeTexture = largeGetter.apply("");
    if (smallTexture != null || largeTexture != null) {
      return new DyedModifierModel(smallTexture, largeTexture);
    }
    return null;
  };

  /** Textures to show */
  @Nullable
  private final Material small;
  @Nullable
  private final Material large;

  @Override
  public RecordLoadable<? extends DyedModifierModel> getLoader() {
    return LOADER;
  }

  @Nullable
  @Override
  public Object getCacheKey(IToolStackView tool, ModifierEntry entry) {
    ModifierId modifier = entry.getId();
    IModDataView data = tool.getPersistentData();
    int color = -1;
    if (data.contains(modifier, Tag.TAG_INT)) {
      color = data.getInt(modifier);
    }
    return new CacheKey(modifier, color);
  }

  @Override
  public Mesh getQuads(IToolStackView tool, ModifierEntry modifier, Function<Material,TextureAtlasSprite> spriteGetter, Transformation transforms, boolean isLarge, int startTintIndex, @Nullable ItemLayerPixels pixels) {
    Material texture = textures[(isLarge ? 2 : 0) | (tool.isBroken() ? 1 : 0)];
    if (texture != null) {
      IModDataView data = tool.getPersistentData();
      ResourceLocation key = modifier.getId();
      if (data.contains(key, Tag.TAG_INT)) {
        quadConsumer.accept(MantleItemLayerModel.getQuadsForSprite(0xFF000000 | data.getInt(key), -1, spriteGetter.apply(texture), transforms, 0, pixels));
      }
    }
    return EMPTY_MESH;
  }

  /** Data class to cache a colored texture */
  private record CacheKey(ModifierId modifier, int color) {}
}
