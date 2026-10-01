package slimeknights.tconstruct.common.data;

import io.github.fabricators_of_create.porting_lib.data.DatapackBuiltinEntriesProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.VanillaRegistries;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.data.WorldgenProvider;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Writes the datapack registry entries (damage types, worldgen) into {@code src/generated}.
 * <p>
 * {@code DataGeneratorEntrypoint#buildRegistry} only feeds the datagen lookups, it does not emit anything - this
 * provider is what actually writes the JSON, so removing it silently dropped every damage type and worldgen entry.
 */
public class TinkerRegistrySets extends DatapackBuiltinEntriesProvider {
  public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder();

  static {
    DamageTypeProvider.register(BUILDER);
    WorldgenProvider.register(BUILDER);
  }

  public TinkerRegistrySets(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
    super(output, registries, BUILDER, Collections.singleton(TConstruct.MOD_ID));
  }

  public static HolderLookup.Provider createLookup() {
    return BUILDER.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), VanillaRegistries.createLookup());
  }
}
