package com.simplebuilding.version;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Whether vanilla redstone dust would connect to {@code state} (26.2: RedStoneWireBlock#shouldConnectTo rebuilt). */
public final class WireConnect {
    private WireConnect() {
    }

    public static boolean vanilla(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        if (state.is(Blocks.REDSTONE_WIRE)) return true;
        if (state.is(Blocks.REPEATER)) {
            Direction facing = state.getValue(RepeaterBlock.FACING);
            return facing == direction || facing.getOpposite() == direction;
        }
        if (state.is(Blocks.OBSERVER)) return direction == state.getValue(ObserverBlock.FACING);
        return state.isSignalSource() && direction != null;
    }
}
