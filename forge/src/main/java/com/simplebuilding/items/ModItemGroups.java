package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import net.minecraft.world.item.CreativeModeTab;

public final class ModItemGroups {
    public static CreativeModeTab BUILDING_ITEMS_GROUP;

    private ModItemGroups() {
    }

    public static void registerItemGroups() {
        Simplebuilding.LOGGER.info("Registering Item Groups for {}", Simplebuilding.MOD_ID);
    }
}
