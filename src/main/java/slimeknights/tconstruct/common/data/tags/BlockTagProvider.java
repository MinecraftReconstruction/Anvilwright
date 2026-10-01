package slimeknights.tconstruct.common.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import io.github.fabricators_of_create.porting_lib.tags.Tags;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.MetalItemObject;
import slimeknights.mantle.registration.object.WoodBlockObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.registration.GeodeItemObject;
import slimeknights.tconstruct.common.registration.GeodeItemObject.BudSize;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock.GlassColor;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.data.SmelteryCompat;
import slimeknights.tconstruct.smeltery.data.SmelteryCompat.CompatType;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.world.TinkerHeadType;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.block.DirtType;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE;
import static net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE;
import static net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE;
import static net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL;
import static net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL;
import static net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL;
import static net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL;
import static io.github.fabricators_of_create.porting_lib.tags.Tags.Blocks.NEEDS_GOLD_TOOL;
import static io.github.fabricators_of_create.porting_lib.tags.Tags.Blocks.NEEDS_NETHERITE_TOOL;
import static slimeknights.mantle.Mantle.commonResource;
import static slimeknights.tconstruct.common.TinkerTags.Blocks.MINEABLE_MELTING_BLACKLIST;
import static slimeknights.tconstruct.common.TinkerTags.Blocks.UNREPLACABLE_BY_LIQUID;
import net.minecraft.core.registries.Registries;

@SuppressWarnings({"unchecked", "SameParameterValue", "removal"})
public class BlockTagProvider extends FabricTagProvider.BlockTagProvider {

  public BlockTagProvider(FabricDataOutput output, CompletableFuture<Provider> lookupProvider) {
    super(output, lookupProvider);
  }

  @Override
  protected void addTags(HolderLookup.Provider pProvider) {
    this.addCommon();
    this.addTools();
    this.addWorld();
    this.addSmeltery();
    this.addFluids();
    this.addHarvest();
  }

  private void addCommon() {
    // ores
    addMetalTags(TinkerMaterials.cobalt, true);
    addMetalTags(TinkerMaterials.steel, true);
    // tier 3
    addMetalTags(TinkerMaterials.slimesteel, true); // beacon: skyslime and seared stone are expensive enough
    addMetalTags(TinkerMaterials.amethystBronze, false); // not beacon: mostly copper and amethyst
    addMetalTags(TinkerMaterials.roseGold, false); // not beacon: 50% copper
    addMetalTags(TinkerMaterials.pigIron, false); // not beacon: 50% food
    // tier 4
    addMetalTags(TinkerMaterials.cinderslime, true); // beacon: ichor and scorched stone are expensive enough
    addMetalTags(TinkerMaterials.queensSlime, true);
    addMetalTags(TinkerMaterials.manyullyn, true);
    addMetalTags(TinkerMaterials.hepatizon, true);
    addMetalTags(TinkerMaterials.soulsteel, true);
    // tier 5
    addMetalTags(TinkerMaterials.knightmetal, true);
    addMetalTags(TinkerMaterials.knightslime, true);

    // glass
    FabricTagBuilder silicaPanes = getOrCreateTagBuilder(TinkerTags.Blocks.GLASS_PANES_SILICA);
    this.getOrCreateTagBuilder(Tags.Blocks.GLASS).add(TinkerCommons.soulGlass.get());
    this.getOrCreateTagBuilder(Tags.Blocks.GLASS_PANES).add(TinkerCommons.soulGlassPane.get());
    silicaPanes.add(
      Blocks.GLASS_PANE, TinkerCommons.clearGlassPane.get(),
      Blocks.BLACK_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS_PANE, Blocks.BROWN_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS_PANE,
      Blocks.GRAY_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE,
      Blocks.LIME_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.PINK_STAINED_GLASS_PANE,
      Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS_PANE);
    this.getOrCreateTagBuilder(Tags.Blocks.GLASS_COLORLESS).add(TinkerCommons.clearGlass.get());
    this.getOrCreateTagBuilder(Tags.Blocks.GLASS_PANES_COLORLESS).add(TinkerCommons.clearGlassPane.get());
    addGlass(TinkerCommons.clearStainedGlass, "glass/", getOrCreateTagBuilder(Tags.Blocks.STAINED_GLASS));
    addGlass(TinkerCommons.clearStainedGlassPane, "glass_panes/", getOrCreateTagBuilder(Tags.Blocks.STAINED_GLASS_PANES));
    TinkerCommons.clearStainedGlassPane.forEach(pane -> silicaPanes.add(pane));

    // impermeable for all glass
    FabricTagBuilder impermeable = getOrCreateTagBuilder(BlockTags.IMPERMEABLE);
    FabricTagBuilder silicaGlass = getOrCreateTagBuilder(Tags.Blocks.GLASS_SILICA);
    impermeable.add(TinkerCommons.clearGlass.get(), TinkerCommons.soulGlass.get(), TinkerCommons.clearTintedGlass.get(),
                    TinkerSmeltery.searedGlass.get(), TinkerSmeltery.searedSoulGlass.get(), TinkerSmeltery.searedTintedGlass.get(),
                    TinkerSmeltery.scorchedGlass.get(), TinkerSmeltery.scorchedSoulGlass.get(), TinkerSmeltery.scorchedTintedGlass.get());
    silicaGlass.add(TinkerCommons.clearGlass.get());
    TinkerCommons.clearStainedGlass.values().forEach(impermeable::add);
    TinkerCommons.clearStainedGlass.values().forEach(silicaGlass::add);
    getOrCreateTagBuilder(Tags.Blocks.GLASS_TINTED).add(TinkerCommons.clearTintedGlass.get());

    // soul speed on glass
    this.getOrCreateTagBuilder(BlockTags.SOUL_SPEED_BLOCKS).add(TinkerCommons.soulGlass.get(), TinkerCommons.soulGlassPane.get(),
                                              TinkerSmeltery.searedSoulGlass.get(), TinkerSmeltery.searedSoulGlassPane.get(),
                                              TinkerSmeltery.scorchedSoulGlass.get(), TinkerSmeltery.scorchedSoulGlassPane.get());
    this.getOrCreateTagBuilder(BlockTags.SOUL_FIRE_BASE_BLOCKS).add(TinkerCommons.soulGlass.get(), TinkerSmeltery.searedSoulGlass.get(), TinkerSmeltery.scorchedSoulGlass.get());
    this.getOrCreateTagBuilder(TinkerTags.Blocks.TRANSPARENT_OVERLAY).add(TinkerCommons.soulGlass.get(), TinkerCommons.soulGlassPane.get(),
                                                        TinkerSmeltery.searedSoulGlass.get(), TinkerSmeltery.searedSoulGlassPane.get(),
                                                        TinkerSmeltery.scorchedSoulGlass.get(), TinkerSmeltery.scorchedSoulGlassPane.get());
    Function<String,ResourceLocation> createId = name -> new ResourceLocation("create", name);
    Function<String,ResourceLocation> quarkId = name -> new ResourceLocation("quark", name);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.WORKSTATION_ROCK)
      .addOptional(createId.apply("asurine")).addOptional(createId.apply("crimsite")).addOptional(createId.apply("limestone")).addOptional(createId.apply("ochrum")).addOptional(createId.apply("scoria")).addOptional(createId.apply("scorchia")).addOptional(createId.apply("veridium")).addOptional(quarkId.apply("jasper")).addOptional(quarkId.apply("limestone")).addOptional(quarkId.apply("permafrost")).addOptional(quarkId.apply("shale")).addOptional(quarkId.apply("myalite")).add(Blocks.TUFF, Blocks.DRIPSTONE_BLOCK, Blocks.CALCITE).addTags(TinkerTags.Blocks.STONE, TinkerTags.Blocks.BLACKSTONE, TinkerTags.Blocks.GRANITE, TinkerTags.Blocks.DIORITE, TinkerTags.Blocks.ANDESITE, TinkerTags.Blocks.DEEPSLATE, TinkerTags.Blocks.BASALT);

