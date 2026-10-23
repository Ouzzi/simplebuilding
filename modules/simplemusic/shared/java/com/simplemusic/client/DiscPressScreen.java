package com.simplemusic.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import com.simplemusic.press.DiscPressMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Screen of the disc press: the family's container style from SimpleLib (iron box, light inventory box, big result slot,
 * progress arrow; principle 6a). Sheet and blank sit side by side (DP23); with the style switched off Vanilla's furnace
 * picture with its two left slots and the flame painted over and Vanilla-grey slots at the new places.
 * Labels: iron #2E3034 on #9A9DA2 (4.7:1), inventory #404040 on #C6C6C6 (6.1:1).
 */
public class DiscPressScreen extends AbstractContainerScreen<DiscPressMenu> {
    private static final Identifier FURNACE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier PROGRESS = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final UiPalette BOX = UiPalette.IRON;
    private static final int INVENTORY_Y = 84, GAP = 2, INVENTORY_HEIGHT = UiBoxes.FRAME + 58 + 17 + UiBoxes.FRAME_BOTTOM;
    private static final int ARROW_X = 79, ARROW_Y = 35;

    public DiscPressScreen(DiscPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!UiStyleToggle.isEnabled()) {
            super.extractLabels(g, mouseX, mouseY);
            return;
        }
        g.text(font, title, titleLabelX, titleLabelY, BOX.label(), false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, UiPalette.INVENTORY.label(), false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int left = leftPos, top = topPos;
        int done = Math.round(menu.progress() * 24);
        if (!UiStyleToggle.isEnabled()) {
            g.blit(RenderPipelines.GUI_TEXTURED, FURNACE, left, top, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
            // the furnace's input and fuel slots and its flame (x 55..72, y 16..70) give way to the two slots side by side
            g.fill(left + 54, top + 15, left + 75, top + 72, 0xFFC6C6C6);
            for (Slot slot : menu.slots) {
                if (slot.index < 2) vanillaSlot(g, left + slot.x, top + slot.y);
            }
            if (done > 0) g.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS, 24, 16, 0, 0, left + ARROW_X, top + ARROW_Y - 1, done, 16);
            return;
        }
        UiBoxes.box(g, left, top, imageWidth, INVENTORY_Y - UiBoxes.FRAME - GAP, BOX);
        UiBoxes.box(g, left, top + INVENTORY_Y - UiBoxes.FRAME, imageWidth, INVENTORY_HEIGHT, UiPalette.INVENTORY);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            if (slot.index == 2) UiBoxes.bigSlot(g, left + slot.x, top + slot.y, BOX);
            else UiBoxes.slot(g, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : BOX);
        }
        UiBoxes.arrow(g, left + ARROW_X + 1, top + ARROW_Y, Math.round(menu.progress() * UiBoxes.ARROW_WIDTH), BOX);
    }

    /** Vanilla's 18 x 18 slot frame around the item at {@code x, y}: dark top/left, white bottom/right, grey inside. */
    private static void vanillaSlot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFFFFFFFF);
        g.fill(x - 1, y - 1, x + 16, y + 16, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }
}
