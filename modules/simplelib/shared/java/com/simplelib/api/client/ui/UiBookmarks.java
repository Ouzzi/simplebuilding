package com.simplelib.api.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Bookmark tabs at the side of a box (docs/ai/UI-BAUSTEINE.md), like the guide book's: a tab of {@code w x h} whose
 * outer end is rounded; {@code left} = sticks out to the left. The active tab is drawn in the box colour so it
 * joins the box, others are shaded. {@link #greyOut} veils an area that is disabled for the current mode
 * (e.g. "pick a waypoint slot": everything except the slots is greyed out).
 */
public final class UiBookmarks {
    private UiBookmarks() {}

    public static void tab(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean left, UiPalette p, boolean active, boolean hovered) {
        int fill = active ? p.fill() : hovered ? p.light() : UiPalette.scale(p.fill(), 0.82);
        int[] outer = {2, 1};
        // Rim, then the body; the inner end stays square so it tucks under the box frame.
        g.fill(left ? x + outer[0] : x, y, left ? x + w : x + w - outer[0], y + 1, UiBoxes.RIM);
        g.fill(left ? x + outer[0] : x, y + h - 1, left ? x + w : x + w - outer[0], y + h, UiBoxes.RIM);
        g.fill(left ? x + outer[1] : x, y + 1, left ? x + w : x + w - outer[1], y + 2, UiBoxes.RIM);
        g.fill(left ? x + outer[1] : x, y + h - 2, left ? x + w : x + w - outer[1], y + h - 1, UiBoxes.RIM);
        g.fill(left ? x : x + w - 1, y + 2, left ? x + 1 : x + w, y + h - 2, UiBoxes.RIM);
        g.fill(left ? x + 1 : x, y + 2, left ? x + w : x + w - 1, y + h - 2, fill);
        g.fill(left ? x + 2 : x, y + 1, left ? x + w : x + w - 2, y + 2, fill);
        g.fill(left ? x + 2 : x, y + h - 2, left ? x + w : x + w - 2, y + h - 1, fill);
        g.fill(left ? x + 1 : x, y + 2, left ? x + w : x + w - 1, y + 3, p.light());
    }

    /** Red marker over a tab: "click again to confirm" (second-click confirmation, owner N18). */
    public static void confirm(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x99D02020);
    }

    public static void greyOut(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xA0303030);
    }
}
