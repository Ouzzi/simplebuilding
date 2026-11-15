package com.simplebuilding.modules.simpleinterfaces.style;

import com.simplelib.api.client.ui.UiBoxes;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Where the two boxes of the Simple style go on a Vanilla container screen - a pure function of the slot positions
 * (docs/ai/PLAN-SIMPLECONTAINERS-2026-10-08.md, W0-A): the container box in the block's colour around the container
 * slots (from the image top, so the title sits inside it), the light inventory box around the player's slots. Both
 * span all slots plus {@link #SIDE} px left and right (a chest: 0..176). Vanilla leaves only 13-14 px between the two
 * slot groups (slot light edge included). Rule of the W0-B preview ("Kasten-Fuge"), IG57U2 (15.11.): below the last
 * container row {@link #AIR} px of fill like beside it; with free rows >= 16 the bottom frame (7), the top frame (5) and a
 * {@link #MAX_GAP} px divider fit and the rest pads the container box; free rows 10-15: the container box without its 2 px
 * shadow ({@link Variant#NO_SHADOW}), divider up to {@link #MAX_GAP} px from what is left after the air. Free rows 3-9: one box in the block's colour from the top to below the
 * hotbar, the player's part a light panel behind a seam 3 px above the first inventory row ({@link Variant#SEAM}).
 * Less room returns {@code null}: the screen stays Vanilla. No client classes (testable on a server).
 */
public final class BoxLayout {
    /** Space left and right of the outermost slots (5 px frame + 3 px; the slot's light edge is in the right one). */
    public static final int SIDE = 8;
    /** Widest divider between the two boxes. */
    public static final int MAX_GAP = 2;
    /** A slot drawn 16x16 plus its light edge below and right. */
    static final int SLOT = 17;
    /** The seam of {@link Variant#SEAM} lies this many rows above the first inventory slot row. */
    public static final int SEAM = 3;
    /** Fill rows below the last container row's light edge: the same air as on the right side (SIDE - FRAME - 1). */
    public static final int AIR = SIDE - UiBoxes.FRAME - 1;

    /** How the two slot groups share the rows between them (README "Kasten-Fuge"). */
    public enum Variant {
        /** Free rows >= 16: two full boxes, a {@link #MAX_GAP} px divider, air below the container slots. */
        TWO_BOXES,
        /** Free rows 10-15: two boxes, the container box without its 2 px shadow (5 px frame at the bottom too). */
        NO_SHADOW,
        /**
         * Free rows 3-9: one box in the block's colour ({@code container}, down to below the hotbar); {@code inventory} is
         * the light panel inside it, starting at the seam (1 px groove, 1 px light line, then the inventory fill).
         */
        SEAM
    }

    /** One active slot at its item position; {@code player} = the player's own inventory or hotbar. */
    public record Slot(int x, int y, boolean player) {}

    /** A box relative to the screen image. */
    public record Rect(int x, int y, int width, int height) {
        public int bottom() {
            return y + height;
        }
    }

    /**
     * The container box ({@code null} without container slots) and the inventory box - or, for {@link Variant#SEAM},
     * the one box and the light panel inside it.
     */
    public record Layout(@Nullable Rect container, Rect inventory, Variant variant, int shift, int height) {
        public Layout(@Nullable Rect container, Rect inventory, Variant variant) {
            this(container, inventory, variant, 0, inventory.bottom());
        }

        public Layout(@Nullable Rect container, Rect inventory) {
            this(container, inventory, Variant.TWO_BOXES);
        }
    }

    /**
     * N62 (owner 15.11.): the fixed container margin - {@link UiBoxes#MARGIN} px of fill between each box's light inner line
     * and its outermost slots on all four sides (the title keeps its own margin; the slots stay >= MARGIN below it), both
     * boxes shadowed, a divider of at least {@link #MAX_GAP} px. Where Vanilla's rows leave less room, {@link Layout#shift}
     * says how many px the player's slots must move down (client side, before the screen is centred) and
     * {@link Layout#height} the image height then needed; the boxes are given for the moved slots. The box may be 1 px wider
     * than the image (a chest: 0..177), otherwise the right margin would be 2. {@code null} as {@link #compute}.
     */
    public static @Nullable Layout margin(List<Slot> slots, List<Rect> elements, int imageWidth, int imageHeight, int titleY) {
        int[] c = withElements(bounds(slots, false), elements), p = bounds(slots, true);
        if (p == null || c == null || c[1] >= p[1]) return null;
        int edge = UiBoxes.MARGIN + UiBoxes.FRAME;
        int left = Math.max(0, Math.min(c[0], p[0]) - edge);
        int right = Math.min(imageWidth + 1, Math.max(c[2], p[2]) + SLOT + edge);
        int containerTop = Math.max(0, Math.min(c[1] - edge, titleY - UiBoxes.FRAME - 1));
        int containerBottom = c[3] + SLOT + UiBoxes.MARGIN + UiBoxes.FRAME_BOTTOM;
        int shift = Math.max(0, containerBottom + MAX_GAP - (p[1] - edge));
        int inventoryTop = p[1] + shift - edge;
        int inventoryBottom = p[3] + shift + SLOT + UiBoxes.MARGIN + UiBoxes.FRAME_BOTTOM;
        // N64: the inventory box has its own width (as UiMargin): the same as the container box on the storage screens, narrower
        // under a wide container box (beacon power fields)
        int inventoryLeft = Math.max(0, p[0] - edge), inventoryRight = Math.min(imageWidth + 1, p[2] + SLOT + edge);
        return new Layout(new Rect(left, containerTop, right - left, containerBottom - containerTop),
                new Rect(inventoryLeft, inventoryTop, inventoryRight - inventoryLeft, inventoryBottom - inventoryTop), Variant.TWO_BOXES, shift,
                Math.max(imageHeight, inventoryBottom));
    }

    /** {@code c} (container item bounds or {@code null}) grown by {@code elements} as in {@link #compute}. */
    private static int[] withElements(int[] c, List<Rect> elements) {
        for (Rect e : elements) {
            c = c == null ? new int[] {e.x(), e.y(), e.x() + e.width() - SLOT, e.y() + e.height() - SLOT}
                    : new int[] {Math.min(c[0], e.x()), Math.min(c[1], e.y()), Math.max(c[2], e.x() + e.width() - SLOT),
                            Math.max(c[3], e.y() + e.height() - SLOT)};
        }
        return c;
    }

    private BoxLayout() {}

    /**
     * Boxes for {@code slots} on an image {@code imageWidth} x {@code imageHeight} whose title is drawn at
     * {@code titleY}; {@code null} when there are no player slots, the container slots are not above them or the
     * rows between both groups are too few even for the seam variant (< {@link #SEAM}).
     */
    public static @Nullable Layout compute(List<Slot> slots, int imageWidth, int imageHeight, int titleY) {
        return compute(slots, List.of(), imageWidth, imageHeight, titleY);
    }

    /**
     * Like {@link #compute(List, int, int, int)} with further container elements drawn in the container box (a big
     * result slot, an entity preview, a symbol): rectangles including their light edges, counted like container slots.
     */
    public static @Nullable Layout compute(List<Slot> slots, List<Rect> elements, int imageWidth, int imageHeight, int titleY) {
        int[] c = bounds(slots, false), p = bounds(slots, true);
        for (Rect e : elements) {
            // as a slot whose item position is shifted so that slot + light edge covers the element
            c = c == null ? new int[] {e.x(), e.y(), e.x() + e.width() - SLOT, e.y() + e.height() - SLOT}
                    : new int[] {Math.min(c[0], e.x()), Math.min(c[1], e.y()), Math.max(c[2], e.x() + e.width() - SLOT),
                            Math.max(c[3], e.y() + e.height() - SLOT)};
        }
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
        if (free < SEAM || c[1] >= p[1]) return null;
        int containerTop = Math.max(0, Math.min(c[1] - UiBoxes.FRAME - MAX_GAP, titleY - UiBoxes.FRAME - 1));
        if (free < 2 * UiBoxes.FRAME) {
            int seam = p[1] - SEAM;
            return new Layout(new Rect(left, containerTop, right - left, inventoryBottom - containerTop),
                    new Rect(left + UiBoxes.FRAME, seam, right - left - 2 * UiBoxes.FRAME, inventoryBottom - UiBoxes.FRAME_BOTTOM - seam),
                    Variant.SEAM);
        }
        int inventoryTop = p[1] - UiBoxes.FRAME;
        // IG57U2 (owner 15.11.): below the last container row as much air as beside it (slot light edge + AIR px of fill,
        // like the right side); a shadowed box only when the air, both frames and the full divider fit, otherwise the
        // container box drops its 2 px shadow first (Vanilla chests/shulkers/hoppers/dispensers: 13-14 free rows)
        Variant variant;
        int gap;
        if (free >= AIR + UiBoxes.FRAME_BOTTOM + UiBoxes.FRAME + MAX_GAP) {
            variant = Variant.TWO_BOXES;
            gap = MAX_GAP;
        } else if (free >= AIR + 2 * UiBoxes.FRAME) {
            variant = Variant.NO_SHADOW;
            gap = Math.min(MAX_GAP, free - AIR - 2 * UiBoxes.FRAME);
        } else {
            variant = Variant.NO_SHADOW;
            gap = 0; // 10-11 free rows: the boxes touch, the rest is air
        }
        int containerBottom = inventoryTop - gap;
        return new Layout(new Rect(left, containerTop, right - left, containerBottom - containerTop),
                new Rect(left, inventoryTop, right - left, inventoryBottom - inventoryTop), variant);
    }

    /** Min x, min y, max x, max y of the item positions of one group, or {@code null} if it has no slots. */
    public static int[] bounds(List<Slot> slots, boolean player) {
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
