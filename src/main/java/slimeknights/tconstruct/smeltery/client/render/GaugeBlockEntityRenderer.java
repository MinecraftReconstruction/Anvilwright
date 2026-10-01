package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.tconstruct.smeltery.block.entity.GaugeBlockEntity;

import java.util.List;

/** Renderer for the obisidian gauge block */
public class GaugeBlockEntityRenderer implements BlockEntityRenderer<GaugeBlockEntity> {
  public GaugeBlockEntityRenderer(Context context) {}

  @Override
  public void render(GaugeBlockEntity tile, float pPartialTick, PoseStack matrices, MultiBufferSource buffer, int light, int pPackedOverlay) {
    List<FluidCuboid> fluids = FluidCuboid.REGISTRY.get(tile.getBlockState(), List.of());
    if (!fluids.isEmpty()) {
      Storage<FluidVariant> tank = tile.getTank();
      FluidStack fluid = TransferUtil.firstOrEmpty(tank);
      if (!fluid.isEmpty()) {
        if (!fluids.isEmpty()) {
          FluidRenderer.renderCuboids(matrices, buffer.getBuffer(MantleRenderTypes.FLUID), fluids, fluid, light);
        }
      }
    }
  }
}
