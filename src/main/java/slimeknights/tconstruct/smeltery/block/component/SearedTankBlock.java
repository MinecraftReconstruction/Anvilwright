package slimeknights.tconstruct.smeltery.block.component;

import io.github.fabricators_of_create.porting_lib.block.LightEmissiveBlock;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity.ITankBlock;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.mantle.util.BlockEntityHelper;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.function.ToIntFunction;

@SuppressWarnings("removal")
public class SearedTankBlock extends SearedBlock implements ITankBlock, EntityBlock, BlockPickInteractionAware, LightEmissiveBlock {
  @Getter
  private final long capacity;
  /** Block state property for the light level of the contained fluid */
  public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
  /** Gets the light level from a block state */
  public static final ToIntFunction<BlockState> LIGHT_GETTER = state -> state.getValue(SearedTankBlock.LIGHT);
  /** How pistons treat this block */
  private final PushReaction pushReaction;
  public SearedTankBlock(Properties properties, long capacity, PushReaction pushReaction) {
    // vanilla has no overridable piston reaction method (Forge patches one in), so bake it into the properties
    super(properties.pushReaction(pushReaction), true);
    this.capacity = capacity;
    this.pushReaction = pushReaction;
    registerDefaultState(defaultBlockState().setValue(LIGHT, 0));
  }

  public SearedTankBlock(Properties properties, long capacity) {
    this(properties, capacity, PushReaction.BLOCK);
  }

  @Override
  protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(LIGHT);
  }

  @Deprecated
  @Override
  public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
    return 1.0F;
  }

  @Override
  @Nullable
  public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
    return new TankBlockEntity(pPos, pState, this);
  }

  @Deprecated
  @Override
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (FluidTransferHelper.interactWithTank(world, pos, player, hand, hit)) {
      return InteractionResult.SUCCESS;
    }
    return super.use(state, world, pos, player, hand, hit);
  }

  @Override
  public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
    BlockEntity te = world.getBlockEntity(pos);
    if (te instanceof TankBlockEntity) {
      FluidStack fluid = ((TankBlockEntity) te).getTank().getFluid();
      return FluidVariantAttributes.getLuminance(fluid.getType());
    }
    return state.getLightEmission();
  }

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    CompoundTag nbt = stack.getTag();
    if (nbt != null && world.getBlockEntity(pos) instanceof TankBlockEntity tank) {
      tank.updateTank(nbt.getCompound(NBTTags.TANK));
    }
    super.setPlacedBy(world, pos, state, placer, stack);
  }

  @Deprecated
  @Override
  public boolean hasAnalogOutputSignal(BlockState state) {
    return true;
  }

  @Deprecated
  @Override
  public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
    return ITankBlockEntity.getComparatorInputOverride(worldIn, pos);
  }

  @Override
  public ItemStack getPickedStack(BlockState state, BlockGetter view, BlockPos pos, @Nullable Player player, @Nullable HitResult result) {
    ItemStack stack = new ItemStack(this);
    BlockEntityHelper.get(TankBlockEntity.class, view, pos).ifPresent(te -> te.setTankTag(stack));
    return stack;
  }

  @AllArgsConstructor
  public enum TankType implements StringRepresentable {
    FUEL_TANK(TankBlockEntity.DEFAULT_CAPACITY),
    FUEL_GAUGE(TankBlockEntity.DEFAULT_CAPACITY),
    INGOT_TANK(FluidValues.INGOT * 48),
    INGOT_GAUGE(FluidValues.INGOT * 48);

    @Getter
    private final long capacity;

    @Override
    public String getSerializedName() {
      return this.toString().toLowerCase(Locale.US);
    }
  }
}
