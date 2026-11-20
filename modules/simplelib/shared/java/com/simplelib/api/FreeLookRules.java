package com.simplelib.api;

import net.minecraft.util.Mth;
import java.util.Locale;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Free look release rules (IG31-29, owner 31.10.) without client classes, so the plain config readers can fill them on any
 * side: what happens when free look ends ({@link Release}), the client's own choice ({@code config/simplelib-client.json},
 * key {@code freeLookRelease}) and the server's mode + enforce flag (payload on join). The client tick lives in
 * {@code com.simplelib.api.client.FreeLook}.
 */
public final class FreeLookRules {
    /** What happens when free look ends. */
    public enum Release {
        /** The camera eases back to the player's view direction (standard). */
        CAMERA,
        /** The player turns to where the camera looks. */
        PLAYER;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** {@code "camera"} or {@code "player"}, case-insensitive; anything else is {@link #CAMERA}. */
        public static Release parse(String name) {
            return name != null && "player".equalsIgnoreCase(name.trim()) ? PLAYER : CAMERA;
        }
    }

    private static volatile Release client = Release.CAMERA, server = Release.CAMERA;
    private static volatile boolean enforce;
    /** QOL34: show the grey outlined "real" crosshair at the body direction while free look is on (client choice, standard on). */
    private static volatile boolean crosshair = true;

    /** DW04A-2: the mouse wheel zooms while free look is on (client choice, standard on). */
    private static volatile boolean zoom = true;

    public static boolean zoom() {
        return zoom;
    }

    public static void setZoom(boolean on) {
        zoom = on;
    }

    public static boolean crosshair() {
        return crosshair;
    }

    public static void setCrosshair(boolean on) {
        crosshair = on;
    }

    private FreeLookRules() {}

    /** The mode in effect: the server's while it enforces it, else the client's choice. */
    public static Release effective() {
        return enforce ? server : client;
    }

    public static Release client() {
        return client;
    }

    public static void setClient(Release release) {
        client = release == null ? Release.CAMERA : release;
    }

    /** The server's rules (payload on join). */
    public static void setServer(Release release, boolean enforced) {
        server = release == null ? Release.CAMERA : release;
        enforce = enforced;
    }

    /** Leaving a server: back to "camera, not enforced". */
    public static void clearServer() {
        setServer(Release.CAMERA, false);
    }

    public static Release server() {
        return server;
    }

    public static boolean enforced() {
        return enforce;
    }

    /** Shortest signed angle in degrees that turns {@code from} to {@code to} (e.g. 170 to -170 is +20, not -340). */
    public static float shortestDelta(float from, float to) {
        return Mth.wrapDegrees(to - from);
    }

    /** Ease-out (cubic) remaining share of the start offset after {@code t} (0..1) of the duration. */
    public static float easeOutRemaining(float t) {
        float u = 1 - Math.max(0f, Math.min(1f, t));
        return u * u * u;
    }

    /** Camera orientation (camera to world) for a view of {@code yRot}/{@code xRot} degrees, exactly as {@code Camera.setRotation} builds it. */
    public static Quaternionf orientation(float yRot, float xRot) {
        return new Quaternionf().rotationYXZ((float) Math.PI - yRot * ((float) Math.PI / 180f), -xRot * ((float) Math.PI / 180f), 0f);
    }

    /**
     * QOL34 crosshair: where the body direction is on the screen for a camera turned by {@code yawOffset}/{@code pitchOffset}
     * degrees from a body looking {@code playerXRot} up/down; {x, y} in GUI pixels of a {@code guiWidth} x {@code guiHeight}
     * screen drawn with a vertical field of view of {@code fovDegrees}, or null when the body direction is behind the camera.
     * Pure math (no client classes), so a server gametest checks it; the client's {@code FreeLook} adds the on/off rules.
     */
    public static int[] crosshairPoint(float yawOffset, float pitchOffset, float playerXRot, int guiWidth, int guiHeight, double fovDegrees) {
        Quaternionf cam = orientation(yawOffset, Math.max(-90, Math.min(90, playerXRot + pitchOffset)));
        Vector3f v = cam.conjugate().transform(orientation(0, playerXRot).transform(new Vector3f(0, 0, -1)));
        if (v.z >= -1.0e-4f) return null;
        double focal = guiHeight / 2.0 / Math.tan(Math.toRadians(fovDegrees) / 2);
        int x = (int) Math.round(guiWidth / 2.0 + v.x / -v.z * focal);
        int y = (int) Math.round(guiHeight / 2.0 - v.y / -v.z * focal);
        return new int[]{x, y};
    }
}
