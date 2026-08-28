package com.simplebuilding.screen;

import com.simplebuilding.Simplebuilding;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {

    public static MenuType<NetheriteHopperScreenHandler> NETHERITE_HOPPER_SCREEN_HANDLER;

    private ModScreenHandlers() {
    }

    public static void registerScreenHandlers() {
        Simplebuilding.LOGGER.info("Registering Screen Handlers for {}", Simplebuilding.MOD_ID);
    }
}
