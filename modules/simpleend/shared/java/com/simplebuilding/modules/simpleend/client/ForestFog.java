package com.simplebuilding.modules.simpleend.client;

import com.simplebuilding.modules.simpleend.SimpleEndConfig;
import com.simplebuilding.modules.simpleend.world.EndBiomes;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.joml.Vector3fc;

/**
 * Client fog wall (N1 B, 2026-10-16): Vanilla fog belongs to the biome the camera stands in, so from outside the forests
 * (Ash Valley, Dawnlands and their fringes) looked clear. A few points along the view direction (and its horizontal
 * projection, for flying above the forest) are looked up in the loaded chunks' biomes every 100 ms; the nearest forest hit
 * decides where the fog wall stands (see {@link #apply}).
 * The pull is eased in and out over about a second. Missing chunks count as "no forest"; nothing here can throw into
 * the renderer.
 */
public final class ForestFog {
    public static final float CORE_START = 10.0F, CORE_END = 64.0F;
    private static final double[] DISTANCES = {24, 40, 64, 96, 128};
    private static final long SAMPLE_NANOS = 100_000_000L;
    private static final double EASE_SECONDS = 0.35;
    private static final float NONE = Float.MAX_VALUE;

    /** Eased presence (0..1) of a forest ahead and the eased fog end distance it asks for. */
    private static float presence, wallEnd = NONE;
    private static float targetPresence, targetEnd = NONE;
    private static long lastSample, lastFrame;

    private ForestFog() {}

    /** Current ease state, for the client screenshot test log. */
    public static String debug() {
        return "presence " + presence + " wallEnd " + wallEnd + " target " + targetPresence + "/" + targetEnd;
    }

    /**
     * Called at the end of the atmospheric fog setup; pulls the fog distances in place toward a wall that ends just in front of
     * the nearest forest point ahead (never closer than the core's fog end). The pull is geometric, so a far
     * Vanilla fog distance (the End has hundreds of blocks) still closes in clearly: end * (wall / end) ^ strength.
     */
    public static void apply(FogData fog, Camera camera, ClientLevel level) {
        try {
            update(camera, level);
            float t = (float) SimpleEndConfig.forestFogStrength * presence;
            if (t <= 0.001F || wallEnd >= fog.environmentalEnd) return;
            float ratio = wallEnd / fog.environmentalEnd;
            fog.environmentalEnd *= (float) Math.pow(ratio, t);
            fog.environmentalStart = Math.min(fog.environmentalStart, fog.environmentalEnd * CORE_START / CORE_END);
        } catch (RuntimeException e) {
            presence = 0.0F;                         // never break the frame because of a fog nicety
        }
    }

    private static void update(Camera camera, ClientLevel level) {
        long now = System.nanoTime();
        double dt = Math.min(0.5, (now - lastFrame) / 1e9);
        lastFrame = now;
        if (!SimpleEndConfig.forestFogWall || level.dimension() != Level.END) {
            presence = 0.0F;
            targetPresence = 0.0F;
            return;
        }
        if (now - lastSample > SAMPLE_NANOS) {
            lastSample = now;
            var pos = camera.position();
            double nearest = inside(level, pos.x, pos.y, pos.z) ? -1.0 : scan(level, camera);
            targetPresence = nearest > 0 ? 1.0F : 0.0F;
            if (nearest > 0) targetEnd = (float) Math.max(CORE_END, nearest * 0.9);
        }
        float k = (float) (1.0 - Math.exp(-dt / EASE_SECONDS));
        if (targetPresence > 0.0F && presence < 0.01F) wallEnd = targetEnd;
        presence += (targetPresence - presence) * k;
        if (targetPresence > 0.0F && targetEnd != NONE) wallEnd += (targetEnd - wallEnd) * k;
    }

    /** Distance of the nearest forest sample ahead (along the view and its horizontal projection), or -1. */
    private static double scan(ClientLevel level, Camera camera) {
        Vector3fc fwd = camera.forwardVector();
        double hx = fwd.x(), hz = fwd.z();
        double hl = Math.sqrt(hx * hx + hz * hz);
        var pos = camera.position();
        for (double d : DISTANCES) {
            if (inside(level, pos.x + fwd.x() * d, pos.y + fwd.y() * d, pos.z + fwd.z() * d)
                    || hl > 0.05 && inside(level, pos.x + hx / hl * d, pos.y, pos.z + hz / hl * d)) {
                return d;
            }
        }
        return -1.0;
    }

    /** True when the (loaded) biome at the position is a forest core or fringe. */
    public static boolean inside(ClientLevel level, double x, double y, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        ChunkAccess chunk = level.getChunkSource().getChunk(bx >> 4, bz >> 4, ChunkStatus.BIOMES, false);
        if (chunk == null) return false;
        int qy = Math.max(level.getMinY(), Math.min(level.getMaxY() - 1, (int) Math.floor(y))) >> 2;
        Holder<Biome> biome = chunk.getNoiseBiome(bx >> 2, qy, bz >> 2);
        return biome.is(EndBiomes.WATCHER_CORE) || biome.is(EndBiomes.ASH_VALLEY_FRINGE) || biome.is(EndBiomes.DAWNLANDS_FRINGE);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
