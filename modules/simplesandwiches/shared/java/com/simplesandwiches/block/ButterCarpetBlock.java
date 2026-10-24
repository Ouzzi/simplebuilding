package com.simplesandwiches.block;

import com.simplesandwiches.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A butter slice laid flat (1 px like a Vanilla carpet, butter friction): walking in/on it (sinks 1 px, slowed) makes the walker Slippery. */
public class ButterCarpetBlock extends CarpetBlock {
    public ButterCarpetBlock(Properties properties) {
        super(properties);
    }

    /** No collision (WA24-3): the walker sinks in a little and is slowed, so contact is "inside" the carpet, not "on" it. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
        ModEffects.slip(level, entity);
    }
}
