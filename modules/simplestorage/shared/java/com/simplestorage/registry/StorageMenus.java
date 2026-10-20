package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import com.simplestorage.chest.ChestTier;
import com.simplestorage.chest.TieredChestMenu;
import com.simplestorage.chest.TieredChestOpenData;
import com.simplestorage.table.StorageCraftingMenu;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/**
 * Menu types. The tiered chest menu has one type per tier and shape ({@code <tier>_chest}, {@code <tier>_double_chest},
 * {@code <tier>_shulker_box}), like Vanilla's {@code generic_9x*}: the type tells the client the size, so no loader
 * needs extra opening data. Menu types are never saved (no id moves).
 */
public final class StorageMenus {
    public static MenuType<StorageCraftingMenu> STORAGE_CRAFTING_TABLE;
    private static final Map<TieredChestOpenData, MenuType<TieredChestMenu>> TIERED = new LinkedHashMap<>();

    public static void register() {
        STORAGE_CRAFTING_TABLE = Registry.register(BuiltInRegistries.MENU, SimpleStorage.id(SimpleStorage.STORAGE_CRAFTING_TABLE),
                new MenuType<>(StorageCraftingMenu::new, FeatureFlags.VANILLA_SET));
        for (ChestTier tier : ChestTier.values()) {
            tiered(tier.textureName() + "_chest", TieredChestOpenData.of(tier, false));
            tiered(tier.textureName() + "_double_chest", TieredChestOpenData.of(tier, true));
            tiered(tier.textureName() + "_shulker_box", TieredChestOpenData.shulker(tier));
        }
    }

    private static void tiered(String path, TieredChestOpenData data) {
        TIERED.put(data, Registry.register(BuiltInRegistries.MENU, SimpleStorage.id(path),
                new MenuType<>((id, inventory) -> new TieredChestMenu(id, inventory, data), FeatureFlags.VANILLA_SET)));
    }

    /** The menu type of a tiered chest or shulker box menu. */
    public static MenuType<TieredChestMenu> type(TieredChestOpenData data) {
        MenuType<TieredChestMenu> type = TIERED.get(data);
        if (type == null) throw new IllegalStateException("no tiered chest menu type for " + data);
        return type;
    }

    /** Every tiered menu type (the loaders register one screen factory for each). */
    public static Iterable<MenuType<TieredChestMenu>> tieredTypes() {
        return TIERED.values();
    }

    private StorageMenus() {}
}
