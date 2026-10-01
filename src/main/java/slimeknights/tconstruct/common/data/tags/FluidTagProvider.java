package slimeknights.tconstruct.common.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import io.github.fabricators_of_create.porting_lib.tags.Tags;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.fluids.TinkerFluids;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
public class FluidTagProvider extends FabricTagProvider.FluidTagProvider {

  public FluidTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected void addTags(HolderLookup.Provider pProvider) {
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
    this.getOrCreateTagBuilder(TinkerTags.Fluids.SLIME)
        .addTag(TinkerFluids.earthSlime.getTag())
        .addTag(TinkerFluids.skySlime.getTag())
        .addTags(TinkerFluids.ichor.getTag())
        .addTag(TinkerFluids.enderSlime.getTag());

    fluidTag(TinkerFluids.potion);
    fluidTag(TinkerFluids.powderedSnow);

    // drowned want fluids that work nice in water, while wither skeletons want to complement the withering
    // both need to act as a swasher tutorial though
    this.getOrCreateTagBuilder(TinkerTags.Fluids.DROWNED_SWASHER).add(Fluids.LAVA, TinkerFluids.powderedSnow.get(), TinkerFluids.moltenGlass.get(), TinkerFluids.moltenObsidian.get());
    this.getOrCreateTagBuilder(TinkerTags.Fluids.WITHER_SKELETON_SWASHER).add(Fluids.LAVA, TinkerFluids.blazingBlood.get(), TinkerFluids.liquidSoul.get(), TinkerFluids.magma.get());

    // tag local tags with the chemthrower, do not include forge tags as its on other mods to choose how they want to support IE
    // block effects - mostly mining
    this.getOrCreateTagBuilder(TinkerTags.Fluids.CHEMTHROWER_BLOCK_EFFECTS)
      .addTags(
        // small gem
        TinkerFluids.moltenAmethyst.getLocalTag(), TinkerFluids.moltenQuartz.getLocalTag(),
        // large gem
        TinkerFluids.moltenEmerald.getLocalTag(), TinkerFluids.moltenDiamond.getLocalTag(), TinkerFluids.moltenDebris.getLocalTag(),
        // twilight forest
        TinkerFluids.fieryLiquid.getLocalTag()
      );
    // entity effects - most of these have block effects, but we don't want the clouds triggering mostly
    this.getOrCreateTagBuilder(TinkerTags.Fluids.CHEMTHROWER_ENTITY_EFFECTS)
      .add(TinkerFluids.powderedSnow.get())
      .addTags(
        // common
        Tags.Fluids.MILK, TinkerFluids.blazingBlood.getLocalTag(),
        // slime
        TinkerFluids.venom.getLocalTag(),
        // glass
        TinkerFluids.moltenGlass.getLocalTag(), TinkerFluids.liquidSoul.getLocalTag(), TinkerFluids.moltenObsidian.getLocalTag(),
        // clay
        TinkerFluids.moltenClay.getLocalTag(), TinkerFluids.searedStone.getLocalTag(), TinkerFluids.scorchedStone.getLocalTag(),
        // food
        TinkerFluids.honey.getLocalTag(),
        TinkerFluids.mushroomStew.getLocalTag(), TinkerFluids.rabbitStew.getLocalTag(), TinkerFluids.meatSoup.getLocalTag(),
        // tier 2
        TinkerFluids.moltenCopper.getLocalTag(), TinkerFluids.moltenIron.getLocalTag(), TinkerFluids.moltenGold.getLocalTag(),
        // tier 2 compat
        TinkerFluids.moltenZinc.getLocalTag(), TinkerFluids.moltenTin.getLocalTag(), TinkerFluids.moltenAluminum.getLocalTag(),
        TinkerFluids.moltenSilver.getLocalTag(), TinkerFluids.moltenLead.getLocalTag(), TinkerFluids.moltenNickel.getLocalTag(),
        TinkerFluids.moltenPlatinum.getLocalTag(), TinkerFluids.moltenTungsten.getLocalTag(), TinkerFluids.moltenOsmium.getLocalTag(),
        TinkerFluids.moltenUranium.getLocalTag(), TinkerFluids.moltenChromium.getLocalTag(), TinkerFluids.moltenCadmium.getLocalTag(),
        // tier 3
        TinkerFluids.moltenAmethystBronze.getLocalTag(), TinkerFluids.moltenPigIron.getLocalTag(), TinkerFluids.moltenRoseGold.getLocalTag(),
        TinkerFluids.moltenCobalt.getLocalTag(), TinkerFluids.moltenSteel.getLocalTag(),
        // tier 3 compat
        TinkerFluids.moltenBronze.getLocalTag(), TinkerFluids.moltenBrass.getLocalTag(), TinkerFluids.moltenPewter.getLocalTag(),
        TinkerFluids.moltenInvar.getLocalTag(), TinkerFluids.moltenConstantan.getLocalTag(),
        // tier 4
        TinkerFluids.moltenManyullyn.getLocalTag(), TinkerFluids.moltenHepatizon.getLocalTag(), TinkerFluids.moltenNetherite.getLocalTag(),
        TinkerFluids.moltenKnightmetal.getLocalTag(),
        // thermal alloys
        TinkerFluids.moltenLumium.getLocalTag(), TinkerFluids.moltenEnderium.getLocalTag(),
        // mekanism alloys
        TinkerFluids.moltenRefinedGlowstone.getLocalTag(), TinkerFluids.moltenRefinedObsidian.getLocalTag(),
        // cosmere alloys
        TinkerFluids.moltenNicrosil.getLocalTag(), TinkerFluids.moltenDuralumin.getLocalTag(), TinkerFluids.moltenBendalloy.getLocalTag(),
        // twilight forest
        TinkerFluids.moltenSteeleaf.getLocalTag()
      );
    // both effects - all the neat slimes
    this.getOrCreateTagBuilder(TinkerTags.Fluids.CHEMTHROWER_BOTH_EFFECTS)
      // slime
      .addTags(
        // slime
        TinkerFluids.earthSlime.getLocalTag(), TinkerFluids.skySlime.getLocalTag(), TinkerFluids.ichor.getTag(), TinkerFluids.enderSlime.getTag(),
        TinkerFluids.magma.getLocalTag(), TinkerFluids.moltenEnder.getLocalTag(),
        // slime metal
        TinkerFluids.moltenSlimesteel.getLocalTag(), TinkerFluids.moltenQueensSlime.getLocalTag(),
        TinkerFluids.moltenCinderslime.getLocalTag(), TinkerFluids.moltenKnightslime.getLocalTag(),
        // thermal alloys
        TinkerFluids.moltenSignalum.getLocalTag()
      );

    // tooltips //
    this.getOrCreateTagBuilder(TinkerTags.Fluids.GLASS_TOOLTIPS).addTags(TinkerFluids.moltenGlass.getTag(), TinkerFluids.liquidSoul.getTag(), TinkerFluids.moltenObsidian.getTag());
    this.getOrCreateTagBuilder(TinkerTags.Fluids.SLIME_TOOLTIPS).addTags(TinkerFluids.magma.getTag(), TinkerFluids.moltenEnder.getTag(), TinkerTags.Fluids.SLIME);
    this.getOrCreateTagBuilder(TinkerTags.Fluids.BOTTLE_TOOLTIPS).addTags(TinkerFluids.venom.getTag(), TinkerFluids.fieryLiquid.getTag());
    this.getOrCreateTagBuilder(TinkerTags.Fluids.CLAY_TOOLTIPS).addTags(TinkerFluids.moltenClay.getTag(), TinkerFluids.moltenPorcelain.getTag(), TinkerFluids.searedStone.getTag(), TinkerFluids.scorchedStone.getTag());
    this.getOrCreateTagBuilder(TinkerTags.Fluids.METAL_TOOLTIPS).addTags(
        // vanilla ores
        TinkerFluids.moltenIron.getTag(), TinkerFluids.moltenGold.getTag(), TinkerFluids.moltenCopper.getTag(),
        TinkerFluids.moltenCobalt.getTag(), TinkerFluids.moltenSteel.getTag(), TinkerFluids.moltenDebris.getTag(),
        // base alloys
        TinkerFluids.moltenSlimesteel.getTag(), TinkerFluids.moltenAmethystBronze.getTag(), TinkerFluids.moltenRoseGold.getTag(), TinkerFluids.moltenPigIron.getTag(),
        TinkerFluids.moltenManyullyn.getTag(), TinkerFluids.moltenHepatizon.getTag(), TinkerFluids.moltenQueensSlime.getTag(), TinkerFluids.moltenCinderslime.getTag(),
        TinkerFluids.moltenNetherite.getTag(), TinkerFluids.moltenSoulsteel.getTag(), TinkerFluids.moltenKnightmetal.getTag(), TinkerFluids.moltenKnightslime.getTag(),
        // compat ores
        TinkerFluids.moltenTin.getTag(), TinkerFluids.moltenAluminum.getTag(), TinkerFluids.moltenLead.getTag(),
        TinkerFluids.moltenSilver.getTag(), TinkerFluids.moltenNickel.getTag(), TinkerFluids.moltenZinc.getTag(),
        TinkerFluids.moltenPlatinum.getTag(), TinkerFluids.moltenTungsten.getTag(), TinkerFluids.moltenOsmium.getTag(),
        TinkerFluids.moltenUranium.getTag(), TinkerFluids.moltenChromium.getTag(), TinkerFluids.moltenCadmium.getTag(),
        // compat alloys
        TinkerFluids.moltenBronze.getTag(), TinkerFluids.moltenBrass.getTag(), TinkerFluids.moltenElectrum.getTag(),
        TinkerFluids.moltenInvar.getTag(), TinkerFluids.moltenConstantan.getTag(), TinkerFluids.moltenPewter.getTag(),
        // thermal alloys
        TinkerFluids.moltenEnderium.getTag(), TinkerFluids.moltenLumium.getTag(), TinkerFluids.moltenSignalum.getTag(),
        // mekanism alloys
        TinkerFluids.moltenRefinedGlowstone.getTag(), TinkerFluids.moltenRefinedObsidian.getTag(),
        // cosmere alloys
        TinkerFluids.moltenNicrosil.getTag(), TinkerFluids.moltenDuralumin.getTag(), TinkerFluids.moltenBendalloy.getTag(),
        // Twilight alloys
        TinkerFluids.moltenSteeleaf.getTag()
    );

    this.getOrCreateTagBuilder(TinkerTags.Fluids.LARGE_GEM_TOOLTIPS).addTags(TinkerFluids.moltenEmerald.getTag(), TinkerFluids.moltenDiamond.getTag());
    this.getOrCreateTagBuilder(TinkerTags.Fluids.SMALL_GEM_TOOLTIPS).addTags(TinkerFluids.moltenQuartz.getTag(), TinkerFluids.moltenAmethyst.getTag());
    this.getOrCreateTagBuilder(MantleTags.Fluids.SOUP).addTag(TinkerFluids.meatSoup.getTag()).addOptionalTag(TinkerTags.Fluids.SOUP_TOOLTIPS.location());

    // hide upcoming fluids
    this.getOrCreateTagBuilder(TinkerTags.Fluids.HIDDEN_IN_RECIPE_VIEWERS).add(TinkerFluids.moltenSoulsteel.get());
    // hide upcoming fluids that require NBT. Can expand this list if other mods report problems
    this.getOrCreateTagBuilder(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS).add(TinkerFluids.potion.get()).addTag(TinkerTags.Fluids.HIDDEN_IN_RECIPE_VIEWERS);
  }

  @Override
  public String getName() {
    return "Tinkers Construct Fluid TinkerTags";
  }

  /** Adds tags for an unplacable fluid */
  private void fluidTag(FluidObject<?> fluid) {
    // a few fluids are registered without a common tag (sky/ender slime), so only add the common tag when present
    TagKey<Fluid> common = fluid.getCommonTag();
    if (common != null) {
      this.getOrCreateTagBuilder(common).add(fluid.get());
    }
    // the local tag is the one the rest of the mod references (tconstruct:sky_slime and friends)
    this.getOrCreateTagBuilder(fluid.getLocalTag()).add(fluid.get());
  }

  /** Adds tags for a placable fluid */
  private void fluidTag(FlowingFluidObject<?> fluid) {
    this.getOrCreateTagBuilder(fluid.getLocalTag()).add(fluid.getStill(), fluid.getFlowing());
    TagKey<Fluid> tag = fluid.getCommonTag();
    if (tag != null) {
      this.getOrCreateTagBuilder(tag).addTag(fluid.getLocalTag());
    }
  }
}
