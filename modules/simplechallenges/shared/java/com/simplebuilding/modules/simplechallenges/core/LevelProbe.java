package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.price.BiomeMaterials;
import com.simplebuilding.modules.simplechallenges.price.BlockValues;
import com.simplebuilding.modules.simplechallenges.price.CellInfo;
import com.simplebuilding.modules.simplechallenges.price.ReachableBlocks;
import com.simplebuilding.modules.simplechallenges.price.WeightedItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Looks at the world around one cell for the price engine and never generates a chunk: loaded chunks give the real surface
 * blocks (so a tree at the spawn counts as wood), anything else is estimated from the biome and the generator's ground
 * height. At most 64 columns are read per cell.
 */
public final class LevelProbe {
    private static final int LAYERS = 6;

    private LevelProbe() {}

    public static CellInfo cell(ServerLevel level, int cell, int cx, int cz) {
        int stride = Math.max(1, cell / 8);
        int x0 = cx * cell, z0 = cz * cell;
        Map<String, Integer> counts = new LinkedHashMap<>();
        int surface = Integer.MIN_VALUE;
        double column = -1;
        for (int dx = stride / 2; dx < cell; dx += stride) {
            for (int dz = stride / 2; dz < cell; dz += stride) {
                int x = x0 + dx, z = z0 + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null) continue;
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                surface = Math.max(surface, top);
                for (int y = top + 1; y > top + 1 - LAYERS; y--) {
                    String item = ReachableBlocks.itemFor(chunk.getBlockState(new BlockPos(x, y, z)));
                    if (item != null) counts.merge(item, 1, Integer::sum);
                }
                if (cell == 1) column = columnValue(level, chunk, x, z, top);
            }
        }
        int cxm = x0 + cell / 2, czm = z0 + cell / 2;
        List<WeightedItem> items;
        if (counts.isEmpty()) {
            items = BiomeMaterials.itemsFor(biomePath(level, cxm, czm));
        } else {
            items = new ArrayList<>();
            for (Map.Entry<String, Integer> e : counts.entrySet()) items.add(new WeightedItem(e.getKey(), e.getValue(), 64));
        }
        if (cell == 1 && column < 0) column = estimateColumn(level, x0, z0);
        return new CellInfo(items, Math.max(0, column), surface == Integer.MIN_VALUE ? level.getSeaLevel() : surface);
    }

    private static double columnValue(ServerLevel level, LevelChunk chunk, int x, int z, int top) {
        BlockValues values = BlockValues.current();
        double sum = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= level.getMinY(); y--) {
            BlockState state = chunk.getBlockState(pos.set(x, y, z));
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;
            sum += values.value(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        }
        return sum;
    }

    /** A column nobody generated yet: plain stone down to the generator's ground height (ores are unknown before generation). */
    private static double estimateColumn(ServerLevel level, int x, int z) {
        try {
            var gen = level.getChunkSource().getGenerator();
            int height = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
            return Math.max(1, (height - level.getMinY()) * BlockValues.current().value("minecraft:stone"));
        } catch (RuntimeException e) {
            return 1;
        }
    }

    static String biomePath(ServerLevel level, int x, int z) {
        try {
            return level.getUncachedNoiseBiome(x >> 2, level.getSeaLevel() >> 2, z >> 2).unwrapKey()
                    .map(k -> k.identifier().getPath()).orElse("");
        } catch (RuntimeException e) {
            return "";
        }
    }

}
