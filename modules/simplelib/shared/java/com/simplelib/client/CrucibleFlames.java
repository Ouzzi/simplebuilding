package com.simplelib.client;

import com.simplelib.crucible.HeatLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Heat display of the crucible screen (owner feedback N12, images 1/2/4 in previews/refs-n12).
 * <ul>
 *   <li>{@link #draw}: a pixel flame strip rising from the bottom of the crucible box - single pointed tongues with a
 *       red outer edge, red-orange, orange and a bright yellow core low down, a glowing ember row at the very bottom,
 *       sparks breaking off and rising (some twinkle as a small plus). Medium heat is lower, high about a third of the
 *       crucible box, extreme the same shape in blue and a little higher.</li>
 *   <li>{@link #drawSlot}: the furnace-like alternative (image 4) - an inset slot filling up with flames.</li>
 * </ul>
 * One GUI pixel per cell. {@code tools/textures/crucible_n12_2026_10_06.py} draws the same flames for the preview;
 * keep both in step.
 */
public final class CrucibleFlames {
    /** Edge, edge-inside, body, core, base band, ember dark, ember bright, spark. */
    private static final int[] FIRE = {0xFFC81E12, 0xFFE8451A, 0xFFF7921C, 0xFFFFC832, 0xFFFFE98A, 0xFF7A1808, 0xFFFF7A1E, 0xFFFFE070};
    private static final int[] SOUL = {0xFF1A3FA8, 0xFF2370D8, 0xFF3FA9F5, 0xFF8EE0FF, 0xFFDDF8FF, 0xFF0E2A6A, 0xFF3F9CF0, 0xFFC8F4FF};
    static final int SPACING = 10;

    private CrucibleFlames() {}

    static int hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** Typical tongue height in px for {@code heat} over a crucible box {@code section} px high (0 = none). */
    public static int targetPixels(HeatLevel heat, int section) {
        double part = switch (heat) {
            case NONE -> 0.0;
            case MEDIUM -> 1.0 / 4.5;
            case HIGH -> 1.0 / 3.0;
            case EXTREME -> 1.1 / 3.0;
        };
        return (int) Math.round(section * part);
    }

    /** Flame height in px of column {@code c} at time {@code millis}: a low band plus the highest tongue over it. */
    static int height(int c, long millis, int target) {
        double t = millis / 1000.0;
        int flick = (int) (millis / 110);
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
            double amp = 0.78 + 0.3 * Math.sin(t * 2.3 + j * 1.7) + 0.14 * Math.sin(t * 5.1 + j * 0.9) + (hash(j, 9) & 3) * 0.06;
            best = Math.max(best, amp * Math.pow(1 - d, 1.5));
        }
        int jitter = (hash(c, flick) & 3) == 0 ? 1 : 0;
        int h = (int) Math.round(target * (0.3 + 0.75 * best)) + jitter;
        return Math.max(2, Math.min(target * 8 / 5, h));
    }

    /** Colour index of the flame cell {@code k} (0 = bottom) in a column of height {@code h}, {@code edge} px from outside. */
    static int shade(int k, int h, int edge, int ember) {
        if (k == 0) return ember;
        if (edge <= 1) return 0;
        if (k <= 2) return 4;
        if (edge == 2) return 1;
        return k < h * 35 / 100 && edge >= 4 ? 3 : 2;
    }

    /** Flames over {@code x .. x + width}, standing on {@code bottom}. */
    public static void draw(GuiGraphicsExtractor g, int x, int bottom, int width, int section, HeatLevel heat, long millis) {
        int target = targetPixels(heat, section);
        if (target <= 0 || width <= 0) return;
        int[] colors = heat == HeatLevel.EXTREME ? SOUL : FIRE;
        int flick = (int) (millis / 110);
        int[] hs = new int[width + 6];
        for (int i = 0; i < hs.length; i++) hs[i] = height(i - 3, millis, target);
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
                    color = colors[shade(k, h, edge, ember)];
                }
                if (color != runColor) {
                    if (runColor != 0) g.fill(x + c, bottom - k, x + c + 1, bottom - runStart, runColor);
                    runStart = k;
                    runColor = color;
                }
            }
        }
        // Sparks: break off a tongue and rise, swaying; some twinkle as a plus for a moment.
        int life = 9;
        for (int s = 0; s < Math.max(2, width / 14); s++) {
            int clock = flick + (hash(s, 13) & 63);
            int age = clock % life, cycle = clock / life;
            if (age > 6) continue;
            int c = Math.floorMod(hash(s, cycle), width);
            int sway = (int) Math.round(Math.sin((age + s) * 1.3));
            int sx = Math.max(0, Math.min(width - 1, c + sway));
            int sy = bottom - hs[c + 3] - 2 - age * 2;
            g.fill(x + sx, sy, x + sx + 1, sy + 1, age < 3 ? colors[7] : colors[1]);
            if (age == 3 && (hash(s, cycle + 7) & 1) == 0 && sx > 0 && sx < width - 1) {
                g.fill(x + sx - 1, sy, x + sx + 2, sy + 1, colors[6]);
                g.fill(x + sx, sy - 1, x + sx + 1, sy + 2, colors[6]);
                g.fill(x + sx, sy, x + sx + 1, sy + 1, colors[7]);
            }
        }
    }

    /** Fill level in px (of 16) of the heat slot. */
    public static int slotLevel(HeatLevel heat) {
        return switch (heat) {
            case NONE -> 0;
            case MEDIUM -> 6;
            case HIGH -> 11;
            case EXTREME -> 16;
        };
    }

    /**
     * Image 4: a 16x16 inset slot at {@code x, y} filling up with flames - bright base line, orange body with darker
     * flickering tongues, grey heat waves above while it burns. The slot itself is drawn by the screen.
     */
    public static void drawSlot(GuiGraphicsExtractor g, int x, int y, HeatLevel heat, long millis) {
        int level = slotLevel(heat);
        if (level <= 0) return;
        boolean soul = heat == HeatLevel.EXTREME;
        int body = soul ? 0xFF3FA9F5 : 0xFFFFAE1E, tongue = soul ? 0xFF1E5FD0 : 0xFFF26B12, base = soul ? 0xFFC8F4FF : 0xFFFFE34A;
        int top = y + 16 - level;
        g.fill(x, top, x + 16, y + 15, body);
        g.fill(x + 1, y + 15, x + 15, y + 16, base);
        int flick = (int) (millis / 160);
        for (int i = 0; i < 3; i++) {
            int tx = x + 2 + i * 5;
            int h = Math.min(level - 3, 4 + (hash(i, flick) & 3));
            for (int k = 0; k < h; k++) {
                int dx = ((k + i + flick) & 2) == 0 ? 0 : 1;
                g.fill(tx + dx, y + 13 - k, tx + dx + 2, y + 14 - k, tongue);
            }
        }
        int room = 16 - level - 2;
        for (int i = 0; i < 3 && room > 0; i++) {
            int wx = x + 3 + i * 4;
            for (int k = 0; k < Math.min(5, room); k++) {
                int dx = ((k + flick + i) & 2) == 0 ? 0 : 1;
                g.fill(wx + dx, top - 2 - k, wx + dx + 1, top - 1 - k, 0xFF5E5E5E);
            }
        }
    }
}
