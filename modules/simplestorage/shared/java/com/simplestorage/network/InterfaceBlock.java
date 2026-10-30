package com.simplestorage.network;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The interface plate of the chest network (BS29b-3): sits in the cell next to a chest and points at it ({@link #FACING} is the
 * direction to the chest; it drops off when the chest is gone). On its other five sides it connects to cables and other
 * interfaces ({@code north..down} = a wire arm there). A chest with an interface is part of the network of the controller its
 * wires lead to; the controller needs one on itself as well. With {@code chestNetwork} off in the config it turns into a glass
 * pane ({@link NetworkSubstitutes}).
 */
public class InterfaceBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    /** The plate (14 x 14 x 3) and the post from it to the middle, for a chest to the north; turned for the others. */
    private static final Map<Direction, VoxelShape> PLATE = Shapes.rotateAll(Shapes.or(Block.boxZ(14.0, 0.0, 3.0), Block.boxZ(4.0, 3.0, 8.0)));
    private static final VoxelShape CORE = Block.cube(4.0);
    /** The wire arm from the middle to the edge, for each side. */
    private static final Map<Direction, VoxelShape> ARMS = Shapes.rotateAll(Block.boxZ(4.0, 0.0, 8.0));

    private final Function<BlockState, VoxelShape> shapes;

    public InterfaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any().setValue(FACING, Direction.DOWN);
        for (Direction direction : Direction.values()) state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), false);
        this.registerDefaultState(state);
        this.shapes = this.getShapeForEachState(shapeState -> {
            VoxelShape shape = Shapes.or(PLATE.get(shapeState.getValue(FACING)), CORE);
            for (Direction direction : Direction.values()) {
                if (shapeState.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) shape = Shapes.or(shape, ARMS.get(direction));
            }
            return shape;
        });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!NetworkSubstitutes.enabled()) return null;
        Direction facing = context.getClickedFace().getOpposite();
        BlockPos pos = context.getClickedPos();
        if (!(context.getLevel().getBlockState(pos.relative(facing)).getBlock() instanceof ChestBlock)) return null;
        return withArms(this.defaultBlockState().setValue(FACING, facing), context.getLevel(), pos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING))).getBlock() instanceof ChestBlock;
    }

    public static BlockState withArms(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            boolean arm = direction != state.getValue(FACING)
                    && ChestNetwork.conducts(level.getBlockState(pos.relative(direction)), direction.getOpposite());
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), arm);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == state.getValue(FACING) && !(neighborState.getBlock() instanceof ChestBlock)) return Blocks.AIR.defaultBlockState();
        boolean arm = direction != state.getValue(FACING) && ChestNetwork.conducts(neighborState, direction.getOpposite());
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), arm);
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
