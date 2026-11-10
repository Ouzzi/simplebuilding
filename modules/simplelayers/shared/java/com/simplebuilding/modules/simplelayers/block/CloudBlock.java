package com.simplebuilding.modules.simplelayers.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cloud Block (concept §1.1): full collision, breaks instantly by hand, lets light through (no shadow on the ground far below)
 * and softens a landing like the Hay Bale - a DAMAGE factor in {@link #fallOn} (Cloud Block x0.7 = -30 %), not a shorter fall.
 * CLOUDS17: renders like Glass - the texture has clear gaps (alpha 0), so 26.3 puts it in the cutout chunk layer.
 */
public class CloudBlock extends TransparentBlock {
    private final float damageFactor;

    public CloudBlock(Properties properties, float damageFactor) {
        super(properties);
        this.damageFactor = damageFactor;
    }

    public float damageFactor() {
        return damageFactor;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, damageFactor, level.damageSources().fall());
    }

    /**
     * Clouds and mist merge into one surface (the mist plug of a pit looks like the cloud around it). Toward Cloud Stone the face stays
     * (CLOUDS17, like Glass toward any other block): the core skips its translucent face there instead, and through the gaps one sees
     * the shell on the far side, not the sky through the whole island.
     */
    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return neighbor.getBlock() instanceof CloudBlock || neighbor.getBlock() instanceof MistBlock || super.skipRendering(state, neighbor, direction);
    }
}
