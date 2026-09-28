package slimeknights.tconstruct.tools.modifiers.traits.skull;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.item.PiglinsNeutralItem;
import lombok.Getter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.shared.TinkerAttributes;
import slimeknights.tconstruct.tools.modules.armor.GoldenAttributeModule;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

/** @deprecated use {@link GoldenAttributeModule} with {@link TinkerAttributes#CHRYSOPHILITE} */
@Deprecated(forRemoval = true)
public class ChrysophiliteModifier extends NoLevelsModifier {
  /** @deprecated use {@link slimeknights.tconstruct.shared.TinkerAttributes#CHRYSOPHILITE} */
  @Deprecated(forRemoval = true)
  public static final ComputableDataKey<TotalGold> TOTAL_GOLD = TConstruct.createKey("chrysophilite", TotalGold::new);
  public ChrysophiliteModifier() {
    LivingEntityEvents.DROPS.register(ChrysophiliteModifier::onLivingDrops);
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    hookBuilder.addModule(GoldenAttributeModule.builder(TinkerAttributes.CHRYSOPHILITE, Operation.ADDITION).amount(1, 1));
  }

  /** @deprecated use {@link GoldenAttributeModule#hasGold(EquipmentChangeContext, EquipmentSlot)} */
  @Deprecated(forRemoval = true)
  public static boolean hasGold(EquipmentChangeContext context, EquipmentSlot slotType) {
    IToolStackView tool = context.getToolInSlot(slotType);
    if (tool != null) {
      return tool.getVolatileData().getBoolean(ModifiableArmorItem.PIGLIN_NEUTRAL);
    } else {
      LivingEntity living = context.getEntity();
      return living.getItemBySlot(slotType).getItem() instanceof PiglinsNeutralItem piglinsNeutralItem && piglinsNeutralItem.makesPiglinsNeutral(living.getItemBySlot(slotType), living);
    }
  }

  /** @deprecated use {@link slimeknights.tconstruct.shared.TinkerAttributes#CHRYSOPHILITE} */
  @Deprecated(forRemoval = true)
  public static int getTotalGold(@Nullable Entity entity) {
    return Optional.ofNullable(entity)
                   .flatMap(e -> TinkerDataCapability.CAPABILITY.maybeGet(e))
                   .map(data -> data.get(ChrysophiliteModifier.TOTAL_GOLD))
                   .map(TotalGold::getTotalGold)
                   .orElse(0);
  }

  /** Causes more gold armor to drop */
  private static boolean onLivingDrops(LivingEntity target, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit) {
    if (source != null) {
      int gold = getTotalGold(source.getEntity());
      if (gold > 0) {
        float extraChance = 0.04f * gold;
        // check each slot for gold
        for (EquipmentSlot slot : EquipmentSlot.values()) {
          ItemStack stack = target.getItemBySlot(slot);
          RandomSource random = target.getRandom();
          // if the stack is gold, and it drops, we get it
          // don't have to worry about checking if it already dropped, the stacks are removed on drop
          if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack) && (stack.getItem() instanceof PiglinsNeutralItem piglinsNeutralItem && piglinsNeutralItem.makesPiglinsNeutral(stack, target)) && random.nextFloat() < extraChance) {
            // mobs damage items, its kinda weird
            if (stack.isDamageableItem()) {
              stack.setDamageValue(stack.getMaxDamage() - random.nextInt(1 + random.nextInt(Math.max(stack.getMaxDamage() - 3, 1))));
            }
            // remove stack to prevent further drops
            drops.add(target.spawnAtLocation(stack));
            target.setItemSlot(slot, ItemStack.EMPTY);
          }
        }
      }
    }
    return false;
  }

  /** @deprecated use {@link GoldenAttributeModule.TotalGold} */
  @Deprecated(forRemoval = true)
  public static class TotalGold extends GoldenAttributeModule.TotalGold {}
}
