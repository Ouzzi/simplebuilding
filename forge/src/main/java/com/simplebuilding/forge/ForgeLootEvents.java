package com.simplebuilding.forge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.loot.ModLootTableModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeLootEvents {
    private ForgeLootEvents() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        Identifier name = event.getName();
        if (name == null) {
            return;
        }
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, name);
        LootTable table = event.getTable();
        // Forge's LootTableLoadEvent exposes no HolderLookup.Provider; the shared
        // modifications only use it for registry-aware pools (none here). TODO: verify at runtime.
        ModLootTableModifications.apply(key, new ModLootTableModifications.Editor() {
            @Override
            public void addPool(LootPool.Builder pool) {
                table.addPool(pool.build());
            }

            @Override
            public void addBuiltPool(LootPool pool) {
                table.addPool(pool);
            }
        }, null);
    }
}
