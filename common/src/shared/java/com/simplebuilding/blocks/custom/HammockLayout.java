package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.entity.custom.HammockBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * Where and how a hammock hangs (docs/ai/PLAN-HAENGEMATTE-WINKEL-2026-10-02.md, v3: any angle). Two anchors on rope
 * height, the second (dx, dz) from the first with 2 to 4 free cells along the main axis ({@code max(|dx|, |dz|) - 1}):
 * straight, 45 degrees and every slant in between. The cloth is always two blocks long and hangs in the middle; ropes
 * run symmetrically from its spreaders to both anchors.
 *
 * <ul>
 *   <li>rope layer (anchor height): every cell whose inside the line between the anchor centres crosses (through a
 *       grid corner the line steps on diagonally) - a {@link HammockRopeBlock} each;</li>
 *   <li>cloth layer (one below): every cell whose inside the cloth's horizontal reach crosses - a {@link HammockBlock}
 *       each; the cloth head (sleeping spot, loot owner, the block that draws the hammock) holds the head point.</li>
 * </ul>
 * Every block of a hammock keeps the hammock in its {@link HammockBlockEntity} (first anchor and offset), so each block
 * can work out the whole hammock; the block states only carry what vanilla's bed logic and the shapes need.
 */
public final class HammockLayout {
    public static final int MIN_GAP = 2;
    public static final int MAX_GAP = 4;
    /** Cloth blocks: whether the line runs along {@code facing} (shapes along the line) or slants (centred shapes). */
    public static final BooleanProperty STRAIGHT = BooleanProperty.create("straight");
    /** The cloth sags this much (degrees) from the middle towards each spreader. */
    public static final double SAG_DEGREES = 22.5;
    /** Horizontal reach of the cloth from its middle (blocks): the tilted half (one block) plus the spreader's half. */
    public static final double CLOTH_REACH = Math.cos(Math.toRadians(SAG_DEGREES)) + 1.0 / 16.0;

    private HammockLayout() {
    }

    // --- geometry ------------------------------------------------------------------------------------------------

    /** One cell along the line between the anchor centres, relative to the first anchor, with the line's time inside. */
    public record Cell(int x, int z, double in, double out) {
    }

