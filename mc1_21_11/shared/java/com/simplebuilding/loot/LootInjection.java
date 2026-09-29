package com.simplebuilding.loot;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;

/**
 * How the mod's loot reaches vanilla tables: not as pools written into them, but as one pool that
 * rolls a loot table of the mod, {@code simplebuilding:inject/<path of the vanilla table>}
 * (for example {@code data/simplebuilding/loot_table/inject/chests/end_city_treasure.json}). A
 * datapack overrides or empties that file like any other loot table and changes the mod's loot
 * without touching the vanilla table.
 *
 * <p>The shipped inject tables are generated from {@link ModLootTableModifications#apply}, which
 * stays the definition of the defaults (with the chances of {@code docs/LOOT-BALANCE.md}); a game
 * test compares the loaded tables with it. The reference pool is added only where that method adds
 * pools - so the config switch {@code worldGen.enableLootTableChanges} still removes the mod's loot
 * from every chest, and the charged creeper heads stay. The building core pools scale with the
 * config factor through {@link CoreChanceCondition}.
 */
public final class LootInjection {

    public static final String PREFIX = "inject/";

    private LootInjection() {
    }

    /** The mod's inject table for {@code table}. */
    public static ResourceKey<LootTable> injectKey(ResourceKey<LootTable> table) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath("simplebuilding", PREFIX + table.identifier().getPath()));
    }

    /** The pools {@link ModLootTableModifications#apply} gives {@code table} (empty if none, or switched off). */
    public static List<LootPool> defaultPools(ResourceKey<LootTable> table, HolderLookup.Provider registry) {
        List<LootPool> pools = new ArrayList<>();
        if (!"minecraft".equals(table.identifier().getNamespace())) {
            return pools;
        }
        ModLootTableModifications.apply(table, new ModLootTableModifications.Editor() {
            @Override
            public void addPool(LootPool.Builder pool) {
                pools.add(pool.build());
            }

            @Override
            public void addBuiltPool(LootPool pool) {
                pools.add(pool);
            }
        }, registry);
        return pools;
    }

    /**
     * The loader's loot table load hook: adds the reference pool to {@code table} when the mod has
     * loot for it.
     */
    public static void apply(ResourceKey<LootTable> table, ModLootTableModifications.Editor editor, HolderLookup.Provider registry) {
        if (defaultPools(table, registry).isEmpty()) {
            return;
        }
        editor.addPool(LootPool.lootPool()
                .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                .add(NestedLootTable.lootTableReference(injectKey(table))));
    }
}
