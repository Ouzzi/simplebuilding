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
 * projection, for flying above the forest) are looked up in the loaded chunks' biomes every 100 ms; the more and the nearer
 * forest they hit, the closer the fog distances are pulled to the core's fog ({@link #CORE_START}/{@link #CORE_END}).
 * The pull is eased in and out over about a second. Missing chunks count as "no forest"; nothing here can throw into
 * the renderer.
 */
public final class ForestFog {
    public static final float CORE_START = 10.0F, CORE_END = 64.0F;
    private static final double[] DISTANCES = {32, 64, 96, 128};
    private static final double[] WEIGHTS = {1.0, 0.85, 0.7, 0.55};
    private static final long SAMPLE_NANOS = 100_000_000L;
    private static final double EASE_SECONDS = 0.35;

    private static float smoothed;
    private static float target;
    private static long lastSample, lastFrame;

    private ForestFog() {}

    /** Called at the end of the atmospheric fog setup; lowers the environmental fog distances in place. */
    public static void apply(FogData fog, Camera camera, ClientLevel level) {
        try {
            float f = factor(camera, level);
            float s = (float) SimpleEndConfig.forestFogStrength * f;
            if (s <= 0.001F) return;
            fog.environmentalStart = lerp(fog.environmentalStart, Math.min(fog.environmentalStart, CORE_START), s);
            fog.environmentalEnd = lerp(fog.environmentalEnd, Math.min(fog.environmentalEnd, CORE_END), s);
        } catch (RuntimeException e) {
            smoothed = 0.0F;                         // never break the frame because of a fog nicety
        }
    }

    private static float factor(Camera camera, ClientLevel level) {
        long now = System.nanoTime();
        double dt = Math.min(0.5, (now - lastFrame) / 1e9);
        lastFrame = now;
        if (!SimpleEndConfig.forestFogWall || level.dimension() != Level.END) {
            smoothed = 0.0F;
            target = 0.0F;
            return 0.0F;
        }
        if (now - lastSample > SAMPLE_NANOS) {
            lastSample = now;
            target = inside(level, camera.position().x, camera.position().y, camera.position().z) ? 0.0F : scan(level, camera);
        }
        smoothed += (target - smoothed) * (float) (1.0 - Math.exp(-dt / EASE_SECONDS));
        return smoothed;
    }

    private static float scan(ClientLevel level, Camera camera) {
        Vector3fc fwd = camera.forwardVector();
        double hx = fwd.x(), hz = fwd.z();
        double hl = Math.sqrt(hx * hx + hz * hz);
        var pos = camera.position();
        float best = 0.0F;
        for (int i = 0; i < DISTANCES.length; i++) {
            double d = DISTANCES[i];
            if (inside(level, pos.x + fwd.x() * d, pos.y + fwd.y() * d, pos.z + fwd.z() * d)
                    || hl > 0.05 && inside(level, pos.x + hx / hl * d, pos.y, pos.z + hz / hl * d)) {
                best = Math.max(best, (float) WEIGHTS[i]);
            }
        }
        return best;
    }

    /** True when the (loaded) biome at the position is a forest core or fringe. */
    static boolean inside(ClientLevel level, double x, double y, double z) {
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
