package com.simplelights.block;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Item of the two multicolor panes (redstone, copper redstone): one tooltip line, the sneak-free right-click sets the color of strength 1. */
public class LightPaneItem extends BlockItem {
    public LightPaneItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.simplelights.redstone_light_pane").withStyle(ChatFormatting.GRAY));
    }
}
