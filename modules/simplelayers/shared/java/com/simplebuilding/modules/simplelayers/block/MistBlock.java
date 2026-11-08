package com.simplebuilding.modules.simplelayers.block;

import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mist Block (concept §1.1): looks almost like a cloud but has no collision - walk and fall through it (the plug of a mist pit).
 * Not replaceable (building does not overwrite it), not suffocating, does not block the view; drops only with Silk Touch.
 * The hint (A14, config {@code clouds.fogHint}): a slightly see-through rim in the texture and a rare haze sinking from its
 * underside while there is air below.
 */
public class MistBlock extends TransparentBlock {
    public MistBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return neighbor.getBlock() instanceof MistBlock || neighbor.getBlock() instanceof CloudBlock || super.skipRendering(state, neighbor, direction);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!LayersConfig.fogHint || random.nextInt(14) != 0 || !level.getBlockState(pos.below()).isAir()) return;
        level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, state),
                pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
    }
}
