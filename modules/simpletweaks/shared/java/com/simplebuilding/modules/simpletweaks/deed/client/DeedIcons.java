package com.simplebuilding.modules.simpletweaks.deed.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Small pixel icons of the deed card (WH26-9, from the accepted preview A): the claim-credit coin (own icon of the
 * credits, owner wish WH26-9), the key of the rights tab and the arrow of a trade. Drawn as fills, one character per
 * pixel, so no texture atlas entry is needed.
 */
final class DeedIcons {
    private DeedIcons() {}

    private static final String[] COIN = {
            "...yyyyyy...", "..yYYYYYYy..", ".yYYooooYYy.", "yYYoYYYYoYYy", "yYoYyyyYYoYy", "yYoYy...yYoY",
            "yYoYy...yYoY", "yYoYyyyYYoYy", "yYYoYYYYoYYy", ".yYYooooYYy.", "..yYYYYYYy..", "...yyyyyy..."};
    private static final String[] KEY = {
            "..yyyy....", "..yYYy....", "..yYYy....", "...yy.....", "...yy.....", "...yyyyy..", "...yy.yy..",
            "...yyyy...", "...yy.....", "...yyyy..."};
    private static final String[] ARROW = {"......y.", "......yy", "yyyyyyyy", "yyyyyyyy", "......yy", "......y."};
    public static final int COIN_SIZE = 12;

    /** The claim-credit coin, 12 x 12. */
    static void coin(GuiGraphicsExtractor g, int x, int y) { draw(g, x, y, COIN, 'y', 0xFF8A5A00, 'Y', 0xFFF5C518, 'o', 0xFFFFE27A); }
    /** The rights key, 10 x 10. */
    static void key(GuiGraphicsExtractor g, int x, int y) { draw(g, x, y, KEY, 'y', 0xFFD8B640, 'Y', 0xFFFFF2B0, 'o', 0); }
    /** A trade arrow, 8 x 6, in {@code color}. */
    static void arrow(GuiGraphicsExtractor g, int x, int y, int color) { draw(g, x, y, ARROW, 'y', color, 'Y', color, 'o', color); }

    private static void draw(GuiGraphicsExtractor g, int x, int y, String[] rows, char a, int ca, char b, int cb, char c, int cc) {
        for (int j = 0; j < rows.length; j++) {
            String row = rows[j];
            int i = 0;
            while (i < row.length()) {
                char ch = row.charAt(i);
                int end = i + 1;
                while (end < row.length() && row.charAt(end) == ch) end++;
                int color = ch == a ? ca : ch == b ? cb : ch == c ? cc : 0;
                if (color != 0) g.fill(x + i, y + j, x + end, y + j + 1, color);
                i = end;
            }
        }
    }
}
