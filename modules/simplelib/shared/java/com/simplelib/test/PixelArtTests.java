package com.simplelib.test;

import com.simplelib.api.image.PixelArt;
import com.simplelib.api.image.PixelPalette;
import com.simplelib.api.image.PixelRle;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Picture conversion of {@code com.simplelib.api.image} (plan docs/ai/PLAN-B-PICTURE-2026-10-19.md): Lab nearest colour,
 * deterministic dithering, run-length round trip, size limit and scaling. Pure maths, run as GameTests like the rest.
 */
final class PixelArtTests {
    private static final int BLACK = 0x000000, WHITE = 0xFFFFFF, RED = 0xB02E26, BLUE = 0x3C44AA;

    private PixelArtTests() {}

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    private static int[] opaque(int... rgb) {
        int[] out = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            out[i] = 0xFF000000 | rgb[i];
        }
        return out;
    }

    /** Known colours land on the expected entries; Lab of black/white; ties keep the lower index. */
    static void labNearest(GameTestHelper h) {
        PixelPalette p = new PixelPalette(BLACK, WHITE, RED, BLUE);
        check(h, p.nearest(0x101010) == 0, "near black -> black");
        check(h, p.nearest(0xF0F0F0) == 1, "near white -> white");
        check(h, p.nearest(0xC03030) == 2, "reddish -> red");
        check(h, p.nearest(0x2830B0) == 3, "bluish -> blue");
        check(h, p.nearest(RED) == 2 && p.nearest(BLUE) == 3, "exact colours stay");
        float[] white = PixelPalette.lab(255, 255, 255), black = PixelPalette.lab(0, 0, 0);
        check(h, Math.abs(white[0] - 100) < 0.1 && Math.abs(white[1]) < 0.1 && Math.abs(white[2]) < 0.1, "Lab white " + Arrays.toString(white));
        check(h, Math.abs(black[0]) < 0.01, "Lab black " + Arrays.toString(black));
        // Lab, not RGB: this dark slate blue is nearer to mid grey by RGB distance, but nearer to black for the eye.
        PixelPalette dark = new PixelPalette(0x000000, 0x808080);
        check(h, dark.nearest(0x27415B) == 0, "dark slate blue -> black in Lab");
        check(h, new PixelPalette(0x808080, 0x808080).nearest(0x808080) == 0, "tie keeps the lower index");
        check(h, PixelPalette.distance2(RED, RED) == 0f && PixelPalette.distance2(BLACK, WHITE) > 9999f, "distance");
        h.succeed();
    }

    /** Same input, same output; palette colours stay exact; Floyd-Steinberg keeps the mean brightness; Bayer repeats every 4. */
    static void ditherDeterministic(GameTestHelper h) {
        PixelPalette bw = new PixelPalette(BLACK, WHITE);
        int w = 32, hgt = 8;
        int[] ramp = new int[w * hgt];
        for (int y = 0; y < hgt; y++) {
            for (int x = 0; x < w; x++) {
                int v = x * 255 / (w - 1);
                ramp[y * w + x] = 0xFF000000 | v << 16 | v << 8 | v;
            }
        }
        for (PixelArt.Dither d : PixelArt.Dither.values()) {
            int[] a = PixelArt.quantize(ramp, w, hgt, bw, d), b = PixelArt.quantize(ramp, w, hgt, bw, d);
            check(h, Arrays.equals(a, b), d + " is deterministic");
            int[] flat = opaque(BLACK, WHITE, WHITE, BLACK);
            check(h, Arrays.equals(PixelArt.quantize(flat, 2, 2, bw, d), new int[]{0, 1, 1, 0}), d + " keeps palette colours exact");
        }
        // no dither: black up to L* 50 (sRGB ~119, x = 14), white from there on, the same in every row
        int[] none = PixelArt.quantize(ramp, w, hgt, bw, PixelArt.Dither.NONE);
        for (int y = 0; y < hgt; y++) {
            for (int x = 0; x < w; x++) {
                check(h, none[y * w + x] == (x <= 14 ? 0 : 1), "no dither: hard split, wrong at " + x + "," + y);
            }
        }
        int[] fs = PixelArt.quantize(ramp, w, hgt, bw, PixelArt.Dither.FLOYD_STEINBERG);
        int whites = 0;
        for (int v : fs) {
            whites += v;
        }
        double share = whites / (double) fs.length;
        check(h, Math.abs(share - 0.5) < 0.05, "Floyd-Steinberg keeps the mean brightness: " + share);
        check(h, fs[4 * w + 10] != fs[4 * w + 11] || fs[4 * w + 10] != fs[4 * w + 12], "Floyd-Steinberg mixes in the ramp");
        int[] grey = new int[8 * 8];
        Arrays.fill(grey, 0xFF808080);
        int[] bayer = PixelArt.quantize(grey, 8, 8, bw, PixelArt.Dither.BAYER);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                check(h, bayer[y * 8 + x] == bayer[(y + 4) * 8 + x + 4], "Bayer repeats every 4 pixels");
            }
        }
        int ones = Arrays.stream(bayer).sum();
        check(h, ones > 0 && ones < 64, "Bayer mixes grey: " + ones);
        int[] clear = {0x00FFFFFF, 0x7FFFFFFF, 0x80FFFFFF};
        check(h, Arrays.equals(PixelArt.quantize(clear, 3, 1, bw, PixelArt.Dither.FLOYD_STEINBERG), new int[]{-1, -1, 1}), "alpha below 128 stays free");
        h.succeed();
    }

    /** decode(encode(row)) == row for random rows; the documented example; malformed text is refused. */
    static void rleRoundTrip(GameTestHelper h) {
        Random random = new Random(42);
        for (int n = 0; n < 200; n++) {
            int[] row = new int[1 + random.nextInt(300)];
            int value = 0;
            for (int i = 0; i < row.length; i++) {
                if (random.nextInt(4) == 0) {
                    value = random.nextInt(PixelRle.MAX_ENTRIES + 1) - 1;
                }
                row[i] = value;
            }
            String text = PixelRle.encode(row);
            check(h, Arrays.equals(PixelRle.decode(text, 300), row), "round trip " + text);
            check(h, text.length() <= row.length * 2, "never longer than needed");
        }
        check(h, Arrays.equals(PixelRle.decode("3a2b.c", 16), new int[]{0, 0, 0, 1, 1, -1, 2}), "example 3a2b.c");
        check(h, PixelRle.encode(new int[]{51, 51, -1, -1, -1, 0}).equals("2Z3.a"), "encode Z and empty runs");
        check(h, PixelRle.encode(new int[128]).equals("128a"), "a full run");
        for (String bad : new String[]{"3", "1a", "03a", "a#", "a b", "1234567a"}) {
            check(h, PixelRle.decode(bad, 1000) == null, "refuses " + bad);
        }
        check(h, PixelRle.decode("5a", 4) == null && PixelRle.decode("4a", 4).length == 4, "max length");
        check(h, PixelRle.decode("", 4).length == 0, "empty row");
        h.succeed();
    }

    /** fit keeps the aspect ratio under the limit; scale averages down and repeats up. */
    static void sizeLimit(GameTestHelper h) {
        check(h, Arrays.equals(PixelArt.fit(256, 128, 128), new int[]{128, 64}), "256x128 -> 128x64");
        check(h, Arrays.equals(PixelArt.fit(100, 50, 128), new int[]{100, 50}), "small stays");
        check(h, Arrays.equals(PixelArt.fit(1000, 1, 128), new int[]{128, 1}), "thin stays at least 1");
        check(h, Arrays.equals(PixelArt.fit(300, 400, 64), new int[]{48, 64}), "300x400 -> 48x64");
        boolean thrown = false;
        try {
            PixelArt.fit(0, 5, 64);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        check(h, thrown, "empty picture refused");
        int[] four = opaque(BLACK, WHITE, 0x000000, 0xFFFFFF, BLACK, WHITE, BLACK, WHITE, RED, RED, BLUE, BLUE, RED, RED, BLUE, BLUE);
        int[] two = PixelArt.scale(four, 4, 4, 2, 2);
        check(h, two[0] == 0xFF7F7F7F && two[2] == (0xFF000000 | RED) && two[3] == (0xFF000000 | BLUE), "area average " + Integer.toHexString(two[0]));
        int[] up = PixelArt.scale(opaque(RED, BLUE), 2, 1, 4, 2);
        check(h, up[0] == up[1] && up[1] == (0xFF000000 | RED) && up[6] == (0xFF000000 | BLUE), "scaling up repeats");
        int[] half = PixelArt.scale(new int[]{0xFFFF0000, 0x00000000}, 2, 1, 1, 1);
        check(h, half[0] == 0x7FFF0000, "transparent pixels do not darken: " + Integer.toHexString(half[0]));
        h.succeed();
    }
}
