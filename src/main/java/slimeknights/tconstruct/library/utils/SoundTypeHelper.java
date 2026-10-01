package slimeknights.tconstruct.library.utils;

import io.github.fabricators_of_create.porting_lib.block.CustomSoundTypeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Helper to reproduce Forge's block level sound lookup.
 * <p>
 * Upstream calls {@code IForgeBlock#getSoundType(state, level, pos, entity)}, which lets a block vary its sound by
 * position (Forge's default implementation is just the vanilla {@link BlockState#getSoundType()}). Porting Lib
 * exposes the same override point as {@link CustomSoundTypeBlock}, so the two are combined here instead of repeating
 * the check at every call site.
 */
public final class SoundTypeHelper {
  private SoundTypeHelper() {}

  /** Gets the sound type for the given state, honoring the block's Porting Lib override if it has one */
  public static SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
    if (state.getBlock() instanceof CustomSoundTypeBlock custom) {
      return custom.getSoundType(state, level, pos, entity);
    }
    return state.getSoundType();
  }
}
