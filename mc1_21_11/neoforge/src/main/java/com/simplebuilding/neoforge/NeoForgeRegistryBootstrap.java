package com.simplebuilding.neoforge;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.loot.ModLootFunctions;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class NeoForgeRegistryBootstrap {
    private static boolean blocksInitialized;

    private NeoForgeRegistryBootstrap() {
    }

    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            if (!blocksInitialized) {
                blocksInitialized = true;
                ModBlocks.registerModBlocks();
                ModEntities.registerModEntities();
            }
            return;
        }
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            ModItems.registerModItems();
            return;
        }
        if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
            ModDataComponentTypes.registerDataComponentTypes();
            return;
        }
        if (event.getRegistryKey().equals(Registries.LOOT_FUNCTION_TYPE)) {
            // Die Trade-JSONs unter data/simplebuilding/villager_trade/ referenzieren
            // simplebuilding:weighted_enchant und werden auch im NeoForge-Jar ausgeliefert.
            ModLootFunctions.registerLootFunctions();
        }
    }
}
