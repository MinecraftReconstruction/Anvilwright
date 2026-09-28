package slimeknights.tconstruct.library.client.modifiers;

import com.mojang.math.Transformation;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Model for tank modifiers, also displays the fluid.
 * @deprecated use {@link slimeknights.tconstruct.library.client.modifiers.model.FluidModifierModel}
 */
@SuppressWarnings("removal")
public class FluidModifierModel extends NormalModifierModel {
  /** Location used for baking dynamic models, name does not matter so just using a constant */
  private static final ResourceLocation BAKE_LOCATION = TConstruct.getResource("dynamic_fluid_model");

  /** Constant unbaked model instance, as they are all the same */
  public static final IUnbakedModifierModel UNBAKED_INSTANCE = new Unbaked(ToolTankHelper.TANK_HELPER);

  /** Logic for fetching the fluid */
  protected final ToolTankHelper helper;
  /** Textures to show */
  protected final Material[] fluidTextures;

  protected FluidModifierModel(ToolTankHelper helper, @Nullable Material smallTexture, @Nullable Material largeTexture, Material[] fluidTextures) {
    super(smallTexture, largeTexture);
    this.helper = helper;
    this.fluidTextures = fluidTextures;
  }

  public FluidModifierModel(ToolTankHelper helper, @Nullable Material smallTexture, @Nullable Material largeTexture,
                            @Nullable Material smallFull, @Nullable Material largeFull) {
    this(helper, smallTexture, largeTexture, new Material[] { smallFull, largeFull });
  }

  @Override
  public RecordLoadable<? extends NormalModifierModel> getLoader() {
    throw new UnsupportedOperationException("For modifier model maps, use model.FluidModifierModel");
  }

  @Nullable
  @Override
  public Object getCacheKey(IToolStackView tool, ModifierEntry entry) {
    FluidStack fluid = helper.getFluid(tool);
    if (!fluid.isEmpty()) {
      // cache by modifier and fluid
      return new FluidModifierCacheKey(entry.getModifier(), fluid.getFluid());
    }
    return entry != ModifierEntry.EMPTY ? entry.getId() : null;
  }

  @Nullable
  protected Material getTemplate(IToolStackView tool, ModifierEntry entry, FluidStack fluid, boolean isLarge) {
    return fluidTextures[(isLarge ? 1 : 0)];
  }

  @Override
  public Mesh getQuads(IToolStackView tool, ModifierEntry entry, Function<Material,TextureAtlasSprite> spriteGetter, Transformation transforms, boolean isLarge, int startTintIndex, @Nullable ItemLayerPixels pixels) {
    // first, determine stored fluid
    Mesh quads = super.getQuads(tool, entry, spriteGetter, transforms, isLarge, startTintIndex, pixels);
    // modifier must be tank
    // TODO: is there anything that can be done about the fluid? to prevent weird offsets?
    if (entry.getModifier() instanceof TankModifier tank) {
      FluidStack fluid = tank.getFluid(tool);
      // must have fluid
      if (!fluid.isEmpty()) {
        // must have texture for the proper state
        Material template = getTemplate(tank, tool, fluid, isLarge);
        if (template != null) {
          // finally, build (mostly based on bucket model)
//          ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
//          builder.addAll(quads);
          TextureAtlasSprite fluidSprite = FluidVariantRendering.getSprite(fluid.getType());
          int color = FluidVariantRendering.getColor(fluid.getType());
          int luminosity = FluidVariantAttributes.getLuminance(fluid.getType());
          TextureAtlasSprite templateSprite = spriteGetter.apply(template);
//          builder.addAll(ItemTextureQuadConverter.convertTexture(transforms, templateSprite, fluidSprite, 7.498f / 16f, Direction.NORTH, color, -1, luminosity)); TODO: PORT
//          builder.addAll(ItemTextureQuadConverter.convertTexture(transforms, templateSprite, fluidSprite, 8.502f / 16f, Direction.SOUTH, color, -1, luminosity));
//          quads = builder.build();
        }
      }
    }
    // add tank outline quads
    super.addQuads(tool, entry, spriteGetter, transforms, isLarge, startTintIndex, quadConsumer, pixels);
  }

  /** Cache key for the model */
  private record FluidModifierCacheKey(Modifier modifier, Fluid fluid) {}

  public record Unbaked(ToolTankHelper helper) implements IUnbakedModifierModel {
    @Nullable
    @Override
    public IBakedModifierModel forTool(Function<String,Material> smallGetter, Function<String,Material> largeGetter) {
      Material smallTexture = smallGetter.apply("");
      Material largeTexture = largeGetter.apply("");
      Material smallFluid = smallGetter.apply("_full");
      Material largeFluid = largeGetter.apply("_full");
      if (smallTexture != null || largeTexture != null || smallFluid != null || largeFluid != null) {
        return new FluidModifierModel(helper, smallTexture, largeTexture, smallFluid, largeFluid);
      }
      return null;
    }
  }
}
