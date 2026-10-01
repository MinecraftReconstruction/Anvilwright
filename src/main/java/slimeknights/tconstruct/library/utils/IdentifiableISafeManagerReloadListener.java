package slimeknights.tconstruct.library.utils;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;

/**
 * Reload listener that has both a fixed ID (required by Fabric's resource manager) and Mantle's safe-reload hook.
 * <p>
 * <b>Porting note:</b> the original Fabric port kept this in Mantle as
 * {@code slimeknights.mantle.data.fabric.IdentifiableISafeManagerReloadListener}, but that package did not survive
 * Mantle's own 1.20 merge, so the adapter now lives here. Disclosed in {@code docs/BEHAVIOUR-DIFFERENCES.md}.
 */
public abstract class IdentifiableISafeManagerReloadListener implements ISafeManagerReloadListener, IdentifiableResourceReloadListener {
  private final ResourceLocation id;

  protected IdentifiableISafeManagerReloadListener(ResourceLocation id) {
    this.id = id;
  }

  @Override
  public ResourceLocation getFabricId() {
    return id;
  }
}
