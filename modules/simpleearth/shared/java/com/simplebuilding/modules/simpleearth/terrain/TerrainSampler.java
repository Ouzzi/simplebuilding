package com.simplebuilding.modules.simpleearth.terrain;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Samples one terrain tile straight from the world generator, like Chunkbase (concept §4.1): per sample the generator's
 * estimated ground height ({@link ChunkGenerator#getBaseHeight}, ocean floor) and the noise biome at that height
 * ({@link net.minecraft.world.level.biome.BiomeSource#createUncachedResolver}). Neither touches chunks: nothing is loaded,
 * generated or saved, so the earth can show millions of blocks. Safe off the server thread (structure placement uses the
 * same calls from the worldgen workers). Mod worldgen works through the generic biome source.
 */
public final class TerrainSampler {
    private TerrainSampler() {}

    /** A sampled tile: map colour id per sample (0 = unknown) and ground height. */
    public record Tile(byte[] colors, short[] heights) {}

    public static Tile sample(ServerLevel level, int e, int tx, int tz, boolean exploredOnly) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(random);
        int sea = generator.getSeaLevel();
        Path regions = exploredOnly ? regionFolder(level) : null;
        byte[] colors = new byte[TileMath.AREA];
        short[] heights = new short[TileMath.AREA];
        int lastRx = Integer.MIN_VALUE, lastRz = Integer.MIN_VALUE;
        boolean lastExplored = false;
        for (int j = 0; j < TileMath.TILE; j++) {
            for (int i = 0; i < TileMath.TILE; i++) {
                int k = j * TileMath.TILE + i;
                long x = TileMath.sampleBlock((long) tx * TileMath.TILE + i, e), z = TileMath.sampleBlock((long) tz * TileMath.TILE + j, e);
                heights[k] = (short) sea;
                if (Math.abs(x) > TileMath.WORLD_LIMIT || Math.abs(z) > TileMath.WORLD_LIMIT) continue;
                if (regions != null) {
                    int rx = (int) (x >> 9), rz = (int) (z >> 9);
                    if (rx != lastRx || rz != lastRz) {
                        lastRx = rx;
                        lastRz = rz;
                        lastExplored = Files.isRegularFile(regions.resolve("r." + rx + "." + rz + ".mca"));
                    }
                    if (!lastExplored) continue;
                }
                int h = generator.getBaseHeight((int) x, (int) z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                heights[k] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, h));
                MapColor c = color(biomes, (int) x, Math.max(h, sea), (int) z, h < sea - LAKE_DEPTH, 1 << e);
                colors[k] = (byte) c.id;
            }
        }
        return new Tile(colors, heights);
    }

    /** Ground this far below sea level counts as water in a land biome (shallow dips stay land, concept §4.2: no speckles). */
    public static final int LAKE_DEPTH = 4;

    /**
     * The map colour of a sample cell: from {@code step} >= 128 blocks the noise biome is asked at four points of the cell and
     * the most frequent colour wins (concept §4.2 - otherwise narrow rivers and single biome patches flicker into speckles);
     * below that one point in the middle. Only the biome is sampled several times, the height once (the costly part).
     */
    public static MapColor color(BiomeResolver biomes, int x, int y, int z, boolean underwater, int step) {
        int qy = QuartPos.fromBlock(y);
        if (step < 128) return EarthColors.color(biomes.getNoiseBiome(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z)), underwater);
        int d = step / 4;
        MapColor[] votes = new MapColor[4];
        int k = 0;
        for (int dz = -d; dz <= d; dz += 2 * d) {
            for (int dx = -d; dx <= d; dx += 2 * d) {
                Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x + dx), qy, QuartPos.fromBlock(z + dz));
                votes[k++] = EarthColors.color(biome, underwater);
            }
        }
        MapColor best = votes[0];
        int bestCount = 0;
        for (MapColor c : votes) {
            int n = 0;
            for (MapColor o : votes) if (o == c) n++;
            if (n > bestCount) {
                best = c;
                bestCount = n;
            }
        }
        return best;
    }

    /** The dimension's region folder (E8: a region file there = explored). */
    public static Path regionFolder(ServerLevel level) {
        return DimensionType.getStorageFolder(level.dimension(), level.getServer().getWorldPath(LevelResource.ROOT)).resolve("region");
    }
}
