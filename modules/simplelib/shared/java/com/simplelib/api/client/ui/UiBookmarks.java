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

    /**
     * Guide-book style bookmark (owner N38): a ribbon of {@code w x h} whose outer end is rounded, with a light top
     * edge, a shaded bottom and a shaded end, like the guide book's bookmarks. {@code left} = sticks out to the left.
     * The caller draws it before the box so the inner end hides under the frame. Filled ribbons are in the box
     * colour, empty ones shaded and greyish, the hovered one lighter.
     */
    public static void ribbon(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean left, UiPalette p, boolean filled, boolean hovered) {
        int fill = hovered ? p.light() : filled ? p.fill() : UiPalette.mix(UiPalette.scale(p.fill(), 0.8), 0xFF8A8A8A, 0.25);
        int top = UiPalette.mix(fill, 0xFFFFFFFF, 0.35), shade = UiPalette.scale(fill, 0.78), end = UiPalette.scale(fill, 0.86);
        // Offsets are measured from the inner end (0) to the outer end (w); mirrored for left ribbons.
        span(g, x, w, left, 0, w - 1, y, y + 1, UiBoxes.RIM);
        span(g, x, w, left, 0, w - 1, y + h - 1, y + h, UiBoxes.RIM);
        span(g, x, w, left, w - 1, w, y + 1, y + h - 1, UiBoxes.RIM);
        span(g, x, w, left, 0, w - 1, y + 1, y + h - 1, fill);
        span(g, x, w, left, 0, w - 2, y + 1, y + 2, top);
        span(g, x, w, left, 0, w - 1, y + h - 3, y + h - 1, shade);
        span(g, x, w, left, w - 2, w - 1, y + 2, y + h - 3, end);
    }

    private static void span(GuiGraphicsExtractor g, int x, int w, boolean left, int from, int to, int y0, int y1, int color) {
        if (left) g.fill(x + w - to, y0, x + w - from, y1, color);
        else g.fill(x + from, y0, x + to, y1, color);
    }

    /**
     * Square tool button on the Vanilla button sprite (owner N38): highlighted while hovered; a switched-on toggle
     * gets a white bar under its icon, like the selected tabs of the blueprint screen.
     */
    public static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean on, boolean hovered) {
        g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                net.minecraft.resources.Identifier.withDefaultNamespace(hovered ? "widget/button_highlighted" : "widget/button"), x, y, w, h);
        if (on) g.fill(x + 4, y + h - 4, x + w - 4, y + h - 3, 0xFFFFFFFF);
    }

    /** Red marker over a tab: "click again to confirm" (second-click confirmation, owner N18). */
    public static void confirm(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x99D02020);
    }

    public static void greyOut(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xA0303030);
    }
}
