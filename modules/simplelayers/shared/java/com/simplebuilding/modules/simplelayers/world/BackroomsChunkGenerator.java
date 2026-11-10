package com.simplebuilding.modules.simplelayers.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import org.jspecify.annotations.Nullable;

/**
 * Chunk generator {@code simplelayers:backrooms}: fills every chunk from the pure generator of its {@link BackroomsLevel} (field
 * {@code level}, BACK18; missing = {@code lobby}, so the LAYERS10 dimension file stays valid), solid bedrock up to y 63. The
 * seed is the world seed, read from the {@link RandomState} handed to every call. Structures (the Lobby's special rooms) and
 * features (supply chests) run as for any generator.
 */
public class BackroomsChunkGenerator extends ChunkGenerator {
    public static final MapCodec<BackroomsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            Codec.STRING.optionalFieldOf("level", BackroomsLevel.LOBBY.id).forGetter(g -> g.level.id))
            .apply(i, (source, level) -> new BackroomsChunkGenerator(source, BackroomsLevel.byId(level))));

    private final BackroomsLevel level;

    public BackroomsChunkGenerator(BiomeSource biomeSource, BackroomsLevel level) {
        super(biomeSource);
        this.level = level;
    }

    public BackroomsLevel level() {
        return level;
    }

    /** The block of this level at (x, y, z). */
    public static BlockState block(BackroomsLevel level, long seed, int x, int y, int z) {
        return BackroomsBlocks.state(level.at(seed, x, y, z));
    }

    /** The grid seed of a world (stable per world seed; the random state carries it). */
    public static long seed(RandomState randomState) {
        return randomState.seed();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structureManager,
                                                       BiomeManager biomeManager, @Nullable WorldGenRegion carverBiomeRegion, Set<Holder<Biome>> possibleBiomes) {
        long seed = seed(randomState);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        int top = Math.min(BackroomsGrid.HULL_TOP_Y, chunk.getMaxY());
        for (int y = Math.max(0, chunk.getMinY()); y <= top; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockState state = block(level, seed, baseX + x, y, baseZ + z);
                    if (state.isAir()) continue;
                    chunk.setBlockState(pos.set(x, y, z), state);
                    oceanFloor.update(x, y, z, state);
                    surface.update(x, y, z, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        return BackroomsGrid.HULL_TOP_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        long seed = seed(randomState);
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = heightAccessor.getMinY() + i;
            states[i] = block(level, seed, x, y, z);
        }
        return new NoiseColumn(heightAccessor.getMinY(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
        result.add("Backrooms " + level.id + " cell " + Math.floorDiv(feetPos.getX(), BackroomsGrid.CELL) + " " + Math.floorDiv(feetPos.getZ(), BackroomsGrid.CELL));
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getGenDepth() {
        return 64;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }
}
