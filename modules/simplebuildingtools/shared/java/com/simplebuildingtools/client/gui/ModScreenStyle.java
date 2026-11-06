package com.simplebuildingtools.client.gui;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The panel style of the slotless tool screens (SimpleLib's style blocks, as SimpleBuilding's ModScreenStyle drew them). */
public final class ModScreenStyle {
    private ModScreenStyle() {
    }

    /** Whether the panel style is drawn now (the comparison toggle on). */
    public static boolean panels() {
        return UiStyleToggle.isEnabled();
    }

    static UiPalette palette(int fill) {
        for (UiPalette p : new UiPalette[] {UiPalette.IRON, UiPalette.REINFORCED, UiPalette.NETHERITE, UiPalette.ENDERITE}) {
            if (p.fill() == (fill | 0xFF000000)) return p;
        }
        return UiPalette.derived(fill);
    }

    /** A box in {@code fill}; {@code false} = not drawn (toggle off). */
    public static boolean panel(GuiGraphicsExtractor g, int fill, int x, int y, int w, int h) {
        if (!panels()) return false;
        UiBoxes.box(g, x, y, w, h, palette(fill));
        return true;
    }

    /** A sheet of paper as a box (the blueprint editor): paper stays paper, the inventory brightness does not darken it. */
    public static boolean paper(GuiGraphicsExtractor g, int fill, int x, int y, int w, int h) {
        if (!panels()) return false;
        com.simplelib.api.client.ui.UiBrightness saved = com.simplelib.api.client.ui.UiBrightness.current();
        com.simplelib.api.client.ui.UiBrightness.set(com.simplelib.api.client.ui.UiBrightness.LIGHT);
        try {
            UiBoxes.box(g, x, y, w, h, palette(fill));
        } finally {
            com.simplelib.api.client.ui.UiBrightness.set(saved);
        }
        return true;
    }

    /** A light sunk field (lists, summaries) in the inventory grey. */
    public static boolean field(GuiGraphicsExtractor g, int fill, int x, int y, int w, int h) {
        if (!panels()) return false;
        UiBoxes.insetColored(g, x, y, w, h, palette(fill), UiPalette.INVENTORY.fill());
        return true;
    }

    /** Label colour on a {@code fill} panel, {@code fallback} when the style is off. */
    public static int label(int fill, int fallback) {
        return panels() ? palette(fill).label() : fallback;
    }
}
