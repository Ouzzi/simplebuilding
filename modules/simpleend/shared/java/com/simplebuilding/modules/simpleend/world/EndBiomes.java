package com.simplebuilding.modules.simpleend.world;

import com.simplebuilding.modules.simpleend.SimpleEnd;
import com.simplebuilding.modules.simpleend.SimpleEndConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Where Ash Valley and Dawnlands lie (docs/ai/PLAN-SIMPLE-END-2026-10-12.md): the outer End is cut into cells of
 * {@link #CELL}x{@link #CELL} chunks; a share {@link SimpleEndConfig#biomeChance} of the cells holds one round patch
 * with a diameter of 3-6 chunks. Inside a patch the End highlands/midlands (the outer islands' land) become one of the
 * two biomes. The cell's randomness comes from the world's erosion noise at the cell origin, so every world differs
 * and the result depends only on the chunk (a whole chunk is one biome).
 */
public final class EndBiomes {
    public static final ResourceKey<Biome> ASH_VALLEY = ResourceKey.create(Registries.BIOME, SimpleEnd.id("ash_valley"));
    public static final ResourceKey<Biome> DAWNLANDS = ResourceKey.create(Registries.BIOME, SimpleEnd.id("dawnlands"));
    public static final int CELL = 12;
    public static final int NONE = 0, ASH = 1, DAWN = 2;

    /** Noise of the world at a block column (the End sampler's erosion). */
    @FunctionalInterface
    public interface Noise {
        double at(int blockX, int blockZ);
    }

    /** Biome registry of the running server, set when it starts (before levels are created). */
    public static volatile net.minecraft.core.Registry<Biome> serverBiomes;

    private EndBiomes() {}

    public static void serverStarting(net.minecraft.server.MinecraftServer server) {
        serverBiomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
    }

    public static int pick(int chunkX, int chunkZ, Noise noise, double chance) {
        int cellX = Math.floorDiv(chunkX, CELL), cellZ = Math.floorDiv(chunkZ, CELL);
        double n = noise.at(cellX * CELL * 16, cellZ * CELL * 16);
        long s = mix(cellX * 341873128712L + cellZ * 132897987541L + (long) Math.floor(n * 1_000_000.0));
        if ((s & 0xFFFF) / 65536.0 >= chance) return NONE;
        double radius = 1.5 + ((s >>> 20) & 0xFF) / 255.0 * 1.5;          // diameter 3..6 chunks
        int cx = cellX * CELL + 3 + (int) ((s >>> 28) & 0x7FFF) % 6;       // centre keeps the patch inside its cell
        int cz = cellZ * CELL + 3 + (int) ((s >>> 44) & 0x7FFF) % 6;
        double dx = chunkX - cx, dz = chunkZ - cz;
        if (dx * dx + dz * dz > radius * radius) return NONE;
        return ((s >>> 16) & 1) == 0 ? ASH : DAWN;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }
}
