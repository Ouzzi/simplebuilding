package com.simpleores.circuit;

import com.simpleores.line.LineSounds;
import com.simpleores.line.SignalDustBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/**
 * Nihil Core (N46, owner answer CIRC46 F2 = A, 14.11.2026): cores that touch (six sides) form one group. A rising edge at any
 * core of the group toggles the whole group (copper bulb rule); a lit core gives signal 15 to all sides, an unlit one none.
 *
 * <p>Input of one core: the vanilla signals of its non-core neighbours (cores never feed each other, dust never connects to a
 * core, so a lit group does not hold itself on). The six connection properties ({@link #NORTH} ... {@link #DOWN}) say which
 * neighbours are cores of the same kind; the model uses them for the connected texture (Create-casing look without a
 * texture mod: each face is drawn as four quadrants that take a border only towards an unconnected side, blockstate
 * {@code nihil_core.json}, tools/circuit_models.py). Groups are walked up to {@link #MAX_GROUP} cores.
 */
public class NihilCoreBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final Map<Direction, BooleanProperty> CONNECT = new EnumMap<>(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST,
            Direction.SOUTH, SOUTH, Direction.WEST, WEST, Direction.UP, UP, Direction.DOWN, DOWN));
    /** Largest group one edge toggles (a bigger cluster toggles its first 4096 cores, nearest first). */
    public static final int MAX_GROUP = 4096;

    public NihilCoreBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(LIT, false).setValue(POWERED, false);
        for (BooleanProperty p : CONNECT.values()) state = state.setValue(p, false);
        registerDefaultState(state);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, POWERED, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean lit = false;
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            boolean core = n.is(this);
            state = state.setValue(CONNECT.get(d), core);
            if (core && n.getValue(LIT)) lit = true;
        }
        // A new core joins its group in the group's state (one group, one state).
        return state.setValue(LIT, lit);
    }

    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                               BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return state.setValue(CONNECT.get(direction), neighbor.is(this));
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide() && !old.is(this)) update(state, level, pos);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) update(state, level, pos);
    }

    /** Strongest vanilla signal from the neighbours that are not cores. */
    public int input(Level level, BlockPos pos) {
        int best = 0;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (level.getBlockState(n).is(this)) continue;
            best = Math.max(best, level.getSignal(n, d));
            if (best >= 15) break;
        }
        return best;
    }

    /** Stores the input; a rising edge toggles every core of the group. */
    public void update(BlockState state, Level level, BlockPos pos) {
        boolean powered = SignalDustBlock.enabled() && input(level, pos) > 0;
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (!powered) return;
        boolean lit = !state.getValue(LIT);
        for (BlockPos member : group(level, pos)) {
            BlockState s = level.getBlockState(member);
            if (s.is(this) && s.getValue(LIT) != lit) level.setBlock(member, s.setValue(LIT, lit), Block.UPDATE_ALL);
        }
        level.playSound(null, pos, LineSounds.lampToggle(false, lit), SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** The cores connected to {@code start} (breadth first, at most {@link #MAX_GROUP}), {@code start} first. */
    public List<BlockPos> group(BlockGetter level, BlockPos start) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty() && out.size() < MAX_GROUP) {
            BlockPos p = queue.poll();
            out.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && level.getBlockState(n).is(this)) {
                    seen.add(n.immutable());
                    queue.add(n.immutable());
                }
            }
        }
        return out;
    }

    @Override protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(LIT) ? 15 : 0;
    }

    /** 26.3: dust lies next to a core without pointing into it (no self-holding loop). */
    @Override protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return false;
    }

    /** Forge's hook for the same question. */
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return false;
    }
}
