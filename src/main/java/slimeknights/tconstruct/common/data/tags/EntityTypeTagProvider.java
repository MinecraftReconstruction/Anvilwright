package slimeknights.tconstruct.common.data.tags;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.concurrent.CompletableFuture;

import static slimeknights.mantle.Mantle.commonResource;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.COLLECTABLES;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.DISCARDABLE_COLLECTABLES;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.NECROTIC_BLACKLIST;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.REFLECTING_BLACKLIST;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.REFLECTING_PRESERVE_OWNER;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.TRIDENTS;

public class EntityTypeTagProvider extends FabricTagProvider.EntityTypeTagProvider {

  public EntityTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  @SuppressWarnings("removal")
  @Override
  public void addTags(HolderLookup.Provider provider) {
    // mob classes
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.SLIMES).add(
      EntityType.SLIME, EntityType.MAGMA_CUBE,
      TinkerWorld.earthSlimeEntity.get(), TinkerWorld.skySlimeEntity.get(), TinkerWorld.enderSlimeEntity.get(), TinkerWorld.terracubeEntity.get()
    );
    // hostile
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.CREEPERS).add(EntityType.CREEPER);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.SPIDERS).add(EntityType.SPIDER, EntityType.CAVE_SPIDER);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.GUARDIANS).add(EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.SILVERFISH).add(EntityType.SILVERFISH);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.BLAZES).add(EntityType.BLAZE);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.GHASTS).add(EntityType.GHAST);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.PHANTOMS).add(EntityType.PHANTOM);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.SHULKERS).add(EntityType.SHULKER);
    // passive
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.AXOLOTLS).add(EntityType.AXOLOTL);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.BEES).add(EntityType.BEE);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.FROGS).add(EntityType.FROG);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.SQUIDS).add(EntityType.SQUID);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.STRIDERS).add(EntityType.STRIDER);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.TURTLES).add(EntityType.TURTLE);
    // villager
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.VILLAGERS).add(EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.ZOMBIE_VILLAGER);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.ILLAGERS).add(EntityType.EVOKER, EntityType.ILLUSIONER, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.WITCH);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.PIGLINS).add(EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN);

    // melting
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.MELTING_SHOW).add(EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.VILLAGER, EntityType.PLAYER);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.MELTING_HIDE)
      .add(EntityType.GIANT)
      .addTag(TinkerTags.EntityTypes.MELTING_BLACKLIST)
      .addOptionalTag(TinkerTags.HIDDEN_FROM_RECIPE_VIEWERS);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.MELTING_BLACKLIST);

    // meltable
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.MELTABLE_FARM_ANIMALS).add(
      EntityType.CHICKEN, EntityType.RABBIT,
      EntityType.COW, EntityType.MOOSHROOM,
      EntityType.PIG, EntityType.HOGLIN,
      EntityType.SHEEP, EntityType.GOAT,
      EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.MELTABLE_ZOMBIE).add(EntityType.ZOMBIE, EntityType.HUSK, EntityType.ZOMBIE_HORSE);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.MELTABLE_DROWNED).add(EntityType.DROWNED);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.MELTABLE_SKELETON).addTag(EntityTypeTags.SKELETONS).add(EntityType.SKELETON_HORSE);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.MELTABLE_ENDER).add(EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.ENDER_DRAGON);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.MELTABLE_SLIME).add(EntityType.SLIME);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.MELTABLE_MAGMA).add(EntityType.MAGMA_CUBE);

    // behavior
    this.getOrCreateTagBuilder(EntityTypeTags.FROG_FOOD).add(TinkerWorld.skySlimeEntity.get(), TinkerWorld.enderSlimeEntity.get(), TinkerWorld.terracubeEntity.get());
    this.getOrCreateTagBuilder(EntityTypeTags.ARROWS).add(TinkerTools.materialArrow.get());

    // compatability
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.BOBBERS).add(TinkerTools.fishingHook.get());

    // tool logic
    // players use tool daamge util
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.DAMAGE_MODIFIER_BLACKLIST).add(EntityType.PLAYER);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.PIGGYBACKPACK_BLACKLIST);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.SMALL_ARMOR).addTag(TinkerTags.EntityTypes.SLIMES);

    // projectile logic
    this.getOrCreateTagBuilder(TRIDENTS).add(EntityType.TRIDENT, TinkerTools.thrownTool.get());
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.REUSABLE_AMMO).addTag(TRIDENTS);

    // modifiers
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.KILLAGERS).add(EntityType.IRON_GOLEM, EntityType.RAVAGER).addTags(TinkerTags.EntityTypes.VILLAGERS, TinkerTags.EntityTypes.ILLAGERS);
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.BACON_PRODUCER).add(EntityType.PIG, EntityType.PIGLIN, EntityType.HOGLIN);
    // in theory this could just be reusable ammo, but it seems better to keep separate
    this.getOrCreateTagBuilder(TinkerTags.EntityTypes.ENDERFERENCE_ARROW_BLACKLIST).addTag(TRIDENTS);
    // prevent dummy from healing you with necrotic
    this.getOrCreateTagBuilder(NECROTIC_BLACKLIST)
      .addOptional(new ResourceLocation("dummmmmmy", "target_dummy"))
      .addOptionalTag(commonResource(NECROTIC_BLACKLIST.location().getPath()));

    // collecting - TODO 1.21: remove legacy tags
    this.getOrCreateTagBuilder(COLLECTABLES).add(
        EntityType.ITEM, TinkerTools.indestructibleItem.get(),
        EntityType.EXPERIENCE_ORB
      ).addTags(TRIDENTS, DISCARDABLE_COLLECTABLES)
      .addOptionalTag(commonResource(COLLECTABLES.location().getPath()));
    this.getOrCreateTagBuilder(DISCARDABLE_COLLECTABLES).add(EntityType.ARROW, EntityType.SPECTRAL_ARROW, TinkerTools.materialArrow.get())
      .addOptionalTag(commonResource(DISCARDABLE_COLLECTABLES.location().getPath()));

    // reflecting - TODO 1.21: remove legacy tags
    this.getOrCreateTagBuilder(REFLECTING_BLACKLIST).addOptionalTag(commonResource(REFLECTING_BLACKLIST.location().getPath()));
    this.getOrCreateTagBuilder(REFLECTING_PRESERVE_OWNER).add(EntityType.FISHING_BOBBER, TinkerTools.fishingHook.get())
      .addOptionalTag(commonResource(REFLECTING_PRESERVE_OWNER.location().getPath()));
  }

  @Override
  public String getName() {
    return "Tinkers Construct Entity Type TinkerTags";
  }
}
