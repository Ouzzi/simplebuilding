package com.simplebuildingtools.client.gui;

import com.simplelib.api.client.ui.UiBoxes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;

/**
 * A button whose face colour carries its state (OCTUI12=B): green = on, red = locked, axis tile in the axis colour.
 * {@code face} 0 = the plain Vanilla button. The label is white; every face keeps at least 4.5:1 against white
 * (see {@link OctantScreenColours}).
 */
final class StateButton extends Button {
    private final IntSupplier face;

    StateButton(int x, int y, int w, int h, Component msg, OnPress onPress, IntSupplier face) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
        this.face = face;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int color = face.getAsInt();
        if (color == 0 || !this.active) {
            super.extractContents(g, mouseX, mouseY, partialTick);
            return;
        }
        UiBoxes.raised(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), color);
        if (this.isHoveredOrFocused()) {
            g.outline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0xFFFFFFFF);
        }
        var font = Minecraft.getInstance().font;
        Component msg = this.getMessage();
        g.text(font, msg, this.getX() + (this.getWidth() - font.width(msg)) / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF, false);
    }
}
