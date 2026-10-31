package com.simplelib.api.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.FreeLookRules;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Free look (owner 26.10., E29-4; reworked IG31-29, 31.10.): while it is on, the mouse turns only the camera, not the player -
 * like jumping out of the dropship in Apex Legends; walking, flying, attacking and every other input stay normal. Two keys:
 * {@link #KEY} "Free Look" (hold, standard left Alt) and {@link #TOGGLE_KEY} "Toggle Free Look" (unbound). When it ends, the
 * {@link FreeLookRules} mode decides: the camera eases back to where the player looks (standard) or the player turns to where
 * the camera looks. The client picks the mode ({@code config/simplelib-client.json}); a server can enforce its own
 * ({@code simplelib-server.json}, {@code freeLookRelease} + {@code freeLookEnforce}, sent on join).
 *
 * <p>Always off after joining, a world or dimension change and a respawn (every new local player): the hold key only starts
 * free look with a real key press made after the key was seen up - Vanilla's {@code KeyMapping.setAll} copies the physical
 * key state into the mappings when the game regains focus, so a left Alt still "down" from an Alt+Tab used to switch free
 * look on right at the join (IG31-29). The mouse hook ({@code FreeLookTurnMixin}) and the camera hook
 * ({@code FreeLookCameraMixin}) read this class.
 */
public final class FreeLook {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("simplelib", "general"));
    public static final KeyMapping KEY = new KeyMapping("key.simplelib.freelook", InputConstants.KEY_LALT, CATEGORY);
    public static final KeyMapping TOGGLE_KEY = new KeyMapping("key.simplelib.freelook_toggle", InputConstants.UNKNOWN.getValue(), CATEGORY);

    /** Camera offset to the player's own view in degrees. */
    private static float yaw, pitch;
    private static boolean held, toggled;
    /** Set on every reset: the hold key must be seen up once before a press counts. */
    private static boolean waitForRelease = true;
    private static Player lastPlayer;

    private FreeLook() {}

    /** True while free look is on (held or toggled): mouse movement goes to the camera offset. */
    public static boolean active() {
        return held || toggled;
    }

    /** Once per client tick: keys, reset on a new local player, release, easing back. */
    public static void tick(Minecraft mc) {
        Player player = mc.player;
        if (player != lastPlayer) {
            lastPlayer = player;
            reset();
        }
        if (player == null) return;
        boolean was = active();
        boolean down = KEY.isDown();
        if (!down) waitForRelease = false;
        boolean pressed = false;
        while (KEY.consumeClick()) pressed = true;
        boolean screen = mc.gui.screen() != null;
        if (pressed && !waitForRelease && !screen) held = true;
        if (!down || screen) held = false;
        while (TOGGLE_KEY.consumeClick()) {
            if (!screen) toggled = !toggled;
        }
        if (was && !active()) end(player);
        if (!active()) {
            yaw *= 0.66f;
            pitch *= 0.66f;
            if (Math.abs(yaw) < 0.05f) yaw = 0;
            if (Math.abs(pitch) < 0.05f) pitch = 0;
        }
    }

    /** Free look ended: in {@link FreeLookRules.Release#PLAYER} the player takes over the camera's direction at once. */
    private static void end(Player player) {
        if (FreeLookRules.effective() != FreeLookRules.Release.PLAYER) return;
        float y = player.getYRot() + yaw, x = Math.max(-90, Math.min(90, player.getXRot() + pitch));
        player.setYRot(y);
        player.setXRot(x);
        player.yRotO = y;
        player.xRotO = x;
        yaw = 0;
        pitch = 0;
    }

    /** Off, no offset, and the hold key has to be released before it counts again (join, world change, respawn). */
    public static void reset() {
        held = false;
        toggled = false;
        yaw = 0;
        pitch = 0;
        waitForRelease = true;
        while (KEY.consumeClick()) { /* drop presses made before */ }
        while (TOGGLE_KEY.consumeClick()) { /* drop presses made before */ }
    }

    /** Mouse turn while on (same scale as {@code Entity.turn}: degrees = delta * 0.15). */
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

    /** For tests: set the offset and the hold state directly. */
    public static void set(float yawOffset, float pitchOffset, boolean isHeld) {
        yaw = yawOffset;
        pitch = pitchOffset;
        held = isHeld;
    }

    /** For tests: whether free look is toggled on. */
    public static boolean toggled() {
        return toggled;
    }
}
