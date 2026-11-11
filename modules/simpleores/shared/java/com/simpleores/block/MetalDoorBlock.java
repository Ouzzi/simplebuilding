package com.simpleores.block;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** N28 (b-pal28): door of a Simple Ores building metal, like the Vanilla copper door (opens by hand) without ageing. */
public class MetalDoorBlock extends DoorBlock {
    public MetalDoorBlock(BlockBehaviour.Properties properties) {
        super(BlockSetType.COPPER, properties);
    }
}
