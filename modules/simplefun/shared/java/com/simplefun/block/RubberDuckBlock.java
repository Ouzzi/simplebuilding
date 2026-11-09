package com.simplefun.block;

import com.simplefun.registry.ModToys;
import com.simplefun.toys.FunToys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * FUN11-9 Quietscheente: decoration that sits on a water surface (like a lily pad) or on solid ground. A click squeaks
 * and the duck hops for {@link #SQUEAK_TICKS} ticks (state {@code squeak}); while it hops further clicks stay silent, so a
 * held click cannot spam. Placed on soul sand it becomes the pirate duck. Touching it does nothing.
 */
public class RubberDuckBlock extends Block {
  public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
  public static final BooleanProperty PIRATE = BooleanProperty.create("pirate");
  public static final BooleanProperty SQUEAK = BooleanProperty.create("squeak");
  public static final int SQUEAK_TICKS = 6;
  private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 9, 12);

  public RubberDuckBlock(Properties properties) {
    super(properties);
    registerDefaultState(
        stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PIRATE, false).setValue(SQUEAK, false));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, PIRATE, SQUEAK);
  }

  @Override
  protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return SHAPE;
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    BlockState state =
        defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(PIRATE, context.getLevel().getBlockState(context.getClickedPos().below()).is(Blocks.SOUL_SAND));
    return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
  }

  /** On a water source with air above (like a lily pad), or on a sturdy top face. */
  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    BlockPos below = pos.below();
    if (level.getFluidState(pos).isEmpty()
        && level.getFluidState(below).is(FluidTags.WATER)
        && level.getFluidState(below).isSource()) return true;
    return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
  }

  @Override
  protected BlockState updateShape(
      BlockState state,
      LevelReader level,
      ScheduledTickAccess ticks,
      BlockPos pos,
      Direction direction,
      BlockPos neighbourPos,
      BlockState neighbourState,
      RandomSource random) {
    return direction == Direction.DOWN && !canSurvive(state, level, pos)
        ? Blocks.AIR.defaultBlockState()
        : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (!FunToys.on("rubberDuck")) return InteractionResult.PASS;
    if (level instanceof ServerLevel serverLevel) squeak(serverLevel, pos, state);
    return InteractionResult.SUCCESS;
  }

  /** One squeak and hop; {@code false} while the duck still hops (the spam guard). */
  public static boolean squeak(ServerLevel level, BlockPos pos, BlockState state) {
    if (!(state.getBlock() instanceof RubberDuckBlock) || state.getValue(SQUEAK)) return false;
    level.setBlock(pos, state.setValue(SQUEAK, true), Block.UPDATE_CLIENTS);
    level.scheduleTick(pos, state.getBlock(), SQUEAK_TICKS);
    level.playSound(
        null, pos, ModToys.DUCK_SQUEAK, SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.3f);
    boolean water = level.getFluidState(pos.below()).is(FluidTags.WATER);
    level.sendParticles(
        water ? ParticleTypes.SPLASH : ParticleTypes.NOTE,
        pos.getX() + 0.5,
        pos.getY() + (water ? 0.1 : 0.8),
        pos.getZ() + 0.5,
        water ? 6 : 1,
        0.2,
        0.0,
        0.2,
        water ? 0.05 : 0.0);
    return true;
  }

  @Override
  protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (state.getValue(SQUEAK)) level.setBlock(pos, state.setValue(SQUEAK, false), Block.UPDATE_CLIENTS);
  }
}
