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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
    /**
     * The plate reaches to a wall (IG31-6): "a wall beside this edge of the plate". Named from the plate's own view for a chest to the
     * north (E = +x, W = -x, U = up, D = down) and turned with {@link #FACING} like the model ({@link #localSides}).
     */
    public static final BooleanProperty EDGE_E = BooleanProperty.create("edge_e"), EDGE_W = BooleanProperty.create("edge_w"),
            EDGE_U = BooleanProperty.create("edge_u"), EDGE_D = BooleanProperty.create("edge_d");
    private static final BooleanProperty[] EDGES = {EDGE_E, EDGE_W, EDGE_U, EDGE_D};

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
        for (BooleanProperty edge : EDGES) state = state.setValue(edge, false);
        this.registerDefaultState(state);
        this.shapes = this.getShapeForEachState(shapeState -> {
            VoxelShape shape = Shapes.or(PLATE.get(shapeState.getValue(FACING)), CORE);
            for (Direction direction : Direction.values()) {
                if (shapeState.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) shape = Shapes.or(shape, ARMS.get(direction));
            }
            Direction[] sides = localSides(shapeState.getValue(FACING));
            for (int i = 0; i < EDGES.length; i++) {
                if (shapeState.getValue(EDGES[i])) shape = Shapes.or(shape, edgeStrip(shapeState.getValue(FACING), sides[i]));
            }
            return shape;
        });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN, EDGE_E, EDGE_W, EDGE_U, EDGE_D);
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
        Direction[] sides = localSides(state.getValue(FACING));
        for (int i = 0; i < EDGES.length; i++) {
            BlockPos next = pos.relative(sides[i]);
            state = state.setValue(EDGES[i], wall(level, next, level.getBlockState(next), sides[i]));
        }
        return state;
    }

    /** For a plate on {@code facing}: the absolute sides of its local +x, -x, up and down edges (the model turned the same way). */
    public static Direction[] localSides(Direction facing) {
        return switch (facing) {
            case NORTH -> new Direction[] {Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
            case EAST -> new Direction[] {Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN};
            case SOUTH -> new Direction[] {Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN};
            case WEST -> new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN};
            case UP -> new Direction[] {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
            case DOWN -> new Direction[] {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};
        };
    }

    /** A wall beside the plate: a solid face turned to it. The plate then grows to that wall (no see-through gap). */
    private static boolean wall(BlockGetter level, BlockPos neighborPos, BlockState neighborState, Direction side) {
        return neighborState.isFaceSturdy(level, neighborPos, side.getOpposite());
    }

    /** The strip that closes the 1 px gap between the plate's edge on {@code side} and the wall, for a plate on {@code facing}. */
    private static VoxelShape edgeStrip(Direction facing, Direction side) {
        double[] min = new double[3], max = new double[3];
        for (Direction.Axis axis : Direction.Axis.values()) {
            int i = axis.ordinal();
            if (axis == facing.getAxis()) {
                min[i] = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 13.0 : 0.0;
                max[i] = min[i] + 3.0;
            } else if (axis == side.getAxis()) {
                min[i] = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 15.0 : 0.0;
                max[i] = min[i] + 1.0;
            } else {
                min[i] = 1.0;
                max[i] = 15.0;
            }
        }
        return Block.box(min[0], min[1], min[2], max[0], max[1], max[2]);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == state.getValue(FACING) && !(neighborState.getBlock() instanceof ChestBlock)) return Blocks.AIR.defaultBlockState();
        boolean arm = direction != state.getValue(FACING) && ChestNetwork.conducts(neighborState, direction.getOpposite());
        state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), arm);
        Direction[] sides = localSides(state.getValue(FACING));
        for (int i = 0; i < EDGES.length; i++) {
            if (sides[i] == direction) state = state.setValue(EDGES[i], wall(level, neighborPos, neighborState, direction));
        }
        return state;
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
