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
 * Supply and reward chests of the Backrooms levels (BACK18, BACK22: every level of the stack, {@code BandLayout.containers}) for this
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
        if (!(generator instanceof com.simplebuilding.modules.simplelayers.backrooms.stack.BackroomsStackGenerator)) return false;
        long seed = com.simplebuilding.modules.simplelayers.backrooms.stack.BackroomsStackGenerator.seed(level.getLevel().getChunkSource().randomState());
        int cx = Math.floorDiv(origin.getX(), 16), cz = Math.floorDiv(origin.getZ(), 16);
        boolean any = false;
        // BACK22: one feature for all levels of the stack (each level's containers with its own loot table)
        for (int band = 1; band <= com.simplebuilding.modules.simplelayers.backrooms.stack.BandStack.BANDS; band++) {
            var b = com.simplebuilding.modules.simplelayers.backrooms.stack.Bands.get(band);
            ResourceKey<LootTable> table = b.loot();
            if (table == null) continue;
            java.util.List<int[]> spots = new java.util.ArrayList<>();
            b.layout().containers(seed, cx, cz, spots);
            for (int[] spot : spots) {
                // R3: never in a door chamber (a chest on the arrival spot would block the way)
                if (com.simplebuilding.modules.simplelayers.backrooms.door.DoorNet.inChamber(seed, band, cx, cz, spot[0], spot[1], spot[2])) continue;
                BlockPos at = new BlockPos(spot[0], com.simplebuilding.modules.simplelayers.backrooms.stack.BandStack.worldY(band, spot[1]), spot[2]);
                if (!level.getBlockState(at).isAir() || level.getBlockState(at.below()).isAir()) continue;
                level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), 2);
                RandomizableContainer.setBlockEntityLootTable(level, random, at, table);
                any = true;
            }
        }
        return any;
    }
}
