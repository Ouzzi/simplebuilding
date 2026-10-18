package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.saw.Sawing;
import com.simpleblocks.saw.ShapingSawMenu;
import com.simpleblocks.saw.client.ShapingSawScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

/** Fabric client: the Shaping Saw screen and the receiver of its recipe list. */
public final class BlocksFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.<ShapingSawMenu, ShapingSawScreen>register(Sawing.MENU, ShapingSawScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(Sawing.Payload.TYPE, (payload, context) -> context.client().execute(() -> Sawing.receive(payload.entries())));
    }
}
