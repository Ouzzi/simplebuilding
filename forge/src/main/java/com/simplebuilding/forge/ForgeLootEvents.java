package com.simplebuilding.forge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.loot.ModLootTableModifications;
import net.minecraft.core.HolderLookup;
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
    private static volatile HolderLookup.Provider loadingRegistries;

    private ForgeLootEvents() {
    }

    /** Von ReloadableServerRegistriesMixin gesetzt, bevor die Loot-Tabellen geladen werden. */
    public static void setLoadingRegistries(HolderLookup.Provider registries) {
        loadingRegistries = registries;
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        Identifier name = event.getName();
        if (name == null) {
            return;
        }
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, name);
        LootTable table = event.getTable();
        // Forges LootTableLoadEvent liefert keinen HolderLookup.Provider - ihn hinterlegt der Mixin am
        // Registry-Reload (ReloadableServerRegistriesMixin). Ohne ihn keine Aenderung statt eines Absturzes.
        HolderLookup.Provider registries = loadingRegistries;
        if (registries == null) {
            return;
        }
        ModLootTableModifications.apply(key, new ModLootTableModifications.Editor() {
            @Override
            public void addPool(LootPool.Builder pool) {
                table.addPool(pool.build());
            }

            @Override
            public void addBuiltPool(LootPool pool) {
                table.addPool(pool);
            }
        }, registries);
    }
}
