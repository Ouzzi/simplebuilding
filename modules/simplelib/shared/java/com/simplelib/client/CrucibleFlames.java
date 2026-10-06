package com.simplelib.client;

import com.simplelib.crucible.HeatLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Heat display of the crucible screen (owner feedback N12/N12b, images 1/2 in previews/refs-n12): a pixel flame strip
 * rising from the bottom of the crucible box - pointed leaning tongues with a red outer edge, red-orange, orange and a
 * bright yellow core low down, a glowing ember row at the very bottom, sparks breaking off and rising (some twinkle as
 * a small plus). The levels differ clearly (owner N12b): high burns tall and lively, extreme the same in blue; medium is
 * low and calm (slow, no pale base band, few sparks); the afterglow after the heat source is gone shows only embers and
 * a low red glow. One GUI pixel per cell. {@code tools/textures/crucible_n12b_2026_10_06.py} draws the same flames for
 * the preview; keep both in step.
 */
public final class CrucibleFlames {
    /** Edge, edge-inside, body, core, base band, ember dark, ember bright, spark. */
    private static final int[] FIRE = {0xFFC81E12, 0xFFE8451A, 0xFFF7921C, 0xFFFFC832, 0xFFFFE98A, 0xFF7A1808, 0xFFFF7A1E, 0xFFFFE070};
    private static final int[] SOUL = {0xFF1A3FA8, 0xFF2370D8, 0xFF3FA9F5, 0xFF8EE0FF, 0xFFDDF8FF, 0xFF0E2A6A, 0xFF3F9CF0, 0xFFC8F4FF};
    static final int SPACING = 10;
    /** Calmness: 0 lively (high, extreme), 1 medium, 2 afterglow. */
    static final int LIVELY = 0, MEDIUM = 1, GLOW = 2;

    private CrucibleFlames() {}

    static int hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** How calm the flames burn for {@code heat}; {@code glowing} = the source is gone, only the afterglow is left. */
    public static int calm(HeatLevel heat, boolean glowing) {
        if (glowing) return GLOW;
        return heat == HeatLevel.MEDIUM ? MEDIUM : LIVELY;
    }

    /** Typical tongue height in px over a crucible box {@code section} px high (0 = none). */
    public static int targetPixels(HeatLevel heat, boolean glowing, int section) {
        if (glowing) return heat == HeatLevel.NONE ? 0 : Math.max(3, (int) Math.round(section / 16.0));
        double part = switch (heat) {
            case NONE -> 0.0;
            case MEDIUM -> 1.0 / 6.5;
            case HIGH -> 1.0 / 3.0;
            case EXTREME -> 1.1 / 3.0;
        };
        return (int) Math.round(section * part);
    }

    static int flickMillis(int calm) {
        return calm == LIVELY ? 110 : calm == MEDIUM ? 190 : 280;
    }

    /** Flame height in px of column {@code c} at time {@code millis}: a low band plus the highest tongue over it. */
    static int height(int c, long millis, int target, int calm) {
        double t = millis / 1000.0 * (calm == LIVELY ? 1.0 : calm == MEDIUM ? 0.55 : 0.35);
        int flick = (int) (millis / flickMillis(calm));
        double best = 0;
        int first = Math.floorDiv(c, SPACING) - 1;
        for (int j = first; j <= first + 2; j++) {
            double center = j * SPACING + 5 + 1.5 * Math.sin(t * 1.7 + j * 2.1);
            double half = 4.5 + (hash(j, 5) & 3) * 0.5;
            // Leaning tongues: one side steeper (image 1).
            double skew = (hash(j, 3) & 1) == 0 ? 0.3 : -0.3;
            double off = c + 0.5 - center;
            double d = Math.abs(off) / (half * (off < 0 ? 1 + skew : 1 - skew));
            if (d >= 1) continue;
            double amp = calm == LIVELY
                    ? 0.78 + 0.3 * Math.sin(t * 2.3 + j * 1.7) + 0.14 * Math.sin(t * 5.1 + j * 0.9) + (hash(j, 9) & 3) * 0.06
                    : 0.8 + 0.12 * Math.sin(t * 2.3 + j * 1.7) + (hash(j, 9) & 3) * 0.04;
            best = Math.max(best, amp * Math.pow(1 - d, 1.5));
        }
        int jitter = calm == LIVELY && (hash(c, flick) & 3) == 0 ? 1 : 0;
        int h = (int) Math.round(target * (0.3 + 0.75 * best)) + jitter;
        return Math.max(2, Math.min(target * 8 / 5, h));
    }

