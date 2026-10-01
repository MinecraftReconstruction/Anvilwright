package slimeknights.tconstruct.tools;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fabricators_of_create.porting_lib.client_events.event.client.MovementInputUpdateCallback;
import io.github.fabricators_of_create.porting_lib.entity.events.PlayerTickEvents;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.Input;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.utils.IdentifiableISafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.client.armor.AbstractArmorModel;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager;
import slimeknights.tconstruct.library.client.armor.texture.TrimArmorTextureSupplier;
import slimeknights.tconstruct.library.client.book.content.AbstractMaterialContent;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.client.model.DynamicTextureLoader;
import slimeknights.tconstruct.library.client.model.TinkerItemProperties;
import slimeknights.tconstruct.library.client.model.tools.MaterialBlockModel;
import slimeknights.tconstruct.library.client.model.tools.MaterialModel;
import slimeknights.tconstruct.library.client.model.tools.ToolModel;
import slimeknights.tconstruct.library.client.modifiers.DyedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.FluidModifierModel;
import slimeknights.tconstruct.library.client.modifiers.MaterialModifierModel;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelManager;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelManager.ModifierModelRegistrationEvent;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMapManager;
import slimeknights.tconstruct.library.client.modifiers.NormalModifierModel;
import slimeknights.tconstruct.library.client.modifiers.PotionModifierModel;
import slimeknights.tconstruct.library.client.modifiers.TankModifierModel;
import slimeknights.tconstruct.library.client.modifiers.TrimModifierModel;
import slimeknights.tconstruct.library.client.particle.AttackParticle;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.modules.technical.ArmorStatModule;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.HarvestTiers;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.shared.TinkerAttributes;
import slimeknights.tconstruct.shared.TinkerEffects;
import slimeknights.tconstruct.tools.client.CrystalshotRenderer;
import slimeknights.tconstruct.tools.client.FluidEffectProjectileRenderer;
import slimeknights.tconstruct.tools.client.OverslimeModifierModel;
import slimeknights.tconstruct.tools.client.ShieldBannerModifierSpriteSource;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;
import slimeknights.tconstruct.tools.client.ToolContainerScreen;
import slimeknights.tconstruct.tools.client.material.CombatFishingHookRenderer;
import slimeknights.tconstruct.tools.client.material.ThrownShurikenRenderer;
import slimeknights.tconstruct.tools.client.material.ThrownToolRenderer;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;
import slimeknights.tconstruct.tools.logic.DoubleJumpHandler;
import slimeknights.tconstruct.tools.logic.InteractionHandler;
import slimeknights.tconstruct.tools.modules.ranged.ammo.SmashingModule;
import slimeknights.tconstruct.tools.network.TinkerControlPacket;

import java.util.Map;

import static slimeknights.tconstruct.library.client.model.tools.ToolModel.registerItemColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import java.util.function.Consumer;
import static slimeknights.tconstruct.TConstruct.getResource;
import slimeknights.tconstruct.tools.client.ArmorModelHelper;
import slimeknights.tconstruct.tools.client.PlateArmorModel;
import slimeknights.tconstruct.tools.client.SlimelytraArmorModel;
import slimeknights.tconstruct.library.tools.item.IModifiable;

@SuppressWarnings("unused")
public class ToolClientEvents extends ClientEventBase {
  /** Keybinding for interacting using a helmet */
  private static final KeyMapping HELMET_INTERACT = new KeyMapping(TConstruct.makeTranslationKey("key", "helmet_interact")/*, KeyConflictContext.IN_GAME*/, InputConstants.getKey("key.keyboard.z").getValue(), "key.categories.tconstruct");
  /** Keybinding for interacting using leggings */
  private static final KeyMapping LEGGINGS_INTERACT = new KeyMapping(TConstruct.makeTranslationKey("key", "leggings_interact")/*, KeyConflictContext.IN_GAME*/, InputConstants.getKey("key.keyboard.i").getValue(), "key.categories.tconstruct");

  /** Listener to clear modifier cache */
  private static final IdentifiableISafeManagerReloadListener MODIFIER_RELOAD_LISTENER = new IdentifiableISafeManagerReloadListener(TConstruct.getResource("modifier_reload_listener")) {
    @Override
    public void onReloadSafe(ResourceManager manager) {
      ModifierManager.INSTANCE.getAllValues().forEach(modifier -> modifier.clearCache(PackType.CLIENT_RESOURCES));
    }
  };

