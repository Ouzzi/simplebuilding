package com.simplebuilding.items.custom;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Map;

public class SpatulaItem extends ChiselItem {
    public SpatulaItem(Settings settings) {
        super(settings);

        // RUFE die Methode der Basisklasse auf, um die Map für DIESE Instanz zu setzen
        this.setTransformationMap(Map.of(
                Blocks.STONE_BRICKS, Blocks.STONE,
                Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS,
                Blocks.COBBLESTONE, Blocks.CHISELED_STONE_BRICKS // Neue Reparatur-Logik
        ));

        this.setChiselSound(SoundEvents.BLOCK_SAND_FALL);
    }
}