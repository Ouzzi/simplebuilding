package com.simpleblocks.pale;

import com.simplelib.api.aging.Aging;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The one place where a pale pumpkin differs from the Vanilla pumpkin (the "own method" of an aging chain, DW04B-1):
 * Vanilla's shears carve it into a plain carved pumpkin; this keeps the stage and the wax (faded pumpkin -> faded carved
 * pumpkin). Seeds, sound, durability and stats stay Vanilla's.
 */
public class PalePumpkinBlock extends PumpkinBlock {
    public PalePumpkinBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        InteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hit);
        BlockState now = level.getBlockState(pos);
        if (!level.isClientSide() && now.is(Blocks.CARVED_PUMPKIN)) level.setBlock(pos, Aging.sameAge(state, now), Block.UPDATE_ALL);
        return result;
    }
}
