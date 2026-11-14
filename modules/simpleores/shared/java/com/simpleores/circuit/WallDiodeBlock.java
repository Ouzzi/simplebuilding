package com.simpleores.circuit;

import com.simpleores.line.SignalDustBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import org.jspecify.annotations.Nullable;

/**
 * Repeater, Comparator and Signal Dampener on walls and ceilings (N46 wording: "ebenso Repeater, Komparator und Dämpfer",
 * next to the Astral wall dust, {@link WallSignalDustBlock}). Placed by the Vanilla items (and Simple Machines' Dampener) on the
 * side or the underside of a block where the floor version cannot stand, or while sneaking ({@link #placement}); they drop the
 * item they came from.
 *
 * <p>{@link #FACING} points away from the supporting block ({@code down} = under a ceiling); {@link #OUT} picks the output side
 * in the plane of the support ({@link #planeDirs}). Input = the signal from behind (as a floor diode reads it, through a block
 * too), output = strong into the block in front. Kept simple (documented in PLAN-B-CIRC46): no repeater locking, the
 * comparator reads containers directly behind it and side signals of its two in-plane neighbours.
 */
public abstract class WallDiodeBlock extends Block {
    public static final EnumProperty<Direction> FACING = WallSignalDustBlock.FACING;
    /** Index into {@link #planeDirs}: the output side. */
    public static final IntegerProperty OUT = IntegerProperty.create("out", 0, 3);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape[] SHAPES = {
            Block.box(0, 14, 0, 16, 16, 16),   // DOWN: under a ceiling
            null,                              // UP: never
            Block.box(0, 0, 14, 16, 16, 16),   // NORTH: support south of it
            Block.box(0, 0, 0, 16, 16, 2),     // SOUTH
            Block.box(14, 0, 0, 16, 16, 16),   // WEST
            Block.box(0, 0, 0, 2, 16, 16)};    // EAST

    protected WallDiodeBlock(Properties properties) {
        super(properties);
    }

    /** The four output sides in the plane of a wall/ceiling diode, in {@link #OUT} order. */
    public static Direction[] planeDirs(Direction facing) {
        if (facing.getAxis() == Direction.Axis.Y) return new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        return new Direction[]{Direction.UP, facing.getClockWise(), Direction.DOWN, facing.getCounterClockWise()};
    }

    public static Direction output(BlockState state) {
        return planeDirs(state.getValue(FACING))[state.getValue(OUT)];
    }

    /** The state for a wall/ceiling placement on {@code face}: output away from the player like a floor diode. */
    public BlockState stateFor(Direction face, Direction[] looking) {
        Direction[] plane = planeDirs(face);
        for (Direction look : looking) {
            for (int i = 0; i < 4; i++) if (plane[i] == look) return defaultBlockState().setValue(FACING, face).setValue(OUT, i);
        }
        return defaultBlockState().setValue(FACING, face);
    }

    /**
     * Wall/ceiling placement for a diode item, or {@code null} for the floor as before: only on a side or the underside, and
     * only where the floor diode could not stand (nothing sturdy below) or while sneaking - Vanilla building stays unchanged.
     */
    public static @Nullable BlockState placement(Block wall, Block floor, BlockPlaceContext context) {
        if (!SignalDustBlock.enabled() || !(wall instanceof WallDiodeBlock diode)) return null;
        Direction face = context.getClickedFace();
        if (face == Direction.UP) return null;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean floorFits = floor.defaultBlockState().canSurvive(level, pos);
        if (floorFits && !context.isSecondaryUseActive()) return null;
        BlockState state = diode.stateFor(face, context.getNearestLookingDirections());
        return state.canSurvive(level, pos) ? state : null;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OUT, POWERED);
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

    // ---- signal ----

    /** Ticks between an input change and the output change. */
    protected abstract int delay(BlockState state);

    /** The output strength the current inputs ask for. */
    protected abstract int target(Level level, BlockPos pos, BlockState state);

    /** The output strength of this state. */
    public abstract int outputStrength(BlockState state);

    /** The state carrying {@code strength} as its output. */
    protected abstract BlockState withOutput(BlockState state, int strength);

    /** The signal from behind (the side opposite the output), through a block too, like a floor diode. */
    public static int inputSignal(Level level, BlockPos pos, BlockState state) {
        Direction in = output(state).getOpposite();
        BlockPos at = pos.relative(in);
        return Math.max(0, level.getSignal(at, in));
    }

    /** The stronger side signal of the two in-plane neighbours beside the in/out line. */
    public static int sideSignal(Level level, BlockPos pos, BlockState state) {
        Direction out = output(state);
        int best = 0;
        for (Direction d : planeDirs(state.getValue(FACING))) {
            if (d.getAxis() == out.getAxis()) continue;
            BlockPos at = pos.relative(d);
            best = Math.max(best, level.getBlockState(at).getSignal(level, at, d));
        }
        return best;
    }

    private void schedule(Level level, BlockPos pos, BlockState state) {
        if (target(level, pos, state) != outputStrength(state) && !level.getBlockTicks().willTickThisTick(pos, this)) {
            level.scheduleTick(pos, this, delay(state), state.getValue(POWERED) ? TickPriority.VERY_HIGH : TickPriority.HIGH);
        }
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int target = target(level, pos, state);
        if (target == outputStrength(state)) return;
        level.setBlock(pos, withOutput(state, target).setValue(POWERED, target > 0), Block.UPDATE_CLIENTS);
        updateFront(level, pos, state);
        if (target(level, pos, level.getBlockState(pos)) != target) schedule(level, pos, level.getBlockState(pos));
    }

