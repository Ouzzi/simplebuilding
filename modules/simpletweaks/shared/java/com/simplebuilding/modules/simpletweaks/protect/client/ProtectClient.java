package com.simplebuilding.modules.simpletweaks.protect.client;

import com.simplebuilding.modules.simpletweaks.protect.ProtectRulesPayload;
import net.minecraft.client.Minecraft;

/** Client state of the server's protection rules; tied to the connection it came with (a new server starts clean). */
public final class ProtectClient {
    private static volatile Object connection;
    private static volatile boolean freecamCollision;

    public static void rules(ProtectRulesPayload payload) {
        connection = Minecraft.getInstance().getConnection();
        freecamCollision = payload.freecamCollision();
    }
    /** Must Freecam collide with blocks right now? */
    public static boolean forceFreecamCollision() {
        if (!freecamCollision) return false;
        var now = Minecraft.getInstance().getConnection();
        return now != null && now == connection;
    }
    private ProtectClient() {}
}
