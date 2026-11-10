package com.simplebuilding.modules.simplelayers.block;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Chalk (BACK20): draws a {@link ChalkMarkBlock} in its colour on the clicked face - an arrow towards where you click, a cross in
 * the middle, a circle while sneaking; each mark costs one of {@link #USES}. Works everywhere (caves, mines), not only in the
 * Backrooms. A Chalk Box holds chalk of all four colours.
 */
public class ChalkItem extends Item {
    public static final int USES = 64;
    public final ChalkMarkBlock.Color color;

    public ChalkItem(ChalkMarkBlock.Color color, Properties properties) {
        super(properties);
        this.color = color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        if (!ChalkMarkBlock.draw(context.getLevel(), context.getClickedPos(), context.getClickedFace(), context.getClickLocation(), color, false, sneaking)) {
            return InteractionResult.PASS;
        }
        if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        for (String suffix : new String[] {"", ".2", ".3", ".4"}) {
            out.accept(Component.translatable("item.simplelayers.chalk.tooltip" + suffix).withStyle(ChatFormatting.GRAY));
        }
    }
}
