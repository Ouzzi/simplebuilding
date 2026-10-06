package com.simplelib.client;

import com.simplelib.crucible.HeatLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Pixel flames rising from the bottom of the crucible part of the screen (owner feedback 2026-10-06,
 * image 20): an irregular jagged edge, red outside, orange, yellow inside, single sparks breaking off
 * above, the tongues wandering and flickering. Medium heat is lower, high covers about a third of the
 * crucible background, extreme burns blue and a little higher. Drawn in 2x2 GUI pixel cells.
 * {@code tools/textures/crucible_ui_v3_preview_2026_10_06.py} draws the same flames for the preview;
 * keep both in step.
 */
public final class CrucibleFlames {
    public static final int CELL = 2;
    private static final int[] FIRE = {0xFFFFD43B, 0xFFF7941D, 0xFFE8401C, 0xFFF26B21};
    private static final int[] SOUL = {0xFFB8F2FF, 0xFF3FA9F5, 0xFF1E5FD0, 0xFF6CC8FF};

    private CrucibleFlames() {}

    static int hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** Average flame height in cells for {@code heat} over a crucible background {@code section} px high (0 = none). */
    public static int targetCells(HeatLevel heat, int section) {
        double part = switch (heat) {
            case NONE -> 0.0;
            case MEDIUM -> 1.0 / 4.5;
            case HIGH -> 1.0 / 3.0;
            case EXTREME -> 1.1 / 3.0;
        };
        return (int) Math.round(section * part / CELL);
    }

    /** Height in cells of column {@code c} at time {@code millis}. */
    static int height(int c, long millis, int target) {
        double t = millis / 1000.0;
        int flick = (int) (millis / 110);
        double wave = 0.55 + 0.22 * Math.sin(c * 0.3 + t * 2.1) + 0.15 * Math.sin(c * 0.71 - t * 3.3) + 0.08 * Math.sin(c * 1.6 + t * 5.0);
        int jitter = (hash(c / 2, flick) & 3) - 1;
        return Math.max(1, Math.min(target * 8 / 5, (int) Math.round(wave / 0.55 * target) + jitter));
    }

    /** Flames over {@code x .. x + width}, standing on {@code bottom}. */
    public static void draw(GuiGraphicsExtractor g, int x, int bottom, int width, int section, HeatLevel heat, long millis) {
        int target = targetCells(heat, section);
        if (target <= 0) return;
        int[] colors = heat == HeatLevel.EXTREME ? SOUL : FIRE;
        int flick = (int) (millis / 110);
        int cols = (width + CELL - 1) / CELL;
        for (int c = 0; c < cols; c++) {
            int h = height(c, millis, target);
            int orange = h * 62 / 100 + (hash(c, flick + 77) & 1);
            int yellow = h * 30 / 100 + (hash(c, flick + 151) & 1);
            int x0 = x + c * CELL, x1 = Math.min(x + width, x0 + CELL);
            for (int k = 0; k < h; k++) {
                int color = k < yellow ? colors[0] : k < orange ? colors[1] : colors[2];
                g.fill(x0, bottom - (k + 1) * CELL, x1, bottom - k * CELL, color);
            }
        }
        // Sparks: single cells breaking off a tongue and rising for a few steps.
        int life = 7;
        for (int s = 0; s < Math.max(2, cols / 6); s++) {
            int clock = flick + (hash(s, 13) & 63);
            int age = clock % life, cycle = clock / life;
            if (age > 4) continue;
            int c = Math.floorMod(hash(s, cycle), cols);
            int k = height(c, millis, target) + 1 + age;
            int x0 = x + c * CELL;
            g.fill(x0, bottom - (k + 1) * CELL, Math.min(x + width, x0 + CELL), bottom - k * CELL, age < 2 ? colors[2] : colors[3]);
        }
    }
}
