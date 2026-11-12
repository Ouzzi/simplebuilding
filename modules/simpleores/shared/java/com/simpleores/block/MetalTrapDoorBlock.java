package com.simpleores.block;

import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** N28 (b-pal28): trapdoor of a Simple Ores building metal, like the Vanilla copper trapdoor (opens by hand) without ageing. */
public class MetalTrapDoorBlock extends TrapDoorBlock {
    /** The Vanilla constructor takes the sound from the block set (copper); the metal keeps its own (METALS25). */
    private final SoundType sound;

    public MetalTrapDoorBlock(BlockBehaviour.Properties properties, SoundType sound) {
        super(BlockSetType.COPPER, properties);
        this.sound = sound;
    }

    @Override
    protected SoundType getSoundType(BlockState state) {
        return sound;
    }
}
