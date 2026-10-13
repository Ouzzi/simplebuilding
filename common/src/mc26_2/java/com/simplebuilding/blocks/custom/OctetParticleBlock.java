package com.simplebuilding.blocks.custom;

import net.minecraft.world.level.block.Block;

/** 26.2 twin: Block#spawnDestroyParticles has another signature here, so mixed cells keep the neutral cell particles. */
public abstract class OctetParticleBlock extends Block {
    protected OctetParticleBlock(Properties properties) {
        super(properties);
    }
}
