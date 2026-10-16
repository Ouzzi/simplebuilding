package com.simpleblocks;

import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/** Sculk Deepslate in Ancient City chests: one extra pool, one roll of 2-6 blocks with a chance of about 1 in 13 per chest. */
public final class BlocksLoot {
    public static final String ANCIENT_CITY = "minecraft:chests/ancient_city";
    public static final int WEIGHT = 8;
    public static final int EMPTY_WEIGHT = 92;

    private BlocksLoot() {}

    public static void apply(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add) {
        if (!ANCIENT_CITY.equals(key.identifier().toString())) return;
        add.accept(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(BlocksRegistry.SCULK_DEEPSLATE).setWeight(WEIGHT)
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 6))))
                .add(EmptyLootItem.emptyItem().setWeight(EMPTY_WEIGHT)));
    }
}
