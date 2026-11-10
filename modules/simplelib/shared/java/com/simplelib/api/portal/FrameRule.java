package com.simplelib.api.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The frame a portal door needs (BACK22 core §5.2, owner: "Rahmen links, rechts, oben"): a full-collision block left and right of
 * the opening at both heights and a lintel over its full width. A single leaf of a double door whose partner is gone is a door of
 * width 1 with a door (not a block) beside it, so it counts as unframed.
 */
public final class FrameRule {
    private FrameRule() {}

    public static boolean framed(BlockGetter level, PortalShape shape) {
        Direction right = shape.facing().getClockWise();
        BlockPos a = shape.anchor();
        for (int i = 0; i < shape.width(); i++) {
            BlockPos lower = a.relative(right, i);
            BlockState l = level.getBlockState(lower), u = level.getBlockState(lower.above());
            if (!(l.getBlock() instanceof PortalDoorBlock) || l.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER
                    || !(u.getBlock() instanceof PortalDoorBlock) || u.getValue(DoorBlock.HALF) != DoubleBlockHalf.UPPER) {
                return false;
            }
            if (!full(level, lower.above(2))) return false;
        }
        BlockPos leftSide = a.relative(right.getOpposite()), rightSide = a.relative(right, shape.width());
        return full(level, leftSide) && full(level, leftSide.above()) && full(level, rightSide) && full(level, rightSide.above());
    }

    static boolean full(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }
}
