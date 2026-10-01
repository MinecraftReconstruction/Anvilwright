package slimeknights.tconstruct.common.data.tags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.alchemy.Potion;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import slimeknights.mantle.data.BuiltinRegistryTagProvider;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.concurrent.CompletableFuture;

public class PotionTagProvider extends BuiltinRegistryTagProvider<Potion> {
  public PotionTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, BuiltInRegistries.POTION, registriesFuture);
  }

  @Override
  protected void addTags(HolderLookup.Provider provider) {
    tag(TinkerTags.Potions.HIDDEN_FLUID).addOptionalTag(TinkerTags.HIDDEN_FROM_RECIPE_VIEWERS);
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Potion Tags";
  }
}
