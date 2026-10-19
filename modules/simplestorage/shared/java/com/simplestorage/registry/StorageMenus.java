package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import com.simplestorage.table.StorageCraftingMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class StorageMenus {
    public static MenuType<StorageCraftingMenu> STORAGE_CRAFTING_TABLE;

    public static void register() {
        STORAGE_CRAFTING_TABLE = Registry.register(BuiltInRegistries.MENU, SimpleStorage.id(SimpleStorage.STORAGE_CRAFTING_TABLE),
                new MenuType<>(StorageCraftingMenu::new, FeatureFlags.VANILLA_SET));
    }

    private StorageMenus() {}
}
