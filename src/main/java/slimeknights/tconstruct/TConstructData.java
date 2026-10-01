package slimeknights.tconstruct;

import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerClient;
import slimeknights.tconstruct.common.data.DamageTypeProvider;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.data.WorldgenProvider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

public class TConstructData implements DataGeneratorEntrypoint {

  @Override
  public void onInitializeDataGenerator(FabricDataGenerator generator) {
    ExistingFileHelper helper = createExistingFileHelper();
    // the armor model provider serializes the armor texture loaders, and the datagen entrypoint never runs the
    // client initializer that registers them
    TinkerClient.registerArmorTextureLoaders();
    TinkerClient.registerModifierModelLoaders();
    FabricDataGenerator.Pack pack = generator.createPack();
    TConstruct.gatherData(pack, helper);
    TinkerSmeltery.gatherData(pack);
    TinkerModifiers.gatherData(pack, helper);

    TinkerTools.gatherData(pack, helper);
    TinkerFluids.gatherData(pack, helper);
    TinkerWorld.gatherData(pack);
    TinkerGadgets.gatherData(pack);
    TinkerCommons.gatherData(pack, helper);
    TinkerTables.gatherData(pack);
  }

  /**
   * Creates the existing file helper used by the model providers.
   * <p>
   * NOTE(porting): the {@code porting_lib.datagen.existing_resources} argument only covers this repository, but the
   * Tinkers models inherit from Porting Lib's {@code forge:item} models (and use its {@code forge:item/mask} sprites),
   * so every one of them was reported as missing. Feed the Porting Lib jars to the helper as well.
   */
  private static ExistingFileHelper createExistingFileHelper() {
    List<Path> paths = new ArrayList<>();
    String existing = System.getProperty(ExistingFileHelper.EXISTING_RESOURCES);
    if (existing != null) {
      paths.add(Path.of(existing));
    }
    try {
      paths.add(extractPortingLibResources());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to unpack the Porting Lib resources for the model providers", e);
    }
    return ExistingFileHelper.withResources(paths.toArray(Path[]::new));
  }

  /**
   * Unpacks Porting Lib's resources into a temporary pack root.
   * <p>
   * The Porting Lib mod container is a zip file system in development, and the existing file helper only accepts plain
   * paths, so its resources have to be copied somewhere it can read them.
   */
  private static Path extractPortingLibResources() throws IOException {
    for (Path root : FabricLoader.getInstance().getModContainer("porting_lib_base").orElseThrow().getRootPaths()) {
      // in a development environment the mod is a plain directory, and in production it sits inside a zip that the
      // existing file helper cannot read, so unpack it
      Path target = Files.createTempDirectory("tconstruct-datagen-porting_lib_base");
      try (Stream<Path> entries = Files.walk(root)) {
        for (Path entry : entries.toList()) {
          Path destination = target.resolve(root.relativize(entry).toString());
          if (Files.isDirectory(entry)) {
            Files.createDirectories(destination);
          } else {
            Files.createDirectories(destination.getParent());
            Files.copy(entry, destination, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
      // a zip only ever has one root; a directory is returned as is so dev datagen does not copy anything
      if (!target.resolve("assets").toFile().exists()) {
        throw new IOException("Unpacked Porting Lib resources have no assets folder");
      }
      return target;
    }
    throw new IOException("Porting Lib has no root paths");
  }

  @Override
  public void buildRegistry(RegistrySetBuilder registryBuilder) {
    DamageTypeProvider.register(registryBuilder);
    // NOTE(porting): upstream also drives biome modifiers from here; Fabric covers those in code in WorldEvents,
    // and WorldgenProvider only emits the vanilla-worldgen registries.
    WorldgenProvider.register(registryBuilder);
  }
}
