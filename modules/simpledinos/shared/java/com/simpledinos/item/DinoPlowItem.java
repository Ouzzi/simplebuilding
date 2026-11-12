package com.simpledinos.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Dino Plow (a-dinorest2, concept DINO36 6): put on a grown Stegosaurus by its owner (right click), lowered and raised with the jump
 * key, it tills three blocks wide (com.simpledinos.entity.Stegosaurus). 750 durability, repaired with iron ingots, takes Unbreaking and
 * Mending. One tooltip line, because the use is not obvious (AGENTS.md rule 14).
 */
public class DinoPlowItem extends Item {
    public DinoPlowItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> lines,
            TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.simpledinos.dino_plow").withStyle(ChatFormatting.GRAY));
    }
}
