package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * BACK20 round 11 (plan docs/ai/PLAN-B-BACK20-2026-11-10.md): loot ties to the rest of the modpack, each only while the other mod's
 * item is registered (module independence - without it nothing changes): the Pipes' dead-end chests now and then hold Simple
 * Gadgets' Echo Sounder (a jump back to a linked lodestone - set one down at the labyrinth's entrance), the Warehouse racks Simple Lights' white Light Panes (the Backrooms'
 * neon to take home). And one Vanilla tie: abandoned mineshaft chests sometimes hold chalk (miners mark their ways) - a small
 * extra pool, nothing taken away (A12).
 */
public final class BackroomsLoot {
    public static final String PIPES = "simplelayers:chests/backrooms_pipes", WAREHOUSE = "simplelayers:chests/backrooms_warehouse",
            MINESHAFT = "minecraft:chests/abandoned_mineshaft";
    public static final Identifier ECHO_SOUNDER = Identifier.fromNamespaceAndPath("simplegadgets", "echo_sounder"),
            LIGHT_PANE = Identifier.fromNamespaceAndPath("simplelights", "white_light_pane");

    private BackroomsLoot() {}

    public static void apply(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add) {
        switch (key.identifier().toString()) {
            case PIPES -> foreign(ECHO_SOUNDER, 1, 1, 1, 19, add);
            case WAREHOUSE -> foreign(LIGHT_PANE, 2, 4, 3, 17, add);
            case MINESHAFT -> add.accept(pool(LayersRegistry.CHALK, 1, 3, 10, 90));
            default -> {
            }
        }
    }

    static void foreign(Identifier id, int min, int max, int weight, int empty, Consumer<LootPool.Builder> add) {
        BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> add.accept(pool(item, min, max, weight, empty)));
    }

    static LootPool.Builder pool(Item item, int min, int max, int weight, int empty) {
        return LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(item).setWeight(weight).apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max))))
                .add(EmptyLootItem.emptyItem().setWeight(empty));
    }
}
