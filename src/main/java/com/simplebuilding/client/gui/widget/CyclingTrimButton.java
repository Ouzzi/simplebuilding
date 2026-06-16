package com.simplebuilding.client.gui.widget;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.ItemStack;

public class CyclingTrimButton extends Button {
    private final List<ItemStack> items;
    private final int switchInterval;

    public CyclingTrimButton(int x, int y, int width, int height, List<ItemStack> items, OnPress onPress) {
        super(x, y, width, height, net.minecraft.network.chat.Component.empty(), onPress, DEFAULT_NARRATION);
        this.items = items;
        this.switchInterval = 1000;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.extractDefaultSprite(context);

        if (!items.isEmpty()) {
            long time = System.currentTimeMillis();
            int index = (int) ((time / switchInterval) % items.size());

            ItemStack currentStack = items.get(index);
            context.item(currentStack, this.getX() + 2, this.getY() + 2);
        }
    }
}
