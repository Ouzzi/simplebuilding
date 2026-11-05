package com.simplelib.api.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.FreeLookRules;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
    /** For tests (before/after pictures): false = the hand travels with the camera like before QOL34. */
    public static volatile boolean handFix = true;

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
        if (was && !active() && !easing) startEase();
        if (active()) easing = false;
    }

    /** Ease-back duration in ms (IG31-43). */
    public static final long EASE_MS = 200;
    private static boolean easing;
    private static long easeStart;
    private static float easeYaw, easePitch;

    /** Start the frame-based turn back to the player's view over the shortest angle (no path history). */
    private static void startEase() {
        if (yaw == 0 && pitch == 0) return;
        easeYaw = -FreeLookRules.shortestDelta(yaw, 0);
        easePitch = pitch;
        easeStart = System.nanoTime();
        easing = true;
        update();
    }

    /** Called from the render-side getters, so the easing follows frames and not ticks. */
    private static void update() {
        if (!easing) return;
        float t = (System.nanoTime() - easeStart) / (EASE_MS * 1.0e6f);
        if (t >= 1f) {
            yaw = 0;
            pitch = 0;
            easing = false;
            return;
        }
        float r = FreeLookRules.easeOutRemaining(t);
        yaw = easeYaw * r;
        pitch = easePitch * r;
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
        easing = false;
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
        update();
        easing = false;
        yaw += (float) (dYaw * 0.15);
        pitch = Math.max(-90 - playerPitch, Math.min(90 - playerPitch, pitch + (float) (dPitch * 0.15)));
    }

    public static float yawOffset() {
        update();
        return yaw;
    }

    public static float pitchOffset() {
        update();
        return pitch;
    }

    /** For tests: set the offset and the hold state directly. */
    public static void set(float yawOffset, float pitchOffset, boolean isHeld) {
        easing = false;
        yaw = yawOffset;
        pitch = pitchOffset;
        held = isHeld;
    }

    /** For tests: whether free look is toggled on. */
    public static boolean toggled() {
        return toggled;
    }

    /** Camera orientation (camera to world) for a view of {@code yRot}/{@code xRot} degrees, exactly as {@code Camera.setRotation} builds it. */
    private static Quaternionf orientation(float yRot, float xRot) {
        return new Quaternionf().rotationYXZ((float) Math.PI - yRot * ((float) Math.PI / 180f), -xRot * ((float) Math.PI / 180f), 0f);
    }

    /**
     * First person hand (QOL34, owner 03.11.): the hand is drawn in camera space, so with the camera turned away it travelled
     * along with the camera. This is the rotation that puts it back to the body direction: camera^-1 * body, i.e. the hand
     * stays where it is without free look and leaves the screen when the camera looks far enough away - like the arm of
     * a body that does not turn. Null when free look is off or the offset is zero.
     */
    public static Quaternionf handCorrection(float playerYRot, float playerXRot) {
        if (!handFix || (yawOffset() == 0 && pitchOffset() == 0)) return null;
        Quaternionf body = orientation(playerYRot, playerXRot);
        float camPitch = Math.max(-90, Math.min(90, playerXRot + pitch));
        return orientation(playerYRot + yaw, camPitch).conjugate().mul(body);
    }

    /**
     * Where the body direction (the real aim point) is on the screen while free look is on: {x, y} in GUI pixels, or null when
     * it is behind the camera or free look/the option is off. {@code fovDegrees} is the vertical field of view.
     */
    public static boolean crosshairOn() {
        return (active() || easing) && FreeLookRules.crosshair();
    }

    public static int[] crosshairPoint(float playerXRot, int guiWidth, int guiHeight, double fovDegrees) {
        if (!crosshairOn()) return null;
        Quaternionf cam = orientation(yawOffset(), Math.max(-90, Math.min(90, playerXRot + pitchOffset())));
        Vector3f v = cam.conjugate().transform(orientation(0, playerXRot).transform(new Vector3f(0, 0, -1)));
        if (v.z >= -1.0e-4f) return null;
        double focal = guiHeight / 2.0 / Math.tan(Math.toRadians(fovDegrees) / 2);
        int x = (int) Math.round(guiWidth / 2.0 + v.x / -v.z * focal);
        int y = (int) Math.round(guiHeight / 2.0 - v.y / -v.z * focal);
        return new int[]{x, y};
    }
}