    /**
     * Every cell the line from the centre of cell (0, 0) to the centre of cell (dx, dz) crosses inside, in order, both
     * ends included. Exact: the crossing times of the x and z grid lines are compared as integers, so a line through a
     * grid corner steps diagonally (and touches neither side cell).
     */
    public static List<Cell> lineCells(int dx, int dz) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        int sx = Integer.signum(dx);
        int sz = Integer.signum(dz);
        List<Cell> out = new ArrayList<>();
        int cx = 0;
        int cz = 0;
        int kx = 1;
        int kz = 1;
        double in = 0.0;
        while (kx <= ax || kz <= az) {
            // x grid line kx is crossed at (2kx - 1) / (2ax), z grid line kz at (2kz - 1) / (2az): compare crosswise
            long tx = kx <= ax ? (long) (2 * kx - 1) * az : Long.MAX_VALUE;
            long tz = kz <= az ? (long) (2 * kz - 1) * ax : Long.MAX_VALUE;
            if (az == 0) {
                tz = Long.MAX_VALUE;
            }
            if (ax == 0) {
                tx = Long.MAX_VALUE;
            }
            double t;
            int nx = cx;
            int nz = cz;
            if (tx == tz) {
                t = (2.0 * kx - 1) / (2.0 * ax);
                nx += sx;
                nz += sz;
                kx++;
                kz++;
            } else if (tx < tz) {
                t = (2.0 * kx - 1) / (2.0 * ax);
                nx += sx;
                kx++;
            } else {
                t = (2.0 * kz - 1) / (2.0 * az);
                nz += sz;
                kz++;
            }
            out.add(new Cell(cx, cz, in, t));
            cx = nx;
            cz = nz;
            in = t;
        }
        out.add(new Cell(cx, cz, in, 1.0));
        return out;
    }

    /** One possible hammock: the first anchor and the second one's offset (same height). */
    public record Spot(BlockPos anchor, int dx, int dz) {
        public BlockPos otherAnchor() {
            return anchor.offset(dx, 0, dz);
        }

        /** Free cells along the main axis. */
        public int gap() {
            return Math.max(Math.abs(dx), Math.abs(dz)) - 1;
        }

        /** Within the limits: 2 to 4 free cells along the main axis. */
        public boolean valid() {
            return gap() >= MIN_GAP && gap() <= MAX_GAP;
        }

        public boolean straight() {
            return dx == 0 || dz == 0;
        }

        /** Horizontal distance between the anchor centres. */
        public double length() {
            return Math.sqrt((double) dx * dx + (double) dz * dz);
        }

        /** Unit vector from the first to the second anchor (x, z). */
        public double ux() {
            return dx / length();
        }

        public double uz() {
            return dz / length();
        }

        /** The nearest compass direction of the line (foot to head); at exactly 45 degrees {@code f} with B - A ~ f + f.getClockWise(). */
        public Direction facing() {
            if (Math.abs(dx) > Math.abs(dz)) {
                return dx > 0 ? Direction.EAST : Direction.WEST;
            }
            if (Math.abs(dz) > Math.abs(dx)) {
                return dz > 0 ? Direction.SOUTH : Direction.NORTH;
            }
            for (Direction f : Direction.Plane.HORIZONTAL) {
                if (f.getStepX() + f.getClockWise().getStepX() == Integer.signum(dx)
                        && f.getStepZ() + f.getClockWise().getStepZ() == Integer.signum(dz)) {
                    return f;
                }
            }
            return Direction.SOUTH;
        }

        private List<Cell> line() {
            return lineCells(dx, dz);
        }

        /** Rope cells (anchor height), in order from the first anchor. */
        public List<BlockPos> ropeCells() {
            List<Cell> line = line();
            List<BlockPos> out = new ArrayList<>();
            for (int i = 1; i < line.size() - 1; i++) {
                out.add(anchor.offset(line.get(i).x(), 0, line.get(i).z()));
            }
            return out;
        }

        /** Cloth cells (one below the anchors), in order from the first anchor. */
        public List<BlockPos> clothCells() {
            double reach = CLOTH_REACH / length();
            List<Cell> line = line();
            List<BlockPos> out = new ArrayList<>();
            for (int i = 1; i < line.size() - 1; i++) {
                Cell c = line.get(i);
                if (c.out() > 0.5 - reach && c.in() < 0.5 + reach) {
                    out.add(anchor.offset(c.x(), -1, c.z()));
                }
            }
            return out;
        }

        /** The cloth head: the cloth cell holding the head point (on a cell border the one towards the first anchor). */
        public BlockPos clothHead() {
            double t = 0.5 + 0.5 / length() - 1.0E-9;
            for (Cell c : line()) {
                if (c.out() >= t) {
                    return anchor.offset(c.x(), -1, c.z());
                }
            }
            return anchor.below();
        }

        /** Middle between the anchor centres (world x, z). */
        public double middleX() {
            return anchor.getX() + 0.5 + dx / 2.0;
        }

        public double middleZ() {
            return anchor.getZ() + 0.5 + dz / 2.0;
        }

        /** Head point (world x, z): the middle plus half a block towards the second anchor; the lying head is drawn there. */
        public double headX() {
            return middleX() + 0.5 * ux();
        }

        public double headZ() {
            return middleZ() + 0.5 * uz();
        }

        /** Rope cells, then cloth cells. */
        public List<BlockPos> cells() {
            List<BlockPos> out = new ArrayList<>(ropeCells());
            out.addAll(clothCells());
            return out;
        }

        /** Everything the renderer draws: both anchor blocks and the layer below (for frustum culling). */
        public AABB renderBounds() {
            BlockPos b = otherAnchor();
            return new AABB(Math.min(anchor.getX(), b.getX()), anchor.getY() - 1, Math.min(anchor.getZ(), b.getZ()),
                    Math.max(anchor.getX(), b.getX()) + 1, anchor.getY() + 1, Math.max(anchor.getZ(), b.getZ()) + 1);
        }

        /** The same hammock moved by {@code offset}. */
        public Spot moved(BlockPos offset) {
            return new Spot(anchor.offset(offset), dx, dz);
        }
    }

    /** The two anchors may hold a hammock (same height, 2 to 4 free cells along the main axis): its spot, else empty. */
    public static Optional<Spot> between(BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) {
            return Optional.empty();
        }
        Spot spot = new Spot(a, b.getX() - a.getX(), b.getZ() - a.getZ());
        return spot.valid() ? Optional.of(spot) : Optional.empty();
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
        record Try(BlockPos anchor, Direction facing) {
        }
        List<Try> tries = new ArrayList<>();
        if (face.getAxis().isHorizontal()) {
            tries.add(new Try(placeAt.relative(face.getOpposite()), face));
            tries.add(new Try(placeAt.relative(face.getOpposite()).above(), face));
        }
        tries.add(new Try(placeAt.above().relative(looking.getOpposite()), looking));
        tries.add(new Try(placeAt.above().relative(looking), looking.getOpposite()));
        for (Try base : tries) {
            for (int gap = MIN_GAP; gap <= MAX_GAP; gap++) {
                Spot spot = new Spot(base.anchor(), base.facing().getStepX() * (gap + 1), base.facing().getStepZ() * (gap + 1));
                if (fits(level, spot)) {
                    return Optional.of(spot);
                }
            }
        }
        return Optional.empty();
    }

    /** Within the limits, both anchors in place and every rope and cloth cell free. */
    public static boolean fits(Level level, Spot spot) {
        if (!spot.valid() || !isAnchor(level, spot.anchor()) || !isAnchor(level, spot.otherAnchor())) {
            return false;
        }
        for (BlockPos cell : spot.cells()) {
            if (!free(level, cell)) {
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

    /** The block states of a hammock at {@code spot}: the cloth cells, then the rope cells (block entities: {@link #link}). */
    public static Map<BlockPos, BlockState> states(Block hammock, Block rope, Spot spot) {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        BlockPos head = spot.clothHead();
        for (BlockPos cell : spot.clothCells()) {
            out.put(cell, hammock.defaultBlockState().setValue(HammockBlock.FACING, spot.facing()).setValue(STRAIGHT, spot.straight())
                    .setValue(HammockBlock.OCCUPIED, false).setValue(HammockBlock.PART, cell.equals(head) ? BedPart.HEAD : BedPart.FOOT));
        }
        for (BlockPos cell : spot.ropeCells()) {
            out.put(cell, rope.defaultBlockState());
        }
        return out;
    }

    /** Tells every block entity of the hammock at {@code spot} which hammock it belongs to (and the clients). */
    public static void link(Level level, Spot spot) {
        for (BlockPos cell : spot.cells()) {
            if (level.getBlockEntity(cell) instanceof HammockBlockEntity entity) {
                entity.setSpot(spot);
            }
        }
    }

    /**
     * Hangs the hammock {@code states} at {@code spot}: every block without shape updates first (so no half-built
     * hammock tears itself down), then the block entities, then the neighbours.
     */
    public static void hang(Level level, Spot spot, Map<BlockPos, BlockState> states) {
        states.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE | Block.UPDATE_KNOWN_SHAPE));
        link(level, spot);
        states.forEach((pos, state) -> {
            level.updateNeighborsAt(pos, state.getBlock());
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        });
    }

    // --- hanging -----------------------------------------------------------------------------------------------

    /** The hammock the block at {@code pos} belongs to (from its block entity), or null if it has none. */
    public static @Nullable Spot spotAt(BlockGetter level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof HammockBlockEntity hammock ? hammock.spot() : null;
    }

    /**
     * Whether the hammock of the block at {@code pos} is whole: both anchors, every rope cell and every cloth cell (one
     * colour, the head where it belongs) in place and belonging to the same hammock.
     */
    public static boolean intact(BlockGetter level, BlockPos pos) {
        Spot spot = spotAt(level, pos);
        if (spot == null || !spot.valid() || !isAnchor(level, spot.anchor()) || !isAnchor(level, spot.otherAnchor())) {
            return false;
        }
        for (BlockPos cell : spot.ropeCells()) {
            if (!(level.getBlockState(cell).getBlock() instanceof HammockRopeBlock) || !spot.equals(spotAt(level, cell))) {
                return false;
            }
        }
        BlockPos head = spot.clothHead();
        Block cloth = null;
        for (BlockPos cell : spot.clothCells()) {
            BlockState part = level.getBlockState(cell);
            if (!(part.getBlock() instanceof HammockBlock) || cloth != null && !part.is(cloth) || !spot.equals(spotAt(level, cell))
                    || (part.getValue(HammockBlock.PART) == BedPart.HEAD) != cell.equals(head)) {
                return false;
            }
            cloth = part.getBlock();
        }
        return true;
    }

    /** The cloth head (sleeping spot, loot owner) of the hammock that the block at {@code pos} belongs to. */
    public static @Nullable BlockPos clothHead(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Spot spot = spotAt(level, pos);
        if (spot == null || !(state.getBlock() instanceof HammockBlock) && !(state.getBlock() instanceof HammockRopeBlock)) {
            return null;
        }
        BlockPos head = spot.clothHead();
        BlockState headState = level.getBlockState(head);
        return headState.getBlock() instanceof HammockBlock && headState.getValue(HammockBlock.PART) == BedPart.HEAD ? head : null;
    }

    /** Before a part goes: every other cell checks itself next tick (slanted cells often touch only at edges). */
    public static void scheduleChecks(net.minecraft.server.level.ServerLevel level, @Nullable Spot spot, BlockPos pos) {
        if (spot == null) {
            return;
        }
        for (BlockPos other : spot.cells()) {
            BlockState otherState = level.getBlockState(other);
            if (!other.equals(pos) && (otherState.getBlock() instanceof HammockBlock || otherState.getBlock() instanceof HammockRopeBlock)) {
                level.scheduleTick(other, otherState.getBlock(), 1);
            }
        }
    }

    /** Scheduled check of one cell: a broken hammock falls (the cloth head drops the item through its loot table). */
    public static void check(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        if (!intact(level, pos)) {
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
