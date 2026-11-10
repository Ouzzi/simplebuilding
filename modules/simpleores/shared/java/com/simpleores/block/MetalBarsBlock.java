package com.simpleores.block;

import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** N25 (b-metals25): bars of a Simple Ores metal, the Vanilla iron bars with the metal's own pattern (the constructor is protected there). */
public class MetalBarsBlock extends IronBarsBlock {
    public MetalBarsBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
