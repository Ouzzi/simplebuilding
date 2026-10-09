package com.simplebuilding.screen;

import com.simplebuilding.Simplebuilding;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {

    // Wir machen die Variable public, aber weisen sie erst in der Methode zu
    public static MenuType<NetheriteHopperScreenHandler> NETHERITE_HOPPER_SCREEN_HANDLER;
    public static MenuType<BackpackMenu> BACKPACK_MENU;
    public static MenuType<TieredChestMenu> TIERED_CHEST_MENU;
    public static MenuType<com.simplebuilding.fletching.FletchingMenu> FLETCHING_MENU;
    public static MenuType<AutoSmitherMenu> AUTO_SMITHER_MENU;
    public static MenuType<AutonomousCrafterMenu> AUTONOMOUS_CRAFTER_MENU;
    public static MenuType<StorageCraftingMenu> STORAGE_CRAFTING_TABLE_MENU;

    public static void registerScreenHandlers() {
        Simplebuilding.LOGGER.info("Registering Screen Handlers for " + Simplebuilding.MOD_ID);

        NETHERITE_HOPPER_SCREEN_HANDLER = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "netherite_hopper"),
                new ExtendedMenuType<>(
                        NetheriteHopperScreenHandler::new,
                        BlockPos.STREAM_CODEC // Dies registriert automatisch das Netzwerk-Handling für die BlockPos
                )
        );
        BACKPACK_MENU = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "backpack"),
                new ExtendedMenuType<>(BackpackMenu::new, BackpackOpenData.STREAM_CODEC)
        );
        TIERED_CHEST_MENU = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "tiered_chest"),
                new ExtendedMenuType<>(TieredChestMenu::new, TieredChestOpenData.STREAM_CODEC)
        );
        if (com.simplebuilding.version.McVersion.AUTO_SMITHER) {
            AUTO_SMITHER_MENU = Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "auto_smither"),
                    new MenuType<>(AutoSmitherMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET)
            );
        }
        if (com.simplebuilding.version.McVersion.AUTONOMOUS_CRAFTER) {
            AUTONOMOUS_CRAFTER_MENU = Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "autonomous_crafter"),
                    new MenuType<>(AutonomousCrafterMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET)
            );
        }
        if (com.simplebuilding.version.McVersion.STORAGE_CRAFTING_TABLE) {
            STORAGE_CRAFTING_TABLE_MENU = Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "storage_crafting_table"),
                    new MenuType<>(StorageCraftingMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET)
            );
        }
        if (com.simplebuilding.version.McVersion.FLETCHING) {
            FLETCHING_MENU = Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "fletching"),
                    new MenuType<>(com.simplebuilding.fletching.FletchingMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET)
            );
        }
    }
}