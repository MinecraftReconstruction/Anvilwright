package slimeknights.tconstruct.fluids;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.alchemy.PotionUtils;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.client.model.FluidContainerModel;

public class FluidClientEvents extends ClientEventBase {

  public static void clientSetup() {
    setTranslucent(TinkerFluids.honey);
    // slime
    setTranslucent(TinkerFluids.earthSlime);
    setTranslucent(TinkerFluids.skySlime);
    setTranslucent(TinkerFluids.enderSlime);
    // molten
    setTranslucent(TinkerFluids.moltenDiamond);
    setTranslucent(TinkerFluids.moltenEmerald);
    setTranslucent(TinkerFluids.moltenGlass);
    setTranslucent(TinkerFluids.moltenGlass);
    setTranslucent(TinkerFluids.liquidSoul);
    setTranslucent(TinkerFluids.moltenSoulsteel);
    setTranslucent(TinkerFluids.moltenAmethyst);

    itemColors();
  }

  static void itemColors() {
    ColorProviderRegistry.ITEM.register((stack, index) -> index > 0 ? -1 : PotionUtils.getColor(stack), TinkerFluids.potionBucket.asItem());
  }

  private static void setTranslucent(FluidObject<?> fluid) {
    BlockRenderLayerMap.INSTANCE.putFluid(fluid.get(), RenderType.translucent());
    // flowing fluids render with their own fluid, so they need the layer as well
    if (fluid instanceof FlowingFluidObject<?> flowing) {
      BlockRenderLayerMap.INSTANCE.putFluid(flowing.getFlowing(), RenderType.translucent());
    }
  }
}
