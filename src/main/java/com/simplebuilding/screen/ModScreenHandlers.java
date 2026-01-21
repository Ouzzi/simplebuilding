// ModScreenHandlers.java
package com.simplebuilding.screen;

import com.simplebuilding.Simplebuilding;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<NetheriteHopperScreenHandler> NETHERITE_HOPPER_SCREEN_HANDLER = 
        Registry.register(Registries.SCREEN_HANDLER, Identifier.of(Simplebuilding.MOD_ID, "netherite_hopper"),
            new ScreenHandlerType<>(NetheriteHopperScreenHandler::new, FeatureSet.empty()));

    public static void registerScreenHandlers() {
        Simplebuilding.LOGGER.info("Registering Screen Handlers for " + Simplebuilding.MOD_ID);
    }
}