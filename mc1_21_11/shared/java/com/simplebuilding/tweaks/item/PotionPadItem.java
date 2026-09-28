package com.simplebuilding.tweaks.item;

import com.simplebuilding.tweaks.component.TweaksComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * Item der Trank-Pads: ein in der Abklingzeit abgebautes Pad traegt die Restzeit
 * ({@link TweaksComponents#POTION_PAD_COOLDOWN}) und nennt sie im Tooltip (sie laeuft erst gesetzt weiter).
 */
public class PotionPadItem extends BlockItem {
    public PotionPadItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        Integer rest = stack.get(TweaksComponents.POTION_PAD_COOLDOWN);
        if (rest != null && rest > 0) {
            int seconds = (rest + 19) / 20;
            tooltip.accept(Component.translatable("tooltip.simplebuilding.potion_pad.cooling",
                    String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
