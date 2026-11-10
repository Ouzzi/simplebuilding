package com.simplelib.api.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Where a portal door leads: the other end's door (anchor = lower half of its left leaf as seen when walking in, its facing = the
 * walking direction into it, width 1 or 2), the link id both ends share, the rule and the colour of the transition pulse (RGB).
 */
public record PortalTarget(ResourceKey<Level> dimension, BlockPos anchor, Direction facing, int width, long linkId, PortalRule rule, int color) {
    /** Lower half of leaf {@code leaf} (0 = left). */
    public BlockPos leaf(int leaf) {
        return anchor.relative(facing.getClockWise(), Math.max(0, Math.min(leaf, width - 1)));
    }

    /** The standing spot in front of leaf {@code leaf} where a traveller arrives. */
    public BlockPos front(int leaf) {
        return leaf(leaf).relative(facing.getOpposite());
    }
}
