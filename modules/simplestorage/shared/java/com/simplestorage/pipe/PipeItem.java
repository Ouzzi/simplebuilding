package com.simplestorage.pipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The item of a pipe: speed of its step and the one thing that is not obvious, how to turn the output. */
public class PipeItem extends BlockItem {
    public PipeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                java.util.function.Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        if (getBlock() instanceof ItemPipeBlock pipe) {
            builder.accept(Component.translatable("tooltip.simplestorage.pipe.speed", pipe.tier().amount(), pipe.tier().cooldown())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            builder.accept(Component.translatable("tooltip.simplestorage.pipe.turn").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
