package com.simplemusic.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMargin;
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
 * progress arrow; principle 6a). Sheet and blank sit side by side (DP23), the optional dye slot below them (WH26-3; empty slots show grey silhouettes); with the style switched off Vanilla's furnace
 * picture with its two left slots and the flame painted over and Vanilla-grey slots at the new places.
 * Labels: iron #2E3034 on #9A9DA2 (4.7:1), inventory #404040 on #C6C6C6 (6.1:1).
 */
public class DiscPressScreen extends AbstractContainerScreen<DiscPressMenu> {
    private static final Identifier FURNACE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier PROGRESS = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final UiPalette BOX = UiPalette.IRON;
    /** Vanilla's furnace arrow column (unstyled) and the press row (N57-10: row 28, see {@link DiscPressMenu#OUTPUT_Y}). */
    private static final int ARROW_X = 79, ARROW_Y = DiscPressMenu.OUTPUT_Y;
    /** Styled arrow: centred between the blank's light edge (73) and the big result slot (112): 9 px left, 8 px right. */
    private static final int STYLED_ARROW_X = PressMargins.ARROW_X;
    /** N62 boxes (style on: no shift, the iron box ends 3 px under the dye slot, the image 169 high; style off: the furnace's 166). */
    private final UiMargin.Boxes boxes;

    public DiscPressScreen(DiscPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, UiStyleToggle.isEnabled() ? PressMargins.press(menu).height() : 166);
        this.boxes = PressMargins.press(menu);
        if (UiStyleToggle.isEnabled()) UiMargin.moveInventory(menu.slots, this.boxes.shift());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!UiStyleToggle.isEnabled()) {
            super.extractLabels(g, mouseX, mouseY);
            return;
        }
        // Title only, like the other boxed screens: the vanilla "Inventory" label sat on the inventory box's top frame (VIS24).
        g.text(font, title, titleLabelX, titleLabelY, BOX.label(), false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int left = leftPos, top = topPos;
        int done = Math.round(menu.progress() * 24);
        if (!UiStyleToggle.isEnabled()) {
            g.blit(RenderPipelines.GUI_TEXTURED, FURNACE, left, top, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
            // the furnace's input and fuel slots, flame, arrow and result frame (x 54..140, y 15..72) give way; arrow and
            // result frame come back from the same picture at the press row (N57-10)
            g.fill(left + 54, top + 15, left + 141, top + 72, 0xFFC6C6C6);
            g.blit(RenderPipelines.GUI_TEXTURED, FURNACE, left + ARROW_X, top + ARROW_Y - 1, 79.0F, 34.0F, 24, 17, 256, 256);
            g.blit(RenderPipelines.GUI_TEXTURED, FURNACE, left + DiscPressMenu.OUTPUT_X - 5, top + DiscPressMenu.OUTPUT_Y - 5, 111.0F, 30.0F, 26, 26, 256, 256);
            for (Slot slot : menu.slots) {
                if (slot.index < 2 || slot.index == 3) vanillaSlot(g, left + slot.x, top + slot.y);
            }
            if (done > 0) g.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS, 24, 16, 0, 0, left + ARROW_X, top + ARROW_Y - 1, done, 16);
            return;
        }
        // N62: 3 px fill round the press slots, arrow and result and round the inventory, both boxes with the shadow
        UiMargin.Rect c = boxes.container(), i = boxes.inventory();
        UiBoxes.box(g, left + c.x(), top + c.y(), c.width(), c.height(), BOX);
        UiBoxes.box(g, left + i.x(), top + i.y(), i.width(), i.height(), UiPalette.INVENTORY);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            if (slot.index == 2) UiBoxes.bigSlot(g, left + slot.x, top + slot.y, BOX);
            else UiBoxes.slot(g, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : BOX);
        }
        UiBoxes.arrow(g, left + STYLED_ARROW_X, top + ARROW_Y, Math.round(menu.progress() * UiBoxes.ARROW_WIDTH), BOX);
    }

    /** Vanilla's 18 x 18 slot frame around the item at {@code x, y}: dark top/left, white bottom/right, grey inside. */
    private static void vanillaSlot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFFFFFFFF);
        g.fill(x - 1, y - 1, x + 16, y + 16, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }
}
