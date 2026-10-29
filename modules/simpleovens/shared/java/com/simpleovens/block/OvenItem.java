package com.simpleovens.block;

import com.simpleovens.OvenTier;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** A furnace item; its tooltip names the speed (a number shown nowhere else) and the double experience. */
public class OvenItem extends BlockItem {
    private final OvenTier tier;

    public OvenItem(Block block, OvenTier tier, Properties properties) {
        super(block, properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.simpleovens.speed", this.tier.defaultSpeed()).withStyle(ChatFormatting.GRAY));
        if (this.tier.upper()) out.accept(Component.translatable("tooltip.simpleovens.experience").withStyle(ChatFormatting.GRAY));
    }
}
