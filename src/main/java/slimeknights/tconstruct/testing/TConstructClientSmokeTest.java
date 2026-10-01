package slimeknights.tconstruct.testing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.ArrayList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Development-only client self test for the creative inventory.
 * <p>
 * Two of the regressions this port hit while smoke testing only show up when the creative screen is opened: building
 * the tab contents (which throws when a tab contains the same stack twice, or when a tab casts an item to the wrong
 * class) and resolving the baked model of every stack that is drawn (which is where the tank/fluid container models
 * blew up on the missing fluid sprite array). Driving that by hand means clicking through every tab, so instead this
 * class does it once per client start and logs {@code [smoketest]} lines.
 * <p>
 * It only runs when Fabric reports a development environment, so a released jar is unaffected; run
 * {@code ./gradlew runClient}, join a world and look for {@code [smoketest]} in the log.
 */
public class TConstructClientSmokeTest implements ClientModInitializer {
  private static final String TAG = "[smoketest] ";
  /** Number of failures to log in full before switching to a summary */
  private static final int MAX_DETAILED_FAILURES = 10;

  private boolean ran;

  @Override
  public void onInitializeClient() {
    if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
      return;
    }
    ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
  }

  private void onClientTick(Minecraft minecraft) {
    // wait until a world is loaded, which also means the model manager finished its first reload
    if (ran || minecraft.level == null || minecraft.player == null) {
      return;
    }
    ran = true;
    runSmokeTest(minecraft);
  }

  private void runSmokeTest(Minecraft minecraft) {
    TConstruct.LOG.info("{}starting creative inventory smoke test", TAG);
    int passed = 0;
    int failed = 0;
    List<String> failures = new ArrayList<>();

    // building the tab contents is what throws on duplicated stacks and on bad item casts
    if (CreativeModeTabs.tryRebuildTabContents(minecraft.level.enabledFeatures(), minecraft.player.canUseGameMasterBlocks(), minecraft.level.registryAccess())) {
      passed++;
      TConstruct.LOG.info("{}{} PASS  tab contents rebuilt", TAG, "creative/");
    } else {
      failed++;
      failures.add("creative/ rebuild reported no change");
    }

    // then resolve the baked model of every icon and of every stack in every tab, the same calls the screen makes
    ItemRenderer itemRenderer = minecraft.getItemRenderer();
    Set<ItemStack> checked = new LinkedHashSet<>();
    int icons = 0;
    int stacks = 0;
    for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
      ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
      List<ItemStack> toCheck = new ArrayList<>();
      icons++;
      toCheck.add(tab.getIconItem());
      toCheck.addAll(tab.getDisplayItems());
      for (ItemStack stack : toCheck) {
        if (stack.isEmpty() || !checked.add(stack)) {
          continue;
        }
        stacks++;
        try {
          itemRenderer.getModel(stack, minecraft.level, minecraft.player, 0);
        } catch (Throwable e) {
          failed++;
          if (failures.size() < MAX_DETAILED_FAILURES) {
            failures.add(tabId + ": " + BuiltInRegistries.ITEM.getKey(stack.getItem()) + " (" + e + ")");
          }
        }
      }
    }
    passed += stacks;
    TConstruct.LOG.info("{}{} PASS  model lookup for {} tab icons and {} unique stacks", TAG, "creative/", icons, stacks);

    for (String failure : failures) {
      TConstruct.LOG.error("{}{} FAIL  {}", TAG, "creative/", failure);
    }
    auditItemModels(minecraft);
    auditItemSprites(minecraft);
    auditItemRenderLayers(minecraft);
    auditTranslations(minecraft);
    auditAtlas(minecraft);
    TConstruct.LOG.info("{}summary: {} passed, {} failed", TAG, passed, failed);
  }

  /**
   * Counts Tinkers items that resolve to the vanilla missing model, which is what draws as the magenta and black cube
   * players describe as a broken item - i.e. the items that still have no model of their own.
   */
  private static void auditItemModels(Minecraft minecraft) {
    BakedModel missingModel = minecraft.getModelManager().getMissingModel();
    int total = 0;
    List<String> missing = new ArrayList<>();
    for (Item item : BuiltInRegistries.ITEM) {
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
      if (!id.getNamespace().equals(TConstruct.MOD_ID)) {
        continue;
      }
      total++;
      if (minecraft.getItemRenderer().getModel(new ItemStack(item), minecraft.level, minecraft.player, 0) == missingModel) {
        missing.add(id.getPath());
      }
    }
    TConstruct.LOG.info("{}models/ {}/{} tconstruct items use the missing model", TAG, missing.size(), total);
    for (int i = 0; i < missing.size() && i < MAX_DETAILED_FAILURES; i++) {
      TConstruct.LOG.info("{}models/   {}", TAG, missing.get(i));
    }
  }

  /**
   * Forge stitches every texture a model or the client code asks for, Fabric only stitches what the atlas config
   * lists. Report our textures that never made it into the block atlas, since those are exactly the ones that draw
   * as magenta/black and the ones that make a model fail to bake.
   */
  /**
   * Counts items whose baked model draws at least one missing sprite. {@link #auditItemModels} only catches items with
   * no model at all: an item whose model exists but asks for a texture that was never created bakes fine and then
   * draws as the magenta/black checker, which is the other half of what players report as a broken item.
   */
  private static void auditItemSprites(Minecraft minecraft) {
    TextureAtlas atlas = (TextureAtlas) minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
    RandomSource random = RandomSource.create(42);
    int total = 0;
    List<String> broken = new ArrayList<>();
    for (Item item : BuiltInRegistries.ITEM) {
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
      if (!id.getNamespace().equals(TConstruct.MOD_ID)) {
        continue;
      }
      total++;
      BakedModel model = minecraft.getItemRenderer().getModel(new ItemStack(item), minecraft.level, minecraft.player, 0);
      if (hasMissingSprite(model, atlas, random)) {
        broken.add(id.getPath());
      }
    }
    TConstruct.LOG.info("{}sprites/ {}/{} tconstruct items draw a missing texture", TAG, broken.size(), total);
    for (int i = 0; i < broken.size() && i < MAX_DETAILED_FAILURES; i++) {
      TConstruct.LOG.info("{}sprites/   {}", TAG, broken.get(i));
    }
  }

  /** Checks the particle icon and every quad of the model for the missing sprite */
  private static boolean hasMissingSprite(BakedModel model, TextureAtlas atlas, RandomSource random) {
    if (isMissing(atlas, model.getParticleIcon())) {
      return true;
    }
    for (Direction direction : Direction.values()) {
      for (BakedQuad quad : model.getQuads(null, direction, random)) {
        if (isMissing(atlas, quad.getSprite())) {
          return true;
        }
      }
    }
    for (BakedQuad quad : model.getQuads(null, null, random)) {
      if (isMissing(atlas, quad.getSprite())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isMissing(TextureAtlas atlas, @Nullable TextureAtlasSprite sprite) {
    return sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());
  }

  /**
   * Reports the render layer of the items that embed a fluid, which is what Forge used to express with a per-layer
   * render type on the model ({@code RenderTypeGroup}). Fabric renders every non-block item in the translucent layer
   * and takes block items from their block, so this is the evidence for whether those layers survived the port.
   */
  private static void auditItemRenderLayers(Minecraft minecraft) {
    // evidence for BEHAVIOUR-DIFFERENCES 7/8: knockback resistance has to be client syncable, which the knockback
    // sling modifiers rely on
    TConstruct.LOG.info("{}layers/ knockback resistance syncable = {}", TAG, Attributes.KNOCKBACK_RESISTANCE.isClientSyncable());
    ItemStack[] interesting = {
      new ItemStack(TinkerSmeltery.copperCan),
      TinkerFluids.moltenIron.getBucket() == null ? ItemStack.EMPTY : new ItemStack(TinkerFluids.moltenIron.getBucket()),
      new ItemStack(TinkerSmeltery.copperGauge),
    };
    for (ItemStack stack : interesting) {
      if (stack.isEmpty()) {
        continue;
      }
      RenderType type = ItemBlockRenderTypes.getRenderType(stack, false);
      TConstruct.LOG.info("{}layers/ {} -> {}", TAG, BuiltInRegistries.ITEM.getKey(stack.getItem()), type);
    }
  }

  /**
   * Reports items and blocks whose description resolves to the raw translation key, which is what shows up as
   * "block.tconstruct.lavawood" in a tooltip when a language entry is missing.
   */
  private static void auditTranslations(Minecraft minecraft) {
    List<String> untranslated = new ArrayList<>();
    int total = 0;
    for (Item item : BuiltInRegistries.ITEM) {
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
      if (!id.getNamespace().equals(TConstruct.MOD_ID)) {
        continue;
      }
      total++;
      String key = item.getDescriptionId();
      String translated = Component.translatable(key).getString();
      if (translated.equals(key)) {
        untranslated.add(key);
      }
    }
    TConstruct.LOG.info("{}lang/ {}/{} tconstruct items have no translation", TAG, untranslated.size(), total);
    for (int i = 0; i < untranslated.size() && i < MAX_DETAILED_FAILURES; i++) {
      TConstruct.LOG.info("{}lang/   {}", TAG, untranslated.get(i));
    }
  }

  private static void auditAtlas(Minecraft minecraft) {
    TextureAtlas atlas = (TextureAtlas) minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
    Map<ResourceLocation, Resource> textures = minecraft.getResourceManager()
      .listResources("textures", location -> location.getPath().endsWith(".png") && location.getNamespace().equals(TConstruct.MOD_ID));

    // group by the texture folder so the log stays readable
    Map<String,Integer> missingByFolder = new TreeMap<>();
    List<String> names = new ArrayList<>();
    int total = 0;
    for (ResourceLocation texture : textures.keySet()) {
      String path = texture.getPath();
      ResourceLocation spriteId = new ResourceLocation(texture.getNamespace(), path.substring("textures/".length(), path.length() - ".png".length()));
      TextureAtlasSprite sprite = atlas.getSprite(spriteId);
      if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
        total++;
        names.add(spriteId.toString());
        int slash = spriteId.getPath().lastIndexOf('/');
        String folder = slash < 0 ? "" : spriteId.getPath().substring(0, slash);
        missingByFolder.merge(folder, 1, Integer::sum);
      }
    }
    // dump the full list next to the log so it can be diffed between runs
    try {
      Path output = minecraft.gameDirectory.toPath().resolve("smoketest-atlas-missing.txt");
      Files.write(output, names, StandardCharsets.UTF_8);
      TConstruct.LOG.info("{}atlas/ full list written to {}", TAG, output);
    } catch (IOException e) {
      TConstruct.LOG.error("{}atlas/ failed to write the missing sprite list", TAG, e);
    }
    TConstruct.LOG.info("{}atlas/ {} of {} tconstruct textures are missing from the block atlas", TAG, total, textures.size());
    int logged = 0;
    for (Map.Entry<String,Integer> entry : missingByFolder.entrySet()) {
      if (logged++ >= MAX_DETAILED_FAILURES) {
        TConstruct.LOG.info("{}atlas/ ... and {} more folders", TAG, missingByFolder.size() - MAX_DETAILED_FAILURES);
        break;
      }
      TConstruct.LOG.info("{}atlas/ {} missing in {}", TAG, entry.getValue(), entry.getKey());
    }
  }
}
