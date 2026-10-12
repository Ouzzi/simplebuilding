package com.simplebuilding.modules.simpleend.world;

import com.simplebuilding.modules.simpleend.EndWood;
import com.simplebuilding.modules.simpleend.block.EndSticksBlock;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tree shapes of the End woods (owner 2026-10-12), shared by the buds and the biome features:
 * <ul>
 * <li>big: one trunk 8-11 high, 3-4 chorus-like branches (out, then up) with leaf clusters and a crown;</li>
 * <li>huge (2x2 buds): a 2x2 trunk 13-17 high, 4-6 longer branches and a wide crown;</li>
 * <li>scrub (dark): End-stick columns 8-13 high that taper like dripstone, with 0-2 shorter side columns;</li>
 * <li>natural (world generation only): narrow and knobby like a chorus plant, 4-7 high, short side stubs.</li>
 * </ul>
 * A shape is planned first; it is only placed when every trunk block is free, so a bud stays when there is no room.
 */
public final class EndTrees {
    private EndTrees() {}

    private static final class Plan {
        final Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
        final Set<BlockPos> leaves = new LinkedHashSet<>();
        final EndWood wood;

        Plan(EndWood wood) {
            this.wood = wood;
        }

        void log(BlockPos p, Direction.Axis axis) {
            logs.put(p.immutable(), wood.log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
        }

        void blob(BlockPos c, double r, double squash) {
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++)
                for (int dy = -ri; dy <= ri; dy++)
                    for (int dz = -ri; dz <= ri; dz++)
                        if (dx * dx + dz * dz + dy * dy * squash * squash <= r * r + 0.3) leaves.add(c.offset(dx, dy, dz));
        }
    }

