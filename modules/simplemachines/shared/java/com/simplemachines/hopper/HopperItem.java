package com.simplemachines.hopper;

import com.simplemachines.config.MachinesConfig;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The item of a mod hopper; the tooltip names its speed (from the config) and the filter, as SimpleBuilding's did. */
public class HopperItem extends BlockItem {
    private final HopperTier tier;

    public HopperItem(Block block, HopperTier tier, Properties properties) {
        super(block, properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        int cooldown = MachinesConfig.hopperCooldown(tier);
        out.accept((cooldown == 1 ? Component.translatable("tooltip.simplemachines.hopper.speed_every_tick")
                : Component.translatable("tooltip.simplemachines.hopper.speed", cooldown)).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.hopper.filter").withStyle(ChatFormatting.GRAY));
    }
}