    private void updateFront(Level level, BlockPos pos, BlockState state) {
        BlockPos front = pos.relative(output(state));
        level.neighborChanged(front, this, null);
        level.updateNeighborsAtExceptFromFacing(front, this, output(state).getOpposite(), null);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (state.canSurvive(level, pos)) {
            schedule(level, pos, state);
        } else {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (level.isClientSide()) return;
        schedule(level, pos, state); // also after a setting change by setBlock
        if (!old.is(this)) updateFront(level, pos, state);
    }

    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston) updateFront(level, pos, state);
    }

    @Override protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** {@code direction} points from the asking neighbour to this diode: only the block in front gets the output. */
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == output(state).getOpposite() ? outputStrength(state) : 0;
    }

    @Override protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    /** The item it came from (pick block). */
    protected abstract net.minecraft.world.item.Item floorItem();

    @Override protected net.minecraft.world.item.ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new net.minecraft.world.item.ItemStack(floorItem());
    }

    /** Right click cycles the setting (delay, mode, stage) like the floor diode. */
    protected abstract @Nullable BlockState cycled(BlockState state);

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockState next = cycled(state);
        if (next == null || !player.getAbilities().mayBuild) return InteractionResult.PASS;
        level.setBlock(pos, next, Block.UPDATE_ALL);
        level.playSound(player, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, 0.55F);
        if (!level.isClientSide()) schedule(level, pos, next);
        return InteractionResult.SUCCESS;
    }

    // ---- the three kinds ----

    /** Repeater: on/off at 15 after 1-4 redstone ticks (right click). */
    public static class Repeater extends WallDiodeBlock {
        public static final IntegerProperty DELAY = BlockStateProperties.DELAY;

        public Repeater(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OUT, 0).setValue(POWERED, false).setValue(DELAY, 1));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(DELAY);
        }

        @Override protected int delay(BlockState state) {
            return state.getValue(DELAY) * 2;
        }

        @Override protected int target(Level level, BlockPos pos, BlockState state) {
            return inputSignal(level, pos, state) > 0 ? 15 : 0;
        }

        @Override public int outputStrength(BlockState state) {
            return state.getValue(POWERED) ? 15 : 0;
        }

        @Override protected BlockState withOutput(BlockState state, int strength) {
            return state;
        }

        @Override protected net.minecraft.world.item.Item floorItem() {
            return net.minecraft.world.item.Items.REPEATER;
        }

        @Override protected BlockState cycled(BlockState state) {
            return state.cycle(DELAY);
        }
    }

    /** Comparator: compare (input when at least the side signal) or subtract (right click); reads a container behind it. */
    public static class Comparator extends WallDiodeBlock {
        public static final BooleanProperty SUBTRACT = BooleanProperty.create("subtract");
        public static final IntegerProperty POWER = BlockStateProperties.POWER;

        public Comparator(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OUT, 0).setValue(POWERED, false)
                    .setValue(SUBTRACT, false).setValue(POWER, 0));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(SUBTRACT, POWER);
        }

        @Override protected int delay(BlockState state) {
            return 2;
        }

        public static int compare(int input, int side, boolean subtract) {
            return subtract ? Math.max(0, input - side) : input >= side ? input : 0;
        }

        @Override protected int target(Level level, BlockPos pos, BlockState state) {
            Direction in = output(state).getOpposite();
            BlockPos behind = pos.relative(in);
            BlockState back = level.getBlockState(behind);
            int input = back.hasAnalogOutputSignal() ? back.getAnalogOutputSignal(level, behind, output(state)) : inputSignal(level, pos, state);
            return compare(input, sideSignal(level, pos, state), state.getValue(SUBTRACT));
        }

        @Override public int outputStrength(BlockState state) {
            return state.getValue(POWER);
        }

        @Override protected BlockState withOutput(BlockState state, int strength) {
            return state.setValue(POWER, strength);
        }

        @Override protected net.minecraft.world.item.Item floorItem() {
            return net.minecraft.world.item.Items.COMPARATOR;
        }

        @Override protected BlockState cycled(BlockState state) {
            return state.cycle(SUBTRACT);
        }
    }

    /** Signal Dampener (Simple Machines): input minus 0-3 (right click), one redstone tick late. */
    public static class Dampener extends WallDiodeBlock {
        public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
        public static final IntegerProperty POWER = BlockStateProperties.POWER;

        public Dampener(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OUT, 0).setValue(POWERED, false)
                    .setValue(STAGE, 1).setValue(POWER, 0));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(STAGE, POWER);
        }

        @Override protected int delay(BlockState state) {
            return 2;
        }

        @Override protected int target(Level level, BlockPos pos, BlockState state) {
            return Math.max(0, inputSignal(level, pos, state) - state.getValue(STAGE));
        }

        @Override public int outputStrength(BlockState state) {
            return state.getValue(POWER);
        }

        @Override protected BlockState withOutput(BlockState state, int strength) {
            return state.setValue(POWER, strength);
        }

        @Override protected net.minecraft.world.item.Item floorItem() {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(CircuitBlocks.DAMPENER_ID);
        }

        @Override protected BlockState cycled(BlockState state) {
            return state.cycle(STAGE);
        }
    }
}
