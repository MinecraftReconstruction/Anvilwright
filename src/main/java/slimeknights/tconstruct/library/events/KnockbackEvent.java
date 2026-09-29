package slimeknights.tconstruct.library.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for Forge's {@code LivingKnockBackEvent}.
 * <p>
 * Forge fires its event from a patch inside {@link LivingEntity#knockback(double, double, double)}, which gives the event
 * access to both the entity <em>receiving</em> the knockback and the horizontal ratios used to direct it. Porting Lib's
 * {@code LivingEntityEvents.KNOCKBACK_STRENGTH} only exposes the strength and the attacking player, which is not enough
 * for Tinkers: {@code TinkerAttributes.KNOCKBACK_MULTIPLIER} is an attribute on the receiver, and crystalstrike needs the
 * ratios. So we fire our own event from {@link slimeknights.tconstruct.mixin.LivingEntityKnockbackMixin} instead.
 * <p>
 * Disclosed in {@code docs/BEHAVIOUR-DIFFERENCES.md}.
 */
public class KnockbackEvent {
  /** Fired whenever a living entity is knocked back. Listeners may modify any of the values or cancel the knockback entirely. */
  public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, callbacks -> event -> {
    for (Callback callback : callbacks) {
      callback.onKnockback(event);
    }
  });

  private final LivingEntity entity;
  private double strength;
  private double ratioX;
  private double ratioZ;
  private boolean canceled;
  private boolean modified;

  public KnockbackEvent(LivingEntity entity, double strength, double ratioX, double ratioZ) {
    this.entity = entity;
    this.strength = strength;
    this.ratioX = ratioX;
    this.ratioZ = ratioZ;
  }

  /** Entity being knocked back */
  public LivingEntity getEntity() {
    return entity;
  }

  /** Original knockback strength, before knockback resistance is applied */
  public double getStrength() {
    return strength;
  }

  public void setStrength(double strength) {
    this.strength = strength;
    this.modified = true;
  }

  /** X ratio of the direction the entity is knocked towards */
  public double getRatioX() {
    return ratioX;
  }

  public void setRatioX(double ratioX) {
    this.ratioX = ratioX;
    this.modified = true;
  }

  /** Z ratio of the direction the entity is knocked towards */
  public double getRatioZ() {
    return ratioZ;
  }

  public void setRatioZ(double ratioZ) {
    this.ratioZ = ratioZ;
    this.modified = true;
  }

  /** If true, the vanilla knockback logic is skipped entirely */
  public boolean isCanceled() {
    return canceled;
  }

  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
    this.modified = true;
  }

  /** If true, at least one listener changed something, so the mixin replays the vanilla logic using our values */
  public boolean isModified() {
    return modified;
  }

  @FunctionalInterface
  public interface Callback {
    void onKnockback(KnockbackEvent event);
  }
}
