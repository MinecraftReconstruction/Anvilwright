package slimeknights.tconstruct.library.json.variable.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.function.Supplier;

/** Variable that fetches an attribute value */
public record AttributeEntityVariable(Attribute attribute) implements EntityVariable {
  public static final RecordLoadable<AttributeEntityVariable> LOADER = RecordLoadable.create(Loadables.ATTRIBUTE.requiredField("attribute", AttributeEntityVariable::attribute), AttributeEntityVariable::new);

  public AttributeEntityVariable(Supplier<Attribute> attribute) {
    this(attribute.get());
  }

  @Override
  public float getValue(LivingEntity entity) {
    // NOTE(porting): a data pack can point this at any attribute, including Tinkers' own (which mobs do not carry),
    //  and AttributeMap#getValue throws for an attribute the entity type does not have
    AttributeInstance instance = entity.getAttribute(attribute);
    return instance == null ? 0 : (float)instance.getValue();
  }

  @Override
  public RecordLoadable<AttributeEntityVariable> getLoader() {
    return LOADER;
  }
}
