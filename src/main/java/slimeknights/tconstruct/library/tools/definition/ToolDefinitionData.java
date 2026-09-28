package slimeknights.tconstruct.library.tools.definition;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.definition.aoe.IAreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.harvest.IHarvestLogic;
import slimeknights.tconstruct.library.tools.definition.module.IToolModule;
import slimeknights.tconstruct.library.tools.definition.weapon.IWeaponAttack;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.MultiplierNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.IToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.List;

/**
 * This class contains all data pack configurable data for a tool, before materials are factored in.
 * Contains info about how to craft a tool and how it behaves.
 */
public class ToolDefinitionData {
  /** Empty tool data definition instance */
  public static final ToolDefinitionData EMPTY = new ToolDefinitionData(List.of(), ErrorFactory.RUNTIME);
  /** Loadable to parse definition data from JSON */
  public static final RecordLoadable<ToolDefinitionData> LOADABLE = RecordLoadable.create(ToolModule.WITH_HOOKS.list(0).defaultField("modules", List.of(), d -> d.modules), ErrorFactory.FIELD, ToolDefinitionData::new);

  private final List<WithHooks<ToolModule>> modules;
  @Getter
  private final transient ModuleHookMap hooks;

  protected ToolDefinitionData(List<WithHooks<ToolModule>> modules, ErrorFactory error) {
    this.modules = modules;
    this.hooks = ModuleHookMap.createMap(modules, error);
  }


  /* Getters */

  /** Gets the given module from the tool */
  public <T> T getHook(ModuleHook<T> hook) {
    return hooks.getOrDefault(hook);
  }
}
