package com.simplebuilding.datagen;

import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/** 26.3 side of the advancement datagen differences (26.2 twin in src/mc26_2/java): placedBlock takes the block lookup. */
final class AdvancementCompat {
    private AdvancementCompat() {
    }

    /** Vanilla's "placed this block" criterion. */
    static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(Block block) {
        return ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(BuiltInRegistries.BLOCK, block);
    }
}
