package com.simplebuilding.platform;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface HopperSync {
    void broadcastGhostItem(ModHopperBlockEntity blockEntity, int slot, ItemStack stack);

    HopperSync NOOP = (blockEntity, slot, stack) -> {};
}
