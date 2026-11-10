package com.simplelib.api.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/**
 * The opening a portal door forms (BACK22 core §5.2): anchor = lower half of the left leaf as seen when walking in ({@code facing}
 * = walking direction = the door's FACING), width 1 or 2 (double door: two portal doors side by side, same facing, mirrored hinges),
 * and which leaf the asked position belongs to (0 = left).
 */
public record PortalShape(BlockPos anchor, Direction facing, int width, int leaf) {
    /** The shape of the portal door at {@code pos} (either half), or null when there is none. */
    public static @Nullable PortalShape of(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PortalDoorBlock)) return null;
        BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos.immutable();
        state = level.getBlockState(lower);
        if (!(state.getBlock() instanceof PortalDoorBlock)) return null;
        Direction facing = state.getValue(DoorBlock.FACING);
        Direction left = facing.getCounterClockWise();
        if (partner(level, lower.relative(left), state)) return new PortalShape(lower.relative(left), facing, 2, 1);
        if (partner(level, lower.relative(left.getOpposite()), state)) return new PortalShape(lower, facing, 2, 0);
        return new PortalShape(lower, facing, 1, 0);
    }

    private static boolean partner(BlockGetter level, BlockPos pos, BlockState door) {
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof PortalDoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                && s.getValue(DoorBlock.FACING) == door.getValue(DoorBlock.FACING) && s.getValue(DoorBlock.HINGE) != door.getValue(DoorBlock.HINGE);
    }

    /** Lower half of leaf {@code i}. */
    public BlockPos leafPos(int i) {
        return anchor.relative(facing.getClockWise(), i);
    }
}
