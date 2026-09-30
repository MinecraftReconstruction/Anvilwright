package slimeknights.tconstruct.library.events.teleport;

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.tools.data.ModifierIds;

/** Event fired when an entity teleports using the enderporting modifier */
public class EnderportingTeleportEvent extends EntityEvents.Teleport.EntityTeleportEvent {
  /** Modifier that caused the teleport, upstream exposes this on {@code ModifierTeleportEvent} */
  private final ModifierEntry modifier;

  public EnderportingTeleportEvent(Entity entity, double targetX, double targetY, double targetZ, ModifierEntry modifier) {
    super(entity, targetX, targetY, targetZ);
    this.modifier = modifier;
  }

  public EnderportingTeleportEvent(LivingEntity entity, double targetX, double targetY, double targetZ) {
    this(entity, targetX, targetY, targetZ, new ModifierEntry(ModifierIds.enderclearance, 1));
  }

  /** Gets the modifier that caused this teleport */
  public ModifierEntry getModifier() {
    return modifier;
  }

  @Override
  public void sendEvent() {
    EntityEvents.TELEPORT.invoker().onTeleport(this);
  }
}
