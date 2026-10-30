package com.simplelib.api.client.ui;

import com.simplelib.api.client.keys.KeyConflicts;
import com.simplelib.api.client.keys.KeyboardLayout;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The keyboard picture of the key binding page (UI28-1 / WP-25): raised key caps in the SimpleLib style, green when one action
 * sits on the key, red when several do, grey when it is free. A small symbol repeats the state (tick, exclamation mark, dash),
 * so the colours alone are not needed (red-green weakness). Positions come from {@link KeyboardLayout}. Client only.
 */
public final class UiKeyboard {
    /** Cap colours: white text reads 5.5:1 (green), 6.0:1 (red), 5.1:1 (grey) on them. */
    public static final int BOUND = 0xFF2F7D3A, SHARED = 0xFFB03030, FREE = 0xFF6B6F76;
    private static final String[] TICK = {"....#", "...#.", "#.#..", ".#..."}, BANG = {"..#..", "..#..", "..#..", ".....", "..#.."},
            DASH = {".....", ".....", "#####"};

    private UiKeyboard() {}

    public static int color(KeyConflicts.State state) {
        return switch (state) {
            case BOUND -> BOUND;
            case SHARED -> SHARED;
            case FREE -> FREE;
        };
    }

    /**
     * Draws all caps with the picture's corner at ({@code ox}, {@code oy}); {@code scale} is the pixel size of a quarter key
     * (see {@link KeyboardLayout#scale}). {@code state} gives a cell's state, {@code hovered} / {@code selected} are key codes (-1 = none).
     */
    public static void draw(GuiGraphicsExtractor g, Font font, List<KeyboardLayout.Cell> cells, int ox, int oy, int scale,
            Function<KeyboardLayout.Cell, KeyConflicts.State> state, int hovered, int selected) {
        for (KeyboardLayout.Cell c : cells) {
            int x = ox + c.x() * scale, y = oy + c.y() * scale, w = c.w() * scale - 1, h = c.h() * scale - 1;
            KeyConflicts.State s = state.apply(c);
            int face = color(s);
            if (c.code() == hovered) face = UiPalette.mix(face, 0xFFFFFFFF, 0.22);
            UiBoxes.raised(g, x, y, w, h, face);
            if (c.code() == selected) {
                int rim = 0xFFFFFFFF;
                g.fill(x - 1, y - 1, x + w + 1, y, rim);
                g.fill(x - 1, y + h, x + w + 1, y + h + 1, rim);
                g.fill(x - 1, y, x, y + h, rim);
                g.fill(x + w, y, x + w + 1, y + h, rim);
            }
            int ink = UiContrast.ensure(0xFFFFFFFF, face);
            int tw = font.width(c.label());
            if (tw <= w - 2) g.text(font, c.label(), x + (w - tw) / 2, y + (h - 8) / 2 + 1, ink, false);
            if (w >= 12 && h >= 12) symbol(g, x + w - 7, y + 2, switch (s) {
                case BOUND -> TICK;
                case SHARED -> BANG;
                case FREE -> DASH;
            }, ink);
        }
    }

    private static void symbol(GuiGraphicsExtractor g, int x, int y, String[] rows, int color) {
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length(); c++) {
                if (rows[r].charAt(c) == '#') g.fill(x + c, y + r, x + c + 1, y + r + 1, color);
            }
        }
    }
}
