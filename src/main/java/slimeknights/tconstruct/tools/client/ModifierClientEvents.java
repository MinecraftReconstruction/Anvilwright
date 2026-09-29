package slimeknights.tconstruct.tools.client;

import io.github.fabricators_of_create.porting_lib.entity.extensions.MobEffectExtensions;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.fabricators_of_create.porting_lib.event.client.FieldOfViewEvents;
import io.github.fabricators_of_create.porting_lib.event.client.RenderHandCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.events.ToolEquipmentChangeEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.data.FloatMultiplier;
import slimeknights.tconstruct.library.modifiers.modules.technical.ArmorLevelModule;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableBowItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modules.armor.MinimapModule;
import slimeknights.tconstruct.tools.modules.armor.SleevesModule;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Modifier event hooks that run client side */
public class ModifierClientEvents {

  public static void init() {
    ItemTooltipCallback.EVENT.register(ModifierClientEvents::onTooltipEvent);

    RenderHandCallback.EVENT.register(ModifierClientEvents::renderHand);
    ToolEquipmentChangeEvent.EVENT.register(ModifierClientEvents::equipmentChange);
    FieldOfViewEvents.MODIFY.register(ModifierClientEvents::handleZoom);
    // clear the client side caches when leaving a world
    ClientPlayConnectionEvents.DISCONNECT.register(ModifierClientEvents::playerLoggedOut);
  }

  static void onTooltipEvent(ItemStack stack, TooltipFlag context, List<Component> lines) {
    // suppress durability from advanced, we display our own
    if (stack.getItem() instanceof IModifiableDisplay) {
      lines.removeIf(text -> {
        if (text.getContents() instanceof TranslatableContents translatableContents) {
          return translatableContents.getKey().equals("item.durability");
        }
        return false;
      });
    }
  }

  /** Determines whether to render the given hand based on modifiers */
  static void renderHand(RenderHandCallback.RenderHandEvent event) {
    InteractionHand hand = event.getHand();
    Player player = Minecraft.getInstance().player;
    if (player == null) {
      return;
    }
    // when firing your melee weapon with ballista, don't render it in the other hand; makes it look like you duplicated your weapon
    ItemStack held = player.getItemInHand(hand);
    ItemStack opposite = player.getItemInHand(Util.getOpposite(hand));
    if (!held.isEmpty() && !opposite.isEmpty() && opposite.is(TinkerTags.Items.BALLISTAS) && ModifierUtil.getPersistentInt(opposite, ModifiableBowItem.KEY_BALLISTA, 0) == ModifiableBowItem.FLAG_BALLISTA_HELD) {
      event.setCanceled(true);
      return;
    }

    // TODO: PORT - modifiable items have custom first person hand animations (ModifiableItemClientExtension).
    //  Forge installed them per item and called applyForgeHandTransform *instead of* its own transform; Porting Lib's
    //  RenderHandCallback cancels the whole hand render, so wiring this up means re-rendering the item here first.
    //  See docs/BEHAVIOUR-DIFFERENCES.md #24.

    // if the data is set, render the empty offhand
    if (offhand.isEmpty()) {
      if (!player.isInvisible() && mainhand.getItem() != Items.FILLED_MAP && ModifierUtil.getTotalModifierLevel(player, TinkerDataKeys.SHOW_EMPTY_OFFHAND) > 0) {
        PoseStack matrices = event.getPoseStack();
        matrices.pushPose();
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderPlayerArm(matrices, event.getMultiBufferSource(), event.getPackedLight(), event.getEquipProgress(), event.getSwingProgress(), player.getMainArm().getOpposite());
        matrices.popPose();
        event.setCanceled(true);
      }
    }
  }

  /** Handles the zoom modifier zooming */
  static float handleZoom(AbstractClientPlayer player, float fov) {
    AtomicReference<Float> newFovRef = new AtomicReference<>(fov);
    TinkerDataCapability.CAPABILITY.maybeGet(player).ifPresent(data -> {
      float newFov = fov;

      // scaled effects only apply if we have FOV scaling, nothing to do if 0
      float effectScale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
      if (effectScale > 0) {
        FloatMultiplier scaledZoom = data.get(TinkerDataKeys.SCALED_FOV_MODIFIER);
        if (scaledZoom != null) {
          // much easier when 1, save some effort
          if (effectScale == 1) {
            newFov *= scaledZoom.getValue();
          } else {
            // unlerp the fov before multiplitying to make sure we apply the proper amount
            // we could use the original FOV, but someone else may have modified it
            float original = fov;
            newFov *= Mth.lerp(effectScale, 1.0F, scaledZoom.getValue() * original) / original;
          }
        }
      }

      // non-scaled effects are much easier to deal with
      FloatMultiplier constZoom = data.get(TinkerDataKeys.FOV_MODIFIER);
      if (constZoom != null) {
        newFov *= constZoom.getValue();
      }
      newFovRef.set(newFov);
    });
    return newFovRef.get();
  }


