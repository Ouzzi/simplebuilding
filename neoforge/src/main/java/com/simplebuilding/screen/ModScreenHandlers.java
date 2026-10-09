package com.simplebuilding.screen;

import com.simplebuilding.Simplebuilding;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {

    public static MenuType<NetheriteHopperScreenHandler> NETHERITE_HOPPER_SCREEN_HANDLER;
    public static MenuType<BackpackMenu> BACKPACK_MENU;
    public static MenuType<TieredChestMenu> TIERED_CHEST_MENU;
    public static MenuType<com.simplebuilding.fletching.FletchingMenu> FLETCHING_MENU;
    public static MenuType<AutoSmitherMenu> AUTO_SMITHER_MENU;
    public static MenuType<AutonomousCrafterMenu> AUTONOMOUS_CRAFTER_MENU;
    public static MenuType<AstralEnchantingMenu> ASTRAL_ENCHANTING_MENU;
    public static MenuType<StorageCraftingMenu> STORAGE_CRAFTING_TABLE_MENU;

    private ModScreenHandlers() {
    }

    public static void registerScreenHandlers() {
        Simplebuilding.LOGGER.info("Registering Screen Handlers for {}", Simplebuilding.MOD_ID);
    }
}
