package slimeknights.tconstruct.library.client.modifiers.model;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import io.github.fabricators_of_create.porting_lib.models.geometry.SimpleModelState;
import io.github.fabricators_of_create.porting_lib.models.UnbakedGeometryHelper;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import org.joml.Vector3f;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.model.FluidContainerModel;
import slimeknights.tconstruct.library.client.model.ModelHelper;
import slimeknights.tconstruct.library.client.model.tools.ToolModel;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/** Model for a fluid in a tool. */
public record FluidModifierModel(@Nullable Material small, @Nullable Material large, ToolTankHelper tankHelper) implements SimpleModifierModel {
  public static final RecordLoadable<FluidModifierModel> LOADER = RecordLoadable.create(
    ModifierModel.MATERIAL_LOADABLE.nullableField("mask", FluidModifierModel::small),
    ModifierModel.MATERIAL_LOADABLE.nullableField("mask_large", FluidModifierModel::large),
    ToolTankHelper.LOADABLE.defaultField("tank_helper", ToolTankHelper.TANK_HELPER, false, FluidModifierModel::tankHelper),
    FluidModifierModel::new);

  /** Location used for baking dynamic models, name does not matter so just using a constant */
  private static final ResourceLocation BAKE_LOCATION = TConstruct.getResource("dynamic_fluid_model");
  /**
   * The vanilla model bakery uses an orgin of 0.5,0.5,0.5, and forges dynamic fluid code uses the vanilla model bakery. (see{@link net.minecraft.client.renderer.block.model.FaceBakery} {@code #rotateVertexBy()} for vanilla bakery)
   * However, item layer wants an origin of 0,0,0, which is what we expect in our tool models. So cancel out the origin.
   */
  private static final Vector3f ORIGIN = new Vector3f(-0.5f, -0.5f, -0.5f);

  /** Instance with default tank helper */
  public FluidModifierModel(@Nullable Material small, @Nullable Material large) {
    this(small, large, ToolTankHelper.TANK_HELPER);
  }

  /** Cache key for {@link #getCacheKey(IToolStackView, ModifierEntry)} */
  private record CacheKey(Fluid fluid, @Nullable CompoundTag tag) {}

  @Nullable
  @Override
  public Object getCacheKey(IToolStackView tool, ModifierEntry modifier) {
    FluidStack fluid = tankHelper().getFluid(tool);
    if (!fluid.isEmpty()) {
      return new CacheKey(fluid.getFluid(), fluid.getTag());
    }
    return null;
  }

  @Override
  public RecordLoadable<FluidModifierModel> getLoader() {
    return LOADER;
  }

  @Override
  public Mesh getQuads(IToolStackView tool, ModifierEntry modifier, Function<Material, TextureAtlasSprite> spriteGetter, Transformation transforms, boolean isLarge, int startTintIndex, @Nullable ItemLayerPixels pixels) {
    // ensure template exists
    Material template = isLarge ? large() : small();
    if (template != null) {
      // ensure we have fluid
      FluidStack fluid = tankHelper().getFluid(tool);
      if (!fluid.isEmpty()) {
        return addQuads(fluid, template, spriteGetter, transforms);
      }
    }
    return EMPTY_MESH;
  }

  /** Adds quads for the given fluid */
  public static Mesh addQuads(FluidStack fluid, Material template, Function<Material,TextureAtlasSprite> spriteGetter, Transformation transforms) {
    // must have texture for the proper state
    // fluid properties
    // Forge asked the fluid type's client extensions for the texture; Fabric renders fluids through the variant
    TextureAtlasSprite fluidSprite = FluidVariantRendering.getSprite(fluid.getType());

    // build fluid like the forge dynamic container model
    List<BlockElement> unbaked = UnbakedGeometryHelper.createUnbakedItemMaskElements(-1, spriteGetter.apply(template).contents()); // Use template as mask
    // TODO: is there anything that can be done about the fluid? to prevent weird offsets?
    List<BakedQuad> fluidQuads = UnbakedGeometryHelper.bakeElements(unbaked, mat -> fluidSprite, new SimpleModelState(transforms.applyOrigin(ORIGIN).compose(FluidContainerModel.FLUID_TRANSFORM), false), BAKE_LOCATION); // Bake with fluid texture

    // apply brightness and color
    int luminosity = FluidVariantAttributes.getLuminance(fluid.getType());
    if (luminosity > 0) {
      ModelHelper.applyEmissivity(fluidQuads, luminosity);
    }
    int color = FluidVariantRendering.getColor(fluid.getType());
    if (color != -1) {
      ModelHelper.applyColor(fluidQuads, color);
    }
    return ToolModel.ofQuads(fluidQuads);
  }
}
