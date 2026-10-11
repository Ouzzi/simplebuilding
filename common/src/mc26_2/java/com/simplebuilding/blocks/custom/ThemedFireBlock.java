package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 26.2 stub of the 26.3 sulfur/copper fires (queue N33; McVersion.FIRES is false here): nothing is registered. */
public final class ThemedFireBlock {
    public static final float DAMAGE = 1.5F;
    public static final int LIGHT = 12;
    public static Block SULFUR_FIRE;
    public static Block COPPER_FIRE;

    private ThemedFireBlock() {
    }

    public static void register() {
    }

    public static BlockState fireFor(BlockGetter level, BlockPos pos) {
        return null;
    }
}
