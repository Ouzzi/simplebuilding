package com.simplesandwiches.block;

import com.simplesandwiches.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A butter slice laid flat (1 px like a Vanilla carpet, butter friction): walking on it makes the walker Slippery. */
public class ButterCarpetBlock extends CarpetBlock {
    public ButterCarpetBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        ModEffects.slip(level, entity);
    }
}
