package slimeknights.tconstruct.shared;

import io.github.fabricators_of_create.porting_lib.event.common.RecipesUpdatedCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.crafting.RecipeManager;
import slimeknights.mantle.registration.FluidAttributeClientHandler;
import slimeknights.mantle.registration.FluidAttributeHandler;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.DyedArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.FirstArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.FixedArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.MaterialArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.MaterialHasFallbackTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.TrimArmorTextureSupplier;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.fluids.FluidClientEvents;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.gadgets.GadgetClientEvents;
import slimeknights.tconstruct.library.client.book.TinkerBook;
import slimeknights.tconstruct.library.client.data.spritetransformer.FramesSpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.GreyToColorMapping;
import slimeknights.tconstruct.library.client.data.spritetransformer.GreyToSpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.IColorMapping;
import slimeknights.tconstruct.library.client.data.spritetransformer.ISpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.OffsettingSpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.RecolorSpriteTransformer;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.modifiers.DyedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.NormalModifierModel;
import slimeknights.tconstruct.library.client.modifiers.PotionModifierModel;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.library.client.modifiers.model.BannerModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.CompoundModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.ConditionalModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.FluidModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.MaterialHasFallbackModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.MaterialModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.ModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.NestedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.TankModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.TrimModifierModel;
import slimeknights.tconstruct.tools.client.SlimeskullModifierModel;
import slimeknights.tconstruct.smeltery.SmelteryClientEvents;
import slimeknights.tconstruct.tables.TableClientEvents;
import slimeknights.tconstruct.tables.client.PatternGuiTextureLoader;
import slimeknights.tconstruct.tables.client.inventory.BaseTabbedScreen;
import slimeknights.tconstruct.tools.ToolClientEvents;
import slimeknights.tconstruct.tools.client.ClientInteractionHandler;
import slimeknights.tconstruct.tools.client.ModifierClientEvents;
import slimeknights.tconstruct.tools.client.ToolRenderEvents;
import slimeknights.tconstruct.world.WorldClientEvents;

import java.util.function.Consumer;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * This class should only be referenced on the client side
 */
@SuppressWarnings("removal")
public class TinkerClient implements ClientModInitializer {
  /**
   * Called by TConstruct to handle any client side logic that needs to run during the constructor
   */
  @Override
  public void onInitializeClient() {
    registerArmorTextureLoaders();
    registerModifierModelLoaders();
    TinkerBook.initBook();
    // needs to register listeners early enough for minecraft to load
    ModifierIconManager.init();

    // add the recipe cache invalidator to the client
    Consumer<RecipeManager> recipesUpdated = event -> RecipeCacheInvalidator.reload(true);
    RecipesUpdatedCallback.EVENT.register((recipeManager) -> recipesUpdated.accept(recipeManager));

    // register datagen serializers
    ISpriteTransformer.SERIALIZER.registerDeserializer(RecolorSpriteTransformer.NAME, RecolorSpriteTransformer.DESERIALIZER);
    GreyToSpriteTransformer.init();
    ISpriteTransformer.SERIALIZER.registerDeserializer(OffsettingSpriteTransformer.NAME, OffsettingSpriteTransformer.DESERIALIZER);
    ISpriteTransformer.SERIALIZER.registerDeserializer(FramesSpriteTransformer.NAME, FramesSpriteTransformer.DESERIALIZER);
    IColorMapping.SERIALIZER.registerDeserializer(GreyToColorMapping.NAME, GreyToColorMapping.DESERIALIZER);
    FluidClientEvents.clientSetup();
    GadgetClientEvents.init();
    CommonsClientEvents.init();
    SmelteryClientEvents.init();
    TableClientEvents.init();
    ModifierClientEvents.init();
    ToolRenderEvents.init();
    ToolClientEvents.clientSetupEvent();
    WorldClientEvents.clientSetup();
    ClientInteractionHandler.init();

    // client mod compat checks
    if (FabricLoader.getInstance().isModLoaded("inventorytabs") && Config.CLIENT.inventoryTabsCompat.get()) {
      BaseTabbedScreen.COMPAT_SHOW_TABS = false;
    }
  }

  public static int drawString(GuiGraphics graphics, Font font, FormattedCharSequence formattedCharSequence, float i, float j, int k, boolean bl) {
    int l = font.drawInBatch(formattedCharSequence, i, j, k, bl, graphics.pose().last().pose(), graphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
    graphics.flushIfUnmanaged();
    return l;
  }

  /**
   * Registers the loaders for the armor texture layers.
   * <p>
   * NOTE(porting): upstream registers these in its client setup, which the datagen entrypoint never runs - but
   * {@code ArmorModelProvider} has to serialize those loaders, so it calls this as well. Registering twice is a
   * no-op.
   * <p>
   * Without this the {@code tinkering/armor_models} files cannot be parsed at runtime either, which leaves every
   * piece of Tinkers armor without a renderer.
   */
  public static void registerArmorTextureLoaders() {
    if (armorLoadersRegistered) {
      return;
    }
    armorLoadersRegistered = true;
    ArmorTextureSupplier.LOADER.register(getResource("fixed"), FixedArmorTextureSupplier.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("dyed"), DyedArmorTextureSupplier.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("first_present"), FirstArmorTextureSupplier.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("material"), MaterialArmorTextureSupplier.Material.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("persistent_data"), MaterialArmorTextureSupplier.PersistentData.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("trim"), TrimArmorTextureSupplier.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("material_has_fallback"), MaterialHasFallbackTextureSupplier.LOADER);
  }

  /** True once the armor texture loaders were registered */
  private static boolean armorLoadersRegistered;

  /**
   * Registers the loaders for the modifier models.
   * <p>
   * NOTE(porting): same story as {@link #registerArmorTextureLoaders()} - upstream registers these in its client
   * setup, the datagen entrypoint does not run that, and {@code ModifierModelMapProvider} has to serialize them.
   * Without them the {@code tinkering/modifier_models} files cannot be parsed, so no modifier renders on a tool.
   */
  public static void registerModifierModelLoaders() {
    if (modifierModelLoadersRegistered) {
      return;
    }
    modifierModelLoadersRegistered = true;
    ModifierModel.LOADER.register(getResource("empty"), ModifierModel.EMPTY.getLoader());
    ModifierModel.LOADER.register(getResource("compound"), CompoundModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("conditional"), ConditionalModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("trait"), NestedModifierModel.Trait.LOADER);
    ModifierModel.LOADER.register(getResource("crafted"), NestedModifierModel.Crafted.LOADER);
    ModifierModel.LOADER.register(getResource("basic"), NormalModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("dyed"), DyedModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("material_index"), MaterialModifierModel.Index.LOADER);
    ModifierModel.LOADER.register(getResource("persistent_material"), MaterialModifierModel.PersistentData.LOADER);
    ModifierModel.LOADER.register(getResource("dyed_material"), MaterialModifierModel.Dyed.LOADER);
    ModifierModel.LOADER.register(getResource("potion"), PotionModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("armor_trim"), TrimModifierModel.Armor.LOADER);
    ModifierModel.LOADER.register(getResource("custom_trim"), TrimModifierModel.Custom.LOADER);
    ModifierModel.LOADER.register(getResource("banner"), BannerModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("fluid"), FluidModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("tank"), TankModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("material_has_fallback"), MaterialHasFallbackModifierModel.LOADER);
    // specialized
    ModifierModel.LOADER.register(getResource("slimeskull"), SlimeskullModifierModel.LOADER);
  }

  /** True once the modifier model loaders were registered */
  private static boolean modifierModelLoadersRegistered;
}
