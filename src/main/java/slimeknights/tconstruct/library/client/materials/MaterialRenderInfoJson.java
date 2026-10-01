package slimeknights.tconstruct.library.client.materials;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Gson shape of a material render info file.
 * <p>
 * Upstream migrated this to Mantle loadables (see {@link MaterialRenderInfo#LOADABLE}) and deleted the DTO, but the
 * Fabric fork's {@link MaterialRenderInfoLoader} still reads the files with Gson, so the shape it expects lives here.
 */
@RequiredArgsConstructor
public class MaterialRenderInfoJson {
  @Nullable @Getter
  private final ResourceLocation texture;
  @Nullable @Getter
  private final String[] fallbacks;
  @Nullable @Getter
  private final String color;
  @Nullable
  private final Boolean skipUniqueTexture;
  @Getter
  private final int luminosity;

  /** If true, the unique texture inference is skipped */
  public boolean isSkipUniqueTexture() {
    return skipUniqueTexture == Boolean.TRUE;
  }
}
