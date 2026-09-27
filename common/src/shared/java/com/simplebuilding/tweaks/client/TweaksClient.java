package com.simplebuilding.tweaks.client;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksClientHooks;
import com.simplebuilding.tweaks.item.LaserPointerItem;
import com.simplebuilding.tweaks.network.ElytraBoostPayload;
import com.simplebuilding.tweaks.network.LaserPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client-Logik aus Simple Tweaks, loader-neutral: Boost-Taste der Spawn-Elytra
 * (SpawnElytraClient) und das Melden des eigenen Laserpunkts (LaserRenderer/LaserManager).
 * Die Loader rufen {@link #init()} einmal und {@link #tick(Minecraft)} am Ende jedes Client-Ticks.
 */
public final class TweaksClient {
    private static boolean jumpWasDown;
    private static int laserTicks;

    private TweaksClient() {
    }

    public static void init() {
        TweaksClientHooks.setLocalPlayer(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            return player == null ? null : player.getUUID();
        });
        TweaksClientHooks.setClientThread(() -> Minecraft.getInstance().isSameThread());
    }

    public static void tick(Minecraft client) {
        if (client.getConnection() == null) {
            // Getrennt: die Werte des letzten Servers gelten nicht mehr (Audit #16).
            SimpleTweaks.setServerValues(null);
        }
        if (client.isPaused()) {
            return;
        }
        TweaksNetwork.expireLasers();
        LocalPlayer player = client.player;
        if (player == null) {
            jumpWasDown = false;
            return;
        }

        boolean jumpDown = client.options.keyJump.isDown();
        if (jumpDown && !jumpWasDown && player.isFallFlying() && SpawnElytraHud.wearsSpawnElytra(player)) {
            TweaksNetwork.sendToServer(new ElytraBoostPayload());
        }
        jumpWasDown = jumpDown;

        if (isAimingLaser(player) && laserTicks++ % 2 == 0) {
            Vec3 hit = laserHit(player, 1.0f);
            if (hit != null) {
                TweaksNetwork.sendToServer(new LaserPayload(player.getUUID(), (float) hit.x, (float) hit.y, (float) hit.z, true));
            }
        }
    }

    public static boolean isAimingLaser(LocalPlayer player) {
        return SimpleTweaks.effectiveValues().laserEnabled()
                && player.isUsingItem() && player.getUseItem().getItem() instanceof LaserPointerItem
                && !LaserPointerItem.isEmpty(player.getUseItem());
    }

    /** Wo der eigene Laser auftrifft (Reichweite aus der Config), sonst null. */
    public static Vec3 laserHit(LocalPlayer player, float partialTick) {
        HitResult hit = player.pick(SimpleTweaks.effectiveValues().laserRange(), partialTick, false);
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }
}
