package com.simpleores.circuit;

import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Astral / Bridge Redstone item (N46, CIRC46 F4): on the top of a block it lays floor dust as before, on a side or the
 * underside it sticks the wall/ceiling dust ({@link WallSignalDustBlock}) to that face. Falls back to floor dust when the face
 * cannot hold it.
 */
public class LineDustItem extends BlockItem {
    private final Block wall;

    public LineDustItem(Block floor, Block wall, Item.Properties properties) {
        super(floor, properties);
        this.wall = wall;
    }

    @Override protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face != Direction.UP) {
            BlockState state = wall.defaultBlockState().setValue(WallSignalDustBlock.FACING, face);
            if (state.canSurvive(context.getLevel(), context.getClickedPos()) && unobstructed(context, state)) return state;
        }
        return super.getPlacementState(context);
    }

    private static boolean unobstructed(BlockPlaceContext context, BlockState state) {
        return context.getLevel().isUnobstructed(state, context.getClickedPos(), net.minecraft.world.phys.shapes.CollisionContext.empty());
    }

    @Override public void registerBlocks(Map<Block, Item> map, Item item) {
        super.registerBlocks(map, item);
        map.put(wall, item);
    }
}
