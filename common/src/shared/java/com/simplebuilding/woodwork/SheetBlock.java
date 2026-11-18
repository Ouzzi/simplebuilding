package com.simplebuilding.woodwork;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A wooden sheet: a pane of wood that connects like iron bars; an axe strips it (N100, connections kept). */
public class SheetBlock extends IronBarsBlock {
    public SheetBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        InteractionResult stripped = WoodBlocks.strip(stack, state, level, pos, player, hand, hit);
        return stripped != null ? stripped : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
