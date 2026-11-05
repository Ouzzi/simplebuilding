package com.simplepads.client;

import com.simplepads.PadsClientHooks;
import com.simplepads.SimplePads;
import com.simplepads.network.ElytraBoostPayload;
import com.simplepads.network.PadsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Client side of Simple Pads, loader-neutral: Spawn Elytra boost on the jump key (until 04.11. SimpleBuilding's TweaksClient). */
public final class PadsClient {
    private static boolean jumpWasDown;

    private PadsClient() {
    }

    public static void init() {
        PadsClientHooks.setLocalPlayer(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            return player == null ? null : player.getUUID();
        });
        PadsClientHooks.setClientThread(() -> Minecraft.getInstance().isSameThread());
    }

    public static void tick(Minecraft client) {
        if (client.getConnection() == null) {
            // Disconnected: the last server's values no longer apply.
            SimplePads.setSynced(null);
        }
        if (client.isPaused()) {
            return;
        }
        LocalPlayer player = client.player;
        if (player == null) {
            jumpWasDown = false;
            return;
        }
        boolean jumpDown = client.options.keyJump.isDown();
        if (jumpDown && !jumpWasDown && player.isFallFlying() && SpawnElytraHud.wearsSpawnElytra(player)) {
            PadsNetwork.sendToServer(new ElytraBoostPayload());
        }
        jumpWasDown = jumpDown;
    }
}
