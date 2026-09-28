package slimeknights.tconstruct.tools.modifiers.upgrades.general;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.LivingTickEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.TinkerHooks;
import slimeknights.tconstruct.library.modifiers.hook.PlantHarvestModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ShearsModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.TotalArmorLevelModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.List;

public class MagneticModifier extends TotalArmorLevelModifier implements PlantHarvestModifierHook, ShearsModifierHook {
  /** Player modifier data key for haste */
  private static final TinkerDataKey<Integer> MAGNET = TConstruct.createKey("magnet");

  public MagneticModifier() {
    super(MAGNET);
    LivingTickEvent.TICK.register(MagneticModifier::onLivingTick);
  }

  @Override
  public void afterBlockBreak(IToolStackView tool, int level, ToolHarvestContext context) {
    if (!context.isAOE()) {
      TinkerModifiers.magneticEffect.get().apply(context.getLiving(), 30, level - 1);
    }
  }

  @Override
  public int afterEntityHit(IToolStackView tool, int level, ToolAttackContext context, float damageDealt) {
    if (!context.isExtraAttack()) {
      TinkerModifiers.magneticEffect.get().apply(context.getAttacker(), 30, level - 1);
    }
    return 0;
  }

  @Override
  public void afterHarvest(IToolStackView tool, ModifierEntry modifier, UseOnContext context, ServerLevel world, BlockState state, BlockPos pos) {
    Player player = context.getPlayer();
    if (player != null) {
      TinkerModifiers.magneticEffect.get().apply(player, 30, modifier.getLevel() - 1);
    }
  }

  @Override
  public void afterShearEntity(IToolStackView tool, ModifierEntry modifier, Player player, Entity entity, boolean isTarget) {
    if (isTarget) {
      TinkerModifiers.magneticEffect.get().apply(player, 30, modifier.getLevel() - 1);
    }
  }

/** @deprecated use {@link MagneticEffect} or {@link MobEffectModule.ToolUsage} */
@Deprecated(forRemoval = true)
public class MagneticModifier extends Modifier {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addModule(MobEffectModule.builder(TinkerEffects.magnetic).time(RandomLevelingValue.flat(40)).buildToolUsage());
    hookBuilder.addModule(new ArmorLevelModule(TinkerDataKeys.MAGNET, false, null));
  }


  // armor

  /** Called to perform the magnet for armor */
  private static void onLivingTick(LivingTickEvent event) {
    // TOOD: this will run on any held armor that is also melee/harvest, is that a problem?
    LivingEntity entity = event.getEntity();
    if (!entity.isSpectator() && (entity.tickCount & 1) == 0) {
      int level = ModifierUtil.getTotalModifierLevel(entity, MAGNET);
      if (level > 0) {
        applyMagnet(entity, level);
      }
    }
  }

  /** Performs the magnetic effect */
  public static <T extends Entity> void applyVelocity(LivingEntity entity, int amplifier, Class<T> targetClass, int minRange, float speed, int maxPush) {
    // super magnetic - inspired by botanias code
    double x = entity.getX();
    double y = entity.getY();
    double z = entity.getZ();
    float range = minRange + amplifier;
    List<T> targets = entity.level().getEntitiesOfClass(targetClass, new AABB(x - range, y - range, z - range, x + range, y + range, z + range));

    // only pull up to a max targets
    int pulled = 0;
    for (T target : targets) {
      if (target.isRemoved()) {
        continue;
      }
      // calculate direction: item -> player
      Vec3 vec = entity.position()
                       .subtract(target.getX(), target.getY(), target.getZ())
                       .normalize()
                       .scale(speed * (amplifier + 1));
      if (!target.isNoGravity()) {
        vec = vec.add(0, 0.04f, 0);
      }

      // we calculated the movement vector and set it to the correct strength.. now we apply it \o/
      target.setDeltaMovement(target.getDeltaMovement().add(vec));

      pulled++;
      if (pulled > maxPush) {
        break;
      }
    }
  }

  /** @deprecated use {@link MagneticEffect#applyVelocity(Level, Vec3, int, Class, int, float, int)} */
  @Deprecated(forRemoval = true)
  public static <T extends Entity> void applyVelocity(Level level, Vec3 origin, int amplifier, Class<T> targetClass, int minRange, float speed, int maxPush) {
    MagneticEffect.applyVelocity(level, origin, amplifier, targetClass, minRange, speed, maxPush);
  }

  /** @deprecated use {@link MagneticEffect#applyMagnet(LivingEntity, int)} */
  @Deprecated(forRemoval = true)
  public static void applyMagnet(LivingEntity entity, int amplifier) {
    MagneticEffect.applyMagnet(entity, amplifier);
  }
}
