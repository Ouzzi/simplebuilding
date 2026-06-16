package com.simplebuilding.forge;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
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
            }
            return;
        }
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            ModItems.registerModItems();
            return;
        }
        if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
            ModDataComponentTypes.registerDataComponentTypes();
        }
    }
}
