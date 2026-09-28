package com.simplebuilding.version;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;

/**
 * A loot pool entry that rolls another loot table, 26.3 side of the version shim: 26.3's
 * {@code NestedLootTable} holds {@code Holder<LootTable>}s, so the table comes out of the loot table
 * getter the loader hands its loot table event (a reference that is bound when the reload finishes).
 */
public final class LootRefs {

    private LootRefs() {
    }

    public static UniformContainerBase.Builder<?> tableReference(ResourceKey<LootTable> table, HolderGetter.Provider registry) {
        return NestedLootTable.lootTableReference(registry.lookupOrThrow(Registries.LOOT_TABLE).getOrThrow(table));
    }
}
