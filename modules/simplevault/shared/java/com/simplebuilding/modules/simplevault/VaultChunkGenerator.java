package com.simplebuilding.modules.simplevault;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.modules.simplevault.gen.VaultGen;
import com.simplebuilding.modules.simplevault.gen.VaultLayout;
import com.simplebuilding.modules.simplevault.gen.VaultPiece;
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
 * Chunk generator {@code simplevault:bedrock_vault} (UNDER21): every chunk from the pure {@link VaultGen} - solid bedrock with
 * one vault complex per 128 x 128 region, cladding in front of every bedrock face that borders the rooms. Seed = world seed.
 */
public class VaultChunkGenerator extends ChunkGenerator {
    public static final MapCodec<VaultChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)).apply(i, VaultChunkGenerator::new));

    public VaultChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

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
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        int y0 = Math.max(0, chunk.getMinY()), y1 = Math.min(VaultGen.HEIGHT - 1, chunk.getMaxY());
        VaultLayout layout = VaultLayout.at(seed, baseX, baseZ);
        int lo = Math.max(y0, layout.minY() - 2), hi = Math.min(y1, layout.maxY() + 2);
        VaultPiece[] mid = VaultGen.fill(seed, baseX, lo, baseZ, 16, hi - lo + 1, 16);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockState bedrock = VaultBlocks.state(VaultPiece.BEDROCK);
        for (int y = y0; y <= y1; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = y >= lo && y <= hi ? VaultBlocks.state(mid[((y - lo) * 16 + z) * 16 + x]) : bedrock;
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
        return VaultGen.HEIGHT;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        long seed = seed(randomState);
        int h = heightAccessor.getHeight(), min = heightAccessor.getMinY();
        BlockState[] states = new BlockState[h];
        int from = Math.max(0, min), to = Math.min(VaultGen.HEIGHT - 1, min + h - 1);
        VaultPiece[] col = VaultGen.fill(seed, x, from, z, 1, to - from + 1, 1);
        for (int i = 0; i < h; i++) {
            int y = min + i;
            states[i] = y >= from && y <= to ? VaultBlocks.state(col[y - from]) : VaultBlocks.state(VaultPiece.AIR);
        }
        return new NoiseColumn(min, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
        VaultLayout l = VaultLayout.at(seed(randomState), feetPos.getX(), feetPos.getZ());
        VaultLayout.Room r = l.roomAt(feetPos.getX(), feetPos.getY(), feetPos.getZ());
        result.add("Bedrock Vault " + l.rx + " " + l.rz + (r == null ? "" : " " + r.type()));
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
        return VaultGen.HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }
}
