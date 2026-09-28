package com.simplebuilding.version;

import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;

/**
 * A loot pool entry that rolls another loot table, 26.2 side of the version shim (see
 * {@link McVersion}): 26.2 names the table by its key, 26.3 wants a {@code Holder<LootTable>}.
 */
public final class LootRefs {

    private LootRefs() {
    }

    public static LootPoolEntryContainer.Builder<?> tableReference(ResourceKey<LootTable> table, HolderGetter.Provider registry) {
        return NestedLootTable.lootTableReference(table);
    }
}
