package com.simplebuilding.modules.simplemachines;

import com.simplemachines.upgrade.PistonConfigPayload;
import com.simplemachines.upgrade.PistonUpgrades;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Fabric client entry: the server's piston options (audit N16) and the late hooks (nugget hand hint) once the client runs. */
public final class MachinesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PistonConfigPayload.ID, (payload, context) -> context.client().execute(payload::apply));
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PistonUpgrades.registerLateHooks());
    }
}
