package com.simplebuilding.modules.simpleend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * End sticks: the scrub that a bud grows in the dark. A bundle of sticks thicker than a placed stick, shaped like a
 * dripstone column: thickness 1 (tip, top of a column) to 3 (base). The thickness follows the block above, so a
 * column always tapers upwards, also when a player builds one.
 */
public class EndSticksBlock extends Block {
    public static final IntegerProperty THICKNESS = IntegerProperty.create("thickness", 1, 3);
    private static final VoxelShape[] SHAPES = {Block.column(6.0, 0.0, 16.0), Block.column(10.0, 0.0, 16.0), Block.column(14.0, 0.0, 16.0)};

    public EndSticksBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(THICKNESS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(THICKNESS) - 1];
    }

    /** Thickness from the block above: open top = tip, under a tip = middle, under a middle or base = base. */
    public BlockState shaped(BlockState above) {
        int t = above.is(this) ? Math.min(3, above.getValue(THICKNESS) + 1) : 1;
        return defaultBlockState().setValue(THICKNESS, t);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return shaped(context.getLevel().getBlockState(context.getClickedPos().above()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.UP ? shaped(neighbourState) : state;
    }
}
