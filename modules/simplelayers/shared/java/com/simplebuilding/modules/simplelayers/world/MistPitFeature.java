package com.simplebuilding.modules.simplelayers.world;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Mist pit (concept §1.1): a plug of Mist Blocks (1x1 to 3x3, 2-6 deep) in the top of a cloud island. Never over the island's
 * rim: every column of the pit and a 2-block ring around it must be island surface (within 2 of the pit's height) and thick
 * enough. Below the plug (owner question 1, answered by the concept/plan: not bottomless) lies a hollow 3 high with the island's
 * own floor under it; with {@code clouds.bottomlessPits} ("hard") the hollow goes right through the island instead.
 */
public record MistPitFeature() implements Feature {
    public static final MapCodec<MistPitFeature> CODEC = MapCodec.unit(MistPitFeature::new);
    /** The pit, the hollow and at least two floor blocks. */
    static final int FLOOR = 2, HOLLOW = 3;

    @Override
    public MapCodec<MistPitFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int top = surface(level, origin.getX(), origin.getZ());
        if (top == Integer.MIN_VALUE) return false;
        int size = 1 + random.nextInt(3), depth = 2 + random.nextInt(5);
        int x0 = origin.getX() - size / 2, z0 = origin.getZ() - size / 2;
        int need = depth + HOLLOW + FLOOR;
        for (int dx = -2; dx < size + 2; dx++) {
            for (int dz = -2; dz < size + 2; dz++) {
                int t = surface(level, x0 + dx, z0 + dz);
                if (t == Integer.MIN_VALUE || Math.abs(t - top) > 2) return false;
                if (thickness(level, x0 + dx, t, z0 + dz) < need + 1) return false;
            }
        }
        BlockState mist = LayersRegistry.MIST_BLOCK.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                for (int d = 0; d < depth; d++) level.setBlock(p.set(x0 + dx, top - d, z0 + dz), mist, 2);
                if (LayersConfig.bottomlessPits) {
                    for (int y = top - depth; y > level.getMinY() && !level.getBlockState(p.set(x0 + dx, y, z0 + dz)).isAir(); y--) level.setBlock(p, air, 2);
                }
            }
        }
        if (!LayersConfig.bottomlessPits) {
            for (int dx = -1; dx <= size; dx++) {
                for (int dz = -1; dz <= size; dz++) {
                    for (int h = 0; h < HOLLOW; h++) level.setBlock(p.set(x0 + dx, top - depth - h, z0 + dz), air, 2);
                }
            }
        }
        return true;
    }

    /** Height of the top Cloud Block of the column with air above it, or MIN_VALUE. */
    static int surface(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, Math.min(level.getMaxY(), 300), z);
        for (; p.getY() > level.getMinY(); p.move(0, -1, 0)) {
            BlockState s = level.getBlockState(p);
            if (s.isAir()) continue;
            return s.is(LayersRegistry.CLOUD_BLOCK) ? p.getY() : Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    static int thickness(WorldGenLevel level, int x, int top, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, top, z);
        int n = 0;
        while (p.getY() > level.getMinY() && !level.getBlockState(p).isAir() && n < 64) {
            n++;
            p.move(0, -1, 0);
        }
        return n;
    }
}
