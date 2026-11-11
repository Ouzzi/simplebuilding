package com.simplebuilding.modules.simplevault;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.modules.simplevault.SimpleVault;
import com.simplebuilding.modules.simplevault.gen.VaultLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/** The forgotten supply barrel in the vault's Entrance Hall (UNDER21): bread, kelp, a torch or two - enough for the riddles. */
public record VaultCacheFeature() implements Feature {
    public static final MapCodec<VaultCacheFeature> CODEC = MapCodec.unit(VaultCacheFeature::new);
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, SimpleVault.id("vault/supplies"));

    @Override
    public MapCodec<VaultCacheFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!(generator instanceof VaultChunkGenerator)) return false;
        long seed = VaultChunkGenerator.seed(level.getLevel().getChunkSource().randomState());
        int x0 = Math.floorDiv(origin.getX(), 16) * 16, z0 = Math.floorDiv(origin.getZ(), 16) * 16;
        int[] c = VaultLayout.at(seed, x0, z0).cache;
        if (c == null || c[0] < x0 || c[0] >= x0 + 16 || c[2] < z0 || c[2] >= z0 + 16) return false;
        BlockPos at = new BlockPos(c[0], c[1], c[2]);
        if (!level.getBlockState(at).isAir()) return false;
        level.setBlock(at, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, at, LOOT);
        return true;
    }
}
