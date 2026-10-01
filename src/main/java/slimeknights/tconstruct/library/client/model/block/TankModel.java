package slimeknights.tconstruct.library.client.model.block;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.fabricators_of_create.porting_lib.models.TransformTypeDependentItemBakedModel;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.WrapperBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.client.model.ModelData;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.ExtraTextureContext;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.model.ModelHelper;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;
import slimeknights.tconstruct.smeltery.item.TankItem;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This model contains a single scalable fluid that can either be statically rendered or rendered in the TESR. It also supports rendering fluids in the item model
 */
@AllArgsConstructor
public class TankModel implements IUnbakedGeometry<TankModel> {
  protected static final ResourceLocation BAKE_LOCATION = TConstruct.getResource("dynamic_model_baking");

  /** Shared loader instance */
  public static final IGeometryLoader<TankModel> LOADER = TankModel::deserialize;

  protected final SimpleBlockModel model;
  @Nullable
  protected final SimpleBlockModel gui;
  protected final IncrementalFluidCuboid fluid;
  protected final boolean forceModelFluid;

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, BlockModel owner) {
    model.resolveParents(modelGetter, owner);
    if (gui != null) {
      gui.resolveParents(modelGetter, owner);
    }
  }

  @Override
  public BakedModel bake(BlockModel owner, ModelBaker baker, Function<Material,TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides, ResourceLocation location, boolean isGui3d) {
    BakedModel baked = model.bakeModel(owner, transform, overrides, spriteGetter, location);
    // bake the GUI model if present
    BakedModel bakedGui = baked;
    if (gui != null) {
      bakedGui = gui.bakeModel(owner, transform, overrides, spriteGetter, location);
    }
    return new Baked<>(owner, transform, baked, bakedGui, this);
  }

  /** Override to add the fluid part to the item model */
  private static class FluidPartOverride extends ItemOverrides {
    /** Shared override instance, since the logic is not model dependent */
    public static final FluidPartOverride INSTANCE = new FluidPartOverride();

    @Override
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
      // ensure we have a fluid
      if (stack.isEmpty() || !stack.hasTag()) {
        return model;
      }
      // determine fluid
      FluidTank tank = TankItem.getFluidTank(stack);
      if (tank.isEmpty()) {
        return model;
      }
      // always baked model as this override is only used in our model
      if (model instanceof Baked<?> baked)
        return baked.getCachedModel(tank.getFluid(), tank.getCapacity());
      return ((Baked<?>) ModelHelper.unwrap(model, Baked.class)).getCachedModel(tank.getFluid(), tank.getCapacity());
    }
  }

  /**
   * Wrapper that swaps the model for the GUI
   */
  private static class BakedGuiUniqueModel extends ForwardingBakedModel implements TransformTypeDependentItemBakedModel {
    private final BakedModel gui;
    public BakedGuiUniqueModel(BakedModel base, BakedModel gui) {
      wrapped = base;
      this.gui = gui;
    }

    /* Swap out GUI model if needed */

    @Override
    public BakedModel applyTransform(ItemDisplayContext cameraTransformType, PoseStack mat, boolean leftHanded, DefaultTransform defaultTransform) {
      if (cameraTransformType == ItemDisplayContext.GUI) {
        if(gui instanceof TransformTypeDependentItemBakedModel)
          return ((TransformTypeDependentItemBakedModel)gui).applyTransform(cameraTransformType, mat, leftHanded, defaultTransform);
        gui.getTransforms().getTransform(cameraTransformType).apply(leftHanded, mat);
        return gui;
      }
      if(wrapped instanceof TransformTypeDependentItemBakedModel)
        return ((TransformTypeDependentItemBakedModel)wrapped).applyTransform(cameraTransformType, mat, leftHanded, defaultTransform);
      wrapped.getTransforms().getTransform(cameraTransformType).apply(leftHanded, mat);
      return wrapped;
    }
  }

  /**
   * Baked variant to load in the custom overrides
   */
  @SuppressWarnings("removal")
  public static class Baked<T extends TankModel> extends BakedGuiUniqueModel {
    private final BlockModel owner;
    private final ModelState originalTransforms;
    @SuppressWarnings("WeakerAccess")
    protected final TankModel original;
    private final FluidPartOverride overrides = new FluidPartOverride();
    private final Cache<CacheKey,BakedModel> cache = CacheBuilder
      .newBuilder()
      .maximumSize(64)
      .build();

    /** Cache key since fluids don't do equality over amount */
    private record CacheKey(FluidStack fluid, int increments) {}

    @SuppressWarnings("WeakerAccess")
    protected Baked(BlockModel owner, ModelState transforms, BakedModel baked, BakedModel gui, T original) {
      super(baked, gui);
      this.owner = owner;
      this.originalTransforms = transforms;
      this.original = original;
    }

    @Override
    public ItemOverrides getOverrides() {
      return overrides;
    }

    /**
     * Bakes the model with the given fluid element
     * @param owner        Owner for baking, should include the fluid texture
     * @param baseModel    Base model for original elements
     * @param fluid        Fluid element for baking
     * @param color        Color for the fluid part
     * @param luminosity   Luminosity for the fluid part
     * @return  Baked model
     */
    private BakedModel bakeWithFluid(BlockModel owner, SimpleBlockModel baseModel, BlockElement fluid, int color, int luminosity) {
      // setup for baking, using dynamic location and sprite getter
      Function<Material,TextureAtlasSprite> spriteGetter = Material::sprite;
      TextureAtlasSprite particle = spriteGetter.apply(owner.getMaterial("particle"));
      SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(owner.hasAmbientOcclusion(), owner.getGuiLight().lightLikeBlock(), true, owner.getTransforms(), ItemOverrides.EMPTY).particle(particle);
      // first, add all regular elements
      for (BlockElement element : baseModel.getElements()) {
        SimpleBlockModel.bakePart(builder, owner, element, spriteGetter, originalTransforms, quadTransformer, BAKE_LOCATION);
      }
      // next, add in the fluid
      IQuadTransformer fluidTransformer = color == -1 ? quadTransformer : quadTransformer.andThen(ColoredBlockModel.applyColorQuadTransformer(color));
      ColoredBlockModel.bakePart(builder, owner, fluid, luminosity, spriteGetter, originalTransforms.getRotation(), fluidTransformer, originalTransforms.isUvLocked(), BAKE_LOCATION);
      return builder.build(SimpleBlockModel.getRenderTypeGroup(owner));
    }

    /**
     * Gets the model with the fluid part added
     * @param key  Cache key containing fluid and increments
     * @return  Model with the fluid part
     */
    private BakedModel getModel(CacheKey key) {
      // fetch fluid data
      var sprites = FluidVariantRendering.getSprites(stack.getType());
      int color = FluidVariantRendering.getColor(stack.getType());
      int luminosity = FluidVariantAttributes.getLuminance(stack.getType());
      Map<String,Material> textures = ImmutableMap.of(
        "fluid", new Material(sprites[0].atlasLocation(), sprites[0].contents().name()),
        "flowing_fluid", new Material(sprites[1].atlasLocation(), sprites[1].contents().name()));
      BlockModel textured = new ExtraTextureConfiguration(owner, textures);

      // add fluid part
      BlockElement fluid = original.fluid.getPart(stack.getAmount(), FluidVariantAttributes.isLighterThanAir(stack.getType()));
      // bake the model
      BakedModel baked = bakeWithFluid(textured, original.model, fluid, color, luminosity);

      // if we have GUI, bake a GUI variant
      if (original.gui != null) {
        baked = new UniqueGuiModel.Baked(baked, bakeWithFluid(textured, original.gui, fluid, color, 0));
      }

      // return what we ended up with
      return baked;
    }

    /**
     * Gets a cached model with the fluid part added
     * @param fluid  Scaled contained fluid
     * @return  Cached model
     */
    private BakedModel getCachedModel(CacheKey fluid) {
      try {
        return cache.get(fluid, () -> getModel(fluid));
      }
      catch(ExecutionException e) {
        TConstruct.LOG.error(e);
        return this;
      }
    }

    /**
     * Gets a cached model with the fluid part added
     * @param fluid     Fluid contained
     * @param capacity  Tank capacity
     * @return  Cached model
     */
    private BakedModel getCachedModel(FluidStack fluid, long capacity) {
      int increments = original.fluid.getIncrements();
      return getCachedModel(new FluidStack(fluid, ChannelBlockEntity.clampL(fluid.getAmount() * increments / capacity, 1, increments)));
    }



    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
      if(blockView instanceof RenderAttachedBlockView renderAttachedBlockView && renderAttachedBlockView.getBlockEntityRenderAttachment(pos) instanceof ModelData data) {
        if ((original.forceModelFluid || Config.CLIENT.tankFluidModel.get()) && data.has(ModelProperties.FLUID_TANK)) {
          FluidTank tank = data.get(ModelProperties.FLUID_TANK);
          if (tank != null && !tank.getFluid().isEmpty()) {
            ((FabricBakedModel)getCachedModel(tank.getFluid(), tank.getCapacity())).emitBlockQuads(blockView, state, pos, randomSupplier, context);
            return;
          }
        }
      }
      ((FabricBakedModel)wrapped).emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public boolean isVanillaAdapter() {
      return false;
    }

    /** Override to add the fluid part to the item model */
    private class FluidPartOverride extends ItemOverrides {
      @Override
      public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
        // ensure we have a fluid
        if (stack.isEmpty() || !stack.hasTag()) {
          return model;
        }
        // determine fluid
        FluidTank tank = TankItem.getTank(stack, 1);
        if (tank.isEmpty()) {
          return model;
        }
        // always baked model as this override is only used in our model
        return getCachedModel(tank.getFluid(), tank.getCapacity());
      }
    }
  }


  /** Loader for this model */
  public static class Loader implements IGeometryLoader<TankModel> {

    @Override
    public TankModel read(JsonObject modelContents, JsonDeserializationContext deserializationContext) {
      SimpleBlockModel model = SimpleBlockModel.deserialize(deserializationContext, modelContents);
      SimpleBlockModel gui = null;
      if (modelContents.has("gui")) {
        gui = SimpleBlockModel.deserialize(deserializationContext, GsonHelper.getAsJsonObject(modelContents, "gui"));
      }
      IncrementalFluidCuboid fluid = IncrementalFluidCuboid.fromJson(GsonHelper.getAsJsonObject(modelContents, "fluid"));
      boolean forceModelFluid = GsonHelper.getAsBoolean(modelContents, "render_fluid_in_model", false);
      return new TankModel(model, gui, fluid, forceModelFluid);
    }
  }
}