    FabricTagBuilder builder = this.getOrCreateTagBuilder(TinkerTags.Blocks.ANVIL_METAL)
        // tier 3
        .addTag(TinkerMaterials.slimesteel.getBlockTag())
        .addTag(TinkerMaterials.amethystBronze.getBlockTag())
        .addTag(TinkerMaterials.roseGold.getBlockTag())
        .addTag(TinkerMaterials.pigIron.getBlockTag())
        // tier 4
        .addTag(TinkerMaterials.cinderslime.getBlockTag())
        .addTag(TinkerMaterials.queensSlime.getBlockTag())
        .addTag(TinkerMaterials.manyullyn.getBlockTag())
        .addTag(TinkerMaterials.hepatizon.getBlockTag())
        .addTag(TinkerMaterials.knightmetal.getBlockTag())
        .addTag(TinkerMaterials.knightslime.getBlockTag())
        .addOptionalTag(Tags.Blocks.STORAGE_BLOCKS_NETHERITE);
    for (SmelteryCompat compat : SmelteryCompat.values()) {
      if (compat.getType() == CompatType.ALLOY) {
        builder.addOptionalTag(commonResource("storage_blocks/" + compat.getName()));
      }
    }

    // allow using wood variants to make tables
    this.getOrCreateTagBuilder(TinkerTags.Blocks.PLANKLIKE)
        .add(TinkerMaterials.blazewood.get(), TinkerMaterials.nahuatl.get()).addOptionalTag(BlockTags.PLANKS);
    // things the platform connects to on the sides
    this.getOrCreateTagBuilder(TinkerTags.Blocks.PLATFORM_CONNECTIONS)
      .addOptionalTag(new ResourceLocation("architects_palette:nubs")).add(Blocks.LEVER, Blocks.LADDER, Blocks.IRON_BARS, TinkerCommons.goldBars.get(), Blocks.TRIPWIRE_HOOK, Blocks.WALL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.REDSTONE_WIRE).addOptionalTag(Tags.Blocks.GLASS_PANES).addOptionalTag(BlockTags.BUTTONS).addOptionalTag(Tags.Blocks.FENCES).addOptionalTag(BlockTags.WALLS).addOptionalTag(BlockTags.WALL_SIGNS);

    // copper platforms
    FabricTagBuilder copperPlatforms = this.getOrCreateTagBuilder(TinkerTags.Blocks.COPPER_PLATFORMS);
    TinkerCommons.copperPlatform.forEach(block -> copperPlatforms.add(block));
    TinkerCommons.waxedCopperPlatform.forEach(block -> copperPlatforms.add(block));

