package com.simplebuilding.modules.simplelayers.world;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import com.simplebuilding.modules.simplelayers.world.gen.Piece;
import com.simplebuilding.modules.simplelayers.world.gen.ShaftTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * The Backrooms entrance shaft in the Overworld (BACK18, plan docs/ai/PLAN-B-BACK18-2026-11-10.md): one new chunk in
 * {@code backrooms.shaftRarity} (0 = never) gets the fenced-off maintenance shaft of {@link ShaftTemplate} in its middle, only on
 * nearly flat, dry ground. Its rift floor is the portal into Level 0. The Glitched Bedrock stays as the hidden second way in.
 */
public record EntranceShaftFeature() implements Feature {
    public static final MapCodec<EntranceShaftFeature> CODEC = MapCodec.unit(EntranceShaftFeature::new);
    /** Largest height difference of the ground under the barrier (blocks). */
    public static final int MAX_SLOPE = 2;

    @Override
    public MapCodec<EntranceShaftFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!SimpleLayers.backroomsOn() || LayersConfig.shaftRarity <= 0 || random.nextInt(LayersConfig.shaftRarity) != 0) return false;
        int cx = Math.floorDiv(origin.getX(), 16) * 16 + 8, cz = Math.floorDiv(origin.getZ(), 16) * 16 + 8;
        int r = ShaftTemplate.RADIUS - 1, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx + dx, cz + dz);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        if (max - min > MAX_SLOPE) return false;
        BlockPos ground = new BlockPos(cx, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz) - 1, cz);
        BlockState top = level.getBlockState(ground);
        if (!top.getFluidState().isEmpty() || top.isAir() || ground.getY() - ShaftTemplate.DEPTH <= level.getMinY() + 1) return false;
        build(level, ground);
        return true;
    }

    /** Builds the shaft with its pad (dy 0) at {@code ground}; the gametest calls this directly. */
    public static void build(LevelAccessor level, BlockPos ground) {
        int r = ShaftTemplate.RADIUS;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = -ShaftTemplate.DEPTH; dy <= ShaftTemplate.HEIGHT; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    Piece piece = ShaftTemplate.piece(dx, dy, dz);
                    if (piece == null) continue;
                    level.setBlock(p.set(ground.getX() + dx, ground.getY() + dy, ground.getZ() + dz), BackroomsBlocks.state(piece), 2);
                }
            }
        }
    }
}