  /** Wraps a Mantle safe reload listener so Fabric can identify it */
  private static IdentifiableISafeManagerReloadListener wrap(String path, ISafeManagerReloadListener listener) {
    return new IdentifiableISafeManagerReloadListener(TConstruct.getResource(path)) {
      @Override
      public void onReloadSafe(ResourceManager manager) {
        listener.onReloadSafe(manager);
      }
    };
  }

  static void addResourceListener() {
    ModifierModelManager.init(ResourceManagerHelper.get(PackType.CLIENT_RESOURCES));
    MaterialTooltipCache.init(ResourceManagerHelper.get(PackType.CLIENT_RESOURCES));
    DynamicTextureLoader.init();
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(MODIFIER_RELOAD_LISTENER);
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(wrap("plate_armor_model", PlateArmorModel.RELOAD_LISTENER));
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(wrap("slimeskull_armor_model", SlimeskullArmorModel.RELOAD_LISTENER));
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(wrap("slimelytra_armor_model", SlimelytraArmorModel.RELOAD_LISTENER));
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(wrap("harvest_tiers", HarvestTiers.RELOAD_LISTENER));
  }

  static void registerModelLoaders(Map<ResourceLocation, IGeometryLoader<?>> loaders) {
    loaders.put(TConstruct.getResource("material"), MaterialModel.LOADER);
    loaders.put(TConstruct.getResource("tool"), ToolModel.LOADER);
  }

  static void registerModifierModels(ModifierModelRegistrationEvent event) {
    event.registerModel(getResource("normal"), NormalModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("overslime"), OverslimeModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("fluid"), FluidModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("tank"), TankModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("material"), MaterialModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("dyed"), DyedModifierModel.UNBAKED_INSTANCE);
    // trim shows up as valid on every tool, skip to reduce memory overhead on tools using the new system - add it using the new system if you want it
    event.registerModel(getResource("trim"), TrimModifierModel.UNBAKED_INSTANCE);
    ModifierModelMapManager.legacyBlacklist(TrimModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("potion"), PotionModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("smashing_fluid"), new FluidModifierModel.Unbaked(SmashingModule.TANK_HELPER));
  }

  static void registerRenderers() {
    EntityRendererRegistry.register(TinkerTools.indestructibleItem.get(), ItemEntityRenderer::new);
    EntityRendererRegistry.register(TinkerTools.crystalshotEntity.get(), CrystalshotRenderer::new);
  }

  public static void clientSetupEvent() {
    PlayerTickEvents.START.register(ToolClientEvents::handleKeyBindings);
    MovementInputUpdateCallback.EVENT.register(ToolClientEvents::handleInput);
    ArmorModelHelper.init();

    // keybinds
    KeyBindingHelper.registerKeyBinding(HELMET_INTERACT);
    KeyBindingHelper.registerKeyBinding(LEGGINGS_INTERACT);

    // screens
    MenuScreens.register(TinkerTools.toolContainer.get(), ToolContainerScreen::new);

    // properties
    // stone
    TinkerItemProperties.registerToolProperties(TinkerTools.pickaxe.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.sledgeHammer.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.veinHammer.asItem());
    // dirt
    TinkerItemProperties.registerToolProperties(TinkerTools.mattock.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.pickadze.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.excavator.asItem());
    // axe
    TinkerItemProperties.registerToolProperties(TinkerTools.handAxe.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.broadAxe.asItem());
    // leaves
    TinkerItemProperties.registerToolProperties(TinkerTools.kama.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.scythe.asItem());
    // sword
    TinkerItemProperties.registerToolProperties(TinkerTools.dagger.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.sword.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.cleaver.asItem());
    // bow
    TinkerItemProperties.registerCrossbowProperties(TinkerTools.crossbow.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.longbow.asItem());
    // misc
    TinkerItemProperties.registerToolProperties(TinkerTools.flintAndBrick.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.skyStaff.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.earthStaff.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.ichorStaff.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.travelersShield.asItem());
    TinkerItemProperties.registerToolProperties(TinkerTools.plateShield.asItem());

    registerRenderers();
    registerParticleFactories();
    itemColors();
    addResourceListener();
    ModifierModelRegistrationEvent.EVENT.register(ToolClientEvents::registerModifierModels);
    RegisterGeometryLoadersCallback.EVENT.register(ToolClientEvents::registerModelLoaders);
  }

  static void registerParticleFactories() {
    ParticleFactoryRegistry.getInstance().register(TinkerTools.hammerAttackParticle.get(), AttackParticle.Factory::new);
    ParticleFactoryRegistry.getInstance().register(TinkerTools.axeAttackParticle.get(), AttackParticle.Factory::new);
    ParticleFactoryRegistry.getInstance().register(TinkerTools.bonkAttackParticle.get(), AttackParticle.Factory::new);
  }