    /** Colour index of the flame cell {@code k} (0 = bottom) in a column of height {@code h}, {@code edge} px from outside. */
    static int shade(int k, int h, int edge, int ember, int calm) {
        if (k == 0) return ember;
        if (edge <= 1) return 0;
        if (calm == GLOW) return 1;
        if (calm == MEDIUM) return k <= 1 ? 3 : edge == 2 ? 1 : 2;
        if (k <= 2) return 4;
        if (edge == 2) return 1;
        return k < h * 35 / 100 && edge >= 4 ? 3 : 2;
    }

    /** Flames over {@code x .. x + width}, standing on {@code bottom}. */
    public static void draw(GuiGraphicsExtractor g, int x, int bottom, int width, int section, HeatLevel heat, boolean glowing, long millis) {
        int target = targetPixels(heat, glowing, section);
        if (target <= 0 || width <= 0) return;
        int calm = calm(heat, glowing);
        int[] colors = heat == HeatLevel.EXTREME ? SOUL : FIRE;
        int flick = (int) (millis / flickMillis(calm));
        int[] hs = new int[width + 6];
        for (int i = 0; i < hs.length; i++) hs[i] = height(i - 3, millis, target, calm);
        for (int c = 0; c < width; c++) {
            int h = hs[c + 3];
            int ember = (hash(c, flick / 3) & 3) == 0 ? 6 : 5;
            int runStart = 0, runColor = 0;
            for (int k = 0; k <= h; k++) {
                int color = 0;
                if (k < h) {
                    int edge = (h - k + 1) / 2;
                    for (int d = 1; d <= 3 && d < edge; d++) {
                        if (hs[c + 3 - d] <= k || hs[c + 3 + d] <= k) {
                            edge = d;
                            break;
                        }
                    }
                    color = colors[shade(k, h, edge, ember, calm)];
                }
                if (color != runColor) {
                    if (runColor != 0) g.fill(x + c, bottom - k, x + c + 1, bottom - runStart, runColor);
                    runStart = k;
                    runColor = color;
                }
            }
        }
        // Sparks: break off a tongue and rise, swaying; lively flames let some twinkle as a plus for a moment.
        int life = 9;
        int sparks = calm == LIVELY ? Math.max(2, width / 14) : Math.max(1, width / (calm == MEDIUM ? 40 : 70));
        for (int s = 0; s < sparks; s++) {
            int clock = flick + (hash(s, 13) & 63);
            int age = clock % life, cycle = clock / life;
            if (age > (calm == LIVELY ? 6 : 3)) continue;
            int c = Math.floorMod(hash(s, cycle), width);
            int sway = (int) Math.round(Math.sin((age + s) * 1.3));
            int sx = Math.max(0, Math.min(width - 1, c + sway));
            int sy = bottom - hs[c + 3] - 2 - age * 2;
            g.fill(x + sx, sy, x + sx + 1, sy + 1, age < 3 && calm != GLOW ? colors[7] : colors[1]);
            if (calm == LIVELY && age == 3 && (hash(s, cycle + 7) & 1) == 0 && sx > 0 && sx < width - 1) {
                g.fill(x + sx - 1, sy, x + sx + 2, sy + 1, colors[6]);
                g.fill(x + sx, sy - 1, x + sx + 1, sy + 2, colors[6]);
                g.fill(x + sx, sy, x + sx + 1, sy + 1, colors[7]);
            }
        }
    }
}
