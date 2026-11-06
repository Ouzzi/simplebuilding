package com.simplebuilding.modules.simplechisels;

import com.simplechisels.data.ChiselTables;
import com.simplechisels.net.ChiselTablesPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client entry: applies the server's chisel tables (click prediction, hand hint). */
public final class ChiselsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ChiselTablesPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ChiselTables.receive(payload.files())));
    }
}
