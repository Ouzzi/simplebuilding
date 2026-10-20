package com.simplemusic.upload;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds small synthetic Ogg Vorbis files for the JUnit and game tests (own bytes, no recording): real Ogg pages with CRC, the three
 * Vorbis header packets (identification with channels/rate, a comment with a tag, a placeholder setup) and audio pages
 * with granule positions. Not decodable audio - the server check never decodes.
 */
public final class OggFiles {
    public int channels = 1;
    public int rate = 44100;
    public long samples = 44100L * 10;
    public int audioPages = 3;
    public int audioBytesPerPage = 400;
    public int serial = 0x5EED;
    public String tag = "ARTIST=Somebody";
    /** Setup packet size; above 255 * 255 it spans pages. */
    public int setupBytes = 600;

    public static OggFiles mono() {
        return new OggFiles();
    }

    public byte[] build() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int seq = 0;
        byte[] id = new byte[30];
        System.arraycopy(new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'}, 0, id, 0, 7);
        id[11] = (byte) channels;
        OggCheck.put32(id, 12, rate);
        id[28] = (byte) 0xB8;
        id[29] = 1;
        write(out, OggCheck.page(2, 0, serial, seq++, List.of(30), id, 0, 30));
        byte[] comment = comment(tag);
        byte[] setup = new byte[setupBytes];
        System.arraycopy(new byte[]{5, 'v', 'o', 'r', 'b', 'i', 's'}, 0, setup, 0, 7);
        for (int i = 7; i < setup.length; i++) setup[i] = (byte) (i * 31);
        byte[] both = new byte[comment.length + setup.length];
        System.arraycopy(comment, 0, both, 0, comment.length);
        System.arraycopy(setup, 0, both, comment.length, setup.length);
        List<Integer> lacing = new ArrayList<>();
        OggCheck.lacing(lacing, comment.length);
        OggCheck.lacing(lacing, setup.length);
        int at = 0;
        int index = 0;
        boolean continued = false;
        while (index < lacing.size()) {
            int n = Math.min(255, lacing.size() - index);
            int bytes = 0;
            for (int i = index; i < index + n; i++) bytes += lacing.get(i);
            write(out, OggCheck.page(continued ? 1 : 0, 0, serial, seq++, lacing.subList(index, index + n), both, at, bytes));
            continued = lacing.get(index + n - 1) == 255;
            at += bytes;
            index += n;
        }
        for (int p = 0; p < audioPages; p++) {
            byte[] audio = new byte[audioBytesPerPage];
            for (int i = 0; i < audio.length; i++) audio[i] = (byte) (p * 7 + i);
            List<Integer> lace = new ArrayList<>();
            OggCheck.lacing(lace, audio.length);
            long granule = samples * (p + 1) / audioPages;
            write(out, OggCheck.page(p == audioPages - 1 ? 4 : 0, granule, serial, seq++, lace, audio, 0, audio.length));
        }
        return out.toByteArray();
    }

    static byte[] comment(String tag) {
        ByteArrayOutputStream c = new ByteArrayOutputStream();
        c.writeBytes(new byte[]{3, 'v', 'o', 'r', 'b', 'i', 's'});
        byte[] vendor = "test vendor".getBytes();
        le(c, vendor.length);
        c.writeBytes(vendor);
        le(c, tag == null ? 0 : 1);
        if (tag != null) {
            le(c, tag.length());
            c.writeBytes(tag.getBytes());
        }
        c.write(1);
        return c.toByteArray();
    }

    private static void le(ByteArrayOutputStream c, int v) {
        c.write(v);
        c.write(v >> 8);
        c.write(v >> 16);
        c.write(v >> 24);
    }

    private static void write(ByteArrayOutputStream out, byte[] page) {
        out.write(page, 0, page.length);
    }
}
