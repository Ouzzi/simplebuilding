package com.simplebuilding.datagen;

import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.world.level.block.Block;

/** 26.2 side of the advancement datagen differences (26.3 twin in mc26_3/fabric/src/main/java). */
final class AdvancementCompat {
    private AdvancementCompat() {
    }

    /** Vanilla's "placed this block" criterion. */
    static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(Block block) {
        return ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block);
    }
}
