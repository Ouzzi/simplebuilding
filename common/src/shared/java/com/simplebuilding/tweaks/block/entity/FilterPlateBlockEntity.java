package com.simplebuilding.tweaks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Traegt nur den Besitzer der Netherit-/Enderit-Druckplatte (Simple Tweaks: NetheritePressurePlateBlockEntity). */
public class FilterPlateBlockEntity extends OwnedBlockEntity {
    public FilterPlateBlockEntity(BlockPos pos, BlockState state) {
        super(TweaksBlockEntities.FILTER_PLATE, pos, state);
    }
}
