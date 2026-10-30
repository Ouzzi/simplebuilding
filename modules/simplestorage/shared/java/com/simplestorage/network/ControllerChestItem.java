package com.simplestorage.network;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The item of a controller chest: how many chests its network takes (and that higher chests stay out). */
public class ControllerChestItem extends BlockItem {
    public ControllerChestItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                java.util.function.Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        if (getBlock() instanceof ControllerChestBlock controller) {
            builder.accept(Component.translatable("tooltip.simplestorage.controller.limit", controller.step().maxChests())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            builder.accept(Component.translatable("tooltip.simplestorage.controller.rank").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
