package slimeknights.tconstruct.library.materials.json;

import lombok.Data;
import net.minecraft.world.item.Rarity;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.tconstruct.library.json.JsonRedirect;

import javax.annotation.Nullable;
import slimeknights.tconstruct.library.json.JsonCondition;

@SuppressWarnings("ClassCanBeRecord") // GSON does not support records
@Data
@Internal
public class MaterialJson {
  @Nullable
  private final JsonCondition condition;
  @Nullable
  private final Boolean craftable;
  @Nullable
  private final Integer tier;
  @Nullable
  private final Integer sortOrder;
  @Nullable
  private final Rarity rarity;
  @Nullable
  private final Boolean hidden;
  @Nullable
  private final JsonRedirect[] redirect;
}
