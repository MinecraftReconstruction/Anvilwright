package slimeknights.tconstruct.library.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.fabricators_of_create.porting_lib.models.TransformTypeDependentItemBakedModel;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemDisplayContext;
import slimeknights.mantle.client.model.util.SimpleBlockModel;

import java.util.function.Function;

/**
 * Model providing a variant for the GUI
 * <p>
 * Ported from Forge's model API: the baking context is a vanilla {@link BlockModel} and the transform hook is
 * Porting Lib's {@link TransformTypeDependentItemBakedModel} rather than Forge's {@code BakedModelWrapper}.
 * Same shape as this port's {@code TankModel.BakedGuiUniqueModel}, which solved the same problem earlier.
 */
@RequiredArgsConstructor
public class UniqueGuiModel implements IUnbakedGeometry<UniqueGuiModel> {
  /** Shared loader instance */
  public static final IGeometryLoader<UniqueGuiModel> LOADER = UniqueGuiModel::deserialize;

  protected final SimpleBlockModel model;
  protected final SimpleBlockModel gui;

  @Override
  public void resolveParents(Function<ResourceLocation,UnbakedModel> modelGetter, BlockModel owner) {
    model.resolveParents(modelGetter, owner);
    gui.resolveParents(modelGetter, owner);
  }

  @Override
  public BakedModel bake(BlockModel owner, ModelBaker baker, Function<Material,TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides, ResourceLocation location, boolean isGui3d) {
    return new Baked(
      model.bake(owner, baker, spriteGetter, transform, overrides, location, isGui3d),
      gui.bake(owner, baker, spriteGetter, transform, overrides, location, isGui3d)
    );
  }

  /**
   * Wrapper that swaps the model for the GUI
   */
  public static class Baked extends ForwardingBakedModel implements TransformTypeDependentItemBakedModel {
    private final BakedModel gui;

    public Baked(BakedModel base, BakedModel gui) {
      wrapped = base;
      this.gui = gui;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext cameraTransformType, PoseStack mat, boolean leftHanded, DefaultTransform defaultTransform) {
      BakedModel model = cameraTransformType == ItemDisplayContext.GUI ? gui : wrapped;
      if (model instanceof TransformTypeDependentItemBakedModel dependent) {
        return dependent.applyTransform(cameraTransformType, mat, leftHanded, defaultTransform);
      }
      model.getTransforms().getTransform(cameraTransformType).apply(leftHanded, mat);
      return model;
    }
  }

  /** Loader for this model */
  public static UniqueGuiModel deserialize(JsonObject json, JsonDeserializationContext context) {
    return new UniqueGuiModel(
      SimpleBlockModel.deserialize(json, context),
      SimpleBlockModel.deserialize(GsonHelper.getAsJsonObject(json, "gui"), context)
    );
  }
}
