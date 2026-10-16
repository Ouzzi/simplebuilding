package com.simplebuilding.modules.simpleend.world;

import com.simplebuilding.modules.simpleend.EndWood;
import com.simplebuilding.modules.simpleend.block.EndBudBlock;
import com.simplebuilding.modules.simpleend.block.EndLeavesBlock;
import com.simplebuilding.modules.simpleend.block.EndSticksBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tree shapes of the End woods (docs/ai/PLAN-SIMPLE-END-2026-10-12.md, "Umbau 13.10."). One shape code for wood trees
 * (logs, grown from buds outside the End) and stick trees (End sticks, the only trees of the End):
 * a trunk with roots, rising branches that end in leaf clusters, and a crown. Sizes:
 * <ul>
 * <li>{@link Size#BIG} (1 bud): trunk 15-19, 5-7 branches;</li>
 * <li>{@link Size#HUGE} (2x2 buds): 2x2 trunk 23-28, 7-10 branches;</li>
 * <li>{@link Size#COLOSSAL} (4x4 buds): 4x4 trunk (2x2 above 60 %) 38-46, 12-16 long forked branches - the biggest tree.</li>
 * </ul>
 * The trunk must be free; branches stop at obstacles; only loaded (world generation: writable) chunks are touched.
 * Leaves and branches are set without neighbour updates, the leaf distance is computed here. A grown tree marks
 * a few of its leaves to drop a bud or a fruit ({@link EndLeavesBlock#DROP}): on average {@link #BUDS_WOOD} /
 * {@link #BUDS_STICKS} buds and {@link #FRUITS_WOOD} / {@link #FRUITS_STICKS} fruits per tree.
 */
public final class EndTrees {
    public static final double BUDS_WOOD = 0.8, BUDS_STICKS = 1.5, FRUITS_WOOD = 1.0, FRUITS_STICKS = 2.0;

    public enum Size {
        BIG(1, 15, 5, 5, 3, 3, 2, 2, 3.0, 3.6),
        HUGE(2, 23, 6, 7, 4, 4, 4, 2, 3.5, 4.4),
        COLOSSAL(4, 38, 9, 12, 5, 6, 5, 3, 4.0, 5.2);

        public final int width;
        final int minHeight, heightRange, minBranches, branchRange, minOut, outRange, minUp;
        final double blob, crown;

        Size(int width, int minHeight, int heightRange, int minBranches, int branchRange, int minOut, int outRange, int minUp, double blob, double crown) {
            this.width = width;
            this.minHeight = minHeight;
            this.heightRange = heightRange;
            this.minBranches = minBranches;
            this.branchRange = branchRange;
            this.minOut = minOut;
            this.outRange = outRange;
            this.minUp = minUp;
            this.blob = blob;
            this.crown = crown;
        }
    }

    private EndTrees() {}

    /** One part of a tree: its wood blocks in growth order and the leaf clusters it carries when it is complete. */
    static final class Part {
        final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        final List<double[]> blobs = new ArrayList<>();
    }

    /** A planned tree: part 0 is the trunk (must fit), the rest are roots and branches. */
    public static final class Plan {
        final List<Part> parts = new ArrayList<>();
        final EndWood wood;
        final boolean sticks;
        public int height;

        Plan(EndWood wood, boolean sticks) {
            this.wood = wood;
            this.sticks = sticks;
        }

        Part part() {
            Part p = new Part();
            parts.add(p);
            return p;
        }

        BlockState state(Direction.Axis axis, int thickness) {
            if (sticks) return ((EndSticksBlock) wood.sticks).grown(axis, thickness);
            return wood.log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
        }

        public int woodCount() {
            return parts.stream().mapToInt(p -> p.blocks.size()).sum();
        }

        /** Horizontal reach of the wood from the trunk corner, in blocks (leaves add up to a cluster radius). */
        public int reach(BlockPos corner) {
            int max = 0;
            for (Part p : parts)
                for (BlockPos b : p.blocks.keySet()) max = Math.max(max, Math.max(Math.abs(b.getX() - corner.getX()), Math.abs(b.getZ() - corner.getZ())));
            return max;
        }

        /** Highest wood block above the corner. */
        public int top(BlockPos corner) {
            int max = 0;
            for (Part p : parts) for (BlockPos b : p.blocks.keySet()) max = Math.max(max, b.getY() - corner.getY());
            return max;
        }
    }

    /** Result of a grown tree (positions as placed). */
    public record Grown(List<BlockPos> wood, List<BlockPos> leaves, int buds, int fruits) {}

    /** {@code floor(mean)} or one more, so the average over many trees is exactly {@code mean}. */
    public static int marks(RandomSource r, double mean) {
        int base = (int) Math.floor(mean);
        return base + (r.nextDouble() < mean - base ? 1 : 0);
    }

    public static Plan plan(Size size, EndWood w, boolean sticks, RandomSource r, BlockPos corner) {
        Plan plan = new Plan(w, sticks);
        int h = size.minHeight + r.nextInt(size.heightRange);
        plan.height = h;
        int narrowAt = size == Size.COLOSSAL ? (int) (h * 0.6) : h;
        Part trunk = plan.part();
        for (int y = 0; y < h; y++) {
            boolean narrow = y >= narrowAt;
            int from = narrow ? 1 : 0, to = narrow ? 3 : size.width;
            for (int dx = from; dx < to; dx++)
                for (int dz = from; dz < to; dz++) trunk.blocks.put(corner.offset(dx, y, dz), plan.state(Direction.Axis.Y, 3));
        }
        double mid = (size.width - 1) / 2.0;
        trunk.blobs.add(new double[] {corner.getX() + mid, corner.getY() + h, corner.getZ() + mid, size.crown});
        roots(plan, size, corner, r);
        int n = size.minBranches + r.nextInt(size.branchRange);
        double angle = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < n; i++) {
            double share = n == 1 ? 1 : i / (double) (n - 1);
            int y = Mth.clamp((int) (h * (0.45 + 0.47 * share)) + r.nextInt(3) - 1, 2, h - 1);
            angle += 2.39996 + (r.nextDouble() - 0.5) * 0.5;
            int out = (int) Math.round((size.minOut + r.nextInt(size.outRange)) * (1.0 - 0.35 * share));
            int up = size.minUp + r.nextInt(size.minUp + 1);
            BlockPos start = trunkCell(trunk, corner.getY() + y, angle);
            double[] end = branch(plan, start, angle, Math.max(2, out), up, size.blob + r.nextDouble() * 0.5, size == Size.COLOSSAL);
            if (size != Size.BIG && r.nextBoolean()) {
                BlockPos fork = BlockPos.containing(end[4], end[5], end[6]);
                branch(plan, fork, angle + (r.nextBoolean() ? 0.8 : -0.8), Math.max(2, out / 2), Math.max(1, up / 2), size.blob - 0.4, false);
            }
        }
        return plan;
    }

    /** The trunk block at height y that lies furthest in the branch direction. */
    private static BlockPos trunkCell(Part trunk, int y, double angle) {
        double cx = Math.cos(angle), cz = Math.sin(angle);
        BlockPos best = null;
        double bestDot = -1e9;
        for (BlockPos p : trunk.blocks.keySet()) {
            if (p.getY() != y) continue;
            double dot = p.getX() * cx + p.getZ() * cz;
            if (dot > bestDot) {
                bestDot = dot;
                best = p;
            }
        }
        return best;
    }

    /**
     * A branch from a trunk block: {@code out} horizontal and {@code up} upward steps, one axis per step (so the wood
     * stays face-connected), ending in a leaf cluster; long branches carry a second cluster half way. Returns
     * {cx, cy, cz, r, halfX, halfY, halfZ} of the end cluster and the half-way point.
     */
    private static double[] branch(Plan plan, BlockPos start, double angle, int out, int up, double blob, boolean midCluster) {
        Part part = plan.part();
        double tx = start.getX() + 0.5 + Math.cos(angle) * out, tz = start.getZ() + 0.5 + Math.sin(angle) * out;
        BlockPos.MutableBlockPos p = start.mutable();
        int horizontal = Math.abs((int) Math.floor(tx) - start.getX()) + Math.abs((int) Math.floor(tz) - start.getZ());
        int total = horizontal + up;
        int ups = 0;
        BlockPos half = start;
        for (int i = 1; i <= total; i++) {
            boolean goUp = ups < up && (i * up) / total > ups;
            Direction.Axis axis;
            if (goUp) {
                p.move(Direction.UP);
                ups++;
                axis = Direction.Axis.Y;
            } else {
                double dx = tx - (p.getX() + 0.5), dz = tz - (p.getZ() + 0.5);
                if (Math.abs(dx) >= Math.abs(dz)) {
                    p.move(dx > 0 ? Direction.EAST : Direction.WEST);
                    axis = Direction.Axis.X;
                } else {
                    p.move(dz > 0 ? Direction.SOUTH : Direction.NORTH);
                    axis = Direction.Axis.Z;
                }
            }
            part.blocks.put(p.immutable(), plan.state(axis, i <= total * 0.6 ? 2 : 1));
            if (i == total / 2) half = p.immutable();
        }
        part.blobs.add(new double[] {p.getX(), p.getY() + 1, p.getZ(), blob});
        if (midCluster) part.blobs.add(new double[] {half.getX(), half.getY() + 1, half.getZ(), blob - 1.2});
        return new double[] {p.getX(), p.getY() + 1, p.getZ(), blob, half.getX(), half.getY(), half.getZ()};
    }

    /** Root flares at the trunk foot: short columns that get lower away from the trunk (thickness 2: they never hold a stick tree up). */
    private static void roots(Plan plan, Size size, BlockPos corner, RandomSource r) {
        int start = r.nextInt(4);
        int sides = size == Size.BIG ? 2 : 4;
        for (int s = 0; s < sides; s++) {
            Direction dir = Direction.from2DDataValue((start + s) % 4);
            int lanes = size == Size.COLOSSAL ? 2 : 1;
            for (int lane = 0; lane < lanes; lane++) {
                int along = size == Size.COLOSSAL ? 1 + lane : r.nextInt(size.width);
                int len = size == Size.BIG ? 1 : size == Size.HUGE ? 1 + r.nextInt(2) : 2 + r.nextInt(3);
                int high = size == Size.BIG ? 1 + r.nextInt(2) : size == Size.HUGE ? 2 + r.nextInt(2) : 3 + r.nextInt(3);
                // the trunk block on this side, at the chosen lane
                int bx = dir.getStepX() > 0 ? size.width - 1 : dir.getStepX() < 0 ? 0 : along;
                int bz = dir.getStepZ() > 0 ? size.width - 1 : dir.getStepZ() < 0 ? 0 : along;
                Part part = plan.part();
                for (int i = 1; i <= len; i++) {
                    BlockPos foot = corner.offset(bx + dir.getStepX() * i, 0, bz + dir.getStepZ() * i);
                    int columnHeight = Math.max(1, high - (i - 1) * 2);
                    for (int y = 0; y < columnHeight; y++) part.blocks.put(foot.above(y), plan.state(Direction.Axis.Y, 2));
                }
            }
        }
    }

    private static boolean free(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.getBlock() instanceof EndBudBlock;
    }

    /** Running world: only loaded chunks. World generation: only the 3x3 chunks a feature may write. */
    private static boolean writable(WorldGenLevel level, BlockPos origin, BlockPos p) {
        if (level.isOutsideBuildHeight(p)) return false;
        if (level instanceof ServerLevel server) return server.hasChunkAt(p);
        return Math.abs(SectionPos.blockToSectionCoord(p.getX()) - SectionPos.blockToSectionCoord(origin.getX())) <= 1
                && Math.abs(SectionPos.blockToSectionCoord(p.getZ()) - SectionPos.blockToSectionCoord(origin.getZ())) <= 1;
    }

    /** Places the plan; null (nothing changed) when the trunk does not fit. */
    static Grown place(WorldGenLevel level, Plan plan, BlockPos origin, RandomSource r) {
        Part trunk = plan.parts.getFirst();
        for (BlockPos p : trunk.blocks.keySet()) if (!writable(level, origin, p) || !free(level.getBlockState(p))) return null;
        List<BlockPos> wood = new ArrayList<>();
        List<double[]> blobs = new ArrayList<>(trunk.blobs);
        trunk.blocks.forEach((p, s) -> {
            level.setBlock(p, s, 19);
            wood.add(p);
        });
        for (Part part : plan.parts.subList(1, plan.parts.size())) {
            boolean complete = true;
            for (Map.Entry<BlockPos, BlockState> e : part.blocks.entrySet()) {
                BlockPos p = e.getKey();
                BlockState here = level.getBlockState(p);
                if (here.is(e.getValue().getBlock())) continue; // crosses another branch of this tree
                if (!writable(level, origin, p) || !free(here)) {
                    complete = false;
                    break;
                }
                level.setBlock(p, e.getValue(), 18);
                wood.add(p);
            }
            if (complete) blobs.addAll(part.blobs);
        }
        Set<BlockPos> leafSpots = new LinkedHashSet<>();
        for (double[] b : blobs) blob(leafSpots, b[0], b[1], b[2], b[3], 1.3);
        Map<BlockPos, Integer> dist = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> woodSet = new HashSet<>(wood);
        for (BlockPos p : wood) {
            dist.put(p, 0);
            queue.add(p);
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = dist.get(p);
            if (d >= 6) continue;
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                if (leafSpots.contains(n) && !dist.containsKey(n) && !woodSet.contains(n)) {
                    dist.put(n, d + 1);
                    queue.add(n);
                }
            }
        }
        BlockState leaf = plan.wood.leaves.defaultBlockState();
        List<BlockPos> leaves = new ArrayList<>();
        for (BlockPos p : leafSpots) {
            Integer d = dist.get(p);
            if (d == null || d == 0 || !writable(level, origin, p)) continue;
            BlockState here = level.getBlockState(p);
            if (here.isAir() || here.canBeReplaced() && !here.is(BlockTags.LEAVES) && here.getFluidState().isEmpty()) {
                level.setBlock(p, leaf.setValue(LeavesBlock.DISTANCE, d), 18);
                leaves.add(p);
            }
        }
        // Set without neighbour updates: mark the leaves that no air touches (denser model, N65) once the crown stands.
        for (BlockPos p : leaves) {
            BlockState s = level.getBlockState(p);
            if (s.is(plan.wood.leaves) && EndLeavesBlock.isInner(level, p)) level.setBlock(p, s.setValue(EndLeavesBlock.INNER, true), 18);
        }
        int buds = mark(level, leaves, marks(r, plan.sticks ? BUDS_STICKS : BUDS_WOOD), EndLeavesBlock.BUD, r);
        int fruits = mark(level, leaves, marks(r, plan.sticks ? FRUITS_STICKS : FRUITS_WOOD), EndLeavesBlock.FRUIT, r);
        return new Grown(wood, leaves, buds, fruits);
    }

    private static int mark(WorldGenLevel level, List<BlockPos> leaves, int count, int drop, RandomSource r) {
        int done = 0;
        for (int tries = 0; done < count && tries < count * 8 && !leaves.isEmpty(); tries++) {
            BlockPos p = leaves.get(r.nextInt(leaves.size()));
            BlockState s = level.getBlockState(p);
            if (!s.hasProperty(EndLeavesBlock.DROP) || s.getValue(EndLeavesBlock.DROP) != EndLeavesBlock.NONE) continue;
            level.setBlock(p, s.setValue(EndLeavesBlock.DROP, drop), 18);
            done++;
        }
        return done;
    }

    private static void blob(Set<BlockPos> out, double cx, double cy, double cz, double r, double squash) {
        int ri = (int) Math.ceil(r);
        int x0 = (int) Math.floor(cx), y0 = (int) Math.floor(cy), z0 = (int) Math.floor(cz);
        for (int dx = -ri - 1; dx <= ri + 1; dx++)
            for (int dy = -ri; dy <= ri; dy++)
                for (int dz = -ri - 1; dz <= ri + 1; dz++) {
                    double fx = x0 + dx - cx, fz = z0 + dz - cz, fy = (y0 + dy - cy) * squash;
                    if (fx * fx + fz * fz + fy * fy <= r * r + 0.3) out.add(new BlockPos(x0 + dx, y0 + dy, z0 + dz));
                }
    }

    /**
     * Grows a tree of the given size with its trunk corner at {@code corner} (north-west, lowest). {@code buds} are
     * removed first and put back when the trunk does not fit. Returns null when nothing grew.
     */
    public static Grown grow(WorldGenLevel level, BlockPos corner, EndWood w, Size size, boolean sticks, RandomSource r, BlockPos... buds) {
        Map<BlockPos, BlockState> old = new HashMap<>();
        for (BlockPos b : buds) {
            old.put(b, level.getBlockState(b));
            level.setBlock(b, Blocks.AIR.defaultBlockState(), 4);
        }
        Grown grown = place(level, plan(size, w, sticks, r, corner), corner, r);
        if (grown == null) old.forEach((p, s) -> level.setBlock(p, s, 4));
        return grown;
    }

    /** Dark scrub outside the End: tapering columns of natural End sticks (they fall like a stick tree). */
    public static boolean growScrub(WorldGenLevel level, BlockPos base, EndWood w, RandomSource r, boolean fromBud) {
        Plan plan = new Plan(w, true);
        Part trunk = plan.part();
        column(trunk, w, base, 8 + r.nextInt(6));
        int sides = r.nextInt(3);
        int start = r.nextInt(4);
        for (int i = 0; i < sides; i++) {
            BlockPos side = base.relative(Direction.from2DDataValue((start + i) % 4));
            if (level.getBlockState(side.below()).isFaceSturdy(level, side.below(), Direction.UP)) column(plan.part(), w, side, 3 + r.nextInt(5));
        }
        BlockState old = level.getBlockState(base);
        if (fromBud) level.setBlock(base, Blocks.AIR.defaultBlockState(), 4);
        for (BlockPos p : trunk.blocks.keySet()) {
            if (!writable(level, base, p) || !free(level.getBlockState(p))) {
                if (fromBud) level.setBlock(base, old, 4);
                return false;
            }
        }
        for (Part part : plan.parts) {
            for (Map.Entry<BlockPos, BlockState> e : part.blocks.entrySet()) {
                if (!writable(level, base, e.getKey()) || !free(level.getBlockState(e.getKey()))) break;
                level.setBlock(e.getKey(), e.getValue(), 19);
            }
        }
        return true;
    }

    /** One stick column, thick at the bottom and a thin tip on top. */
    private static void column(Part part, EndWood w, BlockPos base, int height) {
        for (int i = 0; i < height; i++) {
            int fromTop = height - 1 - i;
            part.blocks.put(base.above(i), ((EndSticksBlock) w.sticks).grown(Direction.Axis.Y, Math.min(3, fromTop + 1)));
        }
    }
}
