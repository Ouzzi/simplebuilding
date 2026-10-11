package com.simplebuilding.modules.simplemobs;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The Howler construction (owner 11.10.): a Sculk block at the bottom, on it a T of three Sculk Catalysts (the middle
 * one is the body center), on the middle one a jaw ({@code #simplemobs:howler_jaw}: Sculk Shrieker always, Simple
 * Building's Sculk Jaw when present), on the jaw a skeleton skull. The arms may run along X or Z. The sculk sensor that
 * has to hear the horn is not part of the figure; it may stand anywhere within {@link HowlerLogic#SENSOR_RANGE}.
 */
public final class HowlerPattern {
    public static final TagKey<Block> JAW = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MobsRegistry.ID, "howler_jaw"));
    public static final TagKey<Block> REPLACEABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MobsRegistry.ID, "howler_replaceable"));

    /** A complete figure around {@code center} (the middle catalyst) with its arms along {@code axis}. */
    public record Match(BlockPos center, Direction.Axis axis) {
        public BlockPos foot() { return center.below(); }
        public BlockPos jaw() { return center.above(); }
        public BlockPos head() { return center.above(2); }
        public BlockPos arm(boolean positive) { return center.relative(Direction.fromAxisAndDirection(axis, positive ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE)); }
        /** Every block the manifestation consumes. */
        public List<BlockPos> parts() { return List.of(foot(), center, arm(false), arm(true), jaw(), head()); }
    }

    private HowlerPattern() {}

    public static Optional<Match> at(BlockGetter level, BlockPos center) {
        if (!level.getBlockState(center).is(Blocks.SCULK_CATALYST)) return Optional.empty();
        if (!level.getBlockState(center.below()).is(Blocks.SCULK)) return Optional.empty();
        if (!level.getBlockState(center.above()).is(JAW)) return Optional.empty();
        if (!level.getBlockState(center.above(2)).is(Blocks.SKELETON_SKULL)) return Optional.empty();
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            Match m = new Match(center.immutable(), axis);
            if (level.getBlockState(m.arm(false)).is(Blocks.SCULK_CATALYST) && level.getBlockState(m.arm(true)).is(Blocks.SCULK_CATALYST)) return Optional.of(m);
        }
        return Optional.empty();
    }

    /** The figure nearest to a sensor whose body center lies within the sensor range. */
    public static Optional<Match> nearSensor(BlockGetter level, BlockPos sensor) {
        int r = HowlerLogic.SENSOR_RANGE;
        Match best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) {
            p.setWithOffset(sensor, dx, dy, dz);
            if (!level.getBlockState(p).is(Blocks.SCULK_CATALYST)) continue;
            Optional<Match> m = at(level, p);
            double d = p.distSqr(sensor);
            if (m.isPresent() && d < bestDist) { best = m.get(); bestDist = d; }
        }
        return Optional.ofNullable(best);
    }
}
