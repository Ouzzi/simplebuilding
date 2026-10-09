package com.simplemaps.net;

import com.simplemaps.WayfinderData;
import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** Deflate packing of one tile (colours then heights) for the network. */
public final class TileCodec {
    private TileCodec() {}

    public static byte[] pack(byte[] colors, byte[] heights) {
        byte[] raw = new byte[WayfinderData.AREA * 2];
        System.arraycopy(colors, 0, raw, 0, WayfinderData.AREA);
        System.arraycopy(heights, 0, raw, WayfinderData.AREA, WayfinderData.AREA);
        Deflater deflater = new Deflater(3);
        try {
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer));
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /** Colours followed by heights (2 x 16384 bytes), or null for empty or broken data. */
    public static byte[] unpack(byte[] data) {
        if (data == null || data.length == 0) return null;
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data);
            byte[] raw = new byte[WayfinderData.AREA * 2];
            int read = 0;
            while (read < raw.length && !inflater.finished()) {
                int n = inflater.inflate(raw, read, raw.length - read);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break;
                read += n;
            }
            return read == raw.length ? raw : null;
        } catch (DataFormatException e) {
            return null;
        } finally {
            inflater.end();
        }
    }
}
