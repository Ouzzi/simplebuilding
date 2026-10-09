package com.simplemaps;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Rare finds (owner F3): plain map 5 % in cartographer and shipwreck map chests, Nether map 2 % in bastions and
 * fortresses, End map 2 % in end city treasure. One extra pool per table: one roll, item weight vs. empty weight.
 */
public final class MapsLoot {
    /** Table → {map kind (0 plain, 1 Nether, 2 End), item weight, empty weight}. */
    public static final Map<String, int[]> TABLES = Map.of(
            "minecraft:chests/village/village_cartographer", new int[] {0, 1, 19},
            "minecraft:chests/shipwreck_map", new int[] {0, 1, 19},
            "minecraft:chests/bastion_other", new int[] {1, 1, 49},
            "minecraft:chests/nether_bridge", new int[] {1, 1, 49},
            "minecraft:chests/end_city_treasure", new int[] {2, 1, 49});

    private MapsLoot() {}

    public static void apply(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add, HolderGetter.Provider registries) {
        int[] entry = TABLES.get(key.identifier().toString());
        if (entry == null) return;
        Item item = switch (entry[0]) {
            case 1 -> MapsItems.NETHER_WAYFINDER_MAP;
            case 2 -> MapsItems.END_WAYFINDER_MAP;
            default -> MapsItems.WAYFINDER_MAP;
        };
        if (item == null) return;
        add.accept(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(item).setWeight(entry[1]))
                .add(EmptyLootItem.emptyItem().setWeight(entry[2])));
    }
}
