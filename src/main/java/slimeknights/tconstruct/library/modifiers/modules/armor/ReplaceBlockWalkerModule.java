package slimeknights.tconstruct.library.modifiers.modules.armor;

import com.google.common.collect.ImmutableList;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import io.github.fabricators_of_create.porting_lib.event.common.BlockEvents;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.loadable.common.BlockStateLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.json.predicate.tool.ToolContextPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

import static slimeknights.tconstruct.library.modifiers.ModifierEntry.VALID_LEVEL;
import net.minecraft.world.entity.player.Player;

/**
 * Module to replace blocks with another block while walking.
 * @param replacements  List of replacements to perform
 * @param radius        Range to affect
 * @param tool          Tool condition
 */
public record ReplaceBlockWalkerModule(List<BlockReplacement> replacements, LevelingValue radius, IJsonPredicate<IToolContext> tool) implements ArmorWalkRadiusModule<Void>, ModifierModule {
  public static final RecordLoadable<ReplaceBlockWalkerModule> LOADER = RecordLoadable.create(
    BlockReplacement.LOADABLE.list().requiredField("replace", ReplaceBlockWalkerModule::replacements),
    LevelingValue.LOADABLE.requiredField("radius", ReplaceBlockWalkerModule::radius),
    ToolContextPredicate.LOADER.defaultField("tool", ReplaceBlockWalkerModule::tool),
    ReplaceBlockWalkerModule::new);

  /** @apiNote Internal constructor, use {@link #builder()} */
  @Internal
  public ReplaceBlockWalkerModule {}

  @Override
  public float getRadius(IToolStackView tool, ModifierEntry modifier) {
    return radius.compute(modifier.getEffectiveLevel() + tool.getVolatileData().getInt(IModifiable.EXPANDED));
  }

  @Override
  public void onWalk(IToolStackView tool, ModifierEntry modifier, LivingEntity living, BlockPos prevPos, BlockPos newPos) {
    if (this.tool.matches(tool)) {
      ArmorWalkRadiusModule.super.onWalk(tool, modifier, living, prevPos, newPos);
    }
  }

  @Override
  public boolean walkOn(IToolStackView tool, ModifierEntry entry, LivingEntity living, Level world, BlockPos target, MutableBlockPos mutable, Void context) {
    if (world.isEmptyBlock(target)) {
      mutable.set(target.getX(), target.getY() - 1, target.getZ());
      int level = entry.getLevel();
      for (BlockReplacement replacement : replacements) {
        if (replacement.level.test(level)) {
          // target handles matching any desired states like fluid level
          BlockState state = replacement.state;
          if (replacement.target.matches(world.getBlockState(mutable))
              && state.canSurvive(world, mutable) && world.isUnobstructed(state, mutable, CollisionContext.empty())
              && beforePlace(living, world, mutable, state)) {
            world.setBlockAndUpdate(mutable, state);
            world.scheduleTick(mutable, state.getBlock(), Mth.nextInt(living.getRandom(), 60, 120));

            // stop after the first successful replacement, there is no reason to do multiple consecutive
            break;
          }
        }
      }
    }
    return tool.isBroken();
  }

  /** Represents a single replacement handled by this module */
  private record BlockReplacement(IJsonPredicate<BlockState> target, BlockState state, IntRange level) {
    public static final RecordLoadable<BlockReplacement> LOADABLE = RecordLoadable.create(
      BlockPredicate.LOADER.defaultField("target", BlockReplacement::target),
      BlockStateLoadable.DIFFERENCE.directField(BlockReplacement::state), // pulling from this object directly means the keys used are block and properties
      VALID_LEVEL.defaultField("modifier_level", BlockReplacement::level),
      BlockReplacement::new);
  }

  @Override
  public RecordLoadable<ReplaceBlockWalkerModule> getLoader() {
    return LOADER;
  }


  /* Builder */

  public static Builder builder() {
    return new Builder();
  }

  @SuppressWarnings("unused")  // API
  public static class Builder implements LevelingValue.Builder<ReplaceBlockWalkerModule> {
    private final ImmutableList.Builder<BlockReplacement> replacements = ImmutableList.builder();
    @Setter
    @Accessors(fluent = true)
    private IJsonPredicate<IToolContext> tool = ToolContextPredicate.ANY;

    private Builder() {}

    /** Replaces at the given level range */
    private Builder replaceLevelRange(IJsonPredicate<BlockState> target, BlockState replacement, IntRange modifierLevel) {
      this.replacements.add(new BlockReplacement(target, replacement, modifierLevel));
      return this;
    }

    /** Adds the given replacement only at the given level range */
    public Builder replaceLevelRange(IJsonPredicate<BlockState> target, BlockState replacement, int min, int max) {
      return replaceLevelRange(target, replacement, VALID_LEVEL.range(min, max));
    }

    /** Adds the given replacement only at the given level range */
    public Builder replaceMinLevel(IJsonPredicate<BlockState> target, BlockState replacement, int max) {
      return replaceLevelRange(target, replacement, VALID_LEVEL.max(max));
    }

    /** Adds the given replacement only at the given level range */
    public Builder replaceMaxLevel(IJsonPredicate<BlockState> target, BlockState replacement, int min) {
      return replaceLevelRange(target, replacement, VALID_LEVEL.min(min));
    }

    /** Adds the given replacement only at the given level range */
    public Builder replaceAlways(IJsonPredicate<BlockState> target, BlockState replacement) {
      return replaceLevelRange(target, replacement, VALID_LEVEL);
    }

    @Override
    public ReplaceBlockWalkerModule amount(float flat, float eachLevel) {
      List<BlockReplacement> replacements = this.replacements.build();
      if (replacements.isEmpty()) {
        throw new IllegalStateException("Must have at least 1 replacement");
      }
      return new ReplaceBlockWalkerModule(replacements, new LevelingValue(flat, eachLevel), tool);
    }
  }

  /**
   * Fires the block placement check for the replacement. Forge's {@code onBlockPlace} took the entity plus a block
   * snapshot and returned true to cancel; Porting Lib's {@code BlockEvents.BeforePlace} takes a placement context
   * and returns an {@code InteractionResult}, where anything but PASS means the placement was handled and must not
   * go through (see docs/BEHAVIOUR-DIFFERENCES.md #20).
   */
  private static boolean beforePlace(LivingEntity living, Level world, BlockPos pos, BlockState state) {
    // vanilla's BlockPlaceContext wants a player (Forge's variant took any living entity), so non-player walkers skip the check
    if (!(living instanceof Player player)) {
      return true;
    }
    BlockPlaceContext context = new BlockPlaceContext(world, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY,
      new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    return BlockEvents.BEFORE_PLACE.invoker().beforePlace(context) == InteractionResult.PASS;
  }
}
