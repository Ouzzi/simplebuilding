package com.simplebuilding.modules.simpletweaks.deed.client;

import com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedResultPayload;
import net.minecraft.client.Minecraft;

/** Client entry of the deed packets; the loaders call these from their receivers. Loaded only on the client. */
public final class DeedClient {
    private DeedClient() {}

    /**
     * The map view when the holder may see the land (SP22-8), else the editor, reader view or claim sheet. The answer
     * to a trade comes back to the buy tab (WH26-9), which stays open.
     */
    public static void open(DeedOpenPayload payload) {
        var mc = Minecraft.getInstance();
        boolean trading = mc.gui.screen() instanceof DeedTradeScreen && payload.map().owns();
        mc.gui.setScreen(!payload.map().map() ? new DeedScreen(payload) : trading ? new DeedTradeScreen(payload) : new DeedMapScreen(payload));
    }

    public static void result(DeedResultPayload payload) {
        if (Minecraft.getInstance().gui.screen() instanceof DeedScreen screen) screen.onResult(payload);
    }

    public static void chunks(DeedChunksPayload payload) {
        if (Minecraft.getInstance().gui.screen() instanceof DeedMapScreen screen) screen.onChunks(payload);
    }
}
