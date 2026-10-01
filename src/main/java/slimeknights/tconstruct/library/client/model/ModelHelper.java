package slimeknights.tconstruct.library.client.model;

import net.fabricmc.fabric.api.renderer.v1.model.WrapperBakedModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;

import java.util.List;

public class ModelHelper {
  /** Number of ints per vertex in a {@link BakedQuad} (see {@code DefaultVertexFormat.BLOCK}) */
  private static final int VERTEX_SIZE = 8;
  /** Index of the color within a baked vertex */
  private static final int COLOR_INDEX = 3;
  /** Index of the packed light within a baked vertex */
  private static final int LIGHT_INDEX = 6;

  /**
   * Fabric replacement for applying a color to already baked quads, matching Forge's {@code IQuadTransformer#processInPlace(List)}
   * with {@code QuadTransformers.applyingColor(int)}. Modifies the quads in place.
   * @param quads  Quads to modify
   * @param color  Color in ARGB format
   */
  public static void applyColor(List<BakedQuad> quads, int color) {
    // baked quads store the color in ABGR, matching internal sprite color, so swap red and blue
    int abgr = (color & 0xFF00FF00) | (color >> 16 & 0xFF) | (color << 16 & 0xFF0000);
    for (BakedQuad quad : quads) {
      int[] data = quad.getVertices();
      for (int i = COLOR_INDEX; i < data.length; i += VERTEX_SIZE) {
        data[i] = abgr;
      }
    }
  }

  /**
   * Fabric replacement for applying emissivity to already baked quads, matching Forge's {@code IQuadTransformer#processInPlace(List)}
   * with {@code QuadTransformers.settingEmissivity(int)}. Modifies the quads in place.
   * @param quads        Quads to modify
   * @param emissivity   Light level, 0-15
   */
  public static void applyEmissivity(List<BakedQuad> quads, int emissivity) {
    int light = LightTexture.pack(emissivity, emissivity);
    for (BakedQuad quad : quads) {
      int[] data = quad.getVertices();
      for (int i = LIGHT_INDEX; i < data.length; i += VERTEX_SIZE) {
        data[i] = light;
      }
    }
  }

  /**
   * Fully unwrap a model, i.e. return the innermost model.
   */
  public static <T extends BakedModel> T unwrap(BakedModel model, Class<T> modelClass) {
    while (model instanceof WrapperBakedModel wrapper) {
      if (modelClass.isAssignableFrom(model.getClass()))
        return (T) model;
      BakedModel wrapped = wrapper.getWrappedModel();

      if (wrapped == null) {
        return (T) model;
      } else if (wrapped == model) {
        throw new IllegalArgumentException("Model " + model + " is wrapping itself!");
      } else {
        model = wrapped;
      }
    }
    if (!modelClass.isAssignableFrom(model.getClass()))
      throw new RuntimeException("Trying to unwrap " + model + " that isn't assignable to " + modelClass);

    return (T) model;
  }
}
