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
 * estimated ground height ({@link ChunkGenerator#getBaseHeight}, ocean floor), the noise biome at that height and, with
 * {@code surfaceDetail}, the surface the generator would put there (snow line, steep stone, badlands bands - from the height
 * field and the biome temperature)
 * ({@link net.minecraft.world.level.biome.BiomeSource#createUncachedResolver}). Neither touches chunks: nothing is loaded,
 * generated or saved, so the earth can show millions of blocks. Safe off the server thread (structure placement uses the
 * same calls from the worldgen workers). Mod worldgen works through the generic biome source.
 */
public final class TerrainSampler {
    private TerrainSampler() {}

    /** A sampled tile: map colour id per sample (0 = unknown) and ground height. */
    public record Tile(byte[] colors, short[] heights) {}

    public static Tile sample(ServerLevel level, int e, int tx, int tz, boolean exploredOnly) {
        return sample(level, e, tx, tz, exploredOnly, com.simplebuilding.modules.simpleearth.config.EarthServerConfig.biomeVotes,
                com.simplebuilding.modules.simpleearth.config.EarthServerConfig.surfaceDetail);
    }

    /** Heights first (one generator call per sample), then the colours (biome votes, surface detail from the height field). */
    public static Tile sample(ServerLevel level, int e, int tx, int tz, boolean exploredOnly, int votes, boolean surface) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(random);
        int sea = generator.getSeaLevel();
        Path regions = exploredOnly ? regionFolder(level) : null;
        byte[] colors = new byte[TileMath.AREA];
        short[] heights = new short[TileMath.AREA];
        boolean[] known = new boolean[TileMath.AREA];
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
                known[k] = true;
            }
        }
        for (int j = 0; j < TileMath.TILE; j++) {
            for (int i = 0; i < TileMath.TILE; i++) {
                int k = j * TileMath.TILE + i;
                if (!known[k]) continue;
                int x = (int) TileMath.sampleBlock((long) tx * TileMath.TILE + i, e), z = (int) TileMath.sampleBlock((long) tz * TileMath.TILE + j, e);
                colors[k] = (byte) colorAt(biomes, x, heights[k], z, sea, 1 << e, votes, surface ? slope(heights, i, j, 1 << e) : -1).id;
            }
        }
        return new Tile(colors, heights);
    }

    /**
     * The map colour of one sample: biome vote at the ground (or sea level), then with {@code slope >= 0} the surface detail
     * ({@link EarthColors#surface}) of the biome in the middle of the cell. Tests recompute tiles with it.
     */
    public static MapColor colorAt(BiomeResolver biomes, int x, int h, int z, int sea, int step, int votes, double slope) {
        int y = Math.max(h, sea);
        MapColor c = color(biomes, x, y, z, h < sea - LAKE_DEPTH, step, votes);
        if (slope < 0) return c;
        Holder<Biome> centre = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
        return EarthColors.surface(c, centre, x, h, z, sea, slope);
    }

    /** Largest height difference to the four neighbour samples (inside the tile) per block of spacing. */
    public static double slope(short[] heights, int i, int j, int step) {
        int k = j * TileMath.TILE + i, h = heights[k], d = 0;
        if (i > 0) d = Math.max(d, Math.abs(h - heights[k - 1]));
        if (i < TileMath.TILE - 1) d = Math.max(d, Math.abs(h - heights[k + 1]));
        if (j > 0) d = Math.max(d, Math.abs(h - heights[k - TileMath.TILE]));
        if (j < TileMath.TILE - 1) d = Math.max(d, Math.abs(h - heights[k + TileMath.TILE]));
        return d / (double) step;
    }

    /** Ground this far below sea level counts as water in a land biome (shallow dips stay land, concept §4.2: no speckles). */
    public static final int LAKE_DEPTH = 4;

    /**
     * The map colour of a sample cell: from {@code step} >= 128 blocks the noise biome is asked at {@code votes} x {@code votes}
     * points of the cell and the most frequent colour wins (concept §4.2, N10 - otherwise narrow rivers and single biome
     * patches flicker into speckles); below that, or with one vote, one point in the middle. Only the biome is sampled several
     * times, the height once (the costly part).
     */
    public static MapColor color(BiomeResolver biomes, int x, int y, int z, boolean underwater, int step) {
        return color(biomes, x, y, z, underwater, step, 2);
    }

    public static MapColor color(BiomeResolver biomes, int x, int y, int z, boolean underwater, int step, int votes) {
        int qy = QuartPos.fromBlock(y);
        int n = Math.max(1, Math.min(3, votes));
        if (step < 128 || n == 1) return EarthColors.color(biomes.getNoiseBiome(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z)), underwater);
        MapColor[] list = new MapColor[n * n];
        int k = 0;
        for (int b = 0; b < n; b++) {
            int dz = (int) Math.round((b + 0.5) / n * step - step / 2.0);
            for (int a = 0; a < n; a++) {
                int dx = (int) Math.round((a + 0.5) / n * step - step / 2.0);
                Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x + dx), qy, QuartPos.fromBlock(z + dz));
                list[k++] = EarthColors.color(biome, underwater);
            }
        }
        MapColor best = list[0];
        int bestCount = 0;
        for (MapColor c : list) {
            int count = 0;
            for (MapColor o : list) if (o == c) count++;
            if (count > bestCount) {
                best = c;
                bestCount = count;
            }
        }
        return best;
    }

    /** The dimension's region folder (E8: a region file there = explored). */
    public static Path regionFolder(ServerLevel level) {
        return DimensionType.getStorageFolder(level.dimension(), level.getServer().getWorldPath(LevelResource.ROOT)).resolve("region");
    }
}
