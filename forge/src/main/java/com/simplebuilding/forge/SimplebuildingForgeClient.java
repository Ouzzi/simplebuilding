package com.simplebuilding.forge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.forge.networking.ForgeNetworkRegistration;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge client wiring (mod bus, client dist). Currently wires client->server
 * networking. Client registrations (key mappings, menu screens, HUD overlays,
 * item model properties, in-world highlight rendering) are added incrementally.
 */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SimplebuildingForgeClient {
    private SimplebuildingForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientNetworking.setSender(ForgeNetworkRegistration::sendToServer);
    }
}
