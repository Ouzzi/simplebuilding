package com.simplelib.api.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Free look (owner 26.10., E29-4; shared with Simple Earth and later the zoom module, WE25-4): while the key is held the mouse
 * turns only the camera, not the player - like jumping out of the dropship in Apex Legends; walking, flying, attacking and
 * every other input stay normal. On release the camera eases back to where the player looks. Standard key: left Alt.
 * The mouse hook ({@code FreeLookTurnMixin}) and the camera hook ({@code FreeLookCameraMixin}) read this class.
 */
public final class FreeLook {
    public static final KeyMapping KEY = new KeyMapping("key.simplelib.freelook", InputConstants.KEY_LALT,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("simplelib", "general")));
    /** Camera offset to the player's own view in degrees. */
    private static float yaw, pitch;
    private static boolean held;

    private FreeLook() {}

    /** True while the key is held: mouse movement goes to the camera offset. */
    public static boolean active() {
        return held;
    }

    /** Once per client tick: key state; after release the offset eases back (a third per tick). */
    public static void tick(Minecraft mc) {
        held = mc.player != null && mc.gui.screen() == null && KEY.isDown();
        if (!held) {
            yaw *= 0.66f;
            pitch *= 0.66f;
            if (Math.abs(yaw) < 0.05f) yaw = 0;
            if (Math.abs(pitch) < 0.05f) pitch = 0;
        }
    }

    /** Mouse turn while held (same scale as {@code Entity.turn}: degrees = delta * 0.15). */
    public static void turn(double dYaw, double dPitch, float playerPitch) {
        yaw += (float) (dYaw * 0.15);
        pitch = Math.max(-90 - playerPitch, Math.min(90 - playerPitch, pitch + (float) (dPitch * 0.15)));
    }

    public static float yawOffset() {
        return yaw;
    }

    public static float pitchOffset() {
        return pitch;
    }

    /** For tests: set the offset directly. */
    public static void set(float yawOffset, float pitchOffset, boolean isHeld) {
        yaw = yawOffset;
        pitch = pitchOffset;
        held = isHeld;
    }
}
