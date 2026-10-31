package com.simplebuilding.machines;

import java.util.Map;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cell in front of the base at stage 2 (IG31-26): the front quarter of the piston body, which moved half a block forward,
 * and the rod on to the head. It needs the base behind it and the head (or the head's move) in front of it.
 */
public class DoublePistonArmBlock extends DirectionalBlock {
    private static final Map<Direction, VoxelShape> SHAPES = DoublePistons.facing(new double[] {0, 0, 12, 16, 16, 16}, new double[] {6, 6, 0, 10, 10, 12});
    private final DoublePistonVariant variant;

    public DoublePistonArmBlock(DoublePistonVariant variant, BlockBehaviour.Properties properties) {
        super(properties);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public DoublePistonVariant variant() {
        return variant;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Direction f = state.getValue(FACING);
        if (directionToNeighbour == f.getOpposite() && !DoublePistons.isBase(neighbourState, variant, f, 2)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (directionToNeighbour == f && !neighbourState.is(Blocks.MOVING_PISTON) && !DoublePistons.isHead(neighbourState, variant, f)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DoublePistons.creativeBreak(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private com.mojang.serialization.MapCodec<DoublePistonArmBlock> codec;

    // Nur 26.2 verlangt codec(); 26.3 kennt es nicht mehr (darum ohne @Override, wie EndRailBlock).
    protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() {
        if (codec == null) codec = com.simplebuilding.version.BlockCodecs.simple(p -> new DoublePistonArmBlock(variant, p));
        return codec;
    }
}
