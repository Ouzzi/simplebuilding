package com.simplebuilding.modules.simplevault;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.modules.simplevault.block.VaultGateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * The way into the Bedrock Vault (UNDER21): one new Overworld chunk in {@code vault.gapRarity} gets a 2 x 2 gap through the bedrock
 * floor - walls of bedrock, an old ladder down one side, the open Vault Gate at the bottom (on the last bedrock layer, so nobody
 * falls into the void) with its rising motes (A14). Nothing while the vault is off.
 */
public record VaultGapFeature() implements Feature {
    public static final MapCodec<VaultGapFeature> CODEC = MapCodec.unit(VaultGapFeature::new);
    /** Height of the gap above the world floor: gate at +1, open up to +5, bedrock walls around up to +5. */
    public static final int GATE_DY = 1, TOP_DY = 5;

    @Override
    public MapCodec<VaultGapFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!VaultRegistry.on() || VaultConfig.gapRarity <= 0 || random.nextInt(VaultConfig.gapRarity) != 0) return false;
        build(level, new BlockPos(origin.getX(), level.getMinY(), origin.getZ()));
        return true;
    }

    /** Builds the gap with its north-west gate block at {@code floor} + (0, 1, 0); {@code floor} is the world's lowest layer. */
    public static void build(WorldGenLevel level, BlockPos floor) {
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                boolean hole = dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1;
                for (int dy = 0; dy <= TOP_DY; dy++) {
                    BlockPos p = floor.offset(dx, dy, dz);
                    if (!hole || dy == 0) level.setBlock(p, Blocks.BEDROCK.defaultBlockState(), 2);
                    else if (dy == GATE_DY) level.setBlock(p, VaultRegistry.VAULT_GATE.defaultBlockState().setValue(VaultGateBlock.OPEN, true), 2);
                    else if (dx == 0 && dz == 0) level.setBlock(p, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), 2);
                    else level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }
}
