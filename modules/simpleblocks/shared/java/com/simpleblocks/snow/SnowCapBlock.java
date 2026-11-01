package com.simpleblocks.snow;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * SNOW32-FALL (b-snow02, docs/ai/PLAN-B-SNOW02-2026-11-01.md): the single snow layer that falling snow puts on top of fences,
 * walls, stairs, bottom slabs and other blocks that have no full top face (where Vanilla snow cannot stand). It is a thin
 * layer in the air cell above (or, on a bottom slab, drawn half a block lower), has no collision, never stacks, takes nothing
 * from the block below (no state of it changes) and goes away like snow: by hand at once, with a shovel it drops a snowball,
 * in bright light it melts, a block placed into it replaces it, and it vanishes when its support is gone.
 */
public class SnowCapBlock extends Block {
    /** Half a block lower: the support is a bottom slab (top at 0.5) instead of a block whose top reaches the cell edge. */
    public static final BooleanProperty LOWERED = BooleanProperty.create("lowered");
    private static final VoxelShape FLUSH = Block.column(16.0, 0.0, 2.0);
    private static final VoxelShape LOW = Block.column(16.0, -8.0, -6.0);

    /** What the block below offers: no support, a top at the cell edge (fence, wall, stairs), or a top at half height (slab). */
    public enum Support { NONE, FLUSH, HALF }

    public SnowCapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LOWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOWERED);
    }

    /**
     * Where Vanilla snow cannot stand but a layer can rest visually: the highest collision point of the block below is the
     * cell edge or above (fences, walls, panes, stairs) or exactly half (bottom slab). Full top faces are Vanilla's.
     */
    public static Support supportBelow(LevelReader level, BlockPos capPos) {
        BlockPos below = capPos.below();
        BlockState state = level.getBlockState(below);
        if (state.isAir() || !state.getFluidState().isEmpty() || state.getBlock() instanceof SnowCapBlock
                || state.is(SnowBlocks.SNOWLOGGED) || state.is(BlockTags.SNOW) || state.is(BlockTags.ICE)
                || state.is(Blocks.BARRIER) || state.is(Blocks.HONEY_BLOCK) || state.is(Blocks.SOUL_SAND)) {
            return Support.NONE;
        }
        VoxelShape shape = state.getCollisionShape(level, below);
        if (shape.isEmpty() || Block.isFaceFull(shape, Direction.UP)) {
            return Support.NONE;
        }
        double top = shape.max(Axis.Y);
        if (top >= 1.0) return Support.FLUSH;
        return Math.abs(top - 0.5) < 1.0E-4 ? Support.HALF : Support.NONE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(LOWERED) ? LOW : FLUSH;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return supportBelow(level, pos) == (state.getValue(LOWERED) ? Support.HALF : Support.FLUSH);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** Melts in bright block light like Vanilla snow. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBrightness(LightLayer.BLOCK, pos) > 11) {
            level.removeBlock(pos, false);
        }
    }

    /** Vanilla's one-layer snow loot (a snowball with a shovel, none by hand or from melting). */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return Blocks.SNOW.defaultBlockState().getDrops(params);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Blocks.SNOW);
    }
}
