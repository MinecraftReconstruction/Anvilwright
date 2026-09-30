package slimeknights.tconstruct.common.data.tags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.CreativeModeTab;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import slimeknights.mantle.data.BuiltinRegistryTagProvider;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.concurrent.CompletableFuture;

public class CreativeTabTagProvider extends BuiltinRegistryTagProvider<CreativeModeTab> {
  public CreativeTabTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, BuiltInRegistries.CREATIVE_MODE_TAB, registriesFuture);
  }

  @Override
  protected void addTags(HolderLookup.Provider provider) {
    this.tag(TinkerTags.CreativeTabs.HIDDEN_IN_RECIPE_VIEWERS).add(TinkerTables.tabTables.get(), TinkerFluids.tabFluids.get());
  }
}
