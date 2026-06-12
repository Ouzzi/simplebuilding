package com.simplebuilding.client.gui.widget;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class CyclingTrimButton extends ButtonWidget {
    private final List<ItemStack> items;
    private final int switchInterval;

    public CyclingTrimButton(int x, int y, int width, int height, List<ItemStack> items, PressAction onPress) {
        super(x, y, width, height, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.items = items;
        this.switchInterval = 1000;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderButton(matrices, mouseX, mouseY, delta);

        if (!items.isEmpty()) {
            long time = System.currentTimeMillis();
            int index = (int) ((time / switchInterval) % items.size());
            ItemStack currentStack = items.get(index);
            this.drawItem(matrices, currentStack, this.getX() + 2, this.getY() + 2);
        }
    }
}