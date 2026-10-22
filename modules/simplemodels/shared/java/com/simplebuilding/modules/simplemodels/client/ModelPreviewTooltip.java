package com.simplebuilding.modules.simplemodels.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip image of a browser row (SM21 d): the plain base item, an arrow, and the assigned model drawn at twice the
 * size, so the model is visible before it is picked.
 */
public record ModelPreviewTooltip(ItemStack base, ItemStack model) implements ClientTooltipComponent {
    private static final int SIZE = 32;

    @Override public int getHeight(Font font) { return SIZE + 4; }
    @Override public int getWidth(Font font) { return 16 + 4 + font.width("→") + 4 + SIZE; }

    @Override public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor g) {
        g.item(base, x, y + (SIZE - 16) / 2);
        g.text(font, "→", x + 20, y + (SIZE - 8) / 2, 0xFFFFFFFF, true);
        g.pose().pushMatrix();
        g.pose().translate(x + 20 + font.width("→") + 4, y);
        g.pose().scale(2f, 2f);
        g.item(model, 0, 0);
        g.pose().popMatrix();
    }
}
