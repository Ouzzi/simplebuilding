package com.simplesandwiches.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * A slice that can be laid down as a layer ({@link com.simplesandwiches.block.SliceLayerBlock}). Butter always lays down on
 * a click; cheese is food, so it only lays down while sneaking (BS25-14) and is eaten on a normal click.
 * Either kind stacks on an existing stack (pure or mixed) with every click.
 */
public class SliceLayerItem extends BlockItem {
    private final boolean sneakToPlace;

    public SliceLayerItem(Block block, Properties properties, boolean sneakToPlace) {
        super(block, properties);
        this.sneakToPlace = sneakToPlace;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // IG31-7: a click on a slice stack always stacks (sneaking skips the block's own click, which laid a new carpet above)
        var clicked = context.getLevel().getBlockState(context.getClickedPos());
        if (clicked.getBlock() instanceof com.simplesandwiches.block.SliceLayerBlock || clicked.getBlock() instanceof com.simplesandwiches.block.MixedSliceLayerBlock) {
            return com.simplesandwiches.block.SliceLayerBlock.stackSlice(context.getItemInHand(), clicked, context.getLevel(), context.getClickedPos(), context.getPlayer());
        }
        return sneakToPlace && !context.isSecondaryUseActive() ? InteractionResult.PASS : super.useOn(context);
    }
}
