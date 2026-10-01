package slimeknights.tconstruct.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import io.github.fabricators_of_create.porting_lib.util.LazyRegistrar;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import org.apache.logging.log4j.Logger;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.world.data.StructureRepalleter;
import slimeknights.tconstruct.world.worldgen.islands.IslandPiece;
import slimeknights.tconstruct.world.worldgen.islands.IslandStructure;
import slimeknights.tconstruct.world.worldgen.trees.ExtraRootVariantPlacer;
import slimeknights.tconstruct.world.worldgen.trees.SupplierBlockStateProvider;
import slimeknights.tconstruct.world.worldgen.trees.LeaveVineDecorator;
import slimeknights.tconstruct.world.worldgen.trees.config.SlimeFungusConfig;
import slimeknights.tconstruct.world.worldgen.trees.config.SlimeTreeConfig;
import slimeknights.tconstruct.world.worldgen.trees.feature.SlimeFungusFeature;
import slimeknights.tconstruct.world.worldgen.trees.feature.SlimeTreeFeature;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.block.SlimeVineBlock;
import slimeknights.tconstruct.world.block.SlimeVineBlock.VineStage;
import slimeknights.tconstruct.world.block.FoliageType;

/**
 * Contains any logic relevant to structure generation, including trees and islands
 */
@SuppressWarnings("unused")
public final class TinkerStructures extends TinkerModule {
  static final Logger log = Util.getLogger("tinker_structures");
  private static final LazyRegistrar<Feature<?>> FEATURES = LazyRegistrar.create(Registries.FEATURE, TConstruct.MOD_ID);
  private static final LazyRegistrar<StructureType<?>> STRUCTURE_TYPE = LazyRegistrar.create(Registries.STRUCTURE_TYPE, TConstruct.MOD_ID);
  private static final LazyRegistrar<StructurePieceType> STRUCTURE_PIECE = LazyRegistrar.create(Registries.STRUCTURE_PIECE, TConstruct.MOD_ID);
  private static final LazyRegistrar<TreeDecoratorType<?>> TREE_DECORATORS = LazyRegistrar.create(Registries.TREE_DECORATOR_TYPE, TConstruct.MOD_ID);
  private static final LazyRegistrar<RootPlacerType<?>> ROOT_PLACERS = LazyRegistrar.create(Registries.ROOT_PLACER_TYPE, TConstruct.MOD_ID);
  private static final LazyRegistrar<BlockStateProviderType<?>> BLOCK_STATE_PROVIDERS = LazyRegistrar.create(Registries.BLOCK_STATE_PROVIDER_TYPE, TConstruct.MOD_ID);


  public TinkerStructures() {
    // Fabric has no mod event bus: register() with no arguments installs into the vanilla registries
    FEATURES.register();
    STRUCTURE_TYPE.register();
    STRUCTURE_PIECE.register();
    TREE_DECORATORS.register();
    ROOT_PLACERS.register();
    BLOCK_STATE_PROVIDERS.register();
  }


  /*
   * Misc
   */
  public static final RegistryObject<TreeDecoratorType<LeaveVineDecorator>> leaveVineDecorator = TREE_DECORATORS.register("leave_vines", () -> new TreeDecoratorType<>(LeaveVineDecorator.CODEC));
  public static final RegistryObject<RootPlacerType<ExtraRootVariantPlacer>> extraRootVariantPlacer = ROOT_PLACERS.register("extra_root_variants", () -> new RootPlacerType<>(ExtraRootVariantPlacer.CODEC));
  /** Fork content: block state provider used by the slime geodes (upstream removed the feature) */
  public static final RegistryObject<BlockStateProviderType<SupplierBlockStateProvider>> supplierBlockstateProvider = BLOCK_STATE_PROVIDERS.register("supplier", () -> new BlockStateProviderType<>(SupplierBlockStateProvider.CODEC));

