package slimeknights.tconstruct.common.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.InstrumentTagsProvider;
import net.minecraft.world.item.Instruments;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.concurrent.CompletableFuture;

public class InstrumentTagProvider extends InstrumentTagsProvider {
  public InstrumentTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> provider) {
    super(output, provider);
  }

  @Override
  protected void addTags(HolderLookup.Provider pProvider) {
    this.tag(TinkerTags.Instruments.VARIANT_HORNS).add(
      Instruments.PONDER_GOAT_HORN, Instruments.SING_GOAT_HORN, Instruments.SEEK_GOAT_HORN, Instruments.FEEL_GOAT_HORN,
      Instruments.ADMIRE_GOAT_HORN, Instruments.CALL_GOAT_HORN, Instruments.YEARN_GOAT_HORN, Instruments.DREAM_GOAT_HORN
    );
  }
}
