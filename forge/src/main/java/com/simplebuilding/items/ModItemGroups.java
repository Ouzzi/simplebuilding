package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import net.minecraft.world.item.CreativeModeTab;

import java.util.EnumMap;
import java.util.Map;

/**
 * Die vier Kreativ-Tabs der Mod. Registriert werden sie ueber das DeferredRegister des Loaders;
 * sobald es gefeuert hat, traegt {@code assignStaticFields} sie hier ein.
 */
public final class ModItemGroups {
    public static final Map<ModItemGroupsContent.Tab, CreativeModeTab> GROUPS = new EnumMap<>(ModItemGroupsContent.Tab.class);

    private ModItemGroups() {
    }

    public static CreativeModeTab get(ModItemGroupsContent.Tab tab) {
        return GROUPS.get(tab);
    }

    public static void registerItemGroups() {
        Simplebuilding.LOGGER.info("Registering Item Groups for {}", Simplebuilding.MOD_ID);
    }
}
