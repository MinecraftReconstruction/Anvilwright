package slimeknights.tconstruct.common.data.tags;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.fluids.TinkerFluids;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
public class FluidTagProvider extends FabricTagProvider.FluidTagProvider {

  public FluidTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  public void addTags(HolderLookup.Provider provider) {
    // first, register common tags
    // slime
    fluidTag(TinkerFluids.earthSlime);
    fluidTag(TinkerFluids.skySlime);
    fluidTag(TinkerFluids.ichor);
    fluidTag(TinkerFluids.enderSlime);
    fluidTag(TinkerFluids.magma);
    fluidTag(TinkerFluids.venom);
    // basic molten
    fluidTag(TinkerFluids.searedStone);
    fluidTag(TinkerFluids.scorchedStone);
    fluidTag(TinkerFluids.moltenClay);
    fluidTag(TinkerFluids.moltenGlass);
    fluidTag(TinkerFluids.liquidSoul);
    fluidTag(TinkerFluids.moltenPorcelain);
    // fancy molten
    fluidTag(TinkerFluids.moltenObsidian);
    fluidTag(TinkerFluids.moltenEmerald);
    fluidTag(TinkerFluids.moltenQuartz);
    fluidTag(TinkerFluids.moltenDiamond);
    fluidTag(TinkerFluids.moltenAmethyst);
    fluidTag(TinkerFluids.moltenEnder);
    fluidTag(TinkerFluids.blazingBlood);
    // ores
    fluidTag(TinkerFluids.moltenIron);
    fluidTag(TinkerFluids.moltenGold);
    fluidTag(TinkerFluids.moltenCopper);
    fluidTag(TinkerFluids.moltenCobalt);
    fluidTag(TinkerFluids.moltenSteel);
    fluidTag(TinkerFluids.moltenDebris);
    // alloys
    fluidTag(TinkerFluids.moltenSlimesteel);
    fluidTag(TinkerFluids.moltenAmethystBronze);
    fluidTag(TinkerFluids.moltenRoseGold);
    fluidTag(TinkerFluids.moltenPigIron);
    // nether alloys
    fluidTag(TinkerFluids.moltenManyullyn);
    fluidTag(TinkerFluids.moltenHepatizon);
    fluidTag(TinkerFluids.moltenQueensSlime);
    fluidTag(TinkerFluids.moltenCinderslime);
    fluidTag(TinkerFluids.moltenSoulsteel);
    fluidTag(TinkerFluids.moltenNetherite);
    // end alloys
    fluidTag(TinkerFluids.moltenKnightmetal);
    fluidTag(TinkerFluids.moltenKnightslime);
    // compat ores
    fluidTag(TinkerFluids.moltenTin);
    fluidTag(TinkerFluids.moltenAluminum);
    fluidTag(TinkerFluids.moltenLead);
    fluidTag(TinkerFluids.moltenSilver);
    fluidTag(TinkerFluids.moltenNickel);
    fluidTag(TinkerFluids.moltenZinc);
    fluidTag(TinkerFluids.moltenPlatinum);
    fluidTag(TinkerFluids.moltenTungsten);
    fluidTag(TinkerFluids.moltenOsmium);
    fluidTag(TinkerFluids.moltenUranium);
    fluidTag(TinkerFluids.moltenChromium);
    fluidTag(TinkerFluids.moltenCadmium);
    // compat alloys
    fluidTag(TinkerFluids.moltenBronze);
    fluidTag(TinkerFluids.moltenBrass);
    fluidTag(TinkerFluids.moltenElectrum);
    fluidTag(TinkerFluids.moltenInvar);
    fluidTag(TinkerFluids.moltenConstantan);
    fluidTag(TinkerFluids.moltenPewter);
    // thermal compat alloys
    fluidTag(TinkerFluids.moltenEnderium);
    fluidTag(TinkerFluids.moltenLumium);
    fluidTag(TinkerFluids.moltenSignalum);
    // mekanism compat alloys
    fluidTag(TinkerFluids.moltenRefinedGlowstone);
    fluidTag(TinkerFluids.moltenRefinedObsidian);
    // cosmere compat alloys
    fluidTag(TinkerFluids.moltenNicrosil);
    fluidTag(TinkerFluids.moltenDuralumin);
    fluidTag(TinkerFluids.moltenBendalloy);
    // twilight compat fluids
    fluidTag(TinkerFluids.moltenSteeleaf);
    fluidTag(TinkerFluids.fieryLiquid);
    // unplacable fluids
    fluidTag(TinkerFluids.honey);
    fluidTag(TinkerFluids.beetrootSoup);
    fluidTag(TinkerFluids.mushroomStew);
    fluidTag(TinkerFluids.rabbitStew);
    fluidTag(TinkerFluids.meatSoup);

    /* Normal tags */
    this.tag(TinkerTags.Fluids.SLIME)
        .addTag(TinkerFluids.earthSlime.getTag())
        .addTag(TinkerFluids.skySlime.getTag())
        .addTags(TinkerFluids.ichor.getTag())
        .addTag(TinkerFluids.enderSlime.getTag());

    this.getOrCreateTagBuilder(TinkerTags.Fluids.POTION).add(TinkerFluids.potion.get());

    // tooltips //
    this.tag(TinkerTags.Fluids.GLASS_TOOLTIPS).addTag(TinkerFluids.moltenGlass.getLocalTag()).addTag(TinkerFluids.liquidSoul.getLocalTag()).addTag(TinkerFluids.moltenObsidian.getLocalTag());
    this.tag(TinkerTags.Fluids.SLIME_TOOLTIPS).addTag(TinkerFluids.magma.getForgeTag()).addTag(TinkerFluids.blood.getLocalTag()).addTag(TinkerFluids.moltenEnder.getForgeTag()).addTag(TinkerTags.Fluids.SLIME);
    this.tag(TinkerTags.Fluids.CLAY_TOOLTIPS).addTag(TinkerFluids.moltenClay.getLocalTag()).addTag(TinkerFluids.moltenPorcelain.getLocalTag()).addTag(TinkerFluids.searedStone.getLocalTag()).addTag(TinkerFluids.scorchedStone.getLocalTag());
    this.tag(TinkerTags.Fluids.METAL_TOOLTIPS).addTag(
        // vanilla ores
        TinkerFluids.moltenIron.getForgeTag()).addTag(TinkerFluids.moltenGold.getForgeTag()).addTag(TinkerFluids.moltenCopper.getForgeTag()).addTag(TinkerFluids.moltenCobalt.getForgeTag()).addTag(TinkerFluids.moltenDebris.getLocalTag()).addTag(
        // base alloys
        TinkerFluids.moltenSlimesteel.getLocalTag()).addTag(TinkerFluids.moltenAmethystBronze.getLocalTag()).addTag(TinkerFluids.moltenRoseGold.getForgeTag()).addTag(TinkerFluids.moltenPigIron.getLocalTag()).addTag(
        TinkerFluids.moltenManyullyn.getForgeTag()).addTag(TinkerFluids.moltenHepatizon.getForgeTag()).addTag(TinkerFluids.moltenQueensSlime.getLocalTag()).addTag(TinkerFluids.moltenNetherite.getForgeTag()).addTag(
        TinkerFluids.moltenSoulsteel.getLocalTag()).addTag(TinkerFluids.moltenKnightslime.getLocalTag()).addTag(
        // compat ores
        TinkerFluids.moltenTin.getForgeTag()).addTag(TinkerFluids.moltenAluminum.getForgeTag()).addTag(TinkerFluids.moltenLead.getForgeTag()).addTag(TinkerFluids.moltenSilver.getForgeTag()).addTag(
        TinkerFluids.moltenNickel.getForgeTag()).addTag(TinkerFluids.moltenZinc.getForgeTag()).addTag(TinkerFluids.moltenPlatinum.getForgeTag()).addTag(
        TinkerFluids.moltenTungsten.getForgeTag()).addTag(TinkerFluids.moltenOsmium.getForgeTag()).addTag(TinkerFluids.moltenUranium.getForgeTag()).addTag(
        // compat alloys
        TinkerFluids.moltenBronze.getForgeTag()).addTag(TinkerFluids.moltenBrass.getForgeTag()).addTag(TinkerFluids.moltenElectrum.getForgeTag()).addTag(
        TinkerFluids.moltenInvar.getForgeTag()).addTag(TinkerFluids.moltenConstantan.getForgeTag()).addTag(TinkerFluids.moltenPewter.getForgeTag()).addTag(TinkerFluids.moltenSteel.getForgeTag()).addTag(
        // thermal alloys
        TinkerFluids.moltenEnderium.getForgeTag()).addTag(TinkerFluids.moltenLumium.getForgeTag()).addTag(TinkerFluids.moltenSignalum.getForgeTag()).addTag(
        // mekanism alloys
        TinkerFluids.moltenRefinedGlowstone.getForgeTag()).addTag(TinkerFluids.moltenRefinedObsidian.getForgeTag());

    this.tag(TinkerTags.Fluids.LARGE_GEM_TOOLTIPS).addTags(TinkerFluids.moltenEmerald.getLocalTag(), TinkerFluids.moltenDiamond.getLocalTag());
    this.tag(TinkerTags.Fluids.SMALL_GEM_TOOLTIPS).addTags(TinkerFluids.moltenQuartz.getLocalTag(), TinkerFluids.moltenAmethyst.getLocalTag());
    this.tag(TinkerTags.Fluids.SOUP_TOOLTIPS).addTags(TinkerFluids.beetrootSoup.getLocalTag(), TinkerFluids.mushroomStew.getLocalTag(), TinkerFluids.rabbitStew.getLocalTag());
    this.getOrCreateTagBuilder(TinkerTags.Fluids.WATER_TOOLTIPS).forceAddTag(MantleTags.Fluids.WATER);

    // hide upcoming fluids
    tag(TinkerTags.Fluids.HIDDEN_IN_RECIPE_VIEWERS).add(TinkerFluids.moltenSoulsteel.get());
    // hide upcoming fluids that require NBT. Can expand this list if other mods report problems
    tag(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS).add(TinkerFluids.potion.get()).addTag(TinkerTags.Fluids.HIDDEN_IN_RECIPE_VIEWERS);
  }

  @Override
  public String getName() {
    return "Tinkers Construct Fluid TinkerTags";
  }

  /** Tags this fluid using local tags */
  private void tagLocal(FluidObject<?> fluid) {
    getOrCreateTagBuilder(fluid.getLocalTag()).add(fluid.getStill(), fluid.getFlowing());
  }

  /** Adds tags for a placable fluid */
  private void fluidTag(FlowingFluidObject<?> fluid) {
    tag(fluid.getLocalTag()).add(fluid.getStill(), fluid.getFlowing());
    TagKey<Fluid> tag = fluid.getCommonTag();
    if (tag != null) {
      tag(tag).addTag(fluid.getLocalTag());
    }
  }
}
