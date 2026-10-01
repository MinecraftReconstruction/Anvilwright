package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.fluid.TextureFluidType;
import slimeknights.tconstruct.common.TinkerTags;

/** Fluid Type that does not affect slimes */
public class SlimeFluidType extends TextureFluidType {
  public SlimeFluidType(Properties properties) {
    super(properties);
  }

  @Override
  public boolean canDrownIn(LivingEntity entity) {
    return !entity.getType().is(TinkerTags.EntityTypes.SLIMES);
  }

  public static class Inverted extends SlimeFluidType {
    public Inverted(Properties properties) {
      super(properties);
    }

    // TODO: PORT - Forge's initializeClient(Consumer<IClientFluidTypeExtensions>) handed out
    //  slimeknights.mantle.fluid.texture.ClientInvertedFluidType to flip the flowing texture. Fabric renders fluids
    //  through FluidRenderHandlerRegistry, and Mantle's ClientInvertedFluidType is not ported (its file is fully
    //  commented out), so inverted slime fluids render with the regular flowing texture.
    //  See docs/BEHAVIOUR-DIFFERENCES.md #22.
  }
}
