/*
 * Minecraft Forge
 * Copyright (c) 2016-2021.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package slimeknights.tconstruct.library.client.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import io.github.fabricators_of_create.porting_lib.models.CompositeModel;
import io.github.fabricators_of_create.porting_lib.models.DynamicFluidContainerModel;
import io.github.fabricators_of_create.porting_lib.models.QuadTransformers;
import io.github.fabricators_of_create.porting_lib.models.geometry.SimpleModelState;
import net.minecraft.client.renderer.block.model.BlockModel;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.models.UnbakedGeometryHelper;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.fluid.texture.ClientFluidTextureRegistry;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.nbt.TagParser;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;

/**
 * Extension of {@link net.minecraftforge.client.model.DynamicFluidContainerModel} with two additional features: baked tints and fluid stack sensitive models.
 * Does not handle covers as I have never seen a need for them, and it means less code duplication (plus the forge model does the whole cover is mask thing wrong compared to 1.18).
 */
public record FluidContainerModel(FluidStack fluid, boolean flipGas) implements IUnbakedGeometry<FluidContainerModel> {
  public static final IGeometryLoader<FluidContainerModel> LOADER = FluidContainerModel::deserialize;

  /** Clone of same named field from {@link net.minecraftforge.client.model.DynamicFluidContainerModel} */
  public static final Transformation FLUID_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());

  /** Deserializes this model from JSON */
  public static FluidContainerModel deserialize(JsonObject json, JsonDeserializationContext context) {
    FluidStack fluidStack = FluidStack.EMPTY;
    // parse the fluid with an optional tag
    if (json.has("fluid")) {
      JsonElement fluidElement = json.get("fluid");
      Fluid fluid;
      CompoundTag tag = null;
      if (fluidElement.isJsonObject()) {
        JsonObject fluidObject = fluidElement.getAsJsonObject();
        fluid = Loadables.FLUID.getIfPresent(fluidObject, "name");
        if (fluidObject.has("nbt")) {
          try {
            tag = TagParser.parseTag(JsonHelper.DEFAULT_GSON.toJson(fluidObject.get("nbt")));
          } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new com.google.gson.JsonSyntaxException("Invalid NBT in fluid model", e);
          }
        }
      } else {
        fluid = Loadables.FLUID.convert(fluidElement, "fluid");
      }
      fluidStack = new FluidStack(fluid, FluidConstants.BUCKET, tag);
    }
    boolean flipGas = GsonHelper.getAsBoolean(json, "flip_gas", true);
    return new FluidContainerModel(fluidStack, flipGas);
  }

  /** Gets the given sprite, or null if the texture is not present in the model */
  @Nullable
  private static TextureAtlasSprite getSprite(BlockModel context, Function<Material,TextureAtlasSprite> spriteGetter, String key) {
    if (context.hasTexture(key)) {
      return spriteGetter.apply(context.getMaterial(key));
    }
    return null;
  }

  /**
   * Gets the sprite for the contained fluid using the sprite getter of the given bake.
   * <p>
   * NOTE(porting): {@link FluidVariantRendering} reads the global texture atlas, which during a model bake still
   * belongs to the previous resource reload (empty on the first one), so a bucket model baked during that pass got a
   * null fluid sprite and failed to bake entirely. Ask this bake's sprite getter instead; at render time the global
   * lookup works and is preferred, since only it can see stack sensitive textures such as potions.
   */
  private static TextureAtlasSprite getFluidSprite(Function<Material,TextureAtlasSprite> spriteGetter, FluidStack fluid) {
    ResourceLocation still = ClientFluidTextureRegistry.getStillTexture(fluid.getFluid());
    if (still != null) {
      TextureAtlasSprite sprite = spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, still));
      if (sprite != null) {
        return sprite;
      }
    }
    // fluids from other mods have no handler in our registry, and outside of a bake the global lookup is correct
    // (FluidVariantRendering#getSprite throws instead of returning null, so read the array and check it ourselves)
    TextureAtlasSprite[] sprites = FluidVariantRendering.getSprites(fluid.getType());
    TextureAtlasSprite sprite = sprites == null ? null : sprites[0];
    return sprite != null ? sprite : spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation()));
  }

  private static BakedModel bakeInternal(BlockModel context, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation, FluidStack fluid, boolean flipGas) {
    // get basic sprites
    TextureAtlasSprite baseSprite = getSprite(context, spriteGetter, "base");
    TextureAtlasSprite fluidSprite = fluid.isEmpty() ? null : getFluidSprite(spriteGetter, fluid);

    // determine particle
    TextureAtlasSprite particleSprite = getSprite(context, spriteGetter, "particle");
    if (particleSprite == null) particleSprite = fluidSprite;
    if (particleSprite == null) particleSprite = baseSprite;
    if (particleSprite == null) {
      TConstruct.LOG.error("No valid particle sprite for fluid container model, you should supply either 'base' or 'particle'");
      particleSprite = spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation()));
    }

    // if its a gas and we flipping, flip it
    // NOTE(porting): Forge guarantees every fluid has a FluidType, Fabric does not: Milk Lib's milk fluid (which
    //  Tinkers enables, and which the filled copper can variants iterate over) is a plain Fluid with no FluidType.
    //  Ask the Fabric attribute API instead, which is what every other port call site does.
    if (flipGas && !fluid.isEmpty() && FluidVariantAttributes.isLighterThanAir(fluid.getType())) {
      modelState = new SimpleModelState(modelState.getRotation().compose(new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null)));
    }

    // start building the mode
    CompositeModel.Baked.Builder modelBuilder = CompositeModel.Baked.builder(context.hasAmbientOcclusion(), false, false, particleSprite, overrides, context.getTransforms());

    // add in the base
    if (baseSprite != null) {
      modelBuilder.addQuads(UnbakedGeometryHelper.bakeElements(
        UnbakedGeometryHelper.createUnbakedItemElements(0, baseSprite.contents()),
        $ -> baseSprite, modelState, modelLocation
      ));
    }

    // add in fluid
    if (fluidSprite != null) {
      List<BakedQuad> quads = UnbakedGeometryHelper.bakeElements(
        UnbakedGeometryHelper.createUnbakedItemMaskElements(1, spriteGetter.apply(context.getMaterial("fluid")).contents()),
        $ -> fluidSprite,
        new SimpleModelState(modelState.getRotation().compose(FLUID_TRANSFORM), modelState.isUvLocked()),
        modelLocation
      );

      // apply light
      int light = FluidVariantAttributes.getLuminance(fluid.getType());
      if (light > 0) {
        ModelHelper.applyEmissivity(quads, light);
      }
      // apply color
      int color = FluidVariantRendering.getColor(fluid.getType());
      if (color != -1) {
        ModelHelper.applyColor(quads, color);
      }
      modelBuilder.addQuads(quads);
    }
    return modelBuilder.build();
  }

  @Override
  public BakedModel bake(BlockModel context, ModelBaker bakery, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation, boolean isGui3d) {
    // We need to disable GUI 3D and block lighting for this to render properly
    // only do contained fluid if we did not set the fluid in the model properties
    if (fluid.isEmpty()) {
      overrides = new ContainedFluidOverrideHandler(context, overrides, modelState, flipGas);
    }
    try {
      return bakeInternal(context, spriteGetter, modelState, overrides, modelLocation, fluid, flipGas);
    } catch (RuntimeException e) {
      // vanilla only logs the exception message when a model fails to bake, which loses the stack of our own code
      if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) {
        TConstruct.LOG.error("Failed to bake fluid container model {}", modelLocation, e);
      }
      throw e;
    }
  }

  /** Handles swapping the model based on the contained fluid */
  @RequiredArgsConstructor
  private static final class ContainedFluidOverrideHandler extends ItemOverrides {
    private static final ResourceLocation BAKE_LOCATION = TConstruct.getResource("copper_can_dynamic");

    private final Map<FluidStack,BakedModel> cache = Maps.newHashMap(); // contains all the baked models since they'll never change

    private final BlockModel context;
    private final ItemOverrides nested;
    private final ModelState modelState;
    private final boolean flipGas;


    /** Gets the model directly, for creating the cached models */
    private BakedModel getUncahcedModel(FluidStack fluid) {
      return bakeInternal(context, Material::sprite, modelState, ItemOverrides.EMPTY, BAKE_LOCATION, fluid, flipGas);
    }

    @Override
    public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
      BakedModel overriden = nested.resolve(originalModel, stack, world, entity, seed);
      if (overriden != originalModel) return overriden;
      Storage<FluidVariant> handler = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
      Optional<FluidStack> optional = handler == null ? Optional.empty() : Optional.of(TransferUtil.firstCopyOrEmpty(handler));
      if (optional.isPresent()) {
        FluidStack fluid = optional.get();
        fluid.setAmount(FluidConstants.BUCKET); // cache considers amount, so ensure its consistent
        return cache.computeIfAbsent(fluid, this::getUncahcedModel);
      }
      return originalModel;
    }
  }
}
