package com.simplebuilding.items.custom;

import net.minecraft.block.Block;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class NetheriteShulkerItem extends BlockItem {

    public NetheriteShulkerItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        // 1. Super-Aufruf mit allen 5 Argumenten
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);

        // 2. Text hinzufügen (jetzt über textConsumer.accept statt list.add)
        textConsumer.accept(Text.translatable("block.simplebuilding.netherite_shulker.desc").formatted(Formatting.GRAY));
    }
}