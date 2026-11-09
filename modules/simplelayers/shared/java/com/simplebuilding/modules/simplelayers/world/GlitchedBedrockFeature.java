package com.simplebuilding.modules.simplelayers.world;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * The Backrooms entrance in the Overworld (concept §2.1): one new chunk in {@code backrooms.entranceRarity} turns the topmost
 * bedrock block of one column of its bedrock floor (the one a miner reaches first) into Glitched Bedrock. Nothing while the
 * Backrooms are off.
 */
public record GlitchedBedrockFeature() implements Feature {
    public static final MapCodec<GlitchedBedrockFeature> CODEC = MapCodec.unit(GlitchedBedrockFeature::new);

    @Override
    public MapCodec<GlitchedBedrockFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!SimpleLayers.backroomsOn() || random.nextInt(LayersConfig.entranceRarity) != 0) return false;
        BlockPos at = topBedrock(level, origin.getX(), origin.getZ());
        if (at == null) return false;
        level.setBlock(at, LayersRegistry.GLITCHED_BEDROCK.defaultBlockState(), 2);
        return true;
    }

    /** The highest bedrock of the floor layers (bottom 6 blocks) that has a non-bedrock block above it, or null. */
    static BlockPos topBedrock(WorldGenLevel level, int x, int z) {
        int min = level.getMinY();
        for (int y = min + 5; y >= min; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (level.getBlockState(p).is(Blocks.BEDROCK) && !level.getBlockState(p.above()).is(Blocks.BEDROCK)) return p;
        }
        return null;
    }
}
