package com.simplebuilding.enchanting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blazing Obsidian (owner 2026-10-09, queue N27): the glowing sibling of crying obsidian, eight blaze powder around one
 * crying obsidian. As a 5x5 floor under the Astral Enchanting Table it unlocks tiers 40 and 50 ({@link AstralEnchanting}).
 * Instead of tears it now and then lets a small flame rise from a free top face.
 */
public class BlazingObsidianBlock extends Block {
    public BlazingObsidianBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) return;
        BlockPos above = pos.above();
        BlockState up = level.getBlockState(above);
        if (up.canOcclude() && up.isFaceSturdy(level, above, Direction.DOWN)) return;
        level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(),
                0.0, 0.02, 0.0);
    }
}
