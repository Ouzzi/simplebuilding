package com.simplebuilding.blocks.custom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * Where and how a hammock hangs (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md, v2). Two anchors on rope height, between them
 * {@code gap} free cells (2 to 4) in a straight line or at 45 degrees. The cloth is always two blocks long and hangs in
 * the middle; ropes run symmetrically from its spreaders to both anchors.
 *
 * <p>Every block of a hammock carries {@code facing}, {@code diagonal}, {@code gap} and {@code index} (its cell along
 * the line, 0 next to the first anchor), so each block can work out the whole hammock: the upper layer is a
 * {@link HammockRopeBlock} in every cell, the lower layer a {@link HammockBlock} in the cells under the cloth; the
 * cloth head (the sleeping spot, the loot owner, the block that draws the cloth) is the cell of the head point.
 * The line runs along {@code facing}, diagonally along {@code facing} + {@code facing.getClockWise()}.
 */
public final class HammockLayout {
    public static final int MIN_GAP = 2;
    public static final int MAX_GAP = 4;
    public static final BooleanProperty DIAGONAL = BooleanProperty.create("diagonal");
    public static final IntegerProperty GAP = IntegerProperty.create("gap", MIN_GAP, MAX_GAP);
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, MAX_GAP - 1);

    private HammockLayout() {
    }

    // --- geometry ------------------------------------------------------------------------------------------------

    /** Cells under the cloth: gap 2 -> 0,1; gap 3 -> 0,1,2 (half cells at both ends); gap 4 -> 1,2. */
    public static List<Integer> clothCells(int gap) {
        return switch (gap) {
            case 2 -> List.of(0, 1);
            case 3 -> List.of(0, 1, 2);
            default -> List.of(1, 2);
        };
    }

    /** The cloth head: the cell holding the head point (cloth middle + half a block towards the second anchor). */
    public static int headCell(int gap) {
        return gap == 4 ? 2 : 1;
    }

    /** One step along the line. */
    public static BlockPos step(Direction facing, boolean diagonal) {
        BlockPos s = BlockPos.ZERO.relative(facing);
        return diagonal ? s.relative(facing.getClockWise()) : s;
    }

    /**
     * How far (in blocks, along the line) the head point lies from the centre of the cloth head cell - the lying
     * player is drawn there (client mixin); 0 means vanilla's bed position is already right.
     */
    public static double headShift(int gap, boolean diagonal) {
        double cell = diagonal ? Math.sqrt(2.0) : 1.0;
        // line length from the first anchor's face (corner) to the head point, minus the head cell's centre
        return (gap * cell / 2.0 + 0.5) - (headCell(gap) + 0.5) * cell;
    }

    /** One possible hammock: first anchor, direction, straight or diagonal, number of free cells. */
    public record Spot(BlockPos anchor, Direction facing, boolean diagonal, int gap) {
        public BlockPos step() {
            return HammockLayout.step(facing, diagonal);
        }

        /** Upper (rope) cell {@code index}. */
        public BlockPos rope(int index) {
            BlockPos s = step();
            return anchor.offset(s.getX() * (index + 1), 0, s.getZ() * (index + 1));
        }

        public BlockPos otherAnchor() {
            return rope(gap);
        }

        public BlockPos clothHead() {
            return rope(headCell(gap)).below();
        }
    }

    /** The hammock the block {@code state} at {@code pos} belongs to. */
    public static Spot spotOf(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(HammockBlock.FACING);
        boolean diagonal = state.getValue(DIAGONAL);
        int gap = state.getValue(GAP);
        int index = state.getValue(INDEX);
        BlockPos upper = state.getBlock() instanceof HammockRopeBlock ? pos : pos.above();
        BlockPos s = step(facing, diagonal);
        return new Spot(upper.offset(-s.getX() * (index + 1), 0, -s.getZ() * (index + 1)), facing, diagonal, gap);
    }

    /** The two anchors may hold a hammock (same height, straight or 45 degrees, 2 to 4 free cells): its spot, else empty. */
    public static Optional<Spot> between(BlockPos a, BlockPos b) {
        int dx = b.getX() - a.getX();
        int dz = b.getZ() - a.getZ();
        if (a.getY() != b.getY() || dx == 0 && dz == 0) {
            return Optional.empty();
        }
        boolean diagonal = dx != 0 && dz != 0;
        if (diagonal && Math.abs(dx) != Math.abs(dz)) {
            return Optional.empty();
        }
        int gap = Math.max(Math.abs(dx), Math.abs(dz)) - 1;
        if (gap < MIN_GAP || gap > MAX_GAP) {
            return Optional.empty();
        }
        int sx = Integer.signum(dx);
        int sz = Integer.signum(dz);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos s = step(facing, diagonal);
            if (s.getX() == sx && s.getZ() == sz) {
                return Optional.of(new Spot(a, facing, diagonal, gap));
            }
        }
        return Optional.empty();
    }

    // --- anchors and placing -----------------------------------------------------------------------------------

    /**
     * Anchors: any block with a collision shape that is not replaceable and not part of a hammock - full blocks,
     * fences, walls, rods, glass, leaves. Air, plants, water and snow layers do not hold a rope.
     */
    public static boolean isAnchor(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.canBeReplaced() || state.getBlock() instanceof HammockBlock
                || state.getBlock() instanceof HammockRopeBlock) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * The straight hammock a single click hangs. A click on the side of a block uses that block (or the one above it)
     * as the first anchor and the hammock spans away from it; otherwise the player's facing (or its opposite) is the
     * direction and the clicked spot is under the first rope cell. The shortest fitting gap wins.
     */
    public static Optional<Spot> find(Level level, BlockPos placeAt, Direction face, Direction looking) {
        List<Spot> tries = new ArrayList<>();
        if (face.getAxis().isHorizontal()) {
            tries.add(new Spot(placeAt.relative(face.getOpposite()), face, false, 0));
            tries.add(new Spot(placeAt.relative(face.getOpposite()).above(), face, false, 0));
        }
        tries.add(new Spot(placeAt.above().relative(looking.getOpposite()), looking, false, 0));
        tries.add(new Spot(placeAt.above().relative(looking), looking.getOpposite(), false, 0));
        for (Spot base : tries) {
            for (int gap = MIN_GAP; gap <= MAX_GAP; gap++) {
                Spot spot = new Spot(base.anchor(), base.facing(), false, gap);
                if (fits(level, spot)) {
                    return Optional.of(spot);
                }
            }
        }
        return Optional.empty();
    }

    /** Both anchors in place and every cell between them free (upper layer) as well as the cells under the cloth. */
    public static boolean fits(Level level, Spot spot) {
        if (!isAnchor(level, spot.anchor()) || !isAnchor(level, spot.otherAnchor())) {
            return false;
        }
        for (int i = 0; i < spot.gap(); i++) {
            if (!free(level, spot.rope(i))) {
                return false;
            }
        }
        for (int i : clothCells(spot.gap())) {
            if (!free(level, spot.rope(i).below())) {
                return false;
            }
        }
        return true;
    }

    private static boolean free(Level level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced() && level.getWorldBorder().isWithinBounds(pos)
                && !level.isOutsideBuildHeight(pos);
    }

    /** Whether no entity stands where the cloth of the hammock would collide. */
    public static boolean unobstructed(Level level, Map<BlockPos, BlockState> states) {
        for (Map.Entry<BlockPos, BlockState> entry : states.entrySet()) {
            if (!level.isUnobstructed(entry.getValue(), entry.getKey(), CollisionContext.empty())) {
                return false;
            }
        }
        return true;
    }

    /** The block states of a hammock at {@code spot}: the cloth cells, then the rope cells. */
    public static Map<BlockPos, BlockState> states(Block hammock, Block rope, Spot spot) {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        for (int i : clothCells(spot.gap())) {
            out.put(spot.rope(i).below(), clothState(hammock, spot, i));
        }
        for (int i = 0; i < spot.gap(); i++) {
            out.put(spot.rope(i), ropeState(rope, spot, i));
        }
        return out;
    }

    private static BlockState clothState(Block hammock, Spot spot, int index) {
        return hammock.defaultBlockState().setValue(HammockBlock.FACING, spot.facing()).setValue(DIAGONAL, spot.diagonal())
                .setValue(GAP, spot.gap()).setValue(INDEX, index).setValue(HammockBlock.OCCUPIED, false)
                .setValue(HammockBlock.PART, index == headCell(spot.gap()) ? BedPart.HEAD : BedPart.FOOT);
    }

    private static BlockState ropeState(Block rope, Spot spot, int index) {
        return rope.defaultBlockState().setValue(HammockRopeBlock.FACING, spot.facing()).setValue(DIAGONAL, spot.diagonal())
                .setValue(GAP, spot.gap()).setValue(INDEX, index);
    }

    // --- hanging -----------------------------------------------------------------------------------------------

    /**
     * Whether the hammock of the block {@code state} at {@code pos} is whole: both anchors, every rope cell and every
     * cloth cell (one colour, the head where it belongs) in place on the same line.
     */
    public static boolean intact(BlockGetter level, BlockPos pos, BlockState state) {
        Spot spot = spotOf(pos, state);
        if (!isAnchor(level, spot.anchor()) || !isAnchor(level, spot.otherAnchor())) {
            return false;
        }
        for (int i = 0; i < spot.gap(); i++) {
            BlockState rope = level.getBlockState(spot.rope(i));
            if (!(rope.getBlock() instanceof HammockRopeBlock) || !sameLine(rope, spot, i)) {
                return false;
            }
        }
        Block cloth = null;
        for (int i : clothCells(spot.gap())) {
            BlockState part = level.getBlockState(spot.rope(i).below());
            if (!(part.getBlock() instanceof HammockBlock) || cloth != null && !part.is(cloth) || !sameLine(part, spot, i)
                    || (part.getValue(HammockBlock.PART) == BedPart.HEAD) != (i == headCell(spot.gap()))) {
                return false;
            }
            cloth = part.getBlock();
        }
        return true;
    }

    private static boolean sameLine(BlockState state, Spot spot, int index) {
        return state.getValue(HammockBlock.FACING) == spot.facing() && state.getValue(DIAGONAL) == spot.diagonal()
                && state.getValue(GAP) == spot.gap() && state.getValue(INDEX) == index;
    }

    /** Every other block position of the hammock of {@code state} at {@code pos} (rope cells and cloth cells). */
    public static List<BlockPos> otherCells(BlockPos pos, BlockState state) {
        Spot spot = spotOf(pos, state);
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < spot.gap(); i++) {
            out.add(spot.rope(i));
        }
        for (int i : clothCells(spot.gap())) {
            out.add(spot.rope(i).below());
        }
        out.remove(pos);
        return out;
    }

    /** The cloth head (sleeping spot, loot owner) of the hammock that the block at {@code pos} belongs to. */
    public static @Nullable BlockPos clothHead(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof HammockBlock) && !(state.getBlock() instanceof HammockRopeBlock)) {
            return null;
        }
        BlockPos head = spotOf(pos, state).clothHead();
        BlockState headState = level.getBlockState(head);
        return headState.getBlock() instanceof HammockBlock && headState.getValue(HammockBlock.PART) == BedPart.HEAD ? head : null;
    }

    /** After a part went: every other cell checks itself next tick (diagonal cells only touch at edges). */
    public static void scheduleChecks(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState state) {
        for (BlockPos other : otherCells(pos, state)) {
            BlockState otherState = level.getBlockState(other);
            if (otherState.getBlock() instanceof HammockBlock || otherState.getBlock() instanceof HammockRopeBlock) {
                level.scheduleTick(other, otherState.getBlock(), 1);
            }
        }
    }

    /** Scheduled check of one cell: a broken hammock falls (the cloth head drops the item through its loot table). */
    public static void check(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState state) {
        if (!intact(level, pos, state)) {
            level.destroyBlock(pos, true);
        }
    }

    /**
     * Refusal without text (owner rule): the dispenser's fail sound for this player only and, where a hammock could not
     * be hung, a puff of smoke.
     */
    public static void refuse(net.minecraft.server.level.ServerPlayer player, @Nullable BlockPos at) {
        com.simplebuilding.util.Feedback.playTo(player, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.4F);
        if (at != null) {
            player.level().sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, at.getX() + 0.5, at.getY() + 0.5,
                    at.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.01);
        }
    }

    /**
     * Creative breaking: removes the cloth head (the loot owner) quietly first, so the falling rest drops nothing, as
     * vanilla does for beds. {@code pos} is the block the player breaks.
     */
    public static void quietlyRemoveLootOwner(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (level.isClientSide() || !player.preventsBlockDrops()) {
            return;
        }
        BlockPos head = clothHead(level, pos);
        if (head != null && !head.equals(pos)) {
            BlockState headState = level.getBlockState(head);
            level.setBlock(head, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            level.levelEvent(player, 2001, head, Block.getId(headState));
        }
    }
}