    private static boolean free(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.REPLACEABLE_BY_TREES)
                || s.getBlock() instanceof com.simplebuilding.modules.simpleend.block.EndBudBlock;
    }

    /** Places the plan when all logs fit; leaves fill free space only and get their real log distance. */
    private static boolean place(WorldGenLevel level, Plan plan) {
        for (BlockPos p : plan.logs.keySet()) {
            if (level.isOutsideBuildHeight(p) || !free(level.getBlockState(p))) return false;
        }
        plan.logs.forEach((p, s) -> level.setBlock(p, s, 19));
        Map<BlockPos, Integer> dist = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(plan.logs.keySet());
        plan.logs.keySet().forEach(p -> dist.put(p, 0));
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = dist.get(p);
            if (d >= 6) continue;
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                if (plan.leaves.contains(n) && !dist.containsKey(n)) {
                    dist.put(n, d + 1);
                    queue.add(n);
                }
            }
        }
        BlockState leaf = plan.wood.leaves.defaultBlockState();
        for (BlockPos p : plan.leaves) {
            Integer d = dist.get(p);
            if (d == null || d == 0 || level.isOutsideBuildHeight(p)) continue;
            BlockState here = level.getBlockState(p);
            if (here.isAir() || here.canBeReplaced()) level.setBlock(p, leaf.setValue(LeavesBlock.DISTANCE, d), 19);
        }
        return true;
    }

    private static Direction.Axis axis(Direction d) {
        return d.getAxis();
    }

    private static void branch(Plan plan, BlockPos from, Direction dir, int out, int up, double blob) {
        BlockPos.MutableBlockPos p = from.mutable();
        for (int i = 0; i < out; i++) plan.log(p.move(dir), axis(dir));
        for (int i = 0; i < up; i++) plan.log(p.move(Direction.UP), Direction.Axis.Y);
        plan.blob(p.immutable().above(), blob, 1.4);
    }

    public static boolean growBig(WorldGenLevel level, BlockPos base, EndWood w, RandomSource r, boolean fromBud) {
        Plan plan = new Plan(w);
        int h = 8 + r.nextInt(4);
        for (int y = 0; y < h; y++) plan.log(base.above(y), Direction.Axis.Y);
        int branches = 3 + r.nextInt(2);
        int start = r.nextInt(4);
        for (int i = 0; i < branches; i++) {
            Direction dir = Direction.from2DDataValue((start + i) % 4);
            branch(plan, base.above(h - 5 + r.nextInt(3)), dir, 2 + r.nextInt(2), 1 + r.nextInt(2), 2.0);
        }
        plan.blob(base.above(h), 2.6, 1.3);
        return grow(level, base, plan, fromBud ? new BlockPos[] {base} : new BlockPos[0]);
    }

    public static boolean growHuge(WorldGenLevel level, BlockPos corner, EndWood w, RandomSource r, boolean fromBud) {
        Plan plan = new Plan(w);
        int h = 13 + r.nextInt(5);
        for (int y = 0; y < h; y++)
            for (int dx = 0; dx < 2; dx++)
                for (int dz = 0; dz < 2; dz++) plan.log(corner.offset(dx, y, dz), Direction.Axis.Y);
        int branches = 4 + r.nextInt(3);
        int start = r.nextInt(4);
        for (int i = 0; i < branches; i++) {
            Direction dir = Direction.from2DDataValue((start + i) % 4);
            // leave the trunk from the side the branch points to
            BlockPos from = corner.offset(dir.getStepX() > 0 ? 1 : 0, h / 2 + r.nextInt(h / 2 - 2), dir.getStepZ() > 0 ? 1 : 0);
            branch(plan, from, dir, 3 + r.nextInt(3), 2 + r.nextInt(2), 2.6);
        }
        plan.blob(corner.offset(1, h, 1), 3.6, 1.3);
        BlockPos[] buds = fromBud ? new BlockPos[] {corner, corner.east(), corner.south(), corner.south().east()} : new BlockPos[0];
        return grow(level, corner, plan, buds);
    }

    public static boolean growScrub(WorldGenLevel level, BlockPos base, EndWood w, RandomSource r, boolean fromBud) {
        Plan plan = new Plan(w);
        column(plan, w, base, 8 + r.nextInt(6));
        int sides = r.nextInt(3);
        int start = r.nextInt(4);
        for (int i = 0; i < sides; i++) {
            BlockPos side = base.relative(Direction.from2DDataValue((start + i) % 4));
            if (level.getBlockState(side.below()).isFaceSturdy(level, side.below(), Direction.UP)) column(plan, w, side, 3 + r.nextInt(5));
        }
        return grow(level, base, plan, fromBud ? new BlockPos[] {base} : new BlockPos[0]);
    }

    /** One stick column, thick at the bottom and a thin tip on top. */
    private static void column(Plan plan, EndWood w, BlockPos base, int height) {
        for (int i = 0; i < height; i++) {
            int fromTop = height - 1 - i;
            plan.logs.put(base.above(i), w.sticks.defaultBlockState().setValue(EndSticksBlock.THICKNESS, Math.min(3, fromTop + 1)));
        }
    }

    public static boolean growNatural(WorldGenLevel level, BlockPos base, EndWood w, RandomSource r) {
        Plan plan = new Plan(w);
        int h = 4 + r.nextInt(4);
        for (int y = 0; y < h; y++) plan.log(base.above(y), Direction.Axis.Y);
        for (int y = 2; y < h; y++) {
            if (r.nextInt(3) != 0) continue;
            Direction dir = Direction.from2DDataValue(r.nextInt(4));
            BlockPos stub = base.above(y).relative(dir);
            plan.log(stub, axis(dir));
            int up = 1 + r.nextInt(3);
            for (int i = 1; i <= up; i++) plan.log(stub.above(i), Direction.Axis.Y);
            plan.blob(stub.above(up + 1), 1.0, 1.0);
        }
        plan.blob(base.above(h), 1.5, 1.0);
        return place(level, plan);
    }

    private static boolean grow(WorldGenLevel level, BlockPos base, Plan plan, BlockPos[] buds) {
        Map<BlockPos, BlockState> old = new HashMap<>();
        for (BlockPos b : buds) {
            old.put(b, level.getBlockState(b));
            level.setBlock(b, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 4);
        }
        if (place(level, plan)) return true;
        old.forEach((p, s) -> level.setBlock(p, s, 4));
        return false;
    }
}
