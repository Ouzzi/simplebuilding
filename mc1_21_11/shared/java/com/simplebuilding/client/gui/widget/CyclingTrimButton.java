package com.simplebuilding.client.gui.widget;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
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
    protected void renderContents(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderDefaultSprite(context);

        if (!items.isEmpty()) {
            long time = System.currentTimeMillis();
            int index = (int) ((time / switchInterval) % items.size());

            ItemStack currentStack = items.get(index);
            context.renderItem(currentStack, this.getX() + 2, this.getY() + 2);
        }
    }
}
