package dev.simpledimension.common.portal;

/**
 * Maps a portal position in one dimension to the matching position in the linked
 * dimension, using a configurable distance ratio.
 *
 * <p>{@code sourcePerTarget} is "how many source-dimension blocks equal one
 * target-dimension block", exactly like vanilla's nether ratio (8.0). Travelling
 * <em>into</em> the target compresses the horizontal coordinates by that ratio;
 * travelling back to the source expands them again. Y is always preserved.
 */
public final class PortalTravelRules {

    private PortalTravelRules() {
    }

    /** Source -> target: compress horizontal coordinates by the ratio. */
    public static BlockPos3i toTarget(BlockPos3i source, double sourcePerTarget) {
        return scaleHorizontally(source, 1.0 / sanitize(sourcePerTarget));
    }

    /** Target -> source: expand horizontal coordinates by the ratio. */
    public static BlockPos3i toSource(BlockPos3i target, double sourcePerTarget) {
        return scaleHorizontally(target, sanitize(sourcePerTarget));
    }

    private static BlockPos3i scaleHorizontally(BlockPos3i pos, double factor) {
        if (factor == 1.0) {
            return pos;
        }
        int x = (int) Math.floor(pos.x() * factor);
        int z = (int) Math.floor(pos.z() * factor);
        return new BlockPos3i(x, pos.y(), z);
    }

    private static double sanitize(double sourcePerTarget) {
        return sourcePerTarget <= 0 ? 1.0 : sourcePerTarget;
    }
}
