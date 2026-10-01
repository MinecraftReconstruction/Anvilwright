package slimeknights.tconstruct.tools.logic;

import io.github.fabricators_of_create.porting_lib.attributes.PortingLibAttributes;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.Fall.FallEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.shared.TinkerAttributes;

/**
 * Logic to run the double jump attribute
 * <p>
 * <b>Porting note:</b> upstream registers these through Forge's {@code @EventBusSubscriber}/{@code @SubscribeEvent}; on Fabric
 * they are registered explicitly from {@link #init()}, which is called by {@link slimeknights.tconstruct.FabricEvents}.
 */
public class DoubleJumpHandler {
  private static final ResourceLocation JUMPS = TConstruct.getResource("jumps");

  private DoubleJumpHandler() {}

  /** Registers the jump listeners. Call this once during common setup. */
  public static void init() {
    LivingEntityEvents.JUMP.register(DoubleJumpHandler::onJump);
    LivingEntityEvents.FALL.register(DoubleJumpHandler::onLand);
  }

  /** Event handler to reset the number of times we have jumped in mid-air */
  static void onJump(LivingEntity living) {
    if (living.onGround() || (living.verticalCollision && !living.verticalCollisionBelow && living.getAttributeValue(PortingLibAttributes.ENTITY_GRAVITY) < 0)) {
      PersistentDataCapability.CAPABILITY.maybeGet(living).ifPresent(data -> data.remove(JUMPS));
    }
  }

  /** Event handler to reset the number of times we have jumped in mid air */
  static void onLand(FallEvent event) {
    PersistentDataCapability.CAPABILITY.maybeGet(event.getEntity()).ifPresent(data -> data.remove(JUMPS));
  }

  /**
   * Causes the player to jump an extra time, if possible
   * @param entity  Entity instance who wishes to jump again
   * @return  True if the entity jumpped, false if not
   */
  public static boolean extraJump(Player entity) {
    // validate preconditions, no using when swimming, elytra, or on the ground
    if (!entity.onGround() && !entity.onClimbable() && !entity.isInWaterOrBubble()) {
      // determine max jumps
      int extraJumps = Mth.floor(TinkerAttributes.getValue(entity, TinkerAttributes.JUMP_COUNT.get())) - 1;
      if (extraJumps > 0) {
        // check that we can take more jumps
        ModDataNBT data = new ModDataNBT(PersistentDataCapability.getOrWarn(entity));
        int jumps = data.getInt(JUMPS);
        if (jumps < extraJumps) {
          // actually jump, this method is nice enough to work in air
          entity.jumpFromGround();
          RandomSource random = entity.getCommandSenderWorld().getRandom();
          for (int i = 0; i < 4; i++) {
            entity.getCommandSenderWorld().addParticle(ParticleTypes.HAPPY_VILLAGER, entity.getX() - 0.25f + random.nextFloat() * 0.5f, entity.getY(), entity.getZ() - 0.25f + random.nextFloat() * 0.5f, 0, 0, 0);
          }
          entity.playSound(Sounds.EXTRA_JUMP.getSound(), 0.5f, 0.5f);
          data.putInt(JUMPS, jumps + 1);
          return true;
        }
      }
    }
    return false;
  }
}