  /*
   * Features
   */
  /** Overworld variant of slimy trees */
  public static final RegistryObject<SlimeTreeFeature> slimeTree = FEATURES.register("slime_tree", () -> new SlimeTreeFeature(SlimeTreeConfig.CODEC));
  /** Nether variant of slimy trees */
  public static final RegistryObject<SlimeFungusFeature> slimeFungus = FEATURES.register("slime_fungus", () -> new SlimeFungusFeature(SlimeFungusConfig.CODEC));

  /* Greenheart trees */
  public static final ResourceKey<ConfiguredFeature<?,?>> earthSlimeTree = key(Registries.CONFIGURED_FEATURE, "earth_slime_tree");
  public static final ResourceKey<ConfiguredFeature<?,?>> earthSlimeIslandTree = key(Registries.CONFIGURED_FEATURE, "earth_slime_island_tree");
  /* Skyroot trees */
  public static final ResourceKey<ConfiguredFeature<?,?>> skySlimeTree = key(Registries.CONFIGURED_FEATURE, "sky_slime_tree");
  public static final ResourceKey<ConfiguredFeature<?,?>> skySlimeIslandTree = key(Registries.CONFIGURED_FEATURE, "sky_slime_island_tree");

  /* Enderslime trees */
  public static final ResourceKey<ConfiguredFeature<?,?>> enderSlimeTree = key(Registries.CONFIGURED_FEATURE, "ender_slime_tree");
  public static final ResourceKey<ConfiguredFeature<?,?>> enderSlimeTreeTall = key(Registries.CONFIGURED_FEATURE, "ender_slime_tree_tall");

  /* Bloodshroom trees */
  public static final ResourceKey<ConfiguredFeature<?,?>> bloodSlimeFungus = key(Registries.CONFIGURED_FEATURE, "blood_slime_fungus");
  public static final ResourceKey<ConfiguredFeature<?,?>> bloodSlimeIslandFungus = key(Registries.CONFIGURED_FEATURE, "blood_slime_island_fungus");

  /* Deprecated ichor tree */
  public static final ResourceKey<ConfiguredFeature<?,?>> ichorSlimeFungus = key(Registries.CONFIGURED_FEATURE, "ichor_slime_fungus");

  /*
   * Structures
   */
  public static final RegistryObject<StructurePieceType> islandPiece = STRUCTURE_PIECE.register("island", () -> IslandPiece::new);
  public static final RegistryObject<StructureType<IslandStructure>> island = STRUCTURE_TYPE.register("island", () -> () -> IslandStructure.CODEC);


  // island structures - TODO 1.21: rename to better match placement?
  public static final ResourceKey<Structure> earthSlimeIsland = key(Registries.STRUCTURE, "earth_slime_island");
  public static final ResourceKey<Structure> skySlimeIsland = key(Registries.STRUCTURE, "sky_slime_island");
  public static final ResourceKey<Structure> oceanSkyslimeIsland = key(Registries.STRUCTURE, "ocean_skyslime_island");
  public static final ResourceKey<Structure> clayIsland = key(Registries.STRUCTURE, "clay_island");
  public static final ResourceKey<Structure> bloodIsland = key(Registries.STRUCTURE, "blood_island");
  public static final ResourceKey<Structure> endSlimeIsland = key(Registries.STRUCTURE, "end_slime_island");

  // island structure sets
  public static final ResourceKey<StructureSet> overworldOceanIsland = key(Registries.STRUCTURE_SET, "overworld_ocean_island");
  public static final ResourceKey<StructureSet> overworldSkyIsland = key(Registries.STRUCTURE_SET, "overworld_sky_island");
  public static final ResourceKey<StructureSet> netherOceanIsland = key(Registries.STRUCTURE_SET, "nether_ocean_island");
  public static final ResourceKey<StructureSet> endSkyIsland = key(Registries.STRUCTURE_SET, "end_sky_island");


  // NOTE(porting): upstream registers StructureRepalleter through Forge's GatherDataEvent here. Fabric drives
  // datagen through TConstructData/FabricDataGenerator instead, so this listener is not needed.

