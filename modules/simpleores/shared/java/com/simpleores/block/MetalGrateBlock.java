package com.simpleores.block;

import net.minecraft.world.level.block.WaterloggedTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** N25 (b-metals25): grate of a Simple Ores metal, like the Vanilla copper grate (see-through, waterloggable) without ageing. */
public class MetalGrateBlock extends WaterloggedTransparentBlock {
    public MetalGrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