  /* Renders the next shield strap item above the offhand item */

  /** Cache of the current item to render */
  private static final int SLOT_BACKGROUND_SIZE = 22;
  /** Size of the border around the map */
  private static final int MAP_PADDING = 7;
  /** Total map size */
  private static final int MAP_SIZE = 2 * MAP_PADDING + 128;

  @Nonnull
  private static ItemStack nextOffhand = ItemStack.EMPTY;
  @Nonnull
  private static ItemStack currentSleeve = ItemStack.EMPTY;

  /** Items to render for the item frame modifier */
  private static final List<ItemStack> itemFrames = new ArrayList<>();

  /** Clears the caches that must not survive a world change. */
  static void playerLoggedOut(ClientPacketListener handler, Minecraft client) {
    nextOffhand = ItemStack.EMPTY;
    itemFrames.clear();
  }

  /** Update the slot in the first shield slot */
  static void equipmentChange(ToolEquipmentChangeEvent event) {
    if (event.getEntity() != Minecraft.getInstance().player) {
      return;
    }
    EquipmentChangeContext context = event.getContext();
    if (Config.CLIENT.renderShieldSlotItem.get()) {
      if (event.getEntity() == Minecraft.getInstance().player && context.getChangedSlot() == EquipmentSlot.LEGS) {
        IToolStackView tool = context.getToolInSlot(EquipmentSlot.LEGS);
        if (tool != null) {
          ModifierEntry entry = tool.getModifiers().getEntry(TinkerModifiers.shieldStrap.getId());
          if (entry != ModifierEntry.EMPTY) {
            nextOffhand = entry.getHook(ToolInventoryCapability.HOOK).getStack(tool, entry, 0);
            return;
          }
        }
        nextOffhand = ItemStack.EMPTY;
      }
    }
    if (Config.CLIENT.renderSleevesItem.get()) {
      if (context.getChangedSlot() == EquipmentSlot.CHEST) {
        IToolStackView tool = context.getToolInSlot(EquipmentSlot.CHEST);
        if (tool != null) {
          ModifierEntry entry = tool.getModifiers().getEntry(TinkerModifiers.sleeves.getId());
          if (entry != ModifierEntry.EMPTY) {
            currentSleeve = entry.getHook(ToolInventoryCapability.HOOK).getStack(tool, entry, tool.getPersistentData().getInt(SleevesModule.SELECTED_SLOT));
            return;
          }
        }
        currentSleeve = ItemStack.EMPTY;
      }
    }

    if (Config.CLIENT.renderItemFrame.get()) {
      if (event.getEntity() == Minecraft.getInstance().player && context.getChangedSlot() == EquipmentSlot.HEAD) {
        itemFrames.clear();
        IToolStackView tool = context.getToolInSlot(EquipmentSlot.HEAD);
        if (tool != null) {
          ModifierEntry entry = tool.getModifier(TinkerModifiers.itemFrame.getId());
          if (entry.intEffectiveLevel() > 0) {
            entry.getHook(ToolInventoryCapability.HOOK).getAllStacks(tool, entry, itemFrames);
          }
        }
      }
    }
  }

  /** Gets the offset to apply for potion effects on the player */
  private static int getEffectOffset(Player player) {
    boolean hasBeneficial = false;
    for (MobEffectInstance instance : player.getActiveEffects()) {
      // Forge's IClientMobEffectExtensions was a client extension lookup; Porting Lib puts the renderer on the effect
      if (instance.showIcon() && ((MobEffectExtensions)(Object)instance.getEffect()).getRenderer().isVisibleInGui(instance)) {
        if (instance.getEffect().isBeneficial()) {
          hasBeneficial = true;
        } else {
          // negative effects means offset two rows
          return 52;
        }
      }
    }
    // if we found a positive effect, only need one row. Otherwise none
    return hasBeneficial ? 26 : 0;
  }

