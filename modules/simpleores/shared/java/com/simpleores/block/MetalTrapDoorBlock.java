package com.simpleores.block;

import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** N28 (b-pal28): trapdoor of a Simple Ores building metal, like the Vanilla copper trapdoor (opens by hand) without ageing. */
public class MetalTrapDoorBlock extends TrapDoorBlock {
    public MetalTrapDoorBlock(BlockBehaviour.Properties properties) {
        super(BlockSetType.COPPER, properties);
    }
}
