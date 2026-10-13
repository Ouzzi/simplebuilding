package com.simplebuilding.modules.simpleend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * End leaves (owner 2026-10-13): see-through tendril fronds around a small crystal (own model, cutout, never a full
 * cube) with falling tinted leaf particles and now and then a drifting End-rod spark. {@link #DROP} marks the few leaves
 * a growing tree chose to carry its bud (1) or fruit (2); the loot table drops them always. Placed leaves carry nothing.
 */
public class EndLeavesBlock extends TintedParticleLeavesBlock {
    public static final IntegerProperty DROP = IntegerProperty.create("drop", 0, 2);
    public static final int NONE = 0, BUD = 1, FRUIT = 2;

    public EndLeavesBlock(Properties properties) {
        super(0.01F, properties);
        registerDefaultState(defaultBlockState().setValue(DROP, NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DROP);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(160) == 0 && level.getBlockState(pos.below()).isAir()) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(),
                    0.0, -0.01, 0.0);
        }
    }
}
