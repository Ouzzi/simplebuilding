package com.simplelights.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Light Pane (Besitzer N57, 2026-10-14; docs/ai/PLAN-LIGHT-PANE-2026-10-14.md): a flat lamp, 16x16 px and 1 px thick like an item
 * frame, on any of the six sides of a block. {@link #FACING} is the side the pane looks to; it hangs on the block behind it
 * ({@code pos.relative(facing.getOpposite())}) and drops as an item when that block goes or no longer carries it with a full face.
 * It has no collision (one walks through it like through a frame), a 1 px selection box, and can be waterlogged. The state follows
 * the redstone signal ({@link #applySignal}) on placement, on neighbor updates and when placed from a command.
 */
public abstract class AbstractLightPaneBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** The lamp light of every pane that shines. */
    public static final int LIGHT = 15;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.UP, Block.box(0, 0, 0, 16, 1, 16));
        SHAPES.put(Direction.DOWN, Block.box(0, 15, 0, 16, 16, 16));
        SHAPES.put(Direction.NORTH, Block.box(0, 0, 15, 16, 16, 16));
        SHAPES.put(Direction.SOUTH, Block.box(0, 0, 0, 16, 16, 1));
        SHAPES.put(Direction.WEST, Block.box(15, 0, 0, 16, 16, 16));
        SHAPES.put(Direction.EAST, Block.box(0, 0, 0, 1, 16, 16));
    }

    protected AbstractLightPaneBlock(Properties properties) {
        super(properties);
    }

    /** Called from the block constructor: the subclass fields are not set yet, only the property list may be added to. */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    /** The state for the redstone signal at {@code pos} (lit, or the color); the facing and water stay. */
    protected abstract BlockState applySignal(BlockState state, Level level, BlockPos pos);

    /** The pane for the clicked side, or null when the block behind does not carry it (or it would not fit). */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace())
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        return state.canSurvive(level, pos) ? applySignal(state, level, pos) : null;
    }

    public static BlockPos supportPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = supportPos(state, pos);
        return level.getBlockState(support).isFaceSturdy(level, support, state.getValue(FACING), SupportType.FULL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        refresh(state, level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        refresh(state, level, pos);
    }

    /** Server side: moves the state to the current signal, only when it really changes (a setBlock rebuilds the client chunk). */
    protected final void refresh(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        BlockState next = applySignal(state, level, pos);
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
