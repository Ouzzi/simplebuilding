package com.simpleores.circuit;

import com.simpleores.line.SignalDustBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Astral / Bridge Redstone on walls and ceilings (N46, owner answer CIRC46 F4 = A, "like Sticky Redstone"; plain redstone stays
 * Vanilla). The dust item places this block when it is used on the side or the underside of a block ({@link LineDustItem}).
 * {@link #FACING} points away from the supporting block ({@code down} = hangs under a ceiling).
 *
 * <p>Signal, kept close to floor dust: it takes every vanilla signal of its non-dust neighbours and the power of linked dust
 * next to it (floor dust of its line, other wall dust) minus one; it powers the supporting block strongly (as floor dust
 * powers the block below) and every other neighbour weakly; linked dust reads it one step weaker. It counts as dust of its
 * line for floor dust ({@link SignalDustBlock#lineOf}), so both connect. Round an outer edge of the support it links to wall
 * dust on the next face and to floor dust on top ({@link #cornerStrength}). Not covered (PLAN-B-CIRC46): the connection
 * drawing - wall dust always shows its cross.
 */
public class WallSignalDustBlock extends Block {
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class, d -> d != Direction.UP);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    private static final VoxelShape[] SHAPES = {
            Block.box(0, 15, 0, 16, 16, 16),   // DOWN: under a ceiling
            null,                              // UP: never
            Block.box(0, 0, 15, 16, 16, 16),   // NORTH: support south of it
            Block.box(0, 0, 0, 16, 16, 1),     // SOUTH
            Block.box(15, 0, 0, 16, 16, 16),   // WEST
            Block.box(0, 0, 0, 1, 16, 16)};    // EAST

    private final SignalDustBlock.Line line;
    private final Block floorDust;

    public WallSignalDustBlock(SignalDustBlock.Line line, Block floorDust, Properties properties) {
        super(properties);
        this.line = line;
        this.floorDust = floorDust;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWER, 0));
    }

    public SignalDustBlock.Line line() {
        return line;
    }

    /** The floor dust (and item) of this line. */
    public Block floorDust() {
        return floorDust;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get3DDataValue()];
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                               BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    // ---- power ----

    private boolean linked(BlockState state) {
        SignalDustBlock.Line other = SignalDustBlock.lineOf(state);
        return other != null && line.links(other);
    }

    /** Vanilla signals of the non-dust neighbours, then linked dust next to it minus one. */
    public int targetStrength(Level level, BlockPos pos) {
        if (!SignalDustBlock.enabled()) return 0;
        int best = 0;
        for (Direction d : Direction.values()) {
            BlockPos at = pos.relative(d);
            BlockState n = level.getBlockState(at);
            if (SignalDustBlock.lineOf(n) != null) {
                if (linked(n)) best = Math.max(best, n.getValue(POWER) - 1);
                continue;
            }
            best = Math.max(best, n.getSignal(level, at, d));
            if (best >= 15) return 15;
        }
        best = Math.max(best, cornerStrength(level, pos, level.getBlockState(pos)));
        return Math.max(0, best);
    }

    /**
     * Round an outer edge of the supporting block: linked wall dust on the neighbouring face of the same block (its facing is
     * the step direction) and, over the top edge, linked floor dust lying on the support. Floor dust reads this dust over the
     * edge by itself (it looks one block down past a non-conductor, like Vanilla wire).
     */
    private int cornerStrength(Level level, BlockPos pos, BlockState self) {
        if (!(self.getBlock() instanceof WallSignalDustBlock)) return 0;
        Direction facing = self.getValue(FACING);
        int best = 0;
        for (Direction d : WallDiodeBlock.planeDirs(facing)) {
            BlockState n = level.getBlockState(pos.relative(d).relative(facing.getOpposite()));
            boolean corner = n.getBlock() instanceof WallSignalDustBlock ? n.getValue(FACING) == d
                    : d == Direction.UP && n.getBlock() instanceof SignalDustBlock;
            if (corner && linked(n)) best = Math.max(best, n.getValue(POWER) - 1);
        }
        return best;
    }

    private void updatePower(Level level, BlockPos pos, BlockState state) {
        int target = targetStrength(level, pos);
        if (state.getValue(POWER) == target) return;
        level.setBlock(pos, state.setValue(POWER, target), Block.UPDATE_CLIENTS);
        level.updateNeighborsAt(pos, this);
        for (Direction d : Direction.values()) level.updateNeighborsAt(pos.relative(d), this);
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide() && !old.is(this)) updatePower(level, pos, state);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (state.canSurvive(level, pos)) {
            updatePower(level, pos, state);
        } else {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }
    }

    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (movedByPiston) return;
        for (Direction d : Direction.values()) level.updateNeighborsAt(pos.relative(d), this);
    }

    @Override protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** {@code direction} points from the asking neighbour to this dust. */
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        int power = state.getValue(POWER);
        if (power == 0) return 0;
        SignalDustBlock.Line receiver = SignalDustBlock.lineOf(level.getBlockState(pos.relative(direction.getOpposite())));
        if (receiver != null) return line.links(receiver) ? power - 1 : 0;
        return SignalDustBlock.vanillaWireReading() ? 0 : power;
    }

    /** Strong only into the supporting block (floor dust: the block below). */
    @Override protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == state.getValue(FACING) ? getSignal(state, level, pos, direction) : 0;
    }

    /** Vanilla wire connects only to the Bridge line (as with floor dust). */
    @Override protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return line == SignalDustBlock.Line.BRIDGE;
    }
}
