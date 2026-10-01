package slimeknights.tconstruct.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.events.KnockbackEvent;

/**
 * Fires {@link KnockbackEvent} at the head of {@link LivingEntity#knockback(double, double, double)}, standing in for the
 * Forge patch that upstream Tinkers' Construct relies on.
 * <p>
 * When a listener modifies the event we cancel vanilla and replay its body with the new values. The replay is copied from
 * vanilla 1.20.1; if Mojang changes {@code knockback} this has to be updated in lockstep.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityKnockbackMixin {
  @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
  private void tconstruct$onKnockback(double strength, double ratioX, double ratioZ, CallbackInfo ci) {
    LivingEntity self = (LivingEntity) (Object) this;
    KnockbackEvent event = new KnockbackEvent(self, strength, ratioX, ratioZ);
    KnockbackEvent.EVENT.invoker().onKnockback(event);
    // nothing touched the event, let vanilla run untouched
    if (!event.isModified()) {
      return;
    }
    // always take over from here, so a cancelled or reduced-to-nothing knockback behaves like vanilla
    ci.cancel();
    if (event.isCanceled()) {
      return;
    }

    // copied from LivingEntity#knockback
    double newStrength = event.getStrength() * (1.0 - self.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    if (newStrength <= 0.0) {
      return;
    }
    self.hasImpulse = true;
    Vec3 motion = self.getDeltaMovement();
    Vec3 direction = new Vec3(event.getRatioX(), 0.0, event.getRatioZ()).normalize().scale(newStrength);
    self.setDeltaMovement(motion.x / 2.0 - direction.x,
                          self.onGround() ? Math.min(0.4, motion.y / 2.0 + newStrength) : motion.y,
                          motion.z / 2.0 - direction.z);
  }
}
