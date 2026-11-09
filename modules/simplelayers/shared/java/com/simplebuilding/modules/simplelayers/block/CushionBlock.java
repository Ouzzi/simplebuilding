package com.simplebuilding.modules.simplelayers.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cloud Stone (concept §1.1): the island core that takes 10 % off fall damage (damage factor x0.9 like the Hay Bale's x0.2).
 * CLOUDS15: lets sky light through like Ice (registered with noOcclusion, full shape -> Vanilla light dampening 1), so islands
 * cast a soft shadow instead of a black one; the faces between core blocks are skipped so big islands do not render their inside.
 */
public class CushionBlock extends Block {
    private final float damageFactor;

    public CushionBlock(Properties properties, float damageFactor) {
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

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return neighbor.getBlock() instanceof CushionBlock || super.skipRendering(state, neighbor, direction);
    }
}
