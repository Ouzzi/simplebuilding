package com.simplebuilding.screen;

import com.simplebuilding.Simplebuilding;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class ModScreenHandlers {

    // Wir machen die Variable public, aber weisen sie erst in der Methode zu
    public static ScreenHandlerType<NetheriteHopperScreenHandler> NETHERITE_HOPPER_SCREEN_HANDLER;

    public static void registerScreenHandlers() {
        Simplebuilding.LOGGER.info("Registering Screen Handlers for " + Simplebuilding.MOD_ID);

        NETHERITE_HOPPER_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(Simplebuilding.MOD_ID, "netherite_hopper"),
                new ExtendedScreenHandlerType<>(
                        NetheriteHopperScreenHandler::new,
                        BlockPos.PACKET_CODEC // Dies registriert automatisch das Netzwerk-Handling für die BlockPos
                )
        );
    }
}