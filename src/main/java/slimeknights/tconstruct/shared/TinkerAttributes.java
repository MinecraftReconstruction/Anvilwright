package slimeknights.tconstruct.shared;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.common.config.Config;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import slimeknights.mantle.registration.deferred.AttributeDeferredRegister;
import slimeknights.tconstruct.TConstruct;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;

public class TinkerAttributes {
  private static final AttributeDeferredRegister ATTRIBUTES = new AttributeDeferredRegister(TConstruct.MOD_ID);

  /**
   * Reads an attribute value, returning 0 when the entity does not carry the attribute.
   * <p>
   * Fabric's attribute registry is per entity type and cannot express "every entity" (see
   * BEHAVIOUR-DIFFERENCES #6), so our attributes only exist on the player and TCon's own entities. Any mob that
   * jumps or takes fall damage still runs through these code paths, so they have to tolerate a missing attribute.
   */
  public static double getValue(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
    net.minecraft.world.entity.ai.attributes.AttributeInstance instance = entity.getAttribute(attribute);
    return instance == null ? 0 : instance.getValue();
  }

  public TinkerAttributes() {
    ATTRIBUTES.register();
    registerEntityAttributes();
  }

  /**
   * Attaches the TCon attributes to the player.
   * <p>
   * <b>Porting note:</b> upstream adds every attribute below to <em>all</em> living entity types through Forge's
   * EntityAttributeModificationEvent. Fabric's FabricDefaultAttributeRegistry is per entity type and must supply the
   * complete set of defaults, so it cannot express "all entities". The player is registered in full here (which is
   * what the armor and modifier code needs) and TCon's own entities get the relevant ones in
   * {@code TinkerWorld#registerAttributes}; entities from other mods do not receive them. Disclosed in
   * docs/BEHAVIOUR-DIFFERENCES.md.
   */
  public static void registerEntityAttributes() {
    AttributeSupplier.Builder player = Player.createAttributes()
      .add(BOUNCY.get(), 0)
      .add(PROTECTION_CAP.get(), 0.8)
      .add(CHRYSOPHILITE.get(), 0)
      .add(CRITICAL_DAMAGE.get(), 1.5)
      .add(JUMP_BOOST.get(), 0)
      .add(SAFE_FALL_DISTANCE.get(), 0)
      .add(JUMP_COUNT.get(), 1)
      .add(KNOCKBACK_MULTIPLIER.get(), 1)
      .add(MINING_SPEED_MULTIPLIER.get(), 1)
      .add(EXPERIENCE_MULTIPLIER.get(), 1)
      .add(CROUCH_DAMAGE_MULTIPLIER.get(), 1)
      .add(GOOD_EFFECT_DURATION.get(), 1)
      .add(BAD_EFFECT_DURATION.get(), 1);
    FabricDefaultAttributeRegistry.register(EntityType.PLAYER, player);
  }

  // booleans
  /** If true, the entity will bounce. Used to implement slime boots */
  public static final RegistryObject<Attribute> BOUNCY = ATTRIBUTES.registerPercent("generic.bouncy", 0f, true);

  // stats
  /** Changes the speed debuff percentage when the player moves while using an item */
  public static final RegistryObject<Attribute> USE_ITEM_SPEED = ATTRIBUTES.registerPercent("player.use_item_speed", 0.2f, true);
  /** Changes the speed debuff when the player moves while using an item */
  public static final RegistryObject<Attribute> PROTECTION_CAP = ATTRIBUTES.register("generic.protection_cap", 0.8, 0, 0.95f, true);
  /** Percentage boost to critical hits for any airborne attacker, used for {@link slimeknights.tconstruct.tools.data.ModifierIds#dragonborn} */
  public static final RegistryObject<Attribute> CRITICAL_DAMAGE = ATTRIBUTES.register("player.critical_damage", 1.5f, 0, 100, false);
  /** Loot bonus for {@link slimeknights.tconstruct.tools.data.ModifierIds#chrysophilite} */
  public static final RegistryObject<Attribute> CHRYSOPHILITE = ATTRIBUTES.register("generic.chrysophilite", 0, 0, 100, false);

  // stat bonuses
  /** Bonus jump height in blocks */
  public static final RegistryObject<Attribute> JUMP_BOOST = ATTRIBUTES.register("generic.jump_boost", 0, 0, 100, true);
  /** Distance you can safely fall without damage */
  public static final RegistryObject<Attribute> SAFE_FALL_DISTANCE = ATTRIBUTES.register("generic.safe_fall_distance", 0, -10, 100, true);
  /** Number of jumps the player may perform, used by the double jump modifier. */
  public static final RegistryObject<Attribute> JUMP_COUNT = ATTRIBUTES.register("player.jump_count", 1, 1, 100, true);

  // stat multipliers
  /** Multiplier for knockback this entity takes. Similar to {@link net.minecraft.world.entity.ai.attributes.Attributes#KNOCKBACK_RESISTANCE} but can be used to increase knockback */
  public static final RegistryObject<Attribute> KNOCKBACK_MULTIPLIER = ATTRIBUTES.registerMultiplier("generic.knockback_multiplier", true);
  /** Player modifier data key for mining speed multiplier as an additive percentage boost on mining speed. Used for armor haste. */
  public static final RegistryObject<Attribute> MINING_SPEED_MULTIPLIER = ATTRIBUTES.registerMultiplier("player.mining_speed_multiplier", true);
  /** Attribute for experience from all sources */
  public static final RegistryObject<Attribute> EXPERIENCE_MULTIPLIER = ATTRIBUTES.registerMultiplier("player.experience_multiplier", false);
  /** Percentage boost to damage while crouching, used by {@link slimeknights.tconstruct.tools.data.ModifierIds#shulking} */
  public static final RegistryObject<Attribute> CROUCH_DAMAGE_MULTIPLIER = ATTRIBUTES.registerMultiplier("generic.crouch_damage_multiplier", false);
  // effect durations
  /** Percentage boost to positive potion effects */
  public static final RegistryObject<Attribute> GOOD_EFFECT_DURATION = ATTRIBUTES.registerMultiplier("generic.good_effect_duration_multiplier", false);
  /** Percentage boost to negative potion effects, used for {@link slimeknights.tconstruct.tools.data.ModifierIds#magicProtection} */
  public static final RegistryObject<Attribute> BAD_EFFECT_DURATION = ATTRIBUTES.registerMultiplier("generic.bad_effect_duration_multiplier", false);


  /**
   * Makes knockback resistance sync to the client, which modifiers such as springing and flinging rely on.
   * <p>
   * NOTE(porting): an earlier port note claimed vanilla has no {@code Attribute#setSyncable}; that is wrong, vanilla
   * 1.20.1 has it and {@code ServerEntity} filters the synced attributes through {@code isClientSyncable()}. The call
   * is made when the config loads, since Porting Lib only fills in config values at that point.
   */
  public static void syncKnockbackResistance() {
    if (Config.COMMON.syncKnockbackResistance.get()) {
      Attributes.KNOCKBACK_RESISTANCE.setSyncable(true);
    }
  }
}
