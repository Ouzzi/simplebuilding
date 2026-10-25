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
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Samples one terrain tile straight from the world generator, like Chunkbase (concept §4.1): per sample the generator's
 * ground height (standard: the preliminary surface level like Chunkbase, {@code fastHeights}; else the exact
 * {@link ChunkGenerator#getBaseHeight}, ocean floor), the noise biome at that height and, with
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

    public static Tile sample(ServerLevel level, int e, int tx, int tz, boolean exploredOnly, int votes, boolean surface) {
        return sample(level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, exploredOnly ? regionFolder(level) : null,
                e, tx, tz, votes, surface, com.simplebuilding.modules.simpleearth.config.EarthServerConfig.fastHeights);
    }

    /** Nanoseconds spent sampling tiles and their count (WD25-22 measurements: ms per tile). */
    public static final java.util.concurrent.atomic.AtomicLong NANOS = new java.util.concurrent.atomic.AtomicLong(), TILES = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.Map<RandomState, DensitySampler> SURFACE = new java.util.WeakHashMap<>();
    private static volatile boolean fastBroken;

    /**
     * WD25-22, the Chunkbase way: the generator's preliminary surface level ({@code noise_router.chunk_surface_level}) - one
     * density function per column instead of the whole noise column of {@link ChunkGenerator#getBaseHeight}. Null for
     * generators without a noise router (flat, void, mods): they use the exact height.
     */
    static DensitySampler surfaceSampler(ChunkGenerator generator, RandomState random) {
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) return null;
        synchronized (SURFACE) {
            return SURFACE.computeIfAbsent(random, r -> r.getSampler(noise.generatorSettings().value().noiseRouter().chunkSurfaceLevel()));
        }
    }

    public static DensitySampler surfaceSamplerForTests(ChunkGenerator generator, RandomState random) {
        return fastBroken ? null : surfaceSampler(generator, random);
    }

    /** Ground height of one column: fast (preliminary surface, Chunkbase-like) or exact (ocean floor of the noise column). */
    public static int height(ChunkGenerator generator, RandomState random, LevelHeightAccessor heights, DensitySampler fast, int x, int z) {
        if (fast != null) return (int) Math.floor(fast.sampleValue(SamplerContext.EMPTY_UNCACHED, x, 0, z));
        return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, random);
    }

    /** Heights first (one generator call per sample), then the colours (biome votes, surface detail from the height field). */
    public static Tile sample(ChunkGenerator generator, RandomState random, LevelHeightAccessor level, Path regions, int e, int tx, int tz,
                              int votes, boolean surface, boolean fastHeights) {
        long started = System.nanoTime();
        DensitySampler fast = fastHeights && !fastBroken ? surfaceSampler(generator, random) : null;
        if (fast != null) {
            try {
                fast.sampleValue(SamplerContext.EMPTY_UNCACHED, 0, 0, 0);
            } catch (RuntimeException ex) {
                // a router whose surface function needs a chunk context: exact heights from now on
                fastBroken = true;
                fast = null;
                com.simplebuilding.modules.simpleearth.SimpleEarth.LOG.warn("Simple Earth: fast heights unavailable ({}), using exact heights", ex.toString());
            }
        }
        BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(random);
        int sea = generator.getSeaLevel();
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
                int h = height(generator, random, level, fast, (int) x, (int) z);
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
        NANOS.addAndGet(System.nanoTime() - started);
        TILES.incrementAndGet();
        return new Tile(colors, heights);
    }

    /**
     * The map colour of one sample: biome vote at the ground (or sea level), then with {@code slope >= 0} the surface detail
     * ({@link EarthColors#surface}) of the biome in the middle of the cell. Tests recompute tiles with it.
     */
    public static MapColor colorAt(BiomeResolver biomes, int x, int h, int z, int sea, int step, int votes, double slope) {
        int y = Math.max(h, sea);
        boolean underwater = h < sea - LAKE_DEPTH;
        // WD25-22: the vote winner doubles as the surface biome (one biome lookup less per sample)
        Holder<Biome> biome = winner(biomes, x, y, z, underwater, step, votes);
        MapColor c = EarthColors.color(biome, underwater);
        return slope < 0 ? c : EarthColors.surface(c, biome, x, h, z, sea, slope);
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
        return EarthColors.color(winner(biomes, x, y, z, underwater, step, votes), underwater);
    }

    /** The biome of a sample cell: a biome with the most frequent colour of the votes (or the middle point, see {@link #color}). */
    public static Holder<Biome> winner(BiomeResolver biomes, int x, int y, int z, boolean underwater, int step, int votes) {
        int qy = QuartPos.fromBlock(y);
        int n = Math.max(1, Math.min(3, votes));
        if (step < 128 || n == 1) return biomes.getNoiseBiome(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z));
        @SuppressWarnings("unchecked") Holder<Biome>[] held = new Holder[n * n];
        MapColor[] list = new MapColor[n * n];
        int k = 0;
        for (int b = 0; b < n; b++) {
            int dz = (int) Math.round((b + 0.5) / n * step - step / 2.0);
            for (int a = 0; a < n; a++) {
                int dx = (int) Math.round((a + 0.5) / n * step - step / 2.0);
                held[k] = biomes.getNoiseBiome(QuartPos.fromBlock(x + dx), qy, QuartPos.fromBlock(z + dz));
                list[k] = EarthColors.color(held[k], underwater);
                k++;
            }
        }
        int best = 0, bestCount = 0;
        for (int i = 0; i < list.length; i++) {
            int count = 0;
            for (MapColor o : list) if (o == list[i]) count++;
            if (count > bestCount) {
                best = i;
                bestCount = count;
            }
        }
        return held[best];
    }

    /** The dimension's region folder (E8: a region file there = explored). */
    public static Path regionFolder(ServerLevel level) {
        return DimensionType.getStorageFolder(level.dimension(), level.getServer().getWorldPath(LevelResource.ROOT)).resolve("region");
    }
}
