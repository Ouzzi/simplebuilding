package com.simplebuilding.modules.simplelayers.world;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
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

/** Sky chest (concept §1.1 "Belohnung, klein"): now and then a chest with Vanilla loot on a cloud island's surface. */
public record SkyChestFeature() implements Feature {
    public static final MapCodec<SkyChestFeature> CODEC = MapCodec.unit(SkyChestFeature::new);
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, SimpleLayers.id("chests/sky_chest"));

    @Override
    public MapCodec<SkyChestFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int top = MistPitFeature.surface(level, origin.getX(), origin.getZ());
        if (top == Integer.MIN_VALUE) return false;
        BlockPos at = new BlockPos(origin.getX(), top + 1, origin.getZ());
        if (!level.getBlockState(at).isAir() || !level.getBlockState(at.below()).is(LayersRegistry.CLOUD_BLOCK)) return false;
        level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, at, LOOT);
        return true;
    }
}
