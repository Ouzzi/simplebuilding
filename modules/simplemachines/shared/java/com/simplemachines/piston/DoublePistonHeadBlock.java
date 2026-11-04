package com.simplemachines.piston;

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
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The head of the Double Piston: a 4 px plate and a rod like Vanilla's piston head. It needs the base (stage 1) or the arm
 * (stage 2) behind it, else it goes (and the {@code updateShape} chain takes the rest along). No loot: the base drops the piston.
 * {@code short} is Vanilla's own property of the piston head: Vanilla's renderer sets it while the head comes out of the base, so
 * the rod does not stick out at the back. {@code sleeve} is only set on the moving head while stage 2 goes back to 1: the arm's
 * sleeve rides along, and the head drops it once it has arrived ({@link #updateShape}).
 */
public class DoublePistonHeadBlock extends DirectionalBlock {
    private static final Map<Direction, VoxelShape> SHAPES = DoublePistons.facing(new double[] {0, 0, 0, 16, 16, 4}, new double[] {6, 6, 4, 10, 10, 16});
    public static final BooleanProperty SHORT = PistonHeadBlock.SHORT;
    public static final BooleanProperty SLEEVE = BooleanProperty.create("sleeve");
    private final DoublePistonVariant variant;

    public DoublePistonHeadBlock(DoublePistonVariant variant, BlockBehaviour.Properties properties) {
        super(properties);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHORT, false).setValue(SLEEVE, false));
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
        if (directionToNeighbour == state.getValue(FACING).getOpposite() && !DoublePistons.holdsHead(neighbourState, state)) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState shaped = super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        return shaped.is(this) ? shaped.setValue(SLEEVE, false).setValue(SHORT, false) : shaped;
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
        builder.add(FACING, SHORT, SLEEVE);
    }
}
