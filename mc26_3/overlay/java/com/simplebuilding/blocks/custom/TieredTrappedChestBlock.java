package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla trapped-chest signaling with the tier chest's own menu and opener counter. */
public class TieredTrappedChestBlock extends TieredChestBlock {
    public TieredTrappedChestBlock(TieredChestBlock normal, BlockBehaviour.Properties properties) {
        super(normal.tier(), normal.getOpenChestSound(), normal.getCloseChestSound(), properties);
    }

    @Override
    public boolean isTrapped() {
        return true;
    }

    @Override
    protected Stat<Identifier> getOpenChestStat() {
        return Stats.CUSTOM.get(Stats.TRIGGER_TRAPPED_CHEST);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TieredChestBlockEntity chest
                ? Mth.clamp(chest.openerCount(), 0, 15) : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == Direction.UP ? state.getSignal(level, pos, direction) : 0;
    }
}