  public static void bootstrapConfigured(BootstapContext<ConfiguredFeature<?, ?>> bootstapContext) {
    BlockPredicate blockPredicate = BlockPredicate.replaceable();
    FeatureUtils.register(bootstapContext, earthSlimeTree, slimeTree.get(),
      new SlimeTreeConfig.Builder()
        .planted()
        .trunk(TinkerWorld.greenheart.getLog())
        .leaves(TinkerWorld.slimeLeaves.get(FoliageType.EARTH))
        .baseHeight(4).randomHeight(3)
        .build());
    FeatureUtils.register(bootstapContext, earthSlimeIslandTree, slimeTree.get(),
      new SlimeTreeConfig.Builder()
        .trunk(TinkerWorld.greenheart.getLog())
        .leaves(TinkerWorld.slimeLeaves.get(FoliageType.EARTH))
        .baseHeight(4).randomHeight(3)
        .build());
    FeatureUtils.register(bootstapContext, skySlimeTree, slimeTree.get(),
      new SlimeTreeConfig.Builder()
        .planted().canDoubleHeight()
        .trunk(TinkerWorld.skyroot.getLog())
        .leaves(TinkerWorld.slimeLeaves.get(FoliageType.SKY))
        .build());
    FeatureUtils.register(bootstapContext, skySlimeIslandTree, slimeTree.get(),
      new SlimeTreeConfig.Builder()
        .canDoubleHeight()
        .trunk(TinkerWorld.skyroot.getLog())
        .leaves(TinkerWorld.slimeLeaves.get(FoliageType.SKY))
        .vines(TinkerWorld.skySlimeVine.get().defaultBlockState().setValue(SlimeVineBlock.STAGE, VineStage.MIDDLE))
        .build());
    FeatureUtils.register(bootstapContext, enderSlimeTree, slimeTree.get(),
      new SlimeTreeConfig.Builder()
        .planted()
        .trunk(TinkerWorld.greenheart.getLog()) // TODO: temporary until we have proper green trees and ender shrooms
        .leaves(TinkerWorld.slimeLeaves.get(FoliageType.ENDER))
        .build());
    FeatureUtils.register(bootstapContext, enderSlimeTreeTall, slimeTree.get(),
      new SlimeTreeConfig.Builder()
        .trunk(TinkerWorld.greenheart.getLog()) // TODO: temporary until we have proper green trees and ender shrooms
        .leaves(TinkerWorld.slimeLeaves.get(FoliageType.ENDER))
        .vines(TinkerWorld.enderSlimeVine.get().defaultBlockState().setValue(SlimeVineBlock.STAGE, VineStage.MIDDLE))
        .build());
    FeatureUtils.register(bootstapContext, bloodSlimeFungus, slimeFungus.get(),
      new SlimeFungusConfig(
        TinkerTags.Blocks.SLIMY_SOIL,
        TinkerWorld.bloodshroom.getLog().defaultBlockState(),
        TinkerWorld.slimeLeaves.get(FoliageType.BLOOD).defaultBlockState(),
        TinkerWorld.congealedSlime.get(SlimeType.ICHOR).defaultBlockState(),
        blockPredicate,
        true));
    FeatureUtils.register(bootstapContext, bloodSlimeIslandFungus, slimeFungus.get(),
      new SlimeFungusConfig(
        TinkerTags.Blocks.SLIMY_NYLIUM,
        TinkerWorld.bloodshroom.getLog().defaultBlockState(),
        TinkerWorld.slimeLeaves.get(FoliageType.BLOOD).defaultBlockState(),
        TinkerWorld.congealedSlime.get(SlimeType.ICHOR).defaultBlockState(),
        blockPredicate,
        false));
    FeatureUtils.register(bootstapContext, ichorSlimeFungus, slimeFungus.get(),
      new SlimeFungusConfig(
        TinkerTags.Blocks.SLIMY_SOIL,
        TinkerWorld.bloodshroom.getLog().defaultBlockState(),
        TinkerWorld.slimeLeaves.get(FoliageType.ICHOR).defaultBlockState(),
        TinkerWorld.congealedSlime.get(SlimeType.ICHOR).defaultBlockState(),
        blockPredicate,
        false));
  }
}