  static void itemColors() {

    // tint modifiers
    // rock
    registerItemColors(TinkerTools.pickaxe);
    registerItemColors(TinkerTools.sledgeHammer);
    registerItemColors(TinkerTools.veinHammer);
    // dirt
    registerItemColors(TinkerTools.mattock);
    registerItemColors(TinkerTools.pickadze);
    registerItemColors(TinkerTools.excavator);
    // wood
    registerItemColors(TinkerTools.handAxe);
    registerItemColors(TinkerTools.broadAxe);
    // scythe
    registerItemColors(TinkerTools.kama);
    registerItemColors(TinkerTools.scythe);
    // weapon
    registerItemColors(TinkerTools.dagger);
    registerItemColors(TinkerTools.sword);
    registerItemColors(TinkerTools.cleaver);
    // bow
    registerItemColors(TinkerTools.longbow);

    // modifier crystal
    ColorProviderRegistry.ITEM.register((stack, index) -> {
      ModifierId modifier = ModifierCrystalItem.getModifier(stack);
      if (modifier != null) {
        return ResourceColorManager.getColor(Util.makeTranslationKey("modifier", modifier));
      }
      return -1;
    }, TinkerModifiers.modifierCrystal);
  }

  // values to check if a key was being pressed last tick, safe as a static value as we only care about a single player client side
  /** If true, we were jumping last tick */
  private static boolean wasJumping = false;
  /** If true, we were interacting with helmet last tick */
  private static boolean wasHelmetInteracting = false;
  /** If true, we were interacting with leggings last tick */
  private static boolean wasLeggingsInteracting = false;

  /** Called on player tick to handle keybinding presses */
  private static void handleKeyBindings(Player player) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player != null && minecraft.player == player && player.level().isClientSide() && !minecraft.player.isSpectator()) {

      // jumping in mid air for double jump
      // ensure we pressed the key since the last tick, holding should not use all your jumps at once
      boolean isJumping = minecraft.options.keyJump.isDown();
      if (!wasJumping && isJumping) {
        if (DoubleJumpHandler.extraJump(player)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.DOUBLE_JUMP);
        }
      }
      wasJumping = isJumping;

      // helmet interaction
      boolean isHelmetInteracting = HELMET_INTERACT.isDown();
      if (!wasHelmetInteracting && isHelmetInteracting) {
        TooltipKey key = SafeClientAccess.getTooltipKey();
        if (InteractionHandler.startArmorInteract(player, EquipmentSlot.HEAD, key)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.getStartHelmetInteract(key));
        }
      }
      if (wasHelmetInteracting && !isHelmetInteracting) {
        if (InteractionHandler.stopArmorInteract(player, EquipmentSlot.HEAD)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.STOP_HELMET_INTERACT);
        }
      }

      // leggings interaction
      boolean isLeggingsInteract = LEGGINGS_INTERACT.isDown();
      if (!wasLeggingsInteracting && isLeggingsInteract) {
        TooltipKey key = SafeClientAccess.getTooltipKey();
        if (InteractionHandler.startArmorInteract(player, EquipmentSlot.LEGS, key)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.getStartLeggingsInteract(key));
        }
      }
      if (wasLeggingsInteracting && !isLeggingsInteract) {
        if (InteractionHandler.stopArmorInteract(player, EquipmentSlot.LEGS)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.STOP_LEGGINGS_INTERACT);
        }
      }

      wasHelmetInteracting = isHelmetInteracting;
      wasLeggingsInteracting = isLeggingsInteract;
    }
  }

  private static void handleInput(Player player, Input input) {
    if (player.isUsingItem() && !player.isPassenger()) {
      ItemStack using = player.getUseItem();
      // start with the attribute
      double speed = TinkerAttributes.getValue(player, TinkerAttributes.USE_ITEM_SPEED.get());
      // start by calculating tool stat, not an attribute to ensure both hands get their say
      if (using.is(TinkerTags.Items.HELD)) {
        ToolStack tool = ToolStack.from(using);
        speed += tool.getStats().get(ToolStats.USE_ITEM_SPEED) - ToolStats.USE_ITEM_SPEED.getDefaultValue();
      }
      // next, add in deprecated key bonus
      speed = Mth.clamp(speed + ArmorStatModule.getStat(player, TinkerDataKeys.USE_ITEM_SPEED), 0, 1);
      // multiply by 5 to cancel out the vanilla 20%
      input.leftImpulse *= (float) (speed * 5);
      input.forwardImpulse *= (float) (speed * 5);
    }
  }
}
