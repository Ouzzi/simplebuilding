package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A trapped copper chest's contents; like Vanilla's trapped chest it updates its neighbours when the viewer count changes. */
public class TrappedCopperChestBlockEntity extends ChestBlockEntity {
    public TrappedCopperChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAPPED_COPPER_CHEST_BE, pos, state);
    }

    @Override
    protected void signalOpenCount(Level level, BlockPos pos, BlockState state, int previous, int current) {
        super.signalOpenCount(level, pos, state, previous, current);
        if (previous != current) {
            var orientation = net.minecraft.world.level.redstone.ExperimentalRedstoneUtils.initialOrientation(
                    level, state.getValue(ChestBlock.FACING).getOpposite(), Direction.UP);
            level.updateNeighborsAt(pos, state.getBlock(), orientation);
            level.updateNeighborsAt(pos.below(), state.getBlock(), orientation);
        }
    }
}
