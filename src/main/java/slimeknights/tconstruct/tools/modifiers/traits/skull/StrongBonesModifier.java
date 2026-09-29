package slimeknights.tconstruct.tools.modifiers.traits.skull;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityUseItemEvents;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.PotionHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.modifiers.modules.technical.ArmorLevelModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.CureOnRemovalModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;

import javax.annotation.Nonnull;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.fluid.FluidAction;

public class StrongBonesModifier extends TotalArmorLevelModifier {
  private static final TinkerDataKey<Integer> STRONG_BONES = TConstruct.createKey("strong_bones");
  /** Key for modifiers that are boosted by drinking milk */
  public static final TinkerDataKey<Integer> CALCIFIABLE = TConstruct.createKey("calcifable");
  /** Module to add to any calcifiable modifiers */
  public static final ArmorLevelModule CALCIFIABLE_MODULE = new ArmorLevelModule(CALCIFIABLE, false, TinkerTags.Items.HELD_ARMOR);

  public StrongBonesModifier() {
    super(STRONG_BONES, true);
    LivingEntityUseItemEvents.LIVING_USE_ITEM_FINISH.register(StrongBonesModifier::onItemFinishUse);
  }

  @Override
  public void onUnequip(IToolStackView tool, int level, EquipmentChangeContext context) {
    super.onUnequip(tool, level, context);
    if (context.getChangedSlot() == EquipmentSlot.HEAD) {
      IToolStackView replacement = context.getReplacementTool();
      if (replacement == null || replacement.getModifierLevel(this) == 0) {
        // cure effects using the helmet
        PotionHelper.curePotionEffects(context.getEntity(), new ItemStack(tool.getItem()));
      }
    }
  }

  private static boolean drinkMilk(LivingEntity living, int flat, int eachLevel, FluidAction action) {
    // strong bones has to be the helmet as we use it for curing
    // TODO 1.20: can use the new cure effects to make this work in any slot
    ItemStack helmet = living.getItemBySlot(EquipmentSlot.HEAD);
    boolean didSomething = false;
    int level = ModifierUtil.getModifierLevel(helmet, TinkerModifiers.strongBones.getId());
    if (level > 0) {
      MobEffectInstance effect = new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, flat + eachLevel * level);
      effect.getCurativeItems().clear();
      effect.getCurativeItems().add(new ItemStack(helmet.getItem()));
      // on simulate, don't apply the effect, just ask if we can apply
      didSomething = action.execute() ? living.addEffect(effect) : living.canBeAffected(effect);
      // quick exit on simulate: no more information needed
      if (didSomething && action.simulate()) {
        return true;
      }
    }
    level = ArmorLevelModule.getLevel(living, CALCIFIABLE);
    if (level > 0) {
      MobEffectInstance effect = new MobEffectInstance(TinkerModifiers.calcifiedEffect.get(), flat + eachLevel * level, 0);
      didSomething |= action.execute() ? living.addEffect(effect) : living.canBeAffected(effect);
    }
    return didSomething;
  }

  /** Called when you finish drinking milk */
  private static ItemStack onItemFinishUse(LivingEntity living, @Nonnull ItemStack item, int duration, @Nonnull ItemStack result) {
    if (item.getItem() == Items.MILK_BUCKET) {
      drinkMilk(living, 1200);
    }
    return result;
  }


  /* Spilling effect */

  /** Singleton instance spilling effect */
  public static final FluidEffect<FluidEffectContext.Entity> FLUID_EFFECT = FluidEffect.simple((fluid, scale, context, action) -> {
    LivingEntity target = context.getLivingTarget();
    // while we could scale, doing it flat ensures we don't charge extra
    if (target != null && drinkMilk(target, 0, (int)(20*10 * scale.value()), action)) {
      return scale.value();
    }
    return 0;
  });
}
