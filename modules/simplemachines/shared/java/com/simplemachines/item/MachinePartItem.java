package com.simplemachines.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The item of a machine part; its tooltip ({@code tooltip.simplemachines.<path>}) says how the part is used. */
public class MachinePartItem extends BlockItem {
    private final String tooltip;

    public MachinePartItem(Block block, String path, Properties properties) {
        super(block, properties);
        this.tooltip = "tooltip.simplemachines." + path;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable(tooltip).withStyle(ChatFormatting.GRAY));
    }
}
