package com.simplebuilding.neoforge;

import com.simplebuilding.loot.ModLootTableModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

@EventBusSubscriber(modid = com.simplebuilding.Simplebuilding.MOD_ID)
public final class NeoForgeLootEvents {
    private NeoForgeLootEvents() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        Identifier name = event.getName();
        if (name == null) {
            return;
        }

        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, name);
        LootTable table = event.getTable();
        ModLootTableModifications.apply(key, new ModLootTableModifications.Editor() {
            @Override
            public void addPool(LootPool.Builder pool) {
                table.addPool(pool.build());
            }

            @Override
            public void addBuiltPool(LootPool pool) {
                table.addPool(pool);
            }
        }, event.getRegistries());
    }
}
