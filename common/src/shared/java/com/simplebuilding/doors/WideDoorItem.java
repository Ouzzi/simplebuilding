package com.simplebuilding.doors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * Item of a Simple Door. A door needs a free {@code cols x rows} area, so the clicked cell often does not fit by a block (the
 * floor is one cell lower, the second column is blocked on one side). When the clicked cell fails, the nearest cells within one
 * column sideways and two cells down are tried before the placement is refused (IG31-9: the 2x3 door was hard to place).
 */
public final class WideDoorItem extends BlockItem {
    public WideDoorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        if (getPlacementState(context) != null) return context;
        BlockPos base = context.getClickedPos();
        Direction f = context.getHorizontalDirection();
        for (int dy = 0; dy >= -2; dy--) {
            for (Direction side : new Direction[] {null, f.getClockWise(), f.getCounterClockWise()}) {
                if (dy == 0 && side == null) continue;
                BlockPos p = side == null ? base.above(dy) : base.above(dy).relative(side);
                if (!context.getLevel().getBlockState(p).isAir()) continue;
                BlockPlaceContext shifted = BlockPlaceContext.at(context, p, Direction.UP);
                if (getPlacementState(shifted) != null) return shifted;
            }
        }
        return context;
    }
}
