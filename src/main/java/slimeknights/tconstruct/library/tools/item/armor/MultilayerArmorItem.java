package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Armor model that applies multiple texture layers in order */
public class MultilayerArmorItem extends ModifiableArmorItem {
  private final ResourceLocation name;
  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorItem.Type slot, Properties properties) {
    this(material, slot, properties, material.getId());
  }

  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorItem.Type slot, Properties properties, ResourceLocation name) {
    super(material, slot, properties);
    this.name = name;
    ArmorModelManager.registerArmorRenderer(this, armorModel);
  }

  @SuppressWarnings("removal")
  public MultilayerArmorItem(ArmorMaterial material, ArmorItem.Type slot, Properties properties, ToolDefinition toolDefinition) {
    this(material, slot, properties, toolDefinition, new ResourceLocation(material.getName()));
  }

  public MultilayerArmorItem(ArmorMaterial material, ArmorItem.Type slot, Properties properties, ToolDefinition toolDefinition, ResourceLocation name) {
    super(material, slot, properties, toolDefinition);
    this.name = name;
    ArmorModelManager.registerArmorRenderer(this, armorModel);
  }

  /** The armor model dispatcher for this item, registered with the client in {@link ArmorModelManager#init()} */
  private final ArmorModelDispatcher armorModel = new ArmorModelDispatcher() {
    @Override
    protected ResourceLocation getName() {
      return name;
    }
  };

  /** Gets the armor model dispatcher, used by the client setup to register the renderer */
  public ArmorModelDispatcher getArmorModel() {
    return armorModel;
  }

  @Nullable
  @Override
  public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
    return ArmorUtil.getDummyArmorTexture(slot);
  }

// TODO: PORT - Forge's initializeClient(Consumer<IClientItemExtensions>) has no Fabric equivalent. The dispatcher is
//  created as a field instead and registered through ArmorRendererRegistry when ArmorModelManager.init() runs on the
//  client. See docs/BEHAVIOUR-DIFFERENCES.md #23.
}
