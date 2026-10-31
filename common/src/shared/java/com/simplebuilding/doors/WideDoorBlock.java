package com.simplebuilding.doors;

import com.simplebuilding.config.ServerTuning;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Simple Doors (WP-17 / BS28-2): one block class for the 2x2, 2x3 and the half-width doors (two and three high). A door is a set
 * of cells of the same block, each with its place in the block state ({@link #COL}, {@link #ROW}, {@link #SWING}); there is no
 * block entity. Vanilla's conventions: the closed leaf lies on the edge opposite {@code facing}, {@code hinge} is as seen from the
 * front, an open leaf lies along the hinge edge.
 *
 * <ul>
 *   <li><b>Cells.</b> The hinge column is {@code col 0}, the free column {@code col 1}; {@code row 0} is the bottom. Closed: all
 *       {@code cols x rows} cells stand in the door plane. A two-wide door swings its 2 block long leaf 90 degrees about the hinge
 *       edge into the row in front of it: open, only the hinge column stays (as {@code swing=false}) and the leaf's second block
 *       fills the cell in front of each hinge cell ({@code swing=true}); the free column becomes air (the opening). Opening needs the
 *       whole {@code cols x rows} area in front free (concept D1 A); closing needs the free column free again.</li>
 *   <li><b>1.5 wide doors</b> ({@link DoorFormat#narrow}) behave exactly like the two-wide ones (same cells, same swing area), only
 *       their leaf is 24 px: the second column and the swing cell hold a half-length (8 px) piece.</li>
 *   <li><b>Double 1.5 door</b> (IG31-34): a second 1.5 wide door placed with its stub column on the stub column of a mirrored
 *       one shares that column ({@link #TWIN}: the cell holds both 8 px stubs) - two 1.5 doors make a double door 3 blocks wide.
 *       Opening one door turns the shared cells back into the other door's stubs, closing it against them makes them shared again;
 *       a hand opens the door whose half was clicked, redstone at the middle column opens both; breaking one door leaves the
 *       other's stub, breaking the shared column takes both doors (both drop).</li>
 *   <li><b>Half doors</b> are one block wide, the leaf 8 px: it sits on the hinge half of the cell and swings inside the cell, so
 *       nothing needs to be free and no cell changes but its {@code open} state.</li>
 *   <li><b>Redstone</b> on any cell opens the door (rising edge) and closes it (falling edge), like Vanilla; a hand opens only
 *       this door. Breaking any cell removes the whole door and drops one (only the lowest hinge cell has loot; the others fall
 *       through {@code updateShape} like the halves of a Vanilla door).</li>
 * </ul>
 */
public final class WideDoorBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** 0 = hinge column, 1 = free column (a half door only has 0). */
    public static final IntegerProperty COL = IntegerProperty.create("col", 0, 1);
    /** 0 = bottom row (a 2-high door has 0-1, a 3-high one 0-2). */
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 2);
    /** The leaf's second block in front of an open two-wide door. */
    public static final BooleanProperty SWING = BooleanProperty.create("swing");
    /** IG31-34: a closed stub cell of a 1.5 door that also holds the stub of the mirrored 1.5 door next to it (double door). */
    public static final BooleanProperty TWIN = BooleanProperty.create("twin");

    /** Write flags for the cells of one door: clients, no shape updates (the door is mid-change). */
    private static final int CELL_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final Map<Direction, VoxelShape> CLOSED_FULL = Shapes.rotateHorizontal(Block.box(0, 0, 13, 16, 16, 16));
    private static final Map<Direction, VoxelShape> CLOSED_HALF_LEFT = Shapes.rotateHorizontal(Block.box(0, 0, 13, 8, 16, 16));
    private static final Map<Direction, VoxelShape> CLOSED_HALF_RIGHT = Shapes.rotateHorizontal(Block.box(8, 0, 13, 16, 16, 16));
    private static final Map<Direction, VoxelShape> OPEN_FULL_LEFT = Shapes.rotateHorizontal(Block.box(0, 0, 0, 3, 16, 16));
    private static final Map<Direction, VoxelShape> OPEN_FULL_RIGHT = Shapes.rotateHorizontal(Block.box(13, 0, 0, 16, 16, 16));
    private static final Map<Direction, VoxelShape> OPEN_HALF_LEFT = Shapes.rotateHorizontal(Block.box(0, 0, 8, 3, 16, 16));
    private static final Map<Direction, VoxelShape> OPEN_HALF_RIGHT = Shapes.rotateHorizontal(Block.box(13, 0, 8, 16, 16, 16));

    private final DoorFormat format;
    private final BlockSetType type;

    public WideDoorBlock(DoorFormat format, BlockSetType type, BlockBehaviour.Properties properties) {
        super(properties.sound(type.soundType()));
        this.format = format;
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HINGE, DoorHingeSide.LEFT).setValue(OPEN, false)
                .setValue(POWERED, false).setValue(COL, 0).setValue(ROW, 0).setValue(SWING, false).setValue(TWIN, false));
    }

    public DoorFormat format() {
        return format;
    }

    public BlockSetType type() {
        return type;
    }

    // ------------------------------------------------------------------ geometry

    /** Direction from the hinge column to the free column. */
    public static Direction along(BlockState state) {
        Direction f = state.getValue(FACING);
        return state.getValue(HINGE) == DoorHingeSide.LEFT ? f.getClockWise() : f.getCounterClockWise();
    }

    /** The lowest hinge cell: {@code col 0, row 0, swing false}. */
    public static BlockPos origin(BlockPos pos, BlockState state) {
        BlockPos p = pos.below(state.getValue(ROW)).relative(along(state), -state.getValue(COL));
        return state.getValue(SWING) ? p.relative(state.getValue(FACING).getOpposite()) : p;
    }

    public static BlockPos cell(BlockPos origin, BlockState state, int col, int row, boolean swing) {
        BlockPos p = origin.relative(along(state), col).above(row);
        return swing ? p.relative(state.getValue(FACING)) : p;
    }

    /** The state of the cell ({@code col}, {@code row}, {@code swing}) of the same door, in the given open state. */
    private BlockState cellState(BlockState any, int col, int row, boolean swing, boolean open) {
        return any.setValue(COL, col).setValue(ROW, row).setValue(SWING, swing).setValue(OPEN, open).setValue(TWIN, false);
    }

    private boolean isPartner(BlockState state, BlockState other, int col, int row, boolean swing) {
        // a shared stub cell (TWIN) belongs to both doors whatever hinge it stores
        boolean hinge = other.is(this) && (other.getValue(HINGE) == state.getValue(HINGE) || col == 1 && other.getValue(TWIN));
        return hinge && other.getValue(FACING) == state.getValue(FACING)
                && other.getValue(OPEN) == state.getValue(OPEN) && other.getValue(COL) == col && other.getValue(ROW) == row && other.getValue(SWING) == swing;
    }

    private static DoorHingeSide flip(DoorHingeSide hinge) {
        return hinge == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
    }

    /** A shared stub cell seen as a cell of the other of its two doors. */
    private static BlockState otherView(BlockState twin) {
        return twin.setValue(HINGE, flip(twin.getValue(HINGE)));
    }

    /** True when {@code other} is a closed, unshared stub cell (row {@code row}) of a 1.5 door mirrored to {@code state}'s door. */
    private boolean isMirroredStub(BlockState state, BlockState other, int row) {
        return format.narrow && other.is(this) && !other.getValue(TWIN) && !other.getValue(OPEN) && !other.getValue(SWING)
                && other.getValue(COL) == 1 && other.getValue(ROW) == row && other.getValue(FACING) == state.getValue(FACING)
                && other.getValue(HINGE) != state.getValue(HINGE);
    }

    /** Positions and expected (col, row, swing) of the cells of {@code state}'s door in its current open state. */
    private List<int[]> cellsOf(BlockState state) {
        List<int[]> cells = new ArrayList<>();
        boolean open = state.getValue(OPEN);
        for (int r = 0; r < format.rows; r++) {
            if (!open || format.cols == 1) {
                for (int c = 0; c < format.cols; c++) cells.add(new int[] {c, r, 0});
            } else {
                cells.add(new int[] {0, r, 0});
                cells.add(new int[] {0, r, 1});
            }
        }
        return cells;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean left = state.getValue(HINGE) == DoorHingeSide.LEFT;
        boolean open = state.getValue(OPEN);
        Map<Direction, VoxelShape> shapes;
        // 1.5 wide: the second column's leaf (closed) and the swing cell's leaf (open) are 8 px long, on the hinge side / next to the hinge cell
        boolean stub = format.narrow && (open ? state.getValue(SWING) : state.getValue(COL) == 1);
        if (stub && !open && state.getValue(TWIN)) return CLOSED_FULL.get(state.getValue(FACING)); // both stubs
        if (format.half || stub) {
            shapes = open ? (left ? OPEN_HALF_LEFT : OPEN_HALF_RIGHT) : (left ? CLOSED_HALF_LEFT : CLOSED_HALF_RIGHT);
        } else {
            shapes = open ? (left ? OPEN_FULL_LEFT : OPEN_FULL_RIGHT) : CLOSED_FULL;
        }
        return shapes.get(state.getValue(FACING));
    }

    // ------------------------------------------------------------------ placing

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction f = context.getHorizontalDirection();
        DoorHingeSide first = hinge(context, f);
        DoorHingeSide[] order = {first, flip(first)};
        // IG31-34, like Vanilla's double doors: a mirrored 1.5 door next to the click decides the hinge, the stub column is shared
        if (format.narrow) {
            for (DoorHingeSide hinge : order) {
                BlockState state = defaultBlockState().setValue(FACING, f).setValue(HINGE, hinge);
                if (footprintFree(context, pos, state, true)) return state;
            }
        }
        for (DoorHingeSide hinge : order) {
            BlockState state = defaultBlockState().setValue(FACING, f).setValue(HINGE, hinge);
            if (footprintFree(context, pos, state, false)) return state;
        }
        return null;
    }

    /** All cells free; with {@code pairing} the stub column must instead be the closed stub column of a mirrored 1.5 door. */
    private boolean footprintFree(BlockPlaceContext context, BlockPos origin, BlockState state, boolean pairing) {
        Level level = context.getLevel();
        for (int c = 0; c < format.cols; c++) {
            for (int r = 0; r < format.rows; r++) {
                BlockPos p = cell(origin, state, c, r, false);
                if (p.getY() > level.getMaxY() || p.getY() < level.getMinY() || !level.getWorldBorder().isWithinBounds(p)) return false;
                BlockState there = level.getBlockState(p);
                if (pairing && c == 1 ? !isMirroredStub(state, there, r) : !there.canBeReplaced(context)) return false;
            }
        }
        return true;
    }

    /** A wall on one side decides; else the half of the block that was clicked (Vanilla: the hinge goes where you click). */
    private DoorHingeSide hinge(BlockPlaceContext context, Direction f) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean leftWall = false, rightWall = false;
        for (int r = 0; r < format.rows; r++) {
            BlockPos l = pos.above(r).relative(f.getCounterClockWise());
            BlockPos rt = pos.above(r).relative(f.getClockWise());
            leftWall |= level.getBlockState(l).isCollisionShapeFullBlock(level, l);
            rightWall |= level.getBlockState(rt).isCollisionShapeFullBlock(level, rt);
        }
        if (leftWall != rightWall) return leftWall ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
        Vec3 click = context.getClickLocation();
        double cx = click.x - pos.getX();
        double cz = click.z - pos.getZ();
        int sx = f.getStepX();
        int sz = f.getStepZ();
        boolean left = (sx >= 0 || !(cz < 0.5)) && (sx <= 0 || !(cz > 0.5)) && (sz >= 0 || !(cx > 0.5)) && (sz <= 0 || !(cx < 0.5));
        return left ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        for (int c = 0; c < format.cols; c++) {
            for (int r = 0; r < format.rows; r++) {
                if (c == 0 && r == 0) continue;
                BlockPos p = cell(pos, state, c, r, false);
                BlockState cell = cellState(state, c, r, false, false);
                if (c == 1 && isMirroredStub(state, level.getBlockState(p), r)) cell = cell.setValue(TWIN, true);
                level.setBlock(p, cell, Block.UPDATE_ALL);
            }
        }
        if (!level.isClientSide()) updatePower(level, state, pos, null);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(COL) != 0 || state.getValue(ROW) != 0 || state.getValue(SWING)) return true;
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    // ------------------------------------------------------------------ staying together

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == Direction.DOWN && state.getValue(COL) == 0 && state.getValue(ROW) == 0 && !state.getValue(SWING)
                && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(TWIN)) {
            // a shared stub column: when one of the two doors breaks, the cell stays as the other door's stub
            boolean mine = !brokenBy(state, pos, neighbourPos, neighbourState);
            boolean other = !brokenBy(otherView(state), pos, neighbourPos, neighbourState);
            if (!mine && !other) return Blocks.AIR.defaultBlockState();
            if (!mine) return otherView(state).setValue(TWIN, false);
            if (!other) return state.setValue(TWIN, false);
        } else if (brokenBy(state, pos, neighbourPos, neighbourState)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    /** True when the changed neighbour is a cell of {@code state}'s door and no longer the expected part of it. */
    private boolean brokenBy(BlockState state, BlockPos pos, BlockPos neighbourPos, BlockState neighbourState) {
        BlockPos origin = origin(pos, state);
        for (int[] cell : cellsOf(state)) {
            BlockPos p = cell(origin, state, cell[0], cell[1], cell[2] == 1);
            if (p.equals(neighbourPos) && !p.equals(pos) && !isPartner(state, neighbourState, cell[0], cell[1], cell[2] == 1)) return true;
        }
        return false;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()) {
            // Creative: take the cell with the loot away first, without drops (the others follow through updateShape, loot-less).
            BlockPos origin = origin(pos, state);
            if (!origin.equals(pos)) level.setBlock(origin, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            if (state.getValue(TWIN)) {
                BlockPos second = origin(pos, otherView(state));
                if (level.getBlockState(second).is(this)) level.setBlock(second, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ------------------------------------------------------------------ opening

    private boolean on() {
        return ServerTuning.get().features.simpleDoors;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!type.canOpenByHand() || !on()) return InteractionResult.PASS;
        BlockState door = state;
        if (state.getValue(TWIN)) {
            // the shared column: the clicked half picks the door (the stored door's stub lies on its hinge side)
            Direction along = along(state);
            Vec3 hit = hitResult.getLocation().subtract(Vec3.atCenterOf(pos));
            if (hit.x * along.getStepX() + hit.z * along.getStepZ() > 0) door = otherView(state);
        }
        if (!level.isClientSide()) setOpen(player, level, pos, door, !door.getValue(OPEN));
        return InteractionResult.SUCCESS;
    }

    public boolean isOpen(BlockState state) {
        return state.getValue(OPEN);
    }

    /**
     * Opens or closes the whole door of the cell at {@code pos}; false when it did not move (blocked swing area or opening,
     * unloaded chunk, broken door). A blocked try plays the door's closing sound low and puffs a little smoke on the blocking
     * cell, no text.
     */
    public boolean setOpen(@Nullable Entity source, Level level, BlockPos pos, BlockState state, boolean open) {
        if (!state.is(this) || state.getValue(OPEN) == open) return true;
        BlockPos origin = origin(pos, state);
        for (int[] cell : cellsOf(state)) {
            if (!level.getBlockState(cell(origin, state, cell[0], cell[1], cell[2] == 1)).is(this)) return false;
        }
        boolean powered = state.getValue(POWERED);
        List<BlockPos> touched = new ArrayList<>();
        if (format.half) {
            for (int r = 0; r < format.rows; r++) {
                BlockPos p = cell(origin, state, 0, r, false);
                level.setBlock(p, cellState(state, 0, r, false, open), CELL_FLAGS);
                touched.add(p);
            }
        } else if (open) {
            for (int c = 0; c < format.cols; c++) {
                for (int r = 0; r < format.rows; r++) {
                    BlockPos p = cell(origin, state, c, r, true);
                    if (!level.hasChunkAt(p) || !isFree(level.getBlockState(p))) {
                        blocked(source, level, p);
                        return false;
                    }
                }
            }
            for (int r = 0; r < format.rows; r++) {
                BlockPos hinge = cell(origin, state, 0, r, false);
                BlockPos swing = cell(origin, state, 0, r, true);
                BlockPos free = cell(origin, state, 1, r, false);
                BlockState shared = level.getBlockState(free);
                level.setBlock(hinge, cellState(state, 0, r, false, true), CELL_FLAGS);
                level.setBlock(swing, cellState(state, 0, r, true, true), CELL_FLAGS);
                // a shared stub column stays as the other door's stub
                level.setBlock(free, shared.is(this) && shared.getValue(TWIN) ? cellState(otherView(state), 1, r, false, false)
                        : Blocks.AIR.defaultBlockState(), CELL_FLAGS);
                touched.add(hinge);
                touched.add(swing);
                touched.add(free);
            }
        } else {
            for (int r = 0; r < format.rows; r++) {
                BlockPos free = cell(origin, state, 1, r, false);
                if (!isFree(level.getBlockState(free)) && !isMirroredStub(state, level.getBlockState(free), r)) {
                    blocked(source, level, free);
                    return false;
                }
            }
            for (int r = 0; r < format.rows; r++) {
                BlockPos hinge = cell(origin, state, 0, r, false);
                BlockPos swing = cell(origin, state, 0, r, true);
                BlockPos free = cell(origin, state, 1, r, false);
                boolean twin = isMirroredStub(state, level.getBlockState(free), r);
                level.setBlock(swing, Blocks.AIR.defaultBlockState(), CELL_FLAGS);
                level.setBlock(hinge, cellState(state, 0, r, false, false), CELL_FLAGS);
                level.setBlock(free, cellState(state, 1, r, false, false).setValue(TWIN, twin), CELL_FLAGS);
                touched.add(hinge);
                touched.add(swing);
                touched.add(free);
            }
        }
        for (BlockPos p : touched) level.updateNeighborsAt(p, this);
        // null: the opener hears it too (the client does not predict this door, so excluding the player made it silent, IG31-9)
        level.playSound(null, pos, open ? type.doorOpen() : type.doorClose(), SoundSource.BLOCKS, 1.0F,
                level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(source, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return true;
    }

    private static boolean isFree(BlockState state) {
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    private void blocked(@Nullable Entity source, Level level, BlockPos at) {
        level.playSound(null, at, type.doorClose(), SoundSource.BLOCKS, 0.6F, 0.6F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4, 0.25, 0.25, 0.25, 0.01);
        }
    }

    // ------------------------------------------------------------------ redstone

    /**
     * True when a redstone signal reaches the door: next to any position of its plane (also the free column while it is open
     * and air) or, open, next to a swing cell.
     */
    private boolean receivesSignal(Level level, BlockState state, BlockPos pos) {
        BlockPos origin = origin(pos, state);
        for (int c = 0; c < format.cols; c++) {
            for (int r = 0; r < format.rows; r++) {
                if (level.hasNeighborSignal(cell(origin, state, c, r, false))) return true;
                if (state.getValue(OPEN) && !format.half && c == 0 && level.hasNeighborSignal(cell(origin, state, c, r, true))) return true;
            }
        }
        return false;
    }

    /** Polls while an open two-wide door is powered: the opening is air, so its neighbours cannot tell the door the signal is gone. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED) || state.getValue(COL) != 0 || state.getValue(ROW) != 0 || state.getValue(SWING)) return;
        updatePower(level, state, pos, null);
        BlockState now = level.getBlockState(pos);
        if (now.is(this) && now.getValue(POWERED)) level.scheduleTick(pos, this, 8);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide() || block == this) return;
        updatePower(level, state, pos, null);
        if (state.getValue(TWIN)) updatePower(level, otherView(state), pos, null);
    }

    /**
     * Follows the redstone signal of {@code state}'s door. The door's powered flag is read from its lowest hinge cell, and the door
     * is moved from there: a shared stub cell (IG31-34) carries two doors and cannot speak for either.
     */
    private void updatePower(Level level, BlockState state, BlockPos pos, @Nullable Entity source) {
        if (!on()) return;
        BlockPos origin = origin(pos, state);
        BlockState door = level.getBlockState(origin);
        if (!door.is(this) || door.getValue(FACING) != state.getValue(FACING) || door.getValue(HINGE) != state.getValue(HINGE)) return;
        boolean signal = receivesSignal(level, door, origin);
        if (signal == door.getValue(POWERED)) return;
        for (int[] cell : cellsOf(door)) {
            BlockPos p = cell(origin, door, cell[0], cell[1], cell[2] == 1);
            BlockState s = level.getBlockState(p);
            if (s.is(this)) level.setBlock(p, s.setValue(POWERED, signal), CELL_FLAGS);
        }
        BlockState now = level.getBlockState(origin);
        if (now.is(this)) setOpen(source, level, origin, now, signal);
        if (signal && !format.half) level.scheduleTick(origin, this, 8);
    }

    // ------------------------------------------------------------------ misc

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return switch (type) {
            case LAND, AIR -> state.getValue(OPEN);
            case WATER -> false;
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected long getSeed(BlockState state, BlockPos pos) {
        BlockPos o = origin(pos, state);
        return net.minecraft.util.Mth.getSeed(o.getX(), o.getY(), o.getZ());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HINGE, OPEN, POWERED, COL, ROW, SWING, TWIN);
    }
}
