package com.simplebuilding.modules.simplelayers.world;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Supply and reward chests of the deeper Backrooms levels (BACK18): at the spots {@link BackroomsLevel#caches} names for this
 * chunk, a chest with the level's loot table ({@code simplelayers:chests/backrooms_<level>}). Runs in every level's biome; does
 * nothing outside a Backrooms generator.
 */
public record BackroomsCacheFeature() implements Feature {
    public static final MapCodec<BackroomsCacheFeature> CODEC = MapCodec.unit(BackroomsCacheFeature::new);

    public static ResourceKey<LootTable> loot(BackroomsLevel level) {
        return ResourceKey.create(Registries.LOOT_TABLE, SimpleLayers.id("chests/backrooms_" + level.id));
    }

    @Override
    public MapCodec<BackroomsCacheFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!(generator instanceof BackroomsChunkGenerator backrooms)) return false;
        long seed = BackroomsChunkGenerator.seed(level.getLevel().getChunkSource().randomState());
        int x0 = Math.floorDiv(origin.getX(), 16) * 16, z0 = Math.floorDiv(origin.getZ(), 16) * 16;
        boolean any = false;
        for (int[] spot : backrooms.level().caches(seed, x0, z0, 16)) {
            BlockPos at = new BlockPos(spot[0], spot[1], spot[2]);
            if (!level.getBlockState(at).isAir() || level.getBlockState(at.below()).isAir()) continue;
            level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), 2);
            RandomizableContainer.setBlockEntityLootTable(level, random, at, loot(backrooms.level()));
            any = true;
        }
        return any;
    }
}