  /** Render the item in the first shield slot */
//  @SubscribeEvent TODO: PORT
//  static void renderHotbar(RenderGameOverlayEvent.PostLayer event) {
//    Minecraft mc = Minecraft.getInstance();
//    if (mc.options.hideGui) {
//      return;
//    }
//    IIngameOverlay overlay = event.getOverlay();
//    if (overlay != ForgeIngameGui.HOTBAR_ELEMENT) {
//      return;
//    }
//    boolean renderShield = Config.CLIENT.renderShieldSlotItem.get() && !nextOffhand.isEmpty();
//    boolean renderItemFrame = Config.CLIENT.renderItemFrame.get() && !itemFrames.isEmpty();
//    if (!renderItemFrame && !renderShield) {
//      return;
//    }
//    MultiPlayerGameMode playerController = Minecraft.getInstance().gameMode;
//    if (playerController != null && playerController.getPlayerMode() != GameType.SPECTATOR) {
//      Player player = Minecraft.getInstance().player;
//      if (player != null && player == mc.getCameraEntity()) {
//        RenderSystem.enableBlend();
//        RenderSystem.defaultBlendFunc();
//
//        int scaledWidth = mc.getWindow().getGuiScaledWidth();
//        int scaledHeight = mc.getWindow().getGuiScaledHeight();
//        PoseStack matrixStack = event.getMatrixStack();
//        float partialTicks = event.getPartialTicks();
//
//        // want just above the normal hotbar item
//        if (renderShield) {
//          RenderSystem.setShaderTexture(0, Icons.ICONS);
//          int x = scaledWidth / 2 + (player.getMainArm().getOpposite() == HumanoidArm.LEFT ? -117 : 101);
//          int y = scaledHeight - 38;
//          Screen.blit(matrixStack, x - 3, y - 3, player.getOffhandItem().isEmpty() ? 211 : 189, 0, SLOT_BACKGROUND_SIZE, SLOT_BACKGROUND_SIZE, 256, 256);
//          mc.gui.renderSlot(x, y, partialTicks, player, nextOffhand, 11);
//        }
//
//        if (renderItemFrame) {
//          // determine how many items need to be rendered
//          int columns = Config.CLIENT.itemsPerRow.get();
//          int count = itemFrames.size();
//          // need to split items over multiple lines potentially
//          int rows = count / columns;
//          int inLastRow = count % columns;
//          // if we have an exact number, means we should have full in last row
//          if (inLastRow == 0) {
//            inLastRow = columns;
//          } else {
//            // we have an incomplete row that was not counted
//            rows++;
//          }
//          // determine placement of the items
//          Orientation2D location = Config.CLIENT.itemFrameLocation.get();
//          Orientation1D xOrientation = location.getX();
//          Orientation1D yOrientation = location.getY();
//          int xStart = xOrientation.align(scaledWidth - SLOT_BACKGROUND_SIZE * columns) + Config.CLIENT.itemFrameXOffset.get();
//          int yStart = yOrientation.align(scaledHeight - SLOT_BACKGROUND_SIZE * rows) + Config.CLIENT.itemFrameYOffset.get();
//
//          // draw backgrounds
//          RenderSystem.setShaderTexture(0, Icons.ICONS);
//          int lastRow = rows - 1;
//          for (int r = 0; r < lastRow; r++) {
//            for (int c = 0; c < columns; c++) {
//              Screen.blit(matrixStack, xStart + c * SLOT_BACKGROUND_SIZE, yStart + r * SLOT_BACKGROUND_SIZE, 167, 0, SLOT_BACKGROUND_SIZE, SLOT_BACKGROUND_SIZE, 256, 256);
//            }
//          }
//          // last row will be aligned in the direction of x orientation (center, left, or right)
//          int lastRowOffset = xOrientation.align((columns - inLastRow) * 2) * SLOT_BACKGROUND_SIZE / 2;
//          for (int c = 0; c < inLastRow; c++) {
//            Screen.blit(matrixStack, xStart + c * SLOT_BACKGROUND_SIZE + lastRowOffset, yStart + lastRow * SLOT_BACKGROUND_SIZE, 167, 0, SLOT_BACKGROUND_SIZE, SLOT_BACKGROUND_SIZE, 256, 256);
//          }
//
//          // draw items
//          int i = 0;
//          xStart += 3; yStart += 3; // offset from item start instead of frame start
//          for (int r = 0; r < lastRow; r++) {
//            for (int c = 0; c < columns; c++) {
//              mc.gui.renderSlot(xStart + c * SLOT_BACKGROUND_SIZE, yStart + r * SLOT_BACKGROUND_SIZE, partialTicks, player, itemFrames.get(i), i);
//              i++;
//            }
//          }
//          // align last row
//          for (int c = 0; c < inLastRow; c++) {
//            mc.gui.renderSlot(xStart + c * SLOT_BACKGROUND_SIZE + lastRowOffset, yStart + lastRow * SLOT_BACKGROUND_SIZE, partialTicks, player, itemFrames.get(i), i);
//            i++;
//          }
//        }
//
//        RenderSystem.disableBlend();
//      }
//    }
//  }
}
