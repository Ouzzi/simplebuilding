package com.simplestorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The copper cable of the chest network (BS29b-3): a thin cable that connects to every neighbouring cable and to the free sides
 * of interfaces. Also a decoration block - it draws arms like a chorus plant ({@link PipeBlock} shapes), so lines, corners and
 * pillars work. It carries no data; placing, breaking and neighbour changes only tell the controllers to search again.
 * With {@code chestNetwork} switched off in the config it turns into copper bars ({@link NetworkSubstitutes}).
 */
public class CableBlock extends PipeBlock {
    public CableBlock(BlockBehaviour.Properties properties) {
        super(4.0F, properties);
        BlockState state = this.stateDefinition.any();
        for (Direction direction : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), false);
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!NetworkSubstitutes.enabled()) return null;
        return withArms(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    public static BlockState withArms(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction),
                    ChestNetwork.conducts(level.getBlockState(pos.relative(direction)), direction.getOpposite()));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), ChestNetwork.conducts(neighborState, direction.getOpposite()));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        ChestNetwork.changed();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        ChestNetwork.changed();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        ChestNetwork.changed();
        NetworkSubstitutes.replaceIfDisabled(level, pos, state);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        NetworkSubstitutes.replaceIfDisabled(level, pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return NetworkSubstitutes.replaceIfDisabled(level, pos, state) ? InteractionResult.SUCCESS : super.useWithoutItem(state, level, pos, player, hit);
    }
}
