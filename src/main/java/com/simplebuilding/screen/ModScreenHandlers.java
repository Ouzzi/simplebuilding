package com.simplebuilding.screen;

import com.simplebuilding.Simplebuilding;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType; // Wichtig
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos; // Wichtig

public class ModScreenHandlers {

    // ÄNDERUNG 3: Registrierung mit Payload (BlockPos)
    public static final ScreenHandlerType<NetheriteHopperScreenHandler> NETHERITE_HOPPER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(Simplebuilding.MOD_ID, "netherite_hopper"),
                    new ExtendedScreenHandlerType<>(
                            // Factory Lambda: (syncId, inventory, data) -> new Handler(...)
                            NetheriteHopperScreenHandler::new,
                            // Der Codec für die Daten (BlockPos)
                            BlockPos.PACKET_CODEC
                    ));

    public static void registerScreenHandlers() {
        Simplebuilding.LOGGER.info("Registering Screen Handlers for " + Simplebuilding.MOD_ID);
    }
}