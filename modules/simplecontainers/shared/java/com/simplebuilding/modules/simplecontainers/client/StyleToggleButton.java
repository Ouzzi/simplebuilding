package com.simplebuilding.modules.simplecontainers.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Small raised comparison button; the V glyph is deliberately readable at 16x16. */
public final class StyleToggleButton extends Button {
    public StyleToggleButton(int x, int y, OnPress onPress) {
        super(x, y, 16, 16, Component.translatable("simplecontainers.toggle_style"), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int face = UiPalette.mix(UiPalette.INVENTORY.fill(), 0xFFFFFFFF, this.isHoveredOrFocused() ? 0.32 : 0.18);
        UiBoxes.raised(g, this.getX(), this.getY(), 16, 16, face);
        int color = UiStyleToggle.isEnabled() ? UiPalette.DARK_LABEL : UiPalette.INVENTORY.slotTop();
        g.fill(this.getX() + 4, this.getY() + 4, this.getX() + 5, this.getY() + 8, color);
        g.fill(this.getX() + 5, this.getY() + 8, this.getX() + 7, this.getY() + 11, color);
        g.fill(this.getX() + 7, this.getY() + 11, this.getX() + 9, this.getY() + 13, color);
        g.fill(this.getX() + 9, this.getY() + 8, this.getX() + 11, this.getY() + 11, color);
        g.fill(this.getX() + 11, this.getY() + 4, this.getX() + 12, this.getY() + 8, color);
    }
}
