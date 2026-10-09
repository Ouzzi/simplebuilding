package com.simplebuilding.util;

/**
 * Pure geometry of the compact resonance field right of a vanilla recipe-book button (owner 2026-10-08): as high as
 * the button and framed like it (round 2, 2026-10-09: vanilla style), a 9x9 stone heart centred vertically, the white
 * value with shadow after it.
 */
public final class TrimStatsLayout {
    public static final int BOOK_BUTTON_WIDTH = 20;
    public static final int GAP = 2;
    public static final int HEIGHT = 18;
    public static final int ICON_SIZE = 9;
    /** Heart: 3 px from the left edge, vertically centred ((18 - 9) / 2 = 4). */
    public static final int ICON_INSET_X = 3, ICON_INSET_Y = (HEIGHT - ICON_SIZE) / 2;
    public static final int TEXT_X = ICON_INSET_X + ICON_SIZE + 2;
    /** 3 px inside the right edge plus the 1 px text shadow. */
    public static final int TEXT_PAD_RIGHT = 4;
    /** Text row: font height 8 centred in the field ((18 - 8) / 2 = 5). */
    public static final int TEXT_Y = (HEIGHT - 8) / 2;

    private TrimStatsLayout() {
    }

    public record Panel(int x, int y, int width, int height, int iconX, int iconY, int textX, int textY) {
    }

    public static Panel panel(int bookX, int bookY, int textWidth) {
        int width = TEXT_X + Math.max(0, textWidth) + TEXT_PAD_RIGHT;
        int x = bookX + BOOK_BUTTON_WIDTH + GAP;
        return new Panel(x, bookY, width, HEIGHT, x + ICON_INSET_X, bookY + ICON_INSET_Y, x + TEXT_X, bookY + TEXT_Y);
    }
}
