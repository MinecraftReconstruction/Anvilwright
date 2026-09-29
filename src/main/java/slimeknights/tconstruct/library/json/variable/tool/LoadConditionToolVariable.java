package slimeknights.tconstruct.library.json.variable.tool;

import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import slimeknights.mantle.data.loadable.mapping.ConditionalLoadable.ConditionalObject;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.Util;

/**
 * Datagen helper for making conditional {@link ToolVariable}.
 * @param ifTrue      Variable to use if all conditions are true.
 * @param ifFalse     Variable to use if any condition is false.
 * @param conditions  Conditions to evaluate.
 */
@SuppressWarnings("unused") // API
public record LoadConditionToolVariable(ToolVariable ifTrue, ToolVariable ifFalse, ConditionJsonProvider... conditions) implements ToolVariable, ConditionalObject<ToolVariable> {
  @Override
  public float getValue(IToolStackView tool) {
    return (Util.testConditions(conditions) ? ifTrue : ifFalse).getValue(tool);
  }

  @Override
  public RecordLoadable<? extends ToolVariable> getLoader() {
    return ToolVariable.LOADER.getConditionalLoader();
  }
}