    this.getOrCreateTagBuilder(TinkerTags.Blocks.CREATE_ROOTS)
      .addTag(TinkerTags.Blocks.ENDERBARK_ROOTS);
  }

  private void addTools() {
    // vanilla is not tagged, so tag it
    this.getOrCreateTagBuilder(TinkerTags.Blocks.WORKBENCHES)
        .add(Blocks.CRAFTING_TABLE, TinkerTables.craftingStation.get())
        .addOptionalTag(new ResourceLocation("forge:workbench")); // some mods use a non-standard name here, so support it I guess
    this.getOrCreateTagBuilder(TinkerTags.Blocks.TABLES)
        .add(TinkerTables.craftingStation.get(), TinkerTables.partBuilder.get(), TinkerTables.tinkerStation.get());

    // can harvest crops and sugar cane
    this.getOrCreateTagBuilder(TinkerTags.Blocks.HARVESTABLE_STACKABLE)
        .add(Blocks.SUGAR_CANE, Blocks.KELP_PLANT);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.HARVESTABLE_CROPS)
        .addOptionalTag(commonResource("crops")).add(Blocks.NETHER_WART, Blocks.SWEET_BERRY_BUSH).addOptionalTag(BlockTags.CROPS);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.HARVESTABLE_INTERACT)
        .add(Blocks.SWEET_BERRY_BUSH, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.HARVESTABLE)
        .add(Blocks.PUMPKIN, Blocks.BEEHIVE, Blocks.BEE_NEST).addTag(TinkerTags.Blocks.HARVESTABLE_CROPS).addTag(TinkerTags.Blocks.HARVESTABLE_INTERACT).addTag(TinkerTags.Blocks.HARVESTABLE_STACKABLE);
    // just logs for lumber axe, but modpack makers can add more
    this.getOrCreateTagBuilder(TinkerTags.Blocks.TREE_LOGS).addOptionalTag(BlockTags.LOGS);
    // blocks that drop gold and should drop more gold
    this.getOrCreateTagBuilder(TinkerTags.Blocks.CHRYSOPHILITE_ORES).add(Blocks.GILDED_BLACKSTONE).addOptionalTag(Tags.Blocks.ORES_GOLD);
  }


  private void addWorld() {
    // ores
    this.getOrCreateTagBuilder(TinkerTags.Blocks.ORES_COBALT).add(TinkerWorld.cobaltOre.get());
    this.getOrCreateTagBuilder(Tags.Blocks.ORES).addTag(TinkerTags.Blocks.ORES_COBALT);
    this.getOrCreateTagBuilder(Tags.Blocks.ORES_IN_GROUND_NETHERRACK).add(TinkerWorld.cobaltOre.get());
    this.getOrCreateTagBuilder(Tags.Blocks.ORE_RATES_SINGULAR).add(TinkerWorld.cobaltOre.get());
    this.getOrCreateTagBuilder(TinkerTags.Blocks.RAW_BLOCK_COBALT).add(TinkerWorld.rawCobaltBlock.get());
    this.getOrCreateTagBuilder(Tags.Blocks.STORAGE_BLOCKS).add(TinkerToolParts.fakeStorageBlock.get()).addTag(TinkerTags.Blocks.RAW_BLOCK_COBALT);

    // allow the enderman to hold more blocks
    FabricTagBuilder endermanHoldable = this.getOrCreateTagBuilder(BlockTags.ENDERMAN_HOLDABLE);
    endermanHoldable.add(TinkerSmeltery.grout.get(), TinkerSmeltery.netherGrout.get()).addTag(TinkerTags.Blocks.CONGEALED_SLIME);

    // wood
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SLIMY_LOGS)
        .addTags(TinkerWorld.greenheart.getLogBlockTag(), TinkerWorld.skyroot.getLogBlockTag(), TinkerWorld.bloodshroom.getLogBlockTag(), TinkerWorld.enderbark.getLogBlockTag());
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SLIMY_PLANKS).add(TinkerWorld.greenheart.get(), TinkerWorld.skyroot.get(), TinkerWorld.bloodshroom.get(), TinkerWorld.enderbark.get());
    this.getOrCreateTagBuilder(BlockTags.PLANKS).addTag(TinkerTags.Blocks.SLIMY_PLANKS);
    this.getOrCreateTagBuilder(BlockTags.LOGS).addTag(TinkerTags.Blocks.SLIMY_LOGS);
    this.addWoodTags(TinkerWorld.greenheart, false);
    this.addWoodTags(TinkerWorld.skyroot, false);
    this.addWoodTags(TinkerWorld.bloodshroom, false);
    this.addWoodTags(TinkerWorld.enderbark, false);

    // slime blocks
    FabricTagBuilder slimeBlockTagAppender = this.getOrCreateTagBuilder(TinkerTags.Blocks.SLIME_BLOCK);
    FabricTagBuilder congealedTagAppender = this.getOrCreateTagBuilder(TinkerTags.Blocks.CONGEALED_SLIME);
    for (SlimeType type : SlimeType.values()) {
      slimeBlockTagAppender.add(TinkerWorld.slime.get(type));
      congealedTagAppender.add(TinkerWorld.congealedSlime.get(type));
    }

    // foliage
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SLIMY_VINES).add(TinkerWorld.skySlimeVine.get(), TinkerWorld.enderSlimeVine.get());
    FabricTagBuilder leavesTagAppender = this.getOrCreateTagBuilder(TinkerTags.Blocks.SLIMY_LEAVES);
    FabricTagBuilder wartTagAppender = this.getOrCreateTagBuilder(BlockTags.WART_BLOCKS);
    FabricTagBuilder saplingTagAppender = this.getOrCreateTagBuilder(TinkerTags.Blocks.SLIMY_SAPLINGS);
    for (FoliageType type : FoliageType.values()) {
      if (type.isNether()) {
        wartTagAppender.add(TinkerWorld.slimeLeaves.get(type));
        endermanHoldable.add(TinkerWorld.slimeSapling.get(type));
      } else {
        leavesTagAppender.add(TinkerWorld.slimeLeaves.get(type));
        saplingTagAppender.add(TinkerWorld.slimeSapling.get(type));
      }
    }
    this.getOrCreateTagBuilder(BlockTags.LEAVES).addTag(TinkerTags.Blocks.SLIMY_LEAVES);
    this.getOrCreateTagBuilder(BlockTags.SAPLINGS).addTag(TinkerTags.Blocks.SLIMY_SAPLINGS);

    TagAppender<Block> slimyGrass = this.getOrCreateTagBuilder(TinkerTags.Blocks.SLIMY_GRASS);
    TagAppender<Block> slimyNylium = this.getOrCreateTagBuilder(TinkerTags.Blocks.SLIMY_NYLIUM);
    TagAppender<Block> slimySoil = this.getOrCreateTagBuilder(TinkerTags.Blocks.SLIMY_SOIL);
    for (FoliageType type : FoliageType.values()) {
      (type.isNether() ? slimyNylium : slimyGrass).addTag(type.getGrassBlockTag());
    }
    for (DirtType type : DirtType.values()) {
      slimySoil.addTag(type.getBlockTag());
    }
    TinkerWorld.slimeGrass.forEach((dirtType, blockObj) -> blockObj.forEach((grassType, block) -> {
      this.getOrCreateTagBuilder(grassType.getGrassBlockTag()).add(block);
      this.getOrCreateTagBuilder(dirtType.getBlockTag()).add(block);
    }));
    TinkerWorld.slimeDirt.forEach((type, block) -> this.getOrCreateTagBuilder(type.getBlockTag()).add(block));
    FabricTagBuilder enderBarkRoots = this.getOrCreateTagBuilder(TinkerTags.Blocks.ENDERBARK_ROOTS).add(TinkerWorld.enderbarkRoots.get());
    TinkerWorld.slimyEnderbarkRoots.forEach((type, block) -> {
      this.getOrCreateTagBuilder(type.asDirt().getBlockTag()).add(block);
      enderBarkRoots.add(block);
    });
    endermanHoldable.addTag(TinkerTags.Blocks.SLIMY_SOIL);
    tagBlocks(BlockTags.SWORD_EFFICIENT, TinkerWorld.slimeTallGrass, TinkerWorld.slimeFern);
    tagBlocks(BlockTags.REPLACEABLE, TinkerWorld.slimeTallGrass, TinkerWorld.slimeFern);
    tagBlocks(BlockTags.REPLACEABLE_BY_TREES, TinkerWorld.slimeTallGrass, TinkerWorld.slimeFern);
    tagBlocks(BlockTags.AZALEA_ROOT_REPLACEABLE, TinkerWorld.slimeTallGrass, TinkerWorld.slimeFern);

    Consumer<Block> flowerPotAppender = this.getOrCreateTagBuilder(BlockTags.FLOWER_POTS)::add;
    TinkerWorld.pottedSlimeFern.forEach(flowerPotAppender);
    TinkerWorld.pottedSlimeSapling.forEach(flowerPotAppender);

    this.getOrCreateTagBuilder(TinkerTags.Blocks.ENDERBARK_LOGS_CAN_GROW_THROUGH)
        .addTags(TinkerTags.Blocks.SLIMY_VINES, TinkerTags.Blocks.SLIMY_SAPLINGS, TinkerTags.Blocks.CONGEALED_SLIME, TinkerTags.Blocks.ENDERBARK_ROOTS, TinkerTags.Blocks.SLIMY_LEAVES, TinkerTags.Blocks.SLIMY_LOGS);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.ENDERBARK_ROOTS_CAN_GROW_THROUGH)
        .add(Blocks.SNOW).addTags(TinkerTags.Blocks.SLIMY_VINES, TinkerTags.Blocks.SLIMY_SAPLINGS, TinkerTags.Blocks.CONGEALED_SLIME, TinkerTags.Blocks.ENDERBARK_ROOTS);
    // copy of the list of blocks used in vanilla fungus, which really should have been a tag in the first place
    // we use tags so it works with our slimy foliage too
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SLIMY_FUNGUS_CAN_GROW_THROUGH)
      .add(Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.SUGAR_CANE, Blocks.LILY_PAD, Blocks.NETHER_WART, Blocks.COCOA, Blocks.CHORUS_PLANT, Blocks.CHORUS_FLOWER,
             Blocks.SWEET_BERRY_BUSH, Blocks.WARPED_FUNGUS, Blocks.CRIMSON_FUNGUS, Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT, Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT,
             Blocks.SPORE_BLOSSOM, Blocks.MOSS_CARPET, Blocks.BIG_DRIPLEAF, Blocks.BIG_DRIPLEAF_STEM, Blocks.SMALL_DRIPLEAF,
             TinkerWorld.slimeTallGrass.get(FoliageType.ICHOR), TinkerWorld.slimeTallGrass.get(FoliageType.BLOOD),
             TinkerWorld.slimeFern.get(FoliageType.ICHOR), TinkerWorld.slimeFern.get(FoliageType.BLOOD)).addTags(BlockTags.SAPLINGS, BlockTags.FLOWERS, BlockTags.CROPS, BlockTags.CAVE_VINES);


    // slime spawns
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SKY_SLIME_SPAWN).add(TinkerWorld.skyGeode.getBlock(), TinkerWorld.skyGeode.getBudding()).addTag(FoliageType.SKY.getGrassBlockTag());
    this.getOrCreateTagBuilder(TinkerTags.Blocks.EARTH_SLIME_SPAWN).add(TinkerWorld.earthGeode.getBlock(), TinkerWorld.earthGeode.getBudding()).addTag(FoliageType.EARTH.getGrassBlockTag());
    this.getOrCreateTagBuilder(TinkerTags.Blocks.ENDER_SLIME_SPAWN).add(TinkerWorld.enderGeode.getBlock(), TinkerWorld.enderGeode.getBudding()).addTag(FoliageType.ENDER.getGrassBlockTag());

    // budding tag
    getOrCreateTagBuilder(TinkerTags.Blocks.BUDDING).add(TinkerWorld.earthGeode.getBudding(), TinkerWorld.skyGeode.getBudding(), TinkerWorld.ichorGeode.getBudding(), TinkerWorld.enderGeode.getBudding());

    this.getOrCreateTagBuilder(BlockTags.GUARDED_BY_PIGLINS)
        .add(TinkerTables.castChest.get(), TinkerCommons.goldBars.get(), TinkerCommons.goldPlatform.get());
    // piglins are not a fan of zombie piglin corpses
    this.getOrCreateTagBuilder(BlockTags.PIGLIN_REPELLENTS)
        .add(TinkerWorld.heads.get(TinkerHeadType.ZOMBIFIED_PIGLIN), TinkerWorld.wallHeads.get(TinkerHeadType.ZOMBIFIED_PIGLIN));

    // stone variants
    this.getOrCreateTagBuilder(TinkerTags.Blocks.STONE).add(Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.GRANITE).add(Blocks.GRANITE);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.DIORITE).add(Blocks.DIORITE);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.ANDESITE).add(Blocks.ANDESITE);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.BLACKSTONE).add(Blocks.BLACKSTONE);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.DEEPSLATE).add(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.BASALT).add(Blocks.BASALT);
  }

  private void addSmeltery() {
    // seared
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SEARED_BRICKS).add(
      TinkerSmeltery.searedBricks.get(),
      TinkerSmeltery.searedFancyBricks.get(),
      TinkerSmeltery.searedTriangleBricks.get());
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SEARED_BLOCKS)
        .add(TinkerSmeltery.searedStone.get(), TinkerSmeltery.searedCrackedBricks.get(), TinkerSmeltery.searedCobble.get(), TinkerSmeltery.searedPaver.get()).addTag(TinkerTags.Blocks.SEARED_BRICKS);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SMELTERY_BRICKS).addTag(TinkerTags.Blocks.SEARED_BLOCKS);
    this.getOrCreateTagBuilder(BlockTags.WALLS).add(TinkerSmeltery.searedBricks.getWall(), TinkerSmeltery.searedCobble.getWall());

    // scorched
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SCORCHED_BLOCKS).add(
      TinkerSmeltery.scorchedStone.get(),
      TinkerSmeltery.polishedScorchedStone.get(),
      TinkerSmeltery.scorchedBricks.get(),
      TinkerSmeltery.scorchedRoad.get(),
      TinkerSmeltery.chiseledScorchedBricks.get());
    this.getOrCreateTagBuilder(TinkerTags.Blocks.FOUNDRY_BRICKS).addTag(TinkerTags.Blocks.SCORCHED_BLOCKS);
    this.getOrCreateTagBuilder(BlockTags.FENCES).add(TinkerSmeltery.scorchedBricks.getFence(), TinkerMaterials.blazewood.getFence(), TinkerMaterials.nahuatl.getFence());

    this.getOrCreateTagBuilder(TinkerTags.Blocks.CISTERN_CONNECTIONS)
        // cannot add channels as it requires a block state property to properly detect, look into a way to fix this later
        .add(TinkerSmeltery.searedFaucet.get(), TinkerSmeltery.scorchedFaucet.get());

    // tanks
    FabricTagBuilder searedTankTagAppender = this.getOrCreateTagBuilder(TinkerTags.Blocks.SEARED_TANKS);
    TinkerSmeltery.searedTank.values().forEach(searedTankTagAppender::add);
    FabricTagBuilder scorchedTankTagAppender = this.getOrCreateTagBuilder(TinkerTags.Blocks.SCORCHED_TANKS);
    TinkerSmeltery.scorchedTank.values().forEach(scorchedTankTagAppender::add);

    // gauges
    this.getOrCreateTagBuilder(MantleTags.Blocks.ATTACHED_GAUGES).add(TinkerSmeltery.copperGauge.get(), TinkerSmeltery.obsidianGauge.get());

    // structure tags
    // melter supports the heater as a tank
    this.getOrCreateTagBuilder(TinkerTags.Blocks.HEATER_CONTROLLERS)
        .add(TinkerSmeltery.searedMelter.get(), TinkerSmeltery.scorchedAlloyer.get());
    this.getOrCreateTagBuilder(TinkerTags.Blocks.FUEL_TANKS)
        .add(TinkerSmeltery.searedHeater.get()).addTag(TinkerTags.Blocks.SEARED_TANKS).addTag(TinkerTags.Blocks.SCORCHED_TANKS);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SMELTERY_TANKS).addTag(TinkerTags.Blocks.SEARED_TANKS);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.FOUNDRY_TANKS).addTag(TinkerTags.Blocks.SCORCHED_TANKS);
    this.getOrCreateTagBuilder(TinkerTags.Blocks.ALLOYER_TANKS)
        .add(TinkerSmeltery.scorchedAlloyer.get(), TinkerSmeltery.searedMelter.get()).addTag(TinkerTags.Blocks.SEARED_TANKS).addTag(TinkerTags.Blocks.SCORCHED_TANKS);

    // blocks to ignore like air
    this.getOrCreateTagBuilder(TinkerTags.Blocks.STRUCTURE_AIR).add(Blocks.LIGHT, TinkerCommons.glowBlock.get());

    // smeltery blocks
    // floor allows any basic seared blocks and all IO blocks
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SMELTERY_FLOOR)
        .add(TinkerSmeltery.searedLamp.get(), TinkerSmeltery.searedDrain.get(), TinkerSmeltery.searedChute.get(), TinkerSmeltery.searedDuct.get()).addTag(TinkerTags.Blocks.SEARED_BLOCKS);
    // wall allows seared blocks, tanks, glass, and IO
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SMELTERY_WALL)
        .add(TinkerSmeltery.searedGlass.get(), TinkerSmeltery.searedSoulGlass.get(), TinkerSmeltery.searedTintedGlass.get(),
             TinkerSmeltery.searedLadder.get(), TinkerSmeltery.searedLamp.get(),
             TinkerSmeltery.searedDrain.get(), TinkerSmeltery.searedChute.get(), TinkerSmeltery.searedDuct.get()).addTag(TinkerTags.Blocks.SEARED_BLOCKS).addTag(TinkerTags.Blocks.SMELTERY_TANKS);
    // smeltery allows any of the three
    this.getOrCreateTagBuilder(TinkerTags.Blocks.SMELTERY)
        .addTag(TinkerTags.Blocks.SMELTERY_WALL)
        .addTag(TinkerTags.Blocks.SMELTERY_FLOOR)
        .addTag(TinkerTags.Blocks.SMELTERY_TANKS);

    // foundry blocks
    // floor allows any basic seared blocks and all IO blocks
    this.getOrCreateTagBuilder(TinkerTags.Blocks.FOUNDRY_FLOOR)
        .add(TinkerSmeltery.scorchedLamp.get(), TinkerSmeltery.scorchedDrain.get(), TinkerSmeltery.scorchedChute.get(), TinkerSmeltery.scorchedDuct.get()).addTag(TinkerTags.Blocks.SCORCHED_BLOCKS);
    // wall allows seared blocks, tanks, glass, and IO
    this.getOrCreateTagBuilder(TinkerTags.Blocks.FOUNDRY_WALL)
        .add(TinkerSmeltery.scorchedGlass.get(), TinkerSmeltery.scorchedSoulGlass.get(), TinkerSmeltery.scorchedTintedGlass.get(),
             TinkerSmeltery.scorchedLadder.get(), TinkerSmeltery.scorchedLamp.get(),
             TinkerSmeltery.scorchedDrain.get(), TinkerSmeltery.scorchedChute.get(), TinkerSmeltery.scorchedDuct.get()).addTag(TinkerTags.Blocks.SCORCHED_BLOCKS).addTag(TinkerTags.Blocks.FOUNDRY_TANKS);
    // foundry allows any of the three
    this.getOrCreateTagBuilder(TinkerTags.Blocks.FOUNDRY)
        .addTag(TinkerTags.Blocks.FOUNDRY_WALL)
        .addTag(TinkerTags.Blocks.FOUNDRY_FLOOR)
        .addTag(TinkerTags.Blocks.FOUNDRY_TANKS);

    // climb seared ladder
    this.getOrCreateTagBuilder(BlockTags.CLIMBABLE).add(TinkerSmeltery.searedLadder.get(), TinkerSmeltery.scorchedLadder.get());
    this.getOrCreateTagBuilder(BlockTags.DRAGON_IMMUNE).add(TinkerCommons.obsidianPane.get());
  }

  private void addFluids() {
    this.getOrCreateTagBuilder(BlockTags.STRIDER_WARM_BLOCKS).add(TinkerFluids.magma.getBlock(), TinkerFluids.blazingBlood.getBlock());
  }

  private void addHarvest() {
    // commons
    tagBlocks(MINEABLE_WITH_SHOVEL, TinkerCommons.cheeseBlock);
    tagBlocks(MINEABLE_WITH_AXE, TinkerGadgets.punji);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_DIAMOND_TOOL, TinkerCommons.obsidianPane);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_STONE_TOOL, TinkerCommons.ironPlatform);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_IRON_TOOL, TinkerCommons.goldBars, TinkerCommons.goldPlatform, TinkerCommons.cobaltPlatform);
    this.getOrCreateTagBuilder(MINEABLE_WITH_PICKAXE).addTag(TinkerTags.Blocks.COPPER_PLATFORMS);
    this.getOrCreateTagBuilder(NEEDS_STONE_TOOL).addTag(TinkerTags.Blocks.COPPER_PLATFORMS);

    // materials
    tagBlocks(MINEABLE_WITH_AXE, NEEDS_IRON_TOOL, TinkerMaterials.blazewood);
    tagBlocks(MINEABLE_WITH_AXE, NEEDS_DIAMOND_TOOL, TinkerMaterials.nahuatl);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_IRON_TOOL,
      TinkerWorld.cobaltOre, TinkerWorld.rawCobaltBlock, TinkerMaterials.steel, TinkerMaterials.cobalt,
      TinkerMaterials.slimesteel, TinkerMaterials.cinderslime, TinkerMaterials.amethystBronze,
      TinkerMaterials.roseGold, TinkerMaterials.pigIron, TinkerToolParts.fakeStorageBlock);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_DIAMOND_TOOL, TinkerMaterials.queensSlime, TinkerMaterials.manyullyn, TinkerMaterials.hepatizon, TinkerMaterials.soulsteel);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_NETHERITE_TOOL, TinkerMaterials.knightmetal, TinkerMaterials.knightslime);

    // slime
    tagBlocks(MINEABLE_WITH_SHOVEL, TinkerWorld.congealedSlime, TinkerWorld.slimeDirt, TinkerWorld.vanillaSlimeGrass, TinkerWorld.earthSlimeGrass, TinkerWorld.skySlimeGrass, TinkerWorld.enderSlimeGrass, TinkerWorld.ichorSlimeGrass);
    // harvest tiers on shovel blocks
    TinkerWorld.slimeDirt.forEach((type, block) -> this.getOrCreateTagBuilder(Objects.requireNonNull(type.getHarvestTier().getTag())).add(block));
    for (DirtType dirt : DirtType.values()) {
      for (FoliageType grass : FoliageType.values()) {
        Tiers dirtTier = dirt.getHarvestTier();
        Tiers grassTier = grass.getHarvestTier();
        // cannot use tier sorting registry as it's not init during datagen, stuck comparing levels and falling back to ordinal for gold
        Tiers tier;
        if (dirtTier.getLevel() == grassTier.getLevel()) {
          tier = dirtTier.ordinal() > grassTier.ordinal() ? dirtTier : grassTier;
        } else {
          tier = dirtTier.getLevel() > grassTier.getLevel() ? dirtTier : grassTier;
        }
        this.getOrCreateTagBuilder(Objects.requireNonNull(tier.getTag())).add(TinkerWorld.slimeGrass.get(dirt).get(grass));
      }
    }

    tagBlocks(MINEABLE_WITH_HOE, TinkerWorld.slimeLeaves);
    tagLogs(MINEABLE_WITH_AXE, NEEDS_GOLD_TOOL, TinkerWorld.skyroot);
    tagLogs(MINEABLE_WITH_AXE, NEEDS_STONE_TOOL, TinkerWorld.greenheart);
    tagLogs(MINEABLE_WITH_AXE, NEEDS_IRON_TOOL, TinkerWorld.bloodshroom);
    tagLogs(MINEABLE_WITH_AXE, NEEDS_DIAMOND_TOOL, TinkerWorld.enderbark);
    tagPlanks(MINEABLE_WITH_SHOVEL, TinkerWorld.greenheart, TinkerWorld.skyroot, TinkerWorld.bloodshroom, TinkerWorld.enderbark);
    tagPlanks(MINEABLE_WITH_AXE, true, TinkerWorld.greenheart, TinkerWorld.skyroot, TinkerWorld.bloodshroom, TinkerWorld.enderbark);
    tagBlocks(MINEABLE_WITH_SHOVEL, TinkerWorld.slimyEnderbarkRoots);
    tagBlocks(MINEABLE_WITH_AXE, TinkerWorld.skySlimeVine, TinkerWorld.enderSlimeVine, TinkerWorld.enderbarkRoots);
    tagBlocks(MINEABLE_WITH_AXE, TinkerWorld.slimeTallGrass, TinkerWorld.slimeFern);
    tagBlocks(MINEABLE_WITH_PICKAXE, TinkerWorld.earthGeode, TinkerWorld.skyGeode, TinkerWorld.ichorGeode, TinkerWorld.enderGeode);
    tagBlocks(MINEABLE_WITH_PICKAXE, TinkerWorld.steelCluster, TinkerWorld.cobaltCluster, TinkerWorld.knightmetalCluster);
    tagBlocks(NEEDS_DIAMOND_TOOL, TinkerWorld.enderbarkRoots);
    tagBlocks(NEEDS_DIAMOND_TOOL, TinkerWorld.slimyEnderbarkRoots);


    // smeltery
    tagBlocks(MINEABLE_WITH_SHOVEL, TinkerSmeltery.grout, TinkerSmeltery.netherGrout);
    // seared
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_GOLD_TOOL, TinkerSmeltery.searedStone, TinkerSmeltery.searedPaver, TinkerSmeltery.searedCobble, TinkerSmeltery.searedBricks);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_GOLD_TOOL, TinkerSmeltery.searedCrackedBricks, TinkerSmeltery.searedFancyBricks, TinkerSmeltery.searedTriangleBricks, TinkerSmeltery.searedLadder, TinkerSmeltery.searedLamp, TinkerSmeltery.searedGlass, TinkerSmeltery.searedSoulGlass, TinkerSmeltery.searedTintedGlass, TinkerSmeltery.searedGlassPane, TinkerSmeltery.searedSoulGlassPane);
    // scorched
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_GOLD_TOOL, TinkerSmeltery.scorchedBricks, TinkerSmeltery.scorchedRoad);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_GOLD_TOOL, TinkerSmeltery.scorchedStone, TinkerSmeltery.polishedScorchedStone, TinkerSmeltery.chiseledScorchedBricks, TinkerSmeltery.scorchedLadder, TinkerSmeltery.scorchedLamp, TinkerSmeltery.scorchedGlass, TinkerSmeltery.scorchedSoulGlass, TinkerSmeltery.scorchedTintedGlass, TinkerSmeltery.scorchedGlassPane, TinkerSmeltery.scorchedSoulGlassPane);
    // fluids
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_GOLD_TOOL, TinkerSmeltery.searedTank, TinkerSmeltery.scorchedTank);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_GOLD_TOOL, TinkerSmeltery.searedLantern,   TinkerSmeltery.searedFaucet,   TinkerSmeltery.searedChannel,   TinkerSmeltery.searedBasin,   TinkerSmeltery.searedTable,   TinkerSmeltery.searedCastingTank);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_GOLD_TOOL, TinkerSmeltery.scorchedLantern, TinkerSmeltery.scorchedFaucet, TinkerSmeltery.scorchedChannel, TinkerSmeltery.scorchedBasin, TinkerSmeltery.scorchedTable);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_GOLD_TOOL, TinkerSmeltery.searedHeater, TinkerSmeltery.searedMelter, TinkerSmeltery.scorchedAlloyer);
    // tough seared + scorched
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_STONE_TOOL, TinkerSmeltery.searedDrain, TinkerSmeltery.searedChute, TinkerSmeltery.smelteryController, TinkerSmeltery.searedFluidCannon, TinkerSmeltery.copperGauge);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_IRON_TOOL, TinkerSmeltery.searedDuct, TinkerSmeltery.scorchedDuct, TinkerSmeltery.scorchedFluidCannon);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_DIAMOND_TOOL, TinkerSmeltery.scorchedDrain, TinkerSmeltery.scorchedChute, TinkerSmeltery.scorchedProxyTank, TinkerSmeltery.foundryController, TinkerSmeltery.obsidianGauge, TinkerSmeltery.endFluidCannon);

    // tables
    tagBlocks(MINEABLE_WITH_AXE, TinkerTables.craftingStation, TinkerTables.tinkerStation, TinkerTables.partBuilder, TinkerTables.tinkersChest, TinkerTables.partChest);
    tagBlocks(MINEABLE_WITH_PICKAXE, TinkerTables.modifierWorktable);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_STONE_TOOL, TinkerTables.castChest);
    tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_IRON_TOOL, TinkerTables.tinkersAnvil, TinkerTables.scorchedAnvil);

    // custom tool harvest
    // mattock works on all shovel and natural axe
    getOrCreateTagBuilder(TinkerTags.Blocks.MINABLE_WITH_MATTOCK).add(
      Blocks.AZALEA, Blocks.BAMBOO, Blocks.GLOW_LICHEN, Blocks.VINE,
      Blocks.BEE_NEST, Blocks.BEEHIVE,
      Blocks.CARVED_PUMPKIN, Blocks.JACK_O_LANTERN, Blocks.PUMPKIN,
      Blocks.CHORUS_FLOWER, Blocks.CHORUS_PLANT, Blocks.COCOA,
      Blocks.BROWN_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM, Blocks.RED_MUSHROOM_BLOCK).addTags(MINEABLE_WITH_SHOVEL, BlockTags.LOGS);
    // pickadze is shovel or pickaxe
    getOrCreateTagBuilder(TinkerTags.Blocks.MINABLE_WITH_PICKADZE).addTags(MINEABLE_WITH_SHOVEL, MINEABLE_WITH_PICKAXE);
    // hand axe has a leaf bonus
    getOrCreateTagBuilder(TinkerTags.Blocks.MINABLE_WITH_HAND_AXE).addTags(MINEABLE_WITH_AXE, BlockTags.LEAVES);
    // scythe/kama does hoe or shear blocks
    getOrCreateTagBuilder(TinkerTags.Blocks.MINABLE_WITH_SHEARS)
      .add(Blocks.AZALEA, Blocks.COBWEB, Blocks.DRIED_KELP_BLOCK, Blocks.GLOW_LICHEN, Blocks.LILY_PAD, Blocks.REDSTONE_WIRE, Blocks.HANGING_ROOTS,
           Blocks.TRIPWIRE, Blocks.TWISTING_VINES_PLANT, Blocks.TWISTING_VINES, Blocks.VINE, Blocks.WEEPING_VINES_PLANT, Blocks.WEEPING_VINES).addTags(BlockTags.CAVE_VINES, BlockTags.LEAVES, BlockTags.WOOL, BlockTags.SAPLINGS, BlockTags.FLOWERS, BlockTags.CORAL_PLANTS);
    // scythe/kama does hoe or shear blocks
    getOrCreateTagBuilder(TinkerTags.Blocks.MINABLE_WITH_SCYTHE)
      .add(Blocks.KELP, Blocks.KELP_PLANT, Blocks.NETHER_WART, Blocks.SMALL_DRIPLEAF, Blocks.SUGAR_CANE).addTags(MINEABLE_WITH_HOE, TinkerTags.Blocks.MINABLE_WITH_SHEARS, TinkerTags.Blocks.MINABLE_WITH_SWORD, BlockTags.CROPS);
      // added by sword effective tag;
    // sword list is filled to best ability, but will be a bit inexact as vanilla uses materials, hopefully putting this tag under forge will get people to tag their blocks
    getOrCreateTagBuilder(TinkerTags.Blocks.MINABLE_WITH_SWORD).add(Blocks.COBWEB, Blocks.MOSS_BLOCK).addTags(BlockTags.SWORD_EFFICIENT);
    // dagger does hoe or sword blocks plus glass
    getOrCreateTagBuilder(TinkerTags.Blocks.MINABLE_WITH_DAGGER).add(Blocks.GLOWSTONE, Blocks.REDSTONE_LAMP, Blocks.SEA_LANTERN, Blocks.BEACON).addTags(MINEABLE_WITH_HOE, TinkerTags.Blocks.MINABLE_WITH_SWORD, Tags.Blocks.GLASS, Tags.Blocks.GLASS_PANES);

    // melting pan blacklist, basically anything that feels gross due to unsupported melting recipe
    tagBlocks(MINEABLE_MELTING_BLACKLIST, TinkerSmeltery.searedMelter, TinkerSmeltery.smelteryController, TinkerSmeltery.foundryController, TinkerSmeltery.searedLantern, TinkerSmeltery.scorchedLantern, TinkerSmeltery.searedFluidCannon, TinkerSmeltery.scorchedFluidCannon, TinkerSmeltery.endFluidCannon, TinkerSmeltery.searedCastingTank, TinkerSmeltery.scorchedProxyTank);
    tagBlocks(MINEABLE_MELTING_BLACKLIST, TinkerSmeltery.searedTank, TinkerSmeltery.scorchedTank);

    // copy of blocks list from FlowingFluid#canHoldFLuid
    getOrCreateTagBuilder(UNREPLACABLE_BY_LIQUID).add(Blocks.LADDER, Blocks.SUGAR_CANE, Blocks.BUBBLE_COLUMN, Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_GATEWAY, Blocks.STRUCTURE_VOID).addTags(BlockTags.SIGNS, BlockTags.DOORS);
  }

  @Override
  public String getName() {
    return "Tinkers Construct Block Tags";
  }

  /** Applies a tag to a set of suppliers */
  @SafeVarargs
  private void tagBlocks(TagKey<Block> tag, Supplier<? extends Block>... blocks) {
    FabricTagBuilder appender = this.getOrCreateTagBuilder(tag);
    for (Supplier<? extends Block> block : blocks) {
      appender.add(block.get());
    }
  }

  /** Applies a tag to a set of suppliers */
  private void tagBlocks(TagKey<Block> tag, GeodeItemObject... blocks) {
    FabricTagBuilder appender = this.getOrCreateTagBuilder(tag);
    for (GeodeItemObject geode : blocks) {
      appender.add(geode.getBlock());
      appender.add(geode.getBudding());
      for (BudSize size : BudSize.values()) {
        appender.add(geode.getBud(size));
      }
    }
  }

  /** Applies a set of tags to a block */
  @SuppressWarnings("SameParameterValue")
  private void tagBlocks(TagKey<Block> tag1, TagKey<Block> tag2, Supplier<? extends Block>... blocks) {
    tagBlocks(tag1, blocks);
    tagBlocks(tag2, blocks);
  }

  /** Applies a tag to a set of blocks */
  @SafeVarargs
  private void tagBlocks(TagKey<Block> tag, EnumObject<?,? extends Block>... blocks) {
    FabricTagBuilder appender = this.getOrCreateTagBuilder(tag);
    for (EnumObject<?,? extends Block> block : blocks) {
      block.forEach(b -> appender.add(b));
    }
  }

  /** Applies a tag to a set of blocks */
  @SafeVarargs
  private void tagBlocks(TagKey<Block> tag1, TagKey<Block> tag2, EnumObject<?,? extends Block>... blocks) {
    tagBlocks(tag1, blocks);
    tagBlocks(tag2, blocks);
  }

  /** Applies a set of tags to a block */
  private void tagBlocks(TagKey<Block> tag, BuildingBlockObject... blocks) {
    FabricTagBuilder appender = this.getOrCreateTagBuilder(tag);
    for (BuildingBlockObject block : blocks) {
      block.values().forEach(appender::add);
    }
  }

  /** Applies a set of tags to a block */
  @SuppressWarnings("SameParameterValue")
  private void tagBlocks(TagKey<Block> tag1, TagKey<Block> tag2, BuildingBlockObject... blocks) {
    tagBlocks(tag1, blocks);
    tagBlocks(tag2, blocks);
  }

  /** Applies a set of tags to either wood or logs from a block */
  @SuppressWarnings("SameParameterValue")
  private void tagLogs(TagKey<Block> tag1, TagKey<Block> tag2, WoodBlockObject... blocks) {
    for (WoodBlockObject block : blocks) {
      getOrCreateTagBuilder(tag1).add(block.getLog(), block.getWood());
      getOrCreateTagBuilder(tag2).add(block.getLog(), block.getWood());
    }
  }

  /** Adds or removes the planks from the tag. */
  @SuppressWarnings("SameParameterValue")
  private void tagPlanks(TagKey<Block> tag, boolean remove, WoodBlockObject... blocks) {
    for (WoodBlockObject block : blocks) {
      Block[] update = {
        block.getSlab(), block.getStairs(), block.getFence(),
        block.getStrippedLog(), block.getStrippedWood(),
        block.getFenceGate(), block.getDoor(), block.getTrapdoor(),
        block.getPressurePlate(), block.getButton(),
        block.getSign(), block.getWallSign(), block.getHangingSign(), block.getWallHangingSign()
      };
      if (remove) {
        getOrCreateTagBuilder(tag);
      } else {
        getOrCreateTagBuilder(tag).add(block.get()).add(update);
      }
    }
  }

  /** Applies a set of tags to either wood or logs from a block */
  private void tagPlanks(TagKey<Block> tag, WoodBlockObject... blocks) {
    tagPlanks(tag, false, blocks);
  }

  /**
   * Adds relevant tags for a metal object
   * @param metal  Metal object
   */
  private void addMetalTags(MetalItemObject metal, boolean beacon) {
    this.getOrCreateTagBuilder(metal.getBlockTag()).add(metal.get());
    if (beacon) {
      this.getOrCreateTagBuilder(BlockTags.BEACON_BASE_BLOCKS).addTag(metal.getBlockTag());
    }
    this.getOrCreateTagBuilder(Tags.Blocks.STORAGE_BLOCKS).addTag(metal.getBlockTag());
  }

  /** Adds tags for a glass item object */
  private void addGlass(EnumObject<GlassColor,? extends Block> blockObj, String tagPrefix, FabricTagBuilder blockTag) {
    blockObj.forEach((color, block) -> {
      blockTag.add(block);
      this.getOrCreateTagBuilder(TagKey.create(Registries.BLOCK, commonResource(tagPrefix + color.getSerializedName()))).add(block);
    });
  }

  /** Adds all tags relevant to the given wood object */
  private void addWoodTags(WoodBlockObject object, boolean doesBurn) {
    // planks, handled by slimy planks tag
    //this.getOrCreateTagBuilder(BlockTags.PLANKS).add(object.get());
    this.getOrCreateTagBuilder(BlockTags.WOODEN_SLABS).add(object.getSlab());
    this.getOrCreateTagBuilder(BlockTags.WOODEN_STAIRS).add(object.getStairs());
    // logs
    this.getOrCreateTagBuilder(object.getLogBlockTag()).add(object.getLog(), object.getStrippedLog(), object.getWood(), object.getStrippedWood());

    // doors
    this.getOrCreateTagBuilder(BlockTags.WOODEN_FENCES).add(object.getFence());
    this.getOrCreateTagBuilder(Tags.Blocks.FENCES_WOODEN).add(object.getFence());
    this.getOrCreateTagBuilder(BlockTags.FENCE_GATES).add(object.getFenceGate());
    this.getOrCreateTagBuilder(Tags.Blocks.FENCE_GATES_WOODEN).add(object.getFenceGate());
    this.getOrCreateTagBuilder(BlockTags.WOODEN_DOORS).add(object.getDoor());
    this.getOrCreateTagBuilder(BlockTags.WOODEN_TRAPDOORS).add(object.getTrapdoor());
    // redstone
    this.getOrCreateTagBuilder(BlockTags.WOODEN_BUTTONS).add(object.getButton());
    this.getOrCreateTagBuilder(BlockTags.WOODEN_PRESSURE_PLATES).add(object.getPressurePlate());

    if (doesBurn) {
      // regular logs is handled by slimy logs tag
      this.getOrCreateTagBuilder(BlockTags.LOGS_THAT_BURN).addTag(object.getLogBlockTag());
    }

    // signs
    this.getOrCreateTagBuilder(BlockTags.STANDING_SIGNS).add(object.getSign());
    this.getOrCreateTagBuilder(BlockTags.WALL_SIGNS).add(object.getWallSign());
    this.getOrCreateTagBuilder(BlockTags.CEILING_HANGING_SIGNS).add(object.getHangingSign());
    this.getOrCreateTagBuilder(BlockTags.WALL_HANGING_SIGNS).add(object.getWallHangingSign());
  }
}
