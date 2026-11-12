package com.simpleores.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * N35 (a-pal35a, owner 12.11.): weighted pressure plate of a Simple Ores metal. Precious metals count like the gold plate (light, up to 15),
 * the others like the iron plate (heavy, up to 150); the Vanilla constructor is protected.
 */
public class MetalPressurePlateBlock extends WeightedPressurePlateBlock {
    public static final int LIGHT = 15;
    public static final int HEAVY = 150;
    /** The Vanilla constructor takes the sound from the block set; a building metal keeps its own (METALS25), null = the set's. */
    private final SoundType sound;
    /** Light like the gold plate (true) or heavy like the iron plate. */
    public final boolean light;

    public MetalPressurePlateBlock(boolean light, BlockBehaviour.Properties properties, SoundType sound) {
        super(light ? LIGHT : HEAVY, light ? BlockSetType.GOLD : BlockSetType.IRON, properties);
        this.sound = sound;
        this.light = light;
    }

    /** The entity count at which the signal reaches 15 (15 light, 150 heavy). */
    public int maxWeight() {
        return light ? LIGHT : HEAVY;
    }

    @Override
    protected SoundType getSoundType(BlockState state) {
        return sound != null ? sound : super.getSoundType(state);
    }
}
