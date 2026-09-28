package com.simplebuilding.items.custom;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Geschwindigkeitsmesser: die Anzeige selbst ist das HUD
 * ({@code client.gui.SpeedometerHudOverlay}); hier nur der Tooltip, der den seit jeher vorhandenen,
 * aber nie gezeigten Schluessel {@code tooltip.simplebuilding.velocity-gauge.tooltip} benutzt und
 * ohne Verzauberung auf Beruehrung des Konstrukteurs hinweist.
 */
public class VelocityGaugeItem extends Item {

    public VelocityGaugeItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.simplebuilding.velocity-gauge.tooltip").withStyle(ChatFormatting.GRAY));
        if (!stack.isEnchanted()) {
            lines.accept(Component.translatable("tooltip.simplebuilding.velocity-gauge.touch_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
