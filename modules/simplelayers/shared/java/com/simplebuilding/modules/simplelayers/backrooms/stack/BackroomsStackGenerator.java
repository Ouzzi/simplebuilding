package com.simplebuilding.modules.simplelayers.backrooms.stack;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.modules.simplelayers.backrooms.api.Band;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandLayout;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandPiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.CorePiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.DoorEnd;
import com.simplebuilding.modules.simplelayers.backrooms.door.DoorStamp;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import org.jspecify.annotations.Nullable;

/**
 * Chunk generator {@code simplelayers:backrooms_stack} (BACK22 core K1, §4.2): one chunk = a column through all ten levels. Order:
 * (1) deck, separators and floor as single-valued bedrock sections, the hull sections a level reports likewise (no 4096 writes,
 * least memory - concept §3.1); (2) every level writes its chunk through {@link BandLayout#fill} (unlocked section writes, like
 * Vanilla's noise generator); (3) the door pairs of this chunk are stamped ({@link DoorStamp}); (4) heightmaps only for the deck.
 * Containers come from the {@code backrooms_cache} feature. Seed = world seed from the random state.
 */
public class BackroomsStackGenerator extends ChunkGenerator {
    public static final MapCodec<BackroomsStackGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)).apply(i, BackroomsStackGenerator::new));
    private static final Strategy<BlockState> STATES = Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY);

    public BackroomsStackGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    public static long seed(RandomState randomState) {
        return randomState.seed();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** The block of the stack at world (x, y, z) without doors (tests, base column). */
    public static BlockState block(long seed, int x, int y, int z) {
        int band = BandStack.bandAt(y);
        if (band == 0) return y >= BandStack.MIN_Y && y <= BandStack.MAX_Y ? Blocks.BEDROCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
        Band b = Bands.get(band);
        return b.state(b.layout().at(seed, x, BandStack.localY(y), z));
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structureManager,
                                                       BiomeManager biomeManager, @Nullable WorldGenRegion carverBiomeRegion, Set<Holder<Biome>> possibleBiomes) {
        fillChunk(chunk, seed(randomState));
        return CompletableFuture.completedFuture(chunk);
    }

    /** Fills {@code chunk} (any height accessor that covers the stack); returns the number of single-valued sections written. */
    public static int fillChunk(ChunkAccess chunk, long seed) {
        int cx = chunk.getPos().x(), cz = chunk.getPos().z();
        LevelChunkSection[] sections = chunk.getSections();
        int minSection = chunk.getMinSectionY(), single = 0;
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        // (1) bedrock and hull sections, single-valued
        for (int i = 0; i < sections.length; i++) {
            int y0 = (minSection + i) * 16;
            int band = BandStack.bandAt(y0);
            BlockState fill = null;
            if (band == 0) fill = bedrock;
            else {
                Band b = Bands.get(band);
                if (b.layout().hullSection(seed, cx, cz, BandStack.localY(y0) >> 4)) fill = b.state(CorePiece.HULL);
            }
            if (fill != null) {
                sections[i] = new LevelChunkSection(new PalettedContainer<>(fill, STATES), sections[i].getBiomes());
                single++;
            }
        }
        for (LevelChunkSection s : sections) s.acquire();
        try {
            // (2) the levels, (3) their doors
            for (int band = 1; band <= BandStack.BANDS; band++) {
                Band b = Bands.get(band);
                int base = BandStack.baseY(band);
                SectionWriter w = new SectionWriter(sections, minSection, base, b);
                b.layout().fill(seed, cx, cz, w);
                DoorStamp.stamp(seed, band, cx, cz, (x, ly, z, state) -> w.put(x & 15, ly, z & 15, state));
            }
        } finally {
            for (LevelChunkSection s : sections) s.release();
        }
        // (4) heightmaps: the deck is the top of every column
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                ocean.update(x, BandStack.MAX_Y, z, bedrock);
                surface.update(x, BandStack.MAX_Y, z, bedrock);
            }
        }
        return single;
    }

    /** Writes a level's pieces straight into the chunk's sections (sections acquired by the caller). */
    record SectionWriter(LevelChunkSection[] sections, int minSection, int base, Band band) implements com.simplebuilding.modules.simplelayers.backrooms.api.BandWriter {
        @Override
        public void set(int x, int ly, int z, BandPiece piece) {
            if (piece == CorePiece.AIR) return;
            BlockState state = band.state(piece);
            if (state.isAir()) return;
            put(x, ly, z, state);
        }

        void put(int x, int ly, int z, BlockState state) {
            if (ly < 0 || ly >= BandLayout.HEIGHT) return;
            int y = base + ly;
            int i = (y >> 4) - minSection;
            if (i < 0 || i >= sections.length) return;
            sections[i].setBlockState(x, y & 15, z, state, false);
        }
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        return BandStack.MAX_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        long seed = seed(randomState);
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        for (int i = 0; i < states.length; i++) states[i] = block(seed, x, heightAccessor.getMinY() + i, z);
        return new NoiseColumn(heightAccessor.getMinY(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
        int band = BandStack.bandAt(feetPos.getY());
        result.add("Backrooms level " + band + (band == 0 ? " (bedrock)" : " " + Bands.get(band).id() + " ly " + BandStack.localY(feetPos.getY())));
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getMinY() {
        return BandStack.MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return BandStack.HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return BandStack.MIN_Y;
    }
}
