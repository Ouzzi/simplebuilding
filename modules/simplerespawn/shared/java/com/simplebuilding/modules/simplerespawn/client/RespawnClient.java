package com.simplebuilding.modules.simplerespawn.client;

import com.simplebuilding.modules.simplerespawn.SimpleRespawn;
import com.simplebuilding.modules.simplerespawn.network.DownedListPayload;
import com.simplebuilding.modules.simplerespawn.network.RespawnStatePayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Client state of Simple Respawn: the own role from the server, the downed screen, the free "spectate" camera around
 * the own body and the countdown between two server updates. Everything here only shows and blocks input; the server
 * decides.
 */
public final class RespawnClient {
    static int role = RespawnStatePayload.NONE;
    static int ticksLeft;
    static int maxTicks;
    static float progress;
    static String other = "";
    /** "Spectate": screen closed, the camera circles the own body. */
    static boolean spectating;
    static float orbitYaw;
    static float orbitPitch = 25f;
    private static CameraType previousCamera;
    private static int heartbeat;

    private RespawnClient() {}

    public static boolean downed() {
        return role == RespawnStatePayload.DOWNED;
    }

    public static boolean orbiting() {
        return downed() && spectating;
    }

    public static void receive(DownedListPayload payload) {
        SimpleRespawn.CLIENT_DOWNED.clear();
        SimpleRespawn.CLIENT_DOWNED.addAll(payload.players());
    }

    public static void receive(RespawnStatePayload payload) {
        boolean was = downed();
        role = payload.role();
        ticksLeft = payload.ticksLeft();
        maxTicks = payload.maxTicks();
        progress = payload.progress();
        other = payload.other();
        Minecraft mc = Minecraft.getInstance();
        if (was && !downed()) leave(mc);
        if (!was && downed()) {
            spectating = false;
            if (mc.player != null) orbitYaw = mc.player.getYRot();
            orbitPitch = 25f;
        }
    }

    /** Leaving a world forgets everything. */
    public static void reset() {
        Minecraft mc = Minecraft.getInstance();
        if (downed()) leave(mc);
        role = RespawnStatePayload.NONE;
        progress = 0f;
        other = "";
        SimpleRespawn.CLIENT_DOWNED.clear();
    }

    private static void leave(Minecraft mc) {
        stopOrbit(mc);
        Screen screen = mc.gui.screen();
        if (screen instanceof DownedScreen || screen instanceof ViewOnlyInventoryScreen) mc.gui.setScreen(null);
    }

    /** Called at the end of every client tick (mixin on {@code Minecraft#tick}). */
    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            if (role != RespawnStatePayload.NONE) reset();
            return;
        }
        if (!downed()) return;
        // Count down locally between two server updates (the server pauses while someone helps).
        if (progress <= 0f && ticksLeft > 0) ticksLeft--;
        mc.player.setSprinting(false);
        Screen screen = mc.gui.screen();
        if (!spectating && screen == null) mc.gui.setScreen(new DownedScreen());
        if (spectating && screen == null && moving(mc)) {
            // Crawl in the direction the camera looks.
            mc.player.setYRot(orbitYaw);
            mc.player.setYHeadRot(orbitYaw);
        }
        if (++heartbeat >= 25) {
            heartbeat = 0;
            mc.player.playSound(SoundEvents.WARDEN_HEARTBEAT, 0.6f, 1f);
        }
    }

    private static boolean moving(Minecraft mc) {
        var o = mc.options;
        return o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown();
    }

    /** "Spectate" button: close the screen, free camera around the own body. */
    static void spectate(Minecraft mc) {
        spectating = true;
        if (mc.player != null) orbitYaw = mc.player.getYRot();
        previousCamera = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        mc.gui.setScreen(null);
    }

    /** The inventory key while spectating opens the downed screen again. */
    public static void openMenu(Minecraft mc) {
        stopOrbit(mc);
        mc.gui.setScreen(new DownedScreen());
    }

    private static void stopOrbit(Minecraft mc) {
        if (spectating && previousCamera != null) mc.options.setCameraType(previousCamera);
        previousCamera = null;
        spectating = false;
    }

    /** Mouse movement while spectating turns the camera, not the body (same factor as {@code Entity#turn}). */
    public static void turn(double dx, double dy) {
        orbitYaw += (float) dx * 0.15f;
        orbitPitch = Mth.clamp(orbitPitch + (float) dy * 0.15f, -89f, 89f);
    }

    public static float orbitYaw() {
        return orbitYaw;
    }

    public static float orbitPitch() {
        return orbitPitch;
    }

    static void giveUp(Minecraft mc) {
        if (mc.player != null) mc.player.connection.sendCommand("simplerespawn giveup");
    }
}
