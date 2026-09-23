package com.simplebuilding.forge;

import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.loot.ModLootFunctions;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Forces class-loading of the shared registry holders during the matching
 * RegisterEvent so their static {@code Registry.register(...)} initialisers run
 * while the vanilla registries are still unfrozen (mirrors the NeoForge path).
 */
public final class ForgeRegistryBootstrap {
    private static boolean blocksInitialized;

    private ForgeRegistryBootstrap() {
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
            // simplebuilding:weighted_enchant und liegen auch im Forge-Jar; ohne die Registrierung
            // scheitert das Laden der Welt am unbekannten Loot-Funktionstyp. Spaeter (commonSetup)
            // waere zu spaet, dann sind die Registries eingefroren.
            ModLootFunctions.registerLootFunctions();
        }
    }
}
