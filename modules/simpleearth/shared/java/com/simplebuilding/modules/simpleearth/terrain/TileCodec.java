package com.simplebuilding.modules.simpleearth.terrain;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * One tile on the wire and on disk: 64 x 64 Vanilla map colour ids (0 = unknown/unexplored) then 64 x 64 heights as
 * big-endian shorts, deflated. Pure Java for JUnit.
 */
public final class TileCodec {
    public static final int RAW = TileMath.AREA * 3;

    private TileCodec() {}

    public static byte[] encode(byte[] colors, short[] heights) {
        byte[] raw = new byte[RAW];
        System.arraycopy(colors, 0, raw, 0, TileMath.AREA);
        for (int k = 0; k < TileMath.AREA; k++) {
            raw[TileMath.AREA + 2 * k] = (byte) (heights[k] >> 8);
            raw[TileMath.AREA + 2 * k + 1] = (byte) heights[k];
        }
        Deflater d = new Deflater(Deflater.BEST_SPEED);
        try {
            d.setInput(raw);
            d.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            byte[] buf = new byte[4096];
            while (!d.finished()) out.write(buf, 0, d.deflate(buf));
            return out.toByteArray();
        } finally {
            d.end();
        }
    }

    /** Decodes into {@code colors}/{@code heights}; false on broken data (the arrays are then undefined). */
    public static boolean decode(byte[] data, byte[] colors, short[] heights) {
        if (data == null || data.length == 0 || data.length > 64 * 1024) return false;
        Inflater in = new Inflater();
        try {
            in.setInput(data);
            byte[] raw = new byte[RAW];
            int n = 0;
            while (n < RAW && !in.finished()) {
                int got = in.inflate(raw, n, RAW - n);
                if (got == 0 && (in.needsInput() || in.needsDictionary())) return false;
                n += got;
            }
            if (n != RAW) return false;
            System.arraycopy(raw, 0, colors, 0, TileMath.AREA);
            for (int k = 0; k < TileMath.AREA; k++) {
                heights[k] = (short) (((raw[TileMath.AREA + 2 * k] & 0xFF) << 8) | (raw[TileMath.AREA + 2 * k + 1] & 0xFF));
            }
            return true;
        } catch (DataFormatException e) {
            return false;
        } finally {
            in.end();
        }
    }
}
