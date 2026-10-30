package com.simpleredstone.piston;

import com.simpleredstone.RedstoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The front cell of the Double Piston: the barrel the rod runs out of. It has no loot (the base drops the piston) and falls
 * with the base; with the head out it needs the head (ext 1) or the arm (ext 2) in front of it.
 */
public class DoublePistonBarrelBlock extends DirectionalBlock {
    public DoublePistonBarrelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DoublePistons.EXT, 0));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Direction f = state.getValue(FACING);
        if (directionToNeighbour == f.getOpposite() && !DoublePistons.isPart(neighbourState, RedstoneRegistry.DOUBLE_PISTON, f)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (directionToNeighbour == f && state.getValue(DoublePistons.EXT) > 0 && !neighbourState.is(Blocks.MOVING_PISTON)
                && !DoublePistons.isPart(neighbourState, state.getValue(DoublePistons.EXT) == 1 ? RedstoneRegistry.PISTON_HEAD : RedstoneRegistry.PISTON_ARM, f)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()) {
            BlockPos base = DoublePistons.findBase(level, pos, state.getValue(FACING));
            if (base != null) level.setBlock(base, Blocks.AIR.defaultBlockState(), 35);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DoublePistons.EXT);
    }
}
