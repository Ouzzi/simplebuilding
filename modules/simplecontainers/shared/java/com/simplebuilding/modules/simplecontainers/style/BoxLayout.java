package com.simplebuilding.modules.simplecontainers.style;

import com.simplelib.api.client.ui.UiBoxes;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Where the two boxes of the Simple style go on a Vanilla container screen - a pure function of the slot positions
 * (docs/ai/PLAN-SIMPLECONTAINERS-2026-10-08.md, W0-A): the container box in the block's colour around the container
 * slots (from the image top, so the title sits inside it), the light inventory box around the player's slots. Both
 * span all slots plus {@link #SIDE} px left and right (a chest: 0..176). Vanilla leaves only 13-14 px between the two
 * slot groups (slot light edge included). Rule of the W0-B preview ("Kasten-Fuge"): with free rows >= 12 the bottom
 * frame (7) and the top frame (5) fit, the divider gets up to {@link #MAX_GAP} px and the rest pads the container
 * box; the inventory box starts right above the player's slots. Less room returns {@code null}: the screen stays
 * Vanilla (the shadowless and the one-box variants of the preview come with the screens that need them, W1).
 * No client classes (testable on a server).
 */
public final class BoxLayout {
    /** Space left and right of the outermost slots (5 px frame + 3 px; the slot's light edge is in the right one). */
    public static final int SIDE = 8;
    /** Widest divider between the two boxes. */
    public static final int MAX_GAP = 2;
    /** A slot drawn 16x16 plus its light edge below and right. */
    static final int SLOT = 17;

    /** One active slot at its item position; {@code player} = the player's own inventory or hotbar. */
    public record Slot(int x, int y, boolean player) {}

    /** A box relative to the screen image. */
    public record Rect(int x, int y, int width, int height) {
        public int bottom() {
            return y + height;
        }
    }

    /** The container box ({@code null} without container slots) and the inventory box. */
    public record Layout(@Nullable Rect container, Rect inventory) {}

    private BoxLayout() {}

    /**
     * Boxes for {@code slots} on an image {@code imageWidth} x {@code imageHeight} whose title is drawn at
     * {@code titleY}; {@code null} when there are no player slots, the container slots are not above them or the
     * rows between both groups are too few for two full frames.
     */
    public static @Nullable Layout compute(List<Slot> slots, int imageWidth, int imageHeight, int titleY) {
        int[] c = bounds(slots, false), p = bounds(slots, true);
        if (p == null) return null;
        int left = Math.max(0, (c == null ? p[0] : Math.min(c[0], p[0])) - SIDE);
        int right = Math.min(Math.max(imageWidth, 0), (c == null ? p[2] : Math.max(c[2], p[2])) + 16 + SIDE);
        if (right - left < 2 * UiBoxes.FRAME + SLOT) return null;
        int playerBottom = p[3] + SLOT;
        int inventoryBottom = playerBottom + UiBoxes.FRAME_BOTTOM;
        inventoryBottom += Math.max(0, Math.min(MAX_GAP, imageHeight - inventoryBottom));
        if (c == null) {
            int top = Math.max(0, p[1] - UiBoxes.FRAME - MAX_GAP);
            return new Layout(null, new Rect(left, top, right - left, inventoryBottom - top));
        }
        int free = p[1] - (c[3] + SLOT);
        if (free < UiBoxes.FRAME_BOTTOM + UiBoxes.FRAME) return null;
        int gap = Math.min(MAX_GAP, free - UiBoxes.FRAME_BOTTOM - UiBoxes.FRAME);
        int containerTop = Math.max(0, Math.min(c[1] - UiBoxes.FRAME - MAX_GAP, titleY - UiBoxes.FRAME - 1));
        int inventoryTop = p[1] - UiBoxes.FRAME;
        int containerBottom = inventoryTop - gap;
        return new Layout(new Rect(left, containerTop, right - left, containerBottom - containerTop),
                new Rect(left, inventoryTop, right - left, inventoryBottom - inventoryTop));
    }

    /** Min x, min y, max x, max y of the item positions of one group, or {@code null} if it has no slots. */
    static int[] bounds(List<Slot> slots, boolean player) {
        int[] b = null;
        for (Slot s : slots) {
            if (s.player() != player) continue;
            if (b == null) b = new int[] {s.x(), s.y(), s.x(), s.y()};
            b[0] = Math.min(b[0], s.x());
            b[1] = Math.min(b[1], s.y());
            b[2] = Math.max(b[2], s.x());
            b[3] = Math.max(b[3], s.y());
        }
        return b;
    }
}
