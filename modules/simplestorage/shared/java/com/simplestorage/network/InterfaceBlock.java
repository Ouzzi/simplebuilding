package com.simplestorage.network;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import com.simplestorage.config.StorageConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.ChestType;
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
 *
 * <p>DW03C-2 (owner 03.11.): the plate takes the form of what it sits on ({@link #MOUNT}). On a barrel or shulker box it is flush
 * with the full face. On a chest it keeps the chest's width (14 of 16) and, beside the chest, covers only the part that does not
 * move (below the lid, y 0..9) and reaches 1 px into the chest's cell to close the gap; in front it leaves the lock free. Under a
 * chest it stays in its own cell (no coplanar faces with the chest, DW03C-1), on top of a chest it cannot be placed (the lid).
 */
public class InterfaceBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    /**
     * The plate reaches to the edge of its cell (IG31-6, DW03C-2): towards the other half of a double chest always, towards a solid
     * block beside it with {@code interfaceFillsGaps}. Named from the plate's own view for a chest to the north (E = +x, W = -x,
     * U = up, D = down) and turned with {@link #FACING} like the model ({@link #localSides}).
     */
    public static final BooleanProperty EDGE_E = BooleanProperty.create("edge_e"), EDGE_W = BooleanProperty.create("edge_w"),
            EDGE_U = BooleanProperty.create("edge_u"), EDGE_D = BooleanProperty.create("edge_d");
    private static final BooleanProperty[] EDGES = {EDGE_E, EDGE_W, EDGE_U, EDGE_D};
    /** What the plate sits on (DW03C-2), worked out from the block at {@link #FACING}. */
    public static final EnumProperty<Mount> MOUNT = EnumProperty.create("mount", Mount.class);

    /** The plate's form: flush with a full block, a chest seen from the side/back/below/above, a chest seen from the front. */
    public enum Mount implements StringRepresentable {
        FULL("full"), CHEST("chest"), CHEST_FRONT("chest_front");

        private final String name;

        Mount(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    /*
     * The boxes in pixels for a chest to the north (the block models use the same numbers, tools/gen_interface_models.py); z < 0
     * lies in the chest's cell and is drawn but not part of the hitbox.
     */
    private static final double[][] FULL_PLATE = {{0, 0, 0, 16, 16, 3}, {6, 6, 3, 10, 10, 6}};
    private static final double[][] FLAT_PLATE = {{1, 1, 0, 15, 15, 3}, {6, 6, 3, 10, 10, 6}};
    private static final double[][] SIDE_PLATE = {{1, 0, -1, 15, 9, 2}, {6, 6, 2, 10, 9, 6}};
    private static final double[][] FRONT_PLATE = {{1, 0, 0, 15, 9, 2}, {1, 0, -1, 7, 9, 0}, {9, 0, -1, 15, 9, 0}, {7, 0, -1, 9, 7, 0},
            {6, 6, 2, 10, 9, 6}};
    /** Edge strips E, W, U, D of the flat plate (under or on a chest) and E, W of the side plate. */
    private static final double[][] FLAT_EDGES = {{15, 1, 0, 16, 15, 3}, {0, 1, 0, 1, 15, 3}, {1, 15, 0, 15, 16, 3}, {1, 0, 0, 15, 1, 3}};
    private static final double[][] SIDE_EDGES = {{15, 0, -1, 16, 9, 2}, {0, 0, -1, 1, 9, 2}};
    private static final VoxelShape CORE = Block.cube(4.0);
    /** The wire arm from the middle to the edge, for each side. */
    private static final Map<Direction, VoxelShape> ARMS = Shapes.rotateAll(Block.boxZ(4.0, 0.0, 8.0));

    private final Function<BlockState, VoxelShape> shapes;

    public InterfaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(MOUNT, Mount.CHEST);
        for (Direction direction : Direction.values()) state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), false);
        for (BooleanProperty edge : EDGES) state = state.setValue(edge, false);
        this.registerDefaultState(state);
        this.shapes = this.getShapeForEachState(shapeState -> {
            Direction facing = shapeState.getValue(FACING);
            VoxelShape shape = Shapes.or(boxes(facing, plate(shapeState)), CORE);
            for (Direction direction : Direction.values()) {
                if (shapeState.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) shape = Shapes.or(shape, ARMS.get(direction));
            }
            double[][] edges = facing.getAxis().isHorizontal() ? SIDE_EDGES : FLAT_EDGES;
            for (int i = 0; i < edges.length; i++) {
                if (shapeState.getValue(EDGES[i])) shape = Shapes.or(shape, boxes(facing, new double[][] {edges[i]}));
            }
            return shape;
        });
    }

    private static double[][] plate(BlockState state) {
        Mount mount = state.getValue(MOUNT);
        if (mount == Mount.FULL) return FULL_PLATE;
        if (!state.getValue(FACING).getAxis().isHorizontal()) return FLAT_PLATE;
        return mount == Mount.CHEST_FRONT ? FRONT_PLATE : SIDE_PLATE;
    }

    /** The boxes (pixels, chest to the north) turned to {@code facing} like the blockstate's x/y rotations, cut to the own cell. */
    private static VoxelShape boxes(Direction facing, double[][] boxes) {
        VoxelShape shape = Shapes.empty();
        for (double[] b : boxes) {
            double z0 = Math.max(0, b[2]);
            if (b[5] <= z0) continue;
            double[] p = turn(facing, b[0], b[1], z0), q = turn(facing, b[3], b[4], b[5]);
            shape = Shapes.or(shape, Block.box(Math.min(p[0], q[0]), Math.min(p[1], q[1]), Math.min(p[2], q[2]),
                    Math.max(p[0], q[0]), Math.max(p[1], q[1]), Math.max(p[2], q[2])));
        }
        return shape;
    }

    /** A model point (chest to the north) turned to {@code facing}: y 90/180/270 for east/south/west, x 270 for up, x 90 for down. */
    static double[] turn(Direction facing, double x, double y, double z) {
        return switch (facing) {
            case NORTH -> new double[] {x, y, z};
            case EAST -> new double[] {16 - z, y, x};
            case SOUTH -> new double[] {16 - x, y, 16 - z};
            case WEST -> new double[] {z, y, 16 - x};
            case UP -> new double[] {x, 16 - z, y};
            case DOWN -> new double[] {x, z, 16 - y};
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MOUNT, PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN, EDGE_E, EDGE_W, EDGE_U, EDGE_D);
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
        if (!placeableOn(context.getLevel().getBlockState(pos.relative(facing)), facing)) return null;
        return withArms(this.defaultBlockState().setValue(FACING, facing), context.getLevel(), pos);
    }

    /** Whether a new interface may sit on {@code target} pointing {@code facing}: not on top of a chest (DW03C-2, its lid). */
    public static boolean placeableOn(BlockState target, Direction facing) {
        return ControllerLevel.mountable(target.getBlock()) && !(facing == Direction.DOWN && target.getBlock() instanceof ChestBlock);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return ControllerLevel.mountable(level.getBlockState(pos.relative(state.getValue(FACING))).getBlock());
    }

    /** The whole state from the neighbours: the form ({@link #MOUNT}), the wire arms and the edge strips. */
    public static BlockState withArms(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockState target = level.getBlockState(pos.relative(facing));
        state = state.setValue(MOUNT, mountOf(target, facing));
        for (Direction direction : Direction.values()) {
            boolean arm = direction != facing && ChestNetwork.conducts(level.getBlockState(pos.relative(direction)), direction.getOpposite());
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), arm);
        }
        Direction[] sides = localSides(facing);
        for (int i = 0; i < EDGES.length; i++) state = state.setValue(EDGES[i], edge(level, pos, state, target, sides[i]));
        return state;
    }

    /** The form for a plate on {@code target} pointing {@code facing}. */
    public static Mount mountOf(BlockState target, Direction facing) {
        if (!(target.getBlock() instanceof ChestBlock)) return Mount.FULL;
        boolean front = facing.getAxis().isHorizontal() && target.hasProperty(ChestBlock.FACING) && target.getValue(ChestBlock.FACING) == facing.getOpposite();
        return front ? Mount.CHEST_FRONT : Mount.CHEST;
    }

    /**
     * Whether the plate reaches the cell's edge on {@code side}: never when it is flush already (full) or beside a chest up (the lid
     * swings there) or down (the plate stands on the cell's floor); towards the other half of a double chest always; towards a
     * solid block beside it when {@code interfaceFillsGaps} is on.
     */
    private static boolean edge(LevelReader level, BlockPos pos, BlockState state, BlockState target, Direction side) {
        if (state.getValue(MOUNT) == Mount.FULL) return false;
        if (state.getValue(FACING).getAxis().isHorizontal() && side.getAxis() == Direction.Axis.Y) return false;
        if (target.getBlock() instanceof ChestBlock && target.hasProperty(ChestBlock.TYPE) && target.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && ChestBlock.getConnectedDirection(target) == side) return true;
        if (!StorageConfig.interfaceFillsGaps) return false;
        BlockPos next = pos.relative(side);
        return wall(level, next, level.getBlockState(next), side);
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

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == state.getValue(FACING) && !ControllerLevel.mountable(neighborState.getBlock())) return Blocks.AIR.defaultBlockState();
        return withArms(state, level, pos);
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
        if (NetworkSubstitutes.replaceIfDisabled(level, pos, state)) return;
        // Old worlds (before DW03C-2) and a changed interfaceFillsGaps: the form follows without a neighbour update.
        BlockState fresh = withArms(state, level, pos);
        if (fresh != state) level.setBlock(pos, fresh, Block.UPDATE_CLIENTS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return NetworkSubstitutes.replaceIfDisabled(level, pos, state) ? InteractionResult.SUCCESS : super.useWithoutItem(state, level, pos, player, hit);
    }
}
