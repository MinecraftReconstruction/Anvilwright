package slimeknights.tconstruct;

import slimeknights.tconstruct.fluids.FluidEvents;
import slimeknights.tconstruct.shared.CommonsEvents;
import slimeknights.tconstruct.tools.logic.InteractionHandler;
import slimeknights.tconstruct.tools.logic.DoubleJumpHandler;
import slimeknights.tconstruct.tools.logic.ModifierEvents;
import slimeknights.tconstruct.tools.logic.ToolEvents;
import slimeknights.tconstruct.tables.TinkerTables;

public class FabricEvents {
  public static void init() {
    FluidEvents.init();
    ToolEvents.init();
    ModifierEvents.init();
    DoubleJumpHandler.init();
    CommonsEvents.init();
    TinkerTables.init();
    InteractionHandler.init();
  }
  
//  private static class Client {
//    @Environment(EnvType.CLIENT)
//    private static void init() {
//      WorldClientEvents.clientSetup();
//    }
//  }
}
