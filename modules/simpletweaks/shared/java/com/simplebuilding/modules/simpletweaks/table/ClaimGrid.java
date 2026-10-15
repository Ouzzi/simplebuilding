package com.simplebuilding.modules.simpletweaks.table;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/** Cells of the claim map: one byte per chunk (0 = free, 1..255 = palette entry), run-length coded for the wire; owner colors. */
public final class ClaimGrid {
    public static final int MAX_SIDE = 64, MAX_CELLS = MAX_SIDE * MAX_SIDE, MAX_PALETTE = 255;
    /** Worst case of {@link #encode}: a pair per cell. */
    public static final int MAX_ENCODED = MAX_CELLS * 2;

    private ClaimGrid() {}

    /** Pairs {@code (index, run - 1)}, runs of 1..256. */
    public static byte[] encode(byte[] cells) {
        var out = new ByteArrayOutputStream();
        for (int i = 0; i < cells.length; ) {
            int j = i + 1;
            while (j < cells.length && cells[j] == cells[i] && j - i < 256) j++;
            out.write(cells[i]); out.write(j - i - 1);
            i = j;
        }
        return out.toByteArray();
    }

    /** Inverse of {@link #encode}; null if the data does not describe exactly {@code count} cells. */
    public static byte[] decode(byte[] rle, int count) {
        if (count < 0 || count > MAX_CELLS || rle.length % 2 != 0) return null;
        var cells = new byte[count];
        int at = 0;
        for (int i = 0; i < rle.length; i += 2) {
            int run = (rle[i + 1] & 0xFF) + 1;
            if (at + run > count) return null;
            java.util.Arrays.fill(cells, at, at + run, rle[i]);
            at += run;
        }
        return at == count ? cells : null;
    }

    /** A stable, readable color (0xRRGGBB) derived from an owner id; the same owner always gets the same color. */
    public static int color(UUID id) {
        long h = id.getMostSignificantBits() * 0x9E3779B97F4A7C15L ^ id.getLeastSignificantBits() * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        float hue = ((h & 0xFFFF) / 65535f), sat = .55f + ((h >>> 16) & 0xFF) / 255f * .3f, val = .7f + ((h >>> 24) & 0xFF) / 255f * .25f;
        float h6 = hue * 6f; int i = (int) h6; float f = h6 - i, pp = val * (1 - sat), q = val * (1 - sat * f), t = val * (1 - sat * (1 - f));
        float[] rgb = switch (i % 6) { case 0 -> new float[]{val, t, pp}; case 1 -> new float[]{q, val, pp}; case 2 -> new float[]{pp, val, t};
            case 3 -> new float[]{pp, q, val}; case 4 -> new float[]{t, pp, val}; default -> new float[]{val, pp, q}; };
        return Math.round(rgb[0] * 255) << 16 | Math.round(rgb[1] * 255) << 8 | Math.round(rgb[2] * 255);
    }
}
