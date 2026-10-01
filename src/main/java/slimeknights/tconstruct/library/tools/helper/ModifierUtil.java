package slimeknights.tconstruct.library.tools.helper;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerTools;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Generic modifier hooks that don't quite fit elsewhere */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModifierUtil {
  /** Drops an item at the given position */
  public static void dropItem(Level level, double x, double y, double z, ItemStack stack) {
    if (!stack.isEmpty() && !level.isClientSide) {
      ItemEntity ent = new ItemEntity(level, x, y, z, stack);
      ent.setDefaultPickUpDelay();
      RandomSource rand = level.random;
      ent.setDeltaMovement(ent.getDeltaMovement().add((rand.nextFloat() - rand.nextFloat()) * 0.1F,
                                                      rand.nextFloat() * 0.05F,
                                                      (rand.nextFloat() - rand.nextFloat()) * 0.1F));
      level.addFreshEntity(ent);
    }
  }

  /** Drops an item at the entity position */
  public static void dropItem(Entity target, ItemStack stack) {
    if (!stack.isEmpty() && !target.level().isClientSide) {
      ItemEntity ent = new ItemEntity(target.level(), target.getX(), target.getY() + 1, target.getZ(), stack);
      ent.setDefaultPickUpDelay();
      RandomSource rand = target.level().random;
      ent.setDeltaMovement(ent.getDeltaMovement().add((rand.nextFloat() - rand.nextFloat()) * 0.1F,
                                                      rand.nextFloat() * 0.05F,
                                                      (rand.nextFloat() - rand.nextFloat()) * 0.1F));
      target.level().addFreshEntity(ent);
    }
  }

  /** Gets the entity as a living entity, or null if they are not a living entity */
  @Nullable
  public static LivingEntity asLiving(@Nullable Entity entity) {
    if (entity instanceof LivingEntity living) {
      return living;
    }
    return null;
  }

  /** Gets the entity as a player, or null if they are not a player */
  @Nullable
  public static Player asPlayer(@Nullable Entity entity) {
    if (entity instanceof Player player) {
      return player;
    }
    return null;
  }

  /**
   * Checks if the given entity pays the resource costs of using a tool, such as draining the tank or consuming ammo.
   * Creative players get their resources for free, matching {@link ToolDamageUtil#directDamage(IToolStackView, int, LivingEntity, ItemStack)} skipping durability for them.
   * @param entity  Entity using the tool. Anything that is not a creative player consumes, including mobs and null.
   * @return  True if resources should be consumed.
   */
  public static boolean consumesResources(@Nullable LivingEntity entity) {
    return !(entity instanceof Player player) || consumesResources(player);
  }

  /**
   * Checks if the given player pays the resource costs of using a tool, such as draining the tank or consuming ammo.
   * Creative players get their resources for free, matching {@link ToolDamageUtil#directDamage(IToolStackView, int, LivingEntity, ItemStack)} skipping durability for them.
   * @param player  Player using the tool. Null consumes, as only creative players get their resources for free.
   * @return  True if resources should be consumed.
   */
  public static boolean consumesResources(@Nullable Player player) {
    return player == null || !player.isCreative();
  }

  /**
   * Direct method to get the level of a modifier from a stack. If you need to get multiple modifier levels, using {@link ToolStack} is faster
   * @param stack     Stack to check
   * @param modifier  Modifier to search for
   * @return  Modifier level, or 0 if not present or the stack is not modifiable
   */
  public static int getModifierLevel(ItemStack stack, ModifierId modifier) {
    if (!stack.isEmpty() && stack.is(TinkerTags.Items.MODIFIABLE)) {
      CompoundTag nbt = stack.getTag();
      if (nbt != null && nbt.contains(ToolStack.TAG_MODIFIERS, Tag.TAG_LIST)) {
        ListTag list = nbt.getList(ToolStack.TAG_MODIFIERS, Tag.TAG_COMPOUND);
        int size = list.size();
        if (size > 0) {
          String key = modifier.toString();
          for (int i = 0; i < size; i++) {
            CompoundTag entry = list.getCompound(i);
            if (key.equals(entry.getString(ModifierEntry.TAG_MODIFIER))) {
              return entry.getInt(ModifierEntry.TAG_LEVEL);
            }
          }
        }
      }
    }
    return 0;
  }

  /** Checks if the given stack has upgrades */
  public static boolean hasUpgrades(ItemStack stack) {
    if (!stack.isEmpty() && stack.is(TinkerTags.Items.MODIFIABLE)) {
      CompoundTag nbt = stack.getTag();
      return nbt != null && !nbt.getList(ToolStack.TAG_UPGRADES, Tag.TAG_COMPOUND).isEmpty();
    }
    return false;
  }

  /** Checks if the given slot may contain armor */
  public static boolean validArmorSlot(LivingEntity living, EquipmentSlot slot) {
    return slot.isArmor() || living.getItemBySlot(slot).is(TinkerTags.Items.HELD);
  }

  /** Checks if the given slot may contain armor */
  public static boolean validArmorSlot(IToolStackView tool, EquipmentSlot slot) {
    return slot.getType() == Type.ARMOR || tool.hasTag(TinkerTags.Items.HELD);
  }

  /**
   * Adds levels to the given key in entity modifier data for an armor modifier
   * @param tool     Tool instance
   * @param context  Equipment change context
   * @param key      Key to modify
   * @param amount   Amount to add
   */
  public static void addTotalArmorModifierLevel(IToolStackView tool, EquipmentChangeContext context, TinkerDataKey<Integer> key, int amount, boolean allowBroken) {
    if (validArmorSlot(tool, context.getChangedSlot()) && (allowBroken || !tool.isBroken())) {
      context.getTinkerData().ifPresent(data -> {
        int totalLevels = data.get(key, 0) + amount;
        if (totalLevels <= 0) {
          data.remove(key);
        } else {
          data.put(key, totalLevels);
        }
      });
    }
  }

  /**
   * Adds levels to the given key in entity modifier data for an armor modifier
   * @param tool     Tool instance
   * @param context  Equipment change context
   * @param key      Key to modify
   * @param amount   Amount to add
   */
  public static void addTotalArmorModifierLevel(IToolStackView tool, EquipmentChangeContext context, TinkerDataKey<Integer> key, int amount) {
    addTotalArmorModifierLevel(tool, context, key, amount, false);
  }

  /**
   * Adds levels to the given key in entity modifier data for an armor modifier
   * @param tool     Tool instance
   * @param context  Equipment change context
   * @param key      Key to modify
   * @param amount   Amount to add
   */
  public static void addTotalArmorModifierFloat(IToolStackView tool, EquipmentChangeContext context, TinkerDataKey<Float> key, float amount) {
    if (validArmorSlot(tool, context.getChangedSlot()) && !tool.isBroken()) {
      context.getTinkerData().ifPresent(data -> {
        float totalLevels = data.get(key, 0f) + amount;
        if (totalLevels <= 0.005f) {
          data.remove(key);
        } else {
          data.put(key, totalLevels);
        }
      });
    }
  }

  /**
   * Gets the total level from the key in the entity modifier data
   * @param living  Living entity
   * @param key     Key to get
   * @return  Level from the key
   */
  public static int getTotalModifierLevel(LivingEntity living, TinkerDataKey<Integer> key) {
    return TinkerDataCapability.CAPABILITY.maybeGet(living).map(data -> data.get(key)).orElse(0);
  }

  /**
   * Gets the total level from the key in the entity modifier data
   * @param living  Living entity
   * @param key     Key to get
   * @return  Level from the key
   */
  public static float getTotalModifierFloat(LivingEntity living, TinkerDataKey<Float> key) {
    return TinkerDataCapability.CAPABILITY.maybeGet(living).map(data -> data.get(key)).orElse(0f);
  }

  /** Checks if the entity has aqua affinity from either enchants or modifiers */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean hasAquaAffinity(LivingEntity living) {
    return ModifierUtil.getTotalModifierLevel(living, TinkerDataKeys.AQUA_AFFINITY) > 0 || EnchantmentHelper.hasAquaAffinity(living);
  }

  /** Shortcut to get a volatile flag when the tool stack is not needed otherwise */
  public static boolean checkVolatileFlag(ItemStack stack, ResourceLocation flag) {
    CompoundTag nbt = stack.getTag();
    if (nbt != null && nbt.contains(ToolStack.TAG_VOLATILE_MOD_DATA, Tag.TAG_COMPOUND)) {
      return nbt.getCompound(ToolStack.TAG_VOLATILE_MOD_DATA).getBoolean(flag.toString());
    }
    return false;
  }

  /** Shortcut to get a persistent flag when the tool stack is not needed otherwise */
  public static boolean checkPersistentPresent(ItemStack stack, ResourceLocation key) {
    CompoundTag nbt = stack.getTag();
    if (nbt != null && nbt.contains(ToolStack.TAG_VOLATILE_MOD_DATA, Tag.TAG_COMPOUND)) {
      return nbt.getCompound(ToolStack.TAG_VOLATILE_MOD_DATA).contains(key.toString());
    }
    return false;
  }

  /** Shortcut to get a volatile int value when the tool stack is not needed otherwise */
  public static int getVolatileInt(ItemStack stack, ResourceLocation flag) {
    CompoundTag nbt = stack.getTag();
    if (nbt != null && nbt.contains(ToolStack.TAG_VOLATILE_MOD_DATA, Tag.TAG_COMPOUND)) {
      return nbt.getCompound(ToolStack.TAG_VOLATILE_MOD_DATA).getInt(flag.toString());
    }
    return 0;
  }

  /** Shortcut to get a volatile int value when the tool stack is not needed otherwise */
  public static int getPersistentInt(ItemStack stack, ResourceLocation flag, int defealtValue) {
    CompoundTag nbt = stack.getTag();
    if (nbt != null && nbt.contains(ToolStack.TAG_PERSISTENT_MOD_DATA, Tag.TAG_COMPOUND)) {
      CompoundTag persistent = nbt.getCompound(ToolStack.TAG_PERSISTENT_MOD_DATA);
      String flagString = flag.toString();
      if (persistent.contains(flagString, Tag.TAG_INT)) {
        return persistent.getInt(flagString);
      }
    }
    return defealtValue;
  }

  /** Shortcut to get a persistent string value when the tool stack is not needed otherwise */
  public static String getPersistentString(ItemStack stack, ResourceLocation flag) {
    CompoundTag nbt = stack.getTag();
    if (nbt != null && nbt.contains(ToolStack.TAG_PERSISTENT_MOD_DATA, Tag.TAG_COMPOUND)) {
      return nbt.getCompound(ToolStack.TAG_PERSISTENT_MOD_DATA).getString(flag.toString());
    }
    return "";
  }

  /** Checks if a tool can perform the given action */
  public static boolean canPerformAction(IToolStackView tool, ToolAction action) {
    if (!tool.isBroken()) {
      // can the tool do this action inherently?
      if (tool.getHook(ToolHooks.TOOL_ACTION).canPerformAction(tool, action)) {
        return true;
      }
      for (ModifierEntry entry : tool.getModifierList()) {
        if (entry.getHook(ModifierHooks.TOOL_ACTION).canPerformAction(tool, entry, action)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Makes the tool use the blocking animation if the blocking modifier is installed, falling back to the given animation.
   * Allows your tool to block while charging up.
   */
  public static UseAnim blockWhileCharging(IToolStackView tool, UseAnim fallback) {
    return canPerformAction(tool, ToolActions.SHIELD_BLOCK) ? UseAnim.BLOCK : fallback;
  }

  /** Calculates inaccuracy from the conditional tool stat. */
  public static float getInaccuracy(IToolStackView tool, @Nullable LivingEntity living) {
    return 3 * (1 / ConditionalStatModifierHook.getModifiedStat(tool, living, ToolStats.ACCURACY) - 1);
  }

  /** @deprecated use {@link GeneralInteractionModifierHook#addCooldown(IToolStackView, Player, float)} */
  @Deprecated(forRemoval = true)
  public static void addCooldown(IToolStackView tool, Player player) {
    GeneralInteractionModifierHook.addCooldown(tool, player, 1);
  }

  /** Checks if this modifier is the one actively being used. Used for failure sound effects. */
  public static boolean isActiveModifier(IToolStackView tool, ModifierEntry modifier, ModifierEntry activeModifier) {
    // active modifier being us, or a bow is firing and no drawback ammo
    return modifier == activeModifier || (activeModifier.getLevel() == 0 && !tool.getPersistentData().contains(ModifiableLauncherItem.KEY_DRAWBACK_AMMO));
  }

  /**
   * Called before you call {@link Projectile#discard()} to update the fishing rod stack on the player.
   *
   * @param projectile  Projectile, will check if its our fishing bobber.
   * @param damage      Damage to deal to the rod.
   * @param applyCooldown  If true, applies draw speed as an item cooldown.
   * @return hand containing the fishing rod, or null if its in neither hand.
   */
  @SuppressWarnings("UnusedReturnValue") // API
  @Nullable
  public static InteractionHand updateFishingRod(Projectile projectile, int damage, boolean applyCooldown) {
    return updateFishingRod(projectile, damage, applyCooldown, ModifierId.EMPTY);
  }

  /**
   * Called before you call {@link Projectile#discard()} to update the fishing rod stack on the player.
   *
   * @param projectile  Projectile, will check if its our fishing bobber.
   * @param damage      Damage to deal to the rod.
   * @param applyCooldown  If true, applies draw speed as an item cooldown.
   * @param cause       Modifier causing the retraction.
   * @return hand containing the fishing rod, or null if its in neither hand.
   */
  @Nullable
  public static InteractionHand updateFishingRod(Projectile projectile, int damage, boolean applyCooldown, ModifierId cause) {
    if (projectile.getType() == TinkerTools.fishingHook.get() && projectile.getOwner() instanceof LivingEntity living) {
      ItemStack stack = living.getMainHandItem();
      InteractionHand hand = InteractionHand.MAIN_HAND;
      // must be able to cast
      if (!stack.canPerformAction(ToolActions.FISHING_ROD_CAST)) {
        stack = living.getOffhandItem();
        if (!stack.canPerformAction(ToolActions.FISHING_ROD_CAST)) {
          return null;
        }
        hand = InteractionHand.OFF_HAND;
      }
      // must be modifiable
      if (stack.is(TinkerTags.Items.MODIFIABLE)) {
        // skip making the tool stack object if not needed, might be asking just for the hand.
        if (applyCooldown || damage > 0) {
          IToolStackView tool = ToolStack.from(stack);
          // trigger cooldown on the item
          if (applyCooldown && living instanceof Player player) {
            addCooldown(tool, player);
          }
          // damage the rod
          if (damage > 0) {
            // if we are applying cooldown, means this is a full retraction from block so this was primary damage
            // no cooldown is done on secondary effects like entity hitting
            ToolDamageUtil.damageAnimated(tool, damage, living, hand, cause);
          }
        }
        return hand;
      }
    }
    return null;
  }

  /** Interface used for {@link #foodConsumer} */
  public interface FoodConsumer {
    /** Called when food is eaten to notify compat that food was eaten */
    void onConsume(Player player, ItemStack stack, int hunger, float saturation);

    /** Called when a list of foods is eaten at once is eaten to notify compat that food was eaten */
    default void onConsume(Player player, List<ItemStack> stacks, int hunger, float saturation) {}
  }

  /** Instance of the current food consumer, will be either no-op or an implementation calling the Diet API, never null. */
  @Nonnull
  public static FoodConsumer foodConsumer = (player, stack, hunger, saturation) -> {};

  /* Shield disabling */
  /** Map of how to disable shields for different targets */
  private static final Map<EntityType<?>, Consumer<Entity>> SHIELD_DISABLER = new HashMap<>();

  /** Registers a method for shield disabling */
  public static void registerShieldDisabler(Consumer<Entity> disabler, EntityType<?>... types) {
    for (EntityType<?> type : types){
      SHIELD_DISABLER.putIfAbsent(type, disabler);
    }
  }

  /** Disables shield for the target entity */
  public static void disableShield(Entity entity) {
    Consumer<Entity> consumer = SHIELD_DISABLER.get(entity.getType());
    if (consumer != null) {
      consumer.accept(entity);
    }
  }
}
