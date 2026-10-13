package com.simplebuilding.modules.simpleend.world;

import com.simplebuilding.modules.simpleend.SimpleEnd;
import com.simplebuilding.modules.simpleend.SimpleEndConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Where Ash Valley and Dawnlands lie (docs/ai/PLAN-SIMPLE-END-2026-10-12.md, "Umbau 13.10."): the outer End is cut
 * into cells of {@link #CELL}x{@link #CELL} chunks; a share {@link SimpleEndConfig#biomeChance} of the cells holds one
 * large patch, 14-24 chunks across with a wavy rim. Inside a patch the End highlands/midlands/barrens become the
 * biome: the inner {@link #CORE} of the radius is the foggy core (Ash Valley / Dawnlands, biome tag
 * {@code simpleend:watcher_core}), the outside its fringe. The cell's randomness comes from the world's erosion noise
 * at the cell origin, so every world differs; positions are exact to 4 blocks (a biome quart).
 */
public final class EndBiomes {
    public static final ResourceKey<Biome> ASH_VALLEY = ResourceKey.create(Registries.BIOME, SimpleEnd.id("ash_valley"));
    public static final ResourceKey<Biome> DAWNLANDS = ResourceKey.create(Registries.BIOME, SimpleEnd.id("dawnlands"));
    public static final ResourceKey<Biome> ASH_VALLEY_FRINGE = ResourceKey.create(Registries.BIOME, SimpleEnd.id("ash_valley_fringe"));
    public static final ResourceKey<Biome> DAWNLANDS_FRINGE = ResourceKey.create(Registries.BIOME, SimpleEnd.id("dawnlands_fringe"));
    public static final net.minecraft.tags.TagKey<Biome> WATCHER_CORE = net.minecraft.tags.TagKey.create(Registries.BIOME, SimpleEnd.id("watcher_core"));
    public static final int CELL = 40;
    /** Patch radius in chunks: {@link #MIN_RADIUS} plus up to {@link #RADIUS_RANGE}. */
    public static final double MIN_RADIUS = 7.0, RADIUS_RANGE = 5.0;
    /** Share of the (wavy) radius that is core. */
    public static final double CORE = 0.62;
    public static final int NONE = 0, ASH = 1, DAWN = 2, ASH_FRINGE = 3, DAWN_FRINGE = 4;

    /** Noise of the world at a block column (the End sampler's erosion). */
    @FunctionalInterface
    public interface Noise {
        double at(int blockX, int blockZ);
    }

    /** Biome registry of the running server, set when it starts (before levels are created). */
    public static volatile net.minecraft.core.Registry<Biome> serverBiomes;

    private EndBiomes() {}

    public static void serverStopped() {
        serverBiomes = null;
    }

    public static void serverStarting(net.minecraft.server.MinecraftServer server) {
        serverBiomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
    }

    public static int pick(int chunkX, int chunkZ, Noise noise, double chance) {
        return pick(chunkX + 0.5, chunkZ + 0.5, noise, chance);
    }

    /** Biome patch at a position given in chunks (fractions allowed): NONE, ASH, DAWN, ASH_FRINGE or DAWN_FRINGE. */
    public static int pick(double chunkX, double chunkZ, Noise noise, double chance) {
        int cellX = (int) Math.floor(chunkX / CELL), cellZ = (int) Math.floor(chunkZ / CELL);
        double n = noise.at(cellX * CELL * 16, cellZ * CELL * 16);
        long s = mix(cellX * 341873128712L + cellZ * 132897987541L + (long) Math.floor(n * 1_000_000.0));
        if ((s & 0xFFFF) / 65536.0 >= chance) return NONE;
        double radius = MIN_RADIUS + ((s >>> 20) & 0xFF) / 255.0 * RADIUS_RANGE;
        int margin = (int) Math.ceil((MIN_RADIUS + RADIUS_RANGE) * 1.18) + 1;          // the wavy patch stays inside its cell
        double cx = cellX * CELL + margin + ((s >>> 28) & 0x7FFF) % (CELL - 2 * margin + 1);
        double cz = cellZ * CELL + margin + ((s >>> 44) & 0x7FFF) % (CELL - 2 * margin + 1);
        double dx = chunkX - cx, dz = chunkZ - cz;
        double angle = Math.atan2(dz, dx);
        double phase = ((s >>> 8) & 0xFF) / 255.0 * Math.PI * 2;
        double wavy = radius * (1.0 + 0.12 * Math.sin(3 * angle + phase) + 0.06 * Math.sin(5 * angle - 2 * phase));
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > wavy) return NONE;
        boolean ash = ((s >>> 16) & 1) == 0;
        if (d <= wavy * CORE) return ash ? ASH : DAWN;
        return ash ? ASH_FRINGE : DAWN_FRINGE;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }
}
