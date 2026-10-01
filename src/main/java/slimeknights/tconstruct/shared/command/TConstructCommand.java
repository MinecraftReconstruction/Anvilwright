package slimeknights.tconstruct.shared.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.shared.command.argument.MaterialArgument;
import slimeknights.tconstruct.shared.command.argument.MaterialStatsArgument;
import slimeknights.tconstruct.shared.command.argument.MaterialVariantArgument;
import slimeknights.tconstruct.shared.command.argument.ModifierArgument;
import slimeknights.tconstruct.shared.command.argument.ModifierHookArgument;
import slimeknights.tconstruct.shared.command.argument.ModifierTagSource;
import slimeknights.tconstruct.shared.command.argument.SlotTypeArgument;
import slimeknights.tconstruct.shared.command.argument.ToolStatArgument;
import slimeknights.tconstruct.shared.command.subcommand.DurabilityCommand;
import slimeknights.tconstruct.shared.command.subcommand.GenerateHiddenFluidsCommand;
import slimeknights.tconstruct.shared.command.subcommand.GenerateMeltingRecipesCommand;
import slimeknights.tconstruct.shared.command.subcommand.GeneratePartTexturesCommand;
import slimeknights.tconstruct.shared.command.subcommand.MaterialsCommand;
import slimeknights.tconstruct.shared.command.subcommand.ModifierPriorityCommand;
import slimeknights.tconstruct.shared.command.subcommand.ModifierUsageCommand;
import slimeknights.tconstruct.shared.command.subcommand.ModifiersCommand;
import slimeknights.tconstruct.shared.command.subcommand.SlotsCommand;
import slimeknights.tconstruct.shared.command.subcommand.StatsCommand;

import java.util.function.Consumer;
import slimeknights.tconstruct.common.registration.ArgumentTypeDeferredRegister;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.network.chat.Component;

public class TConstructCommand {
  public static final DynamicCommandExceptionType COMPONENT_ERROR = new DynamicCommandExceptionType(error -> (Component)error);
  private static final ArgumentTypeDeferredRegister ARGUMENT_TYPE = new ArgumentTypeDeferredRegister(TConstruct.MOD_ID);

  /** Registers all TConstruct command related content */
  public static void init() {
    ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, TConstruct.resourceString("slot_type"), SlotTypeArgument.class, SingletonArgumentInfo.contextFree(SlotTypeArgument::slotType));
    ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, TConstruct.resourceString("tool_stat"), ToolStatArgument.class, SingletonArgumentInfo.contextFree(ToolStatArgument::stat));
    ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, TConstruct.resourceString("modifier"), ModifierArgument.class, SingletonArgumentInfo.contextFree(ModifierArgument::modifier));
    ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, TConstruct.resourceString("material"), MaterialArgument.class, SingletonArgumentInfo.contextFree(MaterialArgument::material));

    // add command listener
    CommandRegistrationCallback.EVENT.register(TConstructCommand::registerCommand);
  }

  /** Registers a sub command for the root Mantle command */
  private static void register(LiteralArgumentBuilder<CommandSourceStack> root, String name, Consumer<LiteralArgumentBuilder<CommandSourceStack>> consumer) {
    LiteralArgumentBuilder<CommandSourceStack> subCommand = Commands.literal(name);
    consumer.accept(subCommand);
    root.then(subCommand);
  }

  /** Event listener to register the Mantle command */
  private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
    LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(TConstruct.MOD_ID);
    CommandBuildContext context = registryAccess;

    // sub commands
    register(builder, "modifiers", ModifiersCommand::register);
    register(builder, "materials", MaterialsCommand::register);
    register(builder, "tool_stats", StatsCommand::register);
    register(builder, "slots", SlotsCommand::register);
    register(builder, "durability", DurabilityCommand::register);
    register(builder, "report", b -> {
      register(b, "modifier_usage", ModifierUsageCommand::register);
      register(b, "modifier_priority", ModifierPriorityCommand::register);
    });
    register(builder, "generate", b -> {
      register(b, "part_textures", GeneratePartTexturesCommand::register);
      register(b, "melting_recipes", bb -> GenerateMeltingRecipesCommand.register(bb, context));
      register(b, "hidden_fluids_tag", GenerateHiddenFluidsCommand::register);
    });

    // register final command
    dispatcher.register(builder);
  }
}
