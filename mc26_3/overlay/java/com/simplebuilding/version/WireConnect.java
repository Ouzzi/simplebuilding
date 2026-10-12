package com.simplebuilding.version;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Whether vanilla redstone dust would connect to {@code state} (26.3: the block's own hook). */
public final class WireConnect {
    private WireConnect() {
    }

    public static boolean vanilla(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return state.shouldRedstoneWireConnectTo(level, pos, direction);
    }
}
