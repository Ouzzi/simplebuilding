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
    }
}