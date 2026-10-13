package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 26.3: break particles of the mined eighth (mixed cells would otherwise show the neutral ones of the cell). */
public abstract class OctetParticleBlock extends Block {
    protected OctetParticleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void spawnDestroyParticles(Level level, BlockPos pos, BlockState state) {
        super.spawnDestroyParticles(level, pos, OctetCellBlock.particleState(level, pos, state));
    }
}
