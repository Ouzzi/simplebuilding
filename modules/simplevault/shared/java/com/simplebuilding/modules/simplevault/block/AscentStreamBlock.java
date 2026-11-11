package com.simplebuilding.modules.simplevault.block;

import com.simplebuilding.modules.simplevault.VaultTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ascent Stream (UNDER21): the invisible rising column the solved altar opens through the dome shaft. Lifts whatever stands in it
 * (Slow Falling against a fall when one drifts out, the vault never hurts) and at the top of the shaft hands players over to
 * the Overworld ({@link VaultTravel#ascend}). Rising motes show it (A14).
 */
public class AscentStreamBlock extends Block {
    public AscentStreamBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x * 0.5, Math.max(v.y, 0.45), v.z * 0.5);
        entity.resetFallDistance();
        if (level instanceof ServerLevel server) {
            if (entity instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, true, false, true));
            VaultTravel.ascend(server, entity, pos);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + random.nextDouble(), pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.12, 0.0);
        }
    }
}
