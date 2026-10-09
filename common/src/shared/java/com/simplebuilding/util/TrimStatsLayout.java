package com.simplebuilding.util;

/**
 * Pure geometry of the compact resonance field right of a vanilla recipe-book button (owner 2026-10-08): as high as
 * the button and framed like it (round 2, 2026-10-09: vanilla style), a 9x9 cobblestone heart (round 4, N29) centred vertically -
 * a diamond heart at maximum resonance - the white
 * value with shadow after it.
 */
public final class TrimStatsLayout {
    public static final int BOOK_BUTTON_WIDTH = 20;
    public static final int GAP = 2;
    public static final int HEIGHT = 18;
    /** The heart sprite: 9 wide, 9 high (owner N29, 2026-10-09: one pixel narrower again than round 3). */
    public static final int ICON_WIDTH = 9, ICON_HEIGHT = 9;
    /** GUI sprites of the heart: cobblestone, and diamond at maximum resonance (owner N29). */
    public static final String HEART_SPRITE = "resonance_heart", HEART_SPRITE_MAX = "resonance_heart_max";
    /** Heart: 3 px from the left edge, vertically centred ((18 - 9) / 2 = 4). */
    public static final int ICON_INSET_X = 3, ICON_INSET_Y = (HEIGHT - ICON_HEIGHT) / 2;
    public static final int TEXT_X = ICON_INSET_X + ICON_WIDTH + 2;
    /** 3 px inside the right edge plus the 1 px text shadow. */
    public static final int TEXT_PAD_RIGHT = 4;
    /** Text row: font height 8 centred in the field ((18 - 8) / 2 = 5). */
    public static final int TEXT_Y = (HEIGHT - 8) / 2;

    private TrimStatsLayout() {
    }

    public record Panel(int x, int y, int width, int height, int iconX, int iconY, int textX, int textY) {
    }

    /**
     * Maximum resonance: the value has reached the configured base (all three factors at 1.0), with the same half-cent
     * tolerance as the shown value ("3.00x"); the tooltip calls it "capped".
     */
    public static boolean isMaxResonance(double total, double base) {
        return total >= base - 0.005;
    }

    /** The heart sprite for this resonance: the diamond heart once it is at its maximum. */
    public static String heartSprite(double total, double base) {
        return isMaxResonance(total, base) ? HEART_SPRITE_MAX : HEART_SPRITE;
    }

    public static Panel panel(int bookX, int bookY, int textWidth) {
        int width = TEXT_X + Math.max(0, textWidth) + TEXT_PAD_RIGHT;
        int x = bookX + BOOK_BUTTON_WIDTH + GAP;
        return new Panel(x, bookY, width, HEIGHT, x + ICON_INSET_X, bookY + ICON_INSET_Y, x + TEXT_X, bookY + TEXT_Y);
    }
}
