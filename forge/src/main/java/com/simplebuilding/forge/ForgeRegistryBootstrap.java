package com.simplebuilding.forge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.datafix.LegacyItemIds;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.loot.ModLootFunctions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraftforge.registries.ForgeRegistry;
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
            }
            return;
        }
        // Forge entsperrt je RegisterEvent nur die eine Registry - Entitaeten gehoeren in ihr eigenes Ereignis.
        if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
            ModEntities.registerModEntities();
            return;
        }
        if (event.getRegistryKey().equals(Registries.SOUND_EVENT)) {
            com.simplebuilding.util.ModSounds.registerSounds();
            return;
        }
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            ModItems.registerModItems();
            registerLegacyItemAliases(event);
            return;
        }
        if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
            ModDataComponentTypes.registerDataComponentTypes();
            return;
        }
        if (event.getRegistryKey().equals(Registries.LOOT_CONDITION_TYPE)) {
            // simplebuilding:core_chance in the loot injection tables (data/simplebuilding/loot_table/inject/).
            com.simplebuilding.loot.ModLootConditions.register();
        }
        if (event.getRegistryKey().equals(Registries.LOOT_FUNCTION_TYPE)) {
            // Die Trade-JSONs unter data/simplebuilding/villager_trade/ referenzieren
            // simplebuilding:weighted_enchant und liegen auch im Forge-Jar; ohne die Registrierung
            // scheitert das Laden der Welt am unbekannten Loot-Funktionstyp. Spaeter (commonSetup)
            // waere zu spaet, dann sind die Registries eingefroren.
            ModLootFunctions.registerLootFunctions();
        }
    }

    /**
     * Old item ids of renamed items ({@link LegacyItemIds}) as Forge registry aliases. Forge's item
     * registry is a {@code NamespacedDefaultedWrapper} whose {@code getValue}/{@code containsKey} go
     * straight to the {@link ForgeRegistry} (air for an unknown id), so the lookup never reaches
     * {@code NamespacedWrapperAliasMixin}; the ForgeRegistry itself resolves aliases there, and a
     * world whose Forge id snapshot still lists an old id finds it instead of reporting it missing.
     * The holder lookups ({@code get(Identifier/ResourceKey)}) ignore Forge aliases - those stay with
     * the mixin. Aliases can only be added while the registry is open, i.e. in this event.
     */
    private static void registerLegacyItemAliases(RegisterEvent event) {
        if (!(event.getForgeRegistry() instanceof ForgeRegistry<?> items)) {
            return;
        }
        LegacyItemIds.RENAMED.forEach((oldPath, newPath) -> items.addAlias(
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, oldPath),
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, newPath)));
    }
}
