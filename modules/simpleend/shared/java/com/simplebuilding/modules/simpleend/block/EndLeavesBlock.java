package com.simplebuilding.modules.simpleend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

/**
 * End leaves (owner 2026-10-13): see-through tendril fronds around a small crystal (own model, cutout, never a full
 * cube) with falling tinted leaf particles and now and then a drifting End-rod spark. {@link #DROP} marks the few leaves
 * a growing tree chose to carry its bud (1) or fruit (2); the loot table drops them always. Placed leaves carry nothing.
 * Ashwood leaves ({@code web}, owner N55) behave like a cobweb: no collision, whatever moves through is stuck
 * ({@link Entity#makeStuckInBlock}, which also resets the fall distance, so landing in them hurts nothing).
 */
public class EndLeavesBlock extends TintedParticleLeavesBlock {
    public static final IntegerProperty DROP = IntegerProperty.create("drop", 0, 2);
    public static final int NONE = 0, BUD = 1, FRUIT = 2;

    private final boolean web;

    public EndLeavesBlock(Properties properties) {
        this(false, properties);
    }

    public EndLeavesBlock(boolean web, Properties properties) {
        super(0.01F, properties);
        this.web = web;
        registerDefaultState(defaultBlockState().setValue(DROP, NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DROP);
    }

    public boolean isWeb() {
        return web;
    }

    /** Same numbers as Vanilla {@code WebBlock#entityInside}. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!web) {
            super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
            return;
        }
        Vec3 speed = new Vec3(0.25, 0.05F, 0.25);
        if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.WEAVING)) speed = new Vec3(0.5, 0.25, 0.5);
        entity.makeStuckInBlock(state, speed);
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
