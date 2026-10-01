package slimeknights.tconstruct.library.events.teleport;

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.tools.data.ModifierIds;

/** Event fired when an entity teleports using the enderporting modifier */
public class EnderdodgingTeleportEvent extends EntityEvents.Teleport.EntityTeleportEvent {
  public static Event<Teleport> EVENT = EventFactory.createArrayBacked(Teleport.class, callbacks -> event -> {
    for(Teleport e : callbacks)
      e.onTeleport(event);
  });

  /** Modifier that caused the teleport, upstream exposes this on {@code ModifierTeleportEvent} */
  private final ModifierEntry modifier;

  public EnderdodgingTeleportEvent(Entity entity, double targetX, double targetY, double targetZ, ModifierEntry modifier) {
    super(entity, targetX, targetY, targetZ);
    this.modifier = modifier;
  }

  public EnderdodgingTeleportEvent(LivingEntity entity, double targetX, double targetY, double targetZ) {
    this(entity, targetX, targetY, targetZ, new ModifierEntry(ModifierIds.enderclearance, 1));
  }

  /** Gets the modifier that caused this teleport */
  public ModifierEntry getModifier() {
    return modifier;
  }

  @Override
  public void sendEvent() {
    EVENT.invoker().onTeleport(this);
  }

  @FunctionalInterface
  public interface Teleport {
    void onTeleport(EnderdodgingTeleportEvent event);
  }
}
