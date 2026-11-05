package com.simplelib.api;

import net.minecraft.util.Mth;
import java.util.Locale;

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
}
