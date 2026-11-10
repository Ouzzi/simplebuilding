package com.simplebuilding.modules.simplelayers.block;

import com.simplebuilding.modules.simplelayers.world.BackroomsTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rift: what a broken Glitched Bedrock leaves behind (and, BACK18, the portal floor of the entrance shaft and the noclip spots and
 * floor holes inside the Backrooms - where it leads: {@link BackroomsTravel#enterFromRift}) (plan deviation, reason: lasts across restarts and works for every entity -
 * the concept's "fall into the void below"). No collision, unbreakable, faint light; whatever enters it lands in the Backrooms.
 */
public class RiftBlock extends Block {
    public RiftBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
        if (level instanceof ServerLevel server) BackroomsTravel.enterFromRift(server, entity, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0.0, -0.02, 0.0);
        }
    }
}
