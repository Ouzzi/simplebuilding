package com.simplebuilding.blocks.custom;

import com.google.common.collect.Sets;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.version.WireConnect;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Astral Redstone, Nihil Redstone and Bridge Redstone (owner 2026-10-11, docs/ai/PLAN-REDSTONE-LINIEN-2026-10-11.md):
 * vanilla redstone dust as a second and third line. A port of {@code RedstoneWireBlock} and its default evaluator -
 * every vanilla source powers it, it powers what dust powers (the block below and the block it points at, so lamps,
 * pistons, doors and repeaters next to such a block react), power drops by one per step, climbs and drops one block
 * on slopes and shows the same dot/line/cross layout - with one change: dust only exchanges signal with dust of a
 * linked {@link Line}. The vanilla rule "dust does not read dust through a powered block or directly" is extended
 * to every line: {@link #blockSignal} skips every kind of dust, and while a vanilla wire computes its own block
 * signal ({@code RedstoneWireEvaluatorLinesMixin}) this dust emits nothing. Bridge Redstone links all three lines.
 */
public class SignalDustBlock extends Block {
    /** The three lines plus the bridge; {@link #REDSTONE} is vanilla's {@code redstone_wire}. */
    public enum Line {
        REDSTONE, ASTRAL, NIHIL, BRIDGE;

        public boolean links(Line other) {
            return this == other || this == BRIDGE || other == BRIDGE;
        }
    }

    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final Map<Direction, EnumProperty<RedstoneSide>> SIDES = new EnumMap<>(Map.of(
            Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST));
    private static final VoxelShape[] SHAPES = new VoxelShape[81];
    private static int vanillaDepth;
    private static Thread vanillaThread;

    private final Line line;
    private final BlockState crossState;

    public SignalDustBlock(Line line, Properties properties) {
        super(properties);
        this.line = line;
        registerDefaultState(stateDefinition.any().setValue(NORTH, RedstoneSide.NONE).setValue(EAST, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE).setValue(WEST, RedstoneSide.NONE).setValue(POWER, 0));
        crossState = defaultBlockState().setValue(NORTH, RedstoneSide.SIDE).setValue(EAST, RedstoneSide.SIDE)
                .setValue(SOUTH, RedstoneSide.SIDE).setValue(WEST, RedstoneSide.SIDE);
    }

    public Line line() {
        return line;
    }

    /** The line of a dust block (vanilla redstone wire included), or {@code null} for anything else. */
    @Nullable
    public static Line lineOf(BlockState state) {
        if (state.getBlock() instanceof SignalDustBlock dust) return dust.line;
        return state.is(Blocks.REDSTONE_WIRE) ? Line.REDSTONE : null;
    }

    private boolean linksTo(BlockState state) {
        Line other = lineOf(state);
        return other != null && line.links(other);
    }

    /** Astral/Nihil/Bridge follow server.features.endSignals: switched off they carry nothing. */
    public static boolean enabled() {
        return ServerTuning.get().features.endSignals;
    }

    /** Called around RedstoneWireEvaluator#getBlockSignal: a vanilla wire is reading its non-wire neighbours. */
    public static void enterVanillaWire() {
        if (vanillaDepth++ == 0) vanillaThread = Thread.currentThread();
    }

    public static void exitVanillaWire() {
        if (vanillaDepth > 0) vanillaDepth--;
    }

    private static boolean vanillaWireReading() {
        return vanillaDepth > 0 && vanillaThread == Thread.currentThread();
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POWER);
    }

    // ---- shape and connections (RedstoneWireBlock) ----

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = 0;
        for (EnumProperty<RedstoneSide> side : SIDES.values()) index = index * 3 + state.getValue(side).ordinal();
        VoxelShape shape = SHAPES[index];
        if (shape == null) {
            shape = Block.box(3, 0, 3, 13, 1, 13);
            for (Map.Entry<Direction, EnumProperty<RedstoneSide>> e : SIDES.entrySet()) {
                RedstoneSide side = state.getValue(e.getValue());
                if (side == RedstoneSide.NONE) continue;
                shape = Shapes.or(shape, switch (e.getKey()) {
                    case NORTH -> Block.box(3, 0, 0, 13, 1, 8);
                    case SOUTH -> Block.box(3, 0, 8, 13, 1, 16);
                    case WEST -> Block.box(0, 0, 3, 8, 1, 13);
                    default -> Block.box(8, 0, 3, 16, 1, 13);
                });
                if (side == RedstoneSide.UP) shape = Shapes.or(shape, switch (e.getKey()) {
                    case NORTH -> Block.box(3, 0, 0, 13, 16, 1);
                    case SOUTH -> Block.box(3, 0, 15, 13, 16, 16);
                    case WEST -> Block.box(0, 0, 3, 1, 16, 13);
                    default -> Block.box(15, 0, 3, 16, 16, 13);
                });
            }
            SHAPES[index] = shape;
        }
        return shape;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connectionState(context.getLevel(), crossState, context.getClickedPos());
    }

    private BlockState connectionState(BlockGetter level, BlockState state, BlockPos pos) {
        boolean wasDot = isDot(state);
        state = missingConnections(level, defaultBlockState().setValue(POWER, state.getValue(POWER)), pos);
        if (wasDot && isDot(state)) return state;
        boolean north = state.getValue(NORTH).isConnected(), south = state.getValue(SOUTH).isConnected();
        boolean east = state.getValue(EAST).isConnected(), west = state.getValue(WEST).isConnected();
        boolean northSouthEmpty = !north && !south, eastWestEmpty = !east && !west;
        if (!west && northSouthEmpty) state = state.setValue(WEST, RedstoneSide.SIDE);
        if (!east && northSouthEmpty) state = state.setValue(EAST, RedstoneSide.SIDE);
        if (!north && eastWestEmpty) state = state.setValue(NORTH, RedstoneSide.SIDE);
        if (!south && eastWestEmpty) state = state.setValue(SOUTH, RedstoneSide.SIDE);
        return state;
    }

    private BlockState missingConnections(BlockGetter level, BlockState state, BlockPos pos) {
        boolean canConnectUp = !level.getBlockState(pos.above()).isRedstoneConductor(level, pos);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!state.getValue(SIDES.get(direction)).isConnected()) {
                state = state.setValue(SIDES.get(direction), connectingSide(level, pos, direction, canConnectUp));
            }
        }
        return state;
    }

    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                               Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN) {
            return !canSurviveOn(level, neighbourPos, neighbourState) ? Blocks.AIR.defaultBlockState() : state;
        }
        if (direction == Direction.UP) return connectionState(level, state, pos);
        RedstoneSide side = connectingSide(level, pos, direction,
                !level.getBlockState(pos.above()).isRedstoneConductor(level, pos));
        return side.isConnected() == state.getValue(SIDES.get(direction)).isConnected() && !isCross(state)
                ? state.setValue(SIDES.get(direction), side)
                : connectionState(level, crossState.setValue(POWER, state.getValue(POWER)).setValue(SIDES.get(direction), side), pos);
    }

    static boolean isCross(BlockState state) {
        for (EnumProperty<RedstoneSide> side : SIDES.values()) if (!state.getValue(side).isConnected()) return false;
        return true;
    }

    static boolean isDot(BlockState state) {
        for (EnumProperty<RedstoneSide> side : SIDES.values()) if (state.getValue(side).isConnected()) return false;
        return true;
    }

    @Override protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int limit) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (state.getValue(SIDES.get(direction)) == RedstoneSide.NONE || linksTo(level.getBlockState(cursor.setWithOffset(pos, direction)))) continue;
            cursor.move(Direction.DOWN);
            if (linksTo(level.getBlockState(cursor))) {
                BlockPos neighbour = cursor.relative(direction.getOpposite());
                level.neighborShapeChanged(direction.getOpposite(), cursor, neighbour, level.getBlockState(neighbour), flags, limit);
            }
            cursor.setWithOffset(pos, direction).move(Direction.UP);
            if (linksTo(level.getBlockState(cursor))) {
                BlockPos neighbour = cursor.relative(direction.getOpposite());
                level.neighborShapeChanged(direction.getOpposite(), cursor, neighbour, level.getBlockState(neighbour), flags, limit);
            }
        }
    }

    private RedstoneSide connectingSide(BlockGetter level, BlockPos pos, Direction direction, boolean canConnectUp) {
        BlockPos sidePos = pos.relative(direction);
        BlockState sideState = level.getBlockState(sidePos);
        if (canConnectUp) {
            boolean placeableAbove = sideState.getBlock() instanceof TrapDoorBlock || canSurviveOn(level, sidePos, sideState);
            if (placeableAbove && connectsTo(level.getBlockState(sidePos.above()), level, sidePos.above(), null)) {
                return sideState.isFaceSturdy(level, sidePos, direction.getOpposite()) ? RedstoneSide.UP : RedstoneSide.SIDE;
            }
        }
        return !connectsTo(sideState, level, sidePos, direction)
                && (sideState.isRedstoneConductor(level, sidePos) || !connectsTo(level.getBlockState(sidePos.below()), level, sidePos.below(), null))
                ? RedstoneSide.NONE : RedstoneSide.SIDE;
    }

    /** Dust of a linked line, else what vanilla dust connects to (End pistons, lamps and rails of every line, N51). */
    private boolean connectsTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        Line other = lineOf(state);
        if (other != null) return line.links(other);
        if (state.getBlock() instanceof EndSignalBlock || state.getBlock() instanceof EndRailBlock) return direction != null;
        if (state.getBlock() instanceof LineRedstoneBlock source) return direction != null && line.links(source.line());
        return WireConnect.vanilla(state, level, pos, direction);
    }

    /** 26.3/NeoForge: vanilla dust asks this before it connects - only the bridge says yes. */
    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return line == Line.BRIDGE;
    }

    /** Forge's IForgeBlock hook for the same question. */
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return line == Line.BRIDGE;
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSurviveOn(level, pos.below(), level.getBlockState(pos.below()));
    }

    private static boolean canSurviveOn(BlockGetter level, BlockPos pos, BlockState state) {
        return state.isFaceSturdy(level, pos, Direction.UP) || state.is(Blocks.HOPPER);
    }

    // ---- power (DefaultRedstoneWireEvaluator) ----

    private void updatePowerStrength(Level level, BlockPos pos, BlockState state) {
        int target = targetStrength(level, pos);
        if (state.getValue(POWER) == target) return;
        if (level.getBlockState(pos) == state) level.setBlock(pos, state.setValue(POWER, target), Block.UPDATE_CLIENTS);
        Set<BlockPos> toUpdate = Sets.newHashSet();
        toUpdate.add(pos);
        for (Direction direction : Direction.values()) toUpdate.add(pos.relative(direction));
        for (BlockPos update : toUpdate) level.updateNeighborsAt(update, this);
    }

    private int targetStrength(Level level, BlockPos pos) {
        if (!enabled()) return 0;
        int block = blockSignal(level, pos);
        return block == 15 ? 15 : Math.max(block, incomingWireSignal(level, pos));
    }

    /** level.getBestNeighborSignal without any dust: neither direct neighbours nor dust powering a neighbour block. */
    private static int blockSignal(Level level, BlockPos pos) {
        int best = 0;
        for (Direction direction : Direction.values()) {
            BlockPos at = pos.relative(direction);
            BlockState state = level.getBlockState(at);
            if (lineOf(state) != null) continue;
            int signal = state.getSignal(level, at, direction);
            if (signal < 15 && state.isRedstoneConductor(level, at)) {
                for (Direction inner : Direction.values()) {
                    BlockPos source = at.relative(inner);
                    BlockState sourceState = level.getBlockState(source);
                    if (lineOf(sourceState) != null) continue;
                    signal = Math.max(signal, sourceState.getDirectSignal(level, source, inner));
                    if (signal >= 15) break;
                }
            }
            if (signal >= 15) return 15;
            best = Math.max(best, signal);
        }
        return best;
    }

    private int wireSignal(BlockState state) {
        return linksTo(state) ? state.getValue(POWER) : 0;
    }

    private int incomingWireSignal(Level level, BlockPos pos) {
        int signal = 0;
        BlockPos above = pos.above();
        boolean coveredAbove = level.getBlockState(above).isRedstoneConductor(level, above);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            BlockState sideState = level.getBlockState(side);
            signal = Math.max(signal, wireSignal(sideState));
            boolean conductor = sideState.isRedstoneConductor(level, side);
            if (conductor && !coveredAbove) signal = Math.max(signal, wireSignal(level.getBlockState(side.above())));
            else if (!conductor) signal = Math.max(signal, wireSignal(level.getBlockState(side.below())));
        }
        return Math.max(0, signal - 1);
    }

    private void checkCornerChangeAt(Level level, BlockPos pos) {
        if (!linksTo(level.getBlockState(pos))) return;
        level.updateNeighborsAt(pos, this);
        for (Direction direction : Direction.values()) level.updateNeighborsAt(pos.relative(direction), this);
    }

    private void updateNeighborsOfNeighboringWires(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) checkCornerChangeAt(level, pos.relative(direction));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            checkCornerChangeAt(level, level.getBlockState(side).isRedstoneConductor(level, side) ? side.above() : side.below());
        }
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(state.getBlock()) || level.isClientSide()) return;
        updatePowerStrength(level, pos, state);
        for (Direction direction : Direction.Plane.VERTICAL) level.updateNeighborsAt(pos.relative(direction), this);
        updateNeighborsOfNeighboringWires(level, pos);
    }

    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (movedByPiston) return;
        for (Direction direction : Direction.values()) level.updateNeighborsAt(pos.relative(direction), this);
        updatePowerStrength(level, pos, state);
        updateNeighborsOfNeighboringWires(level, pos);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (state.canSurvive(level, pos)) {
            updatePowerStrength(level, pos, state);
        } else {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }
    }

    @Override protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return vanillaWireReading() ? 0 : getSignal(state, level, pos, direction);
    }

    /**
     * Like dust: the block below and the blocks it points at. A dust receiver (only vanilla wire asks directly - this
     * class skips dust in {@link #blockSignal}) gets the signal one step weaker, and only from a linked line.
     */
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (direction == Direction.DOWN) return 0;
        int power = state.getValue(POWER);
        if (power == 0) return 0;
        // Like vanilla: the connections are worked out afresh, the stored ones may lag behind a neighbour placed later.
        if (direction != Direction.UP && !connectionState(level, state, pos).getValue(SIDES.get(direction.getOpposite())).isConnected()) return 0;
        Line receiver = lineOf(level.getBlockState(pos.relative(direction.getOpposite())));
        if (receiver != null) return line.links(receiver) ? power - 1 : 0;
        return vanillaWireReading() ? 0 : power;
    }

    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(POWER);
    }

    @Override protected boolean isSignalSource(BlockState state) {
        return true;
    }

    // ---- interaction and transforms ----

    /** Like vanilla dust: an unconnected cross turns into a dot and back. */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild || !(isCross(state) || isDot(state))) return InteractionResult.PASS;
        BlockState next = connectionState(level, (isCross(state) ? defaultBlockState() : crossState).setValue(POWER, state.getValue(POWER)), pos);
        if (next == state) return InteractionResult.PASS;
        level.setBlockAndUpdate(pos, next);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (state.getValue(SIDES.get(direction)).isConnected() != next.getValue(SIDES.get(direction)).isConnected()
                    && level.getBlockState(side).isRedstoneConductor(level, side)) level.updateNeighborsAt(side, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH)).setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state.setValue(NORTH, state.getValue(EAST)).setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST)).setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state.setValue(NORTH, state.getValue(WEST)).setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST)).setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default -> super.mirror(state, mirror);
        };
    }
}
