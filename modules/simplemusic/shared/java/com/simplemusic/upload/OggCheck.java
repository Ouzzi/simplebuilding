package com.simplemusic.upload;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Server check of an uploaded Ogg Vorbis file without a decoder (concept docs/ai/KONZEPT-MUSIKPLATTE-UPLOAD-2026-10-19.md
 * §4; the server has no stb_vorbis). Plain Java, no game classes.
 *
 * <p>Accepted: one logical stream, every page starts with "OggS" (version 0), has a correct CRC and the next sequence
 * number, the first page is the stream start and holds exactly the Vorbis identification header, the comment and the
 * setup header follow, the last page ends the stream and nothing trails it; one channel (owner decision U4: stereo is
 * refused because OpenAL only places mono sources), a sample rate in range, length = granule of the last page / rate
 * within the limit, size within the limit. {@link #strip} rewrites the file with an empty comment header (tags are
 * never stored or sent on).
 */
public final class OggCheck {
    /** Size, length and sample rate limits (server config, owner decision U3: 4 MiB, 5 minutes). */
    public record Limits(int maxBytes, int maxSeconds, int minRate, int maxRate) {
        public static final Limits DEFAULT = new Limits(4 << 20, 300, 22050, 48000);
    }

    /** Facts of an accepted file. */
    public record Info(int channels, int rate, long samples, int bytes) {
        public int seconds() {
            return (int) Math.ceil(samples / (double) rate);
        }
    }

    /**
     * Outcome: {@code info} when accepted, otherwise the problem key {@code simplemusic.upload.error.<key>} and its
     * arguments.
     */
    public record Result(Info info, String key, List<String> args) {
        public boolean ok() {
            return info != null;
        }

        static Result fail(String key, Object... args) {
            List<String> list = new ArrayList<>();
            for (Object a : args) list.add(String.valueOf(a));
            return new Result(null, key, list);
        }
    }

    /** One parsed page. */
    record Page(int offset, int length, int flags, long granule, int serial, int sequence, int[] segments, int dataOffset) {
        boolean continued() {
            return (flags & 1) != 0;
        }

        boolean first() {
            return (flags & 2) != 0;
        }

        boolean last() {
            return (flags & 4) != 0;
        }
    }

    private static final int[] CRC_TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int r = i << 24;
            for (int j = 0; j < 8; j++) r = (r & 0x80000000) != 0 ? (r << 1) ^ 0x04C11DB7 : r << 1;
            CRC_TABLE[i] = r;
        }
    }

    private OggCheck() {}

    /** The Ogg page CRC (polynomial 0x04C11DB7, start 0, not reflected) over {@code data[from, from + length)}. */
    public static int crc(byte[] data, int from, int length) {
        int crc = 0;
        for (int i = from; i < from + length; i++) crc = (crc << 8) ^ CRC_TABLE[((crc >>> 24) ^ data[i]) & 0xFF];
        return crc;
    }

    public static Result check(byte[] data, Limits limits) {
        if (data.length == 0) return Result.fail("empty");
        if (data.length > limits.maxBytes()) return Result.fail("too_big", limits.maxBytes() / 1024 / 1024);
        List<Page> pages = new ArrayList<>();
        String problem = pages(data, pages);
        if (problem != null) return Result.fail(problem);
        Page first = pages.get(0);
        if (!first.first() || first.continued()) return Result.fail("no_start");
        for (int i = 1; i < pages.size(); i++) {
            Page p = pages.get(i);
            if (p.serial() != first.serial() || p.first()) return Result.fail("two_streams");
            if (p.sequence() != first.sequence() + i) return Result.fail("bad_sequence");
        }
        Page end = pages.get(pages.size() - 1);
        if (!end.last()) return Result.fail("truncated");
        for (int i = 0; i < pages.size() - 1; i++) if (pages.get(i).last()) return Result.fail("trailing");
        List<byte[]> headers = headerPackets(data, pages);
        if (headers == null) return Result.fail("no_vorbis");
        byte[] id = headers.get(0);
        if (first.segments().length != 1 || first.segments()[0] != id.length) return Result.fail("no_vorbis");
        if (!vorbis(id, 1) || id.length < 30 || !vorbis(headers.get(1), 3) || !vorbis(headers.get(2), 5)) return Result.fail("no_vorbis");
        if (le32(id, 7) != 0) return Result.fail("no_vorbis");
        int channels = id[11] & 0xFF;
        long rate = le32(id, 12) & 0xFFFFFFFFL;
        if ((id[29] & 1) == 0) return Result.fail("no_vorbis");
        if (channels == 0) return Result.fail("no_vorbis");
        if (channels != 1) return Result.fail("stereo", channels);
        if (rate < limits.minRate() || rate > limits.maxRate()) return Result.fail("bad_rate", rate, limits.minRate(), limits.maxRate());
        long samples = end.granule();
        if (samples <= 0) return Result.fail("truncated");
        if (samples > (long) limits.maxSeconds() * rate) return Result.fail("too_long", limits.maxSeconds() / 60, limits.maxSeconds() % 60);
        return new Result(new Info(channels, (int) rate, samples, data.length), null, List.of());
    }

    /** Splits the file into pages (CRC checked); null or a problem key. */
    static String pages(byte[] data, List<Page> out) {
        int pos = 0;
        while (pos < data.length) {
            if (data.length - pos < 27) return "truncated";
            if (data[pos] != 'O' || data[pos + 1] != 'g' || data[pos + 2] != 'g' || data[pos + 3] != 'S') return out.isEmpty() ? "not_ogg" : "trailing";
            if (data[pos + 4] != 0) return "not_ogg";
            int flags = data[pos + 5] & 0xFF;
            long granule = le64(data, pos + 6);
            int serial = le32(data, pos + 14);
            int sequence = le32(data, pos + 18);
            int crc = le32(data, pos + 22);
            int count = data[pos + 26] & 0xFF;
            if (data.length - pos < 27 + count) return "truncated";
            int[] segments = new int[count];
            int body = 0;
            for (int i = 0; i < count; i++) {
                segments[i] = data[pos + 27 + i] & 0xFF;
                body += segments[i];
            }
            int length = 27 + count + body;
            if (data.length - pos < length) return "truncated";
            byte[] copy = java.util.Arrays.copyOfRange(data, pos, pos + length);
            copy[22] = copy[23] = copy[24] = copy[25] = 0;
            if (crc(copy, 0, length) != crc) return "bad_crc";
            out.add(new Page(pos, length, flags, granule, serial, sequence, segments, pos + 27 + count));
            pos += length;
        }
        return out.isEmpty() ? "empty" : null;
    }

    /** The first three packets (identification, comment, setup), or null when the stream ends before them. */
    static List<byte[]> headerPackets(byte[] data, List<Page> pages) {
        List<byte[]> packets = new ArrayList<>();
        ByteArrayOutputStream current = new ByteArrayOutputStream();
        for (Page page : pages) {
            int at = page.dataOffset();
            for (int seg : page.segments()) {
                current.write(data, at, seg);
                at += seg;
                if (seg < 255) {
                    packets.add(current.toByteArray());
                    current.reset();
                    if (packets.size() == 3) return packets;
                }
            }
        }
        return null;
    }

    private static boolean vorbis(byte[] packet, int type) {
        return packet.length >= 7 && packet[0] == type && packet[1] == 'v' && packet[2] == 'o' && packet[3] == 'r'
                && packet[4] == 'b' && packet[5] == 'i' && packet[6] == 's';
    }

    /** A minimal comment header: no vendor, no tags, framing bit. */
    static byte[] emptyComment() {
        return new byte[]{3, 'v', 'o', 'r', 'b', 'i', 's', 0, 0, 0, 0, 0, 0, 0, 0, 1};
    }

    /**
     * The same stream with an empty comment header (all tags dropped): page 0 unchanged, the comment and setup headers
     * on fresh pages, the audio pages copied with renumbered sequence and new CRC. Only for files {@link #check} accepted.
     */
    public static byte[] strip(byte[] data) {
        List<Page> pages = new ArrayList<>();
        if (pages(data, pages) != null) throw new IllegalArgumentException("not a valid ogg file");
        List<byte[]> headers = headerPackets(data, pages);
        if (headers == null) throw new IllegalArgumentException("no vorbis headers");
        // the page on which the setup header ends: the audio starts on the next page (Vorbis I spec, section 4.3.1)
        int consumed = 0;
        int setupEndPage = -1;
        outer:
        for (int p = 0; p < pages.size(); p++) {
            for (int seg : pages.get(p).segments()) {
                if (seg < 255 && ++consumed == 3) {
                    setupEndPage = p;
                    break outer;
                }
            }
        }
        Page first = pages.get(0);
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        out.write(data, first.offset(), first.length());
        int sequence = first.sequence() + 1;
        byte[] packed = concat(emptyComment(), headers.get(2));
        List<Integer> lacing = new ArrayList<>();
        lacing(lacing, emptyComment().length);
        lacing(lacing, headers.get(2).length);
        int at = 0;
        int index = 0;
        boolean continued = false;
        while (index < lacing.size()) {
            int n = Math.min(255, lacing.size() - index);
            int bytes = 0;
            for (int i = index; i < index + n; i++) bytes += lacing.get(i);
            byte[] page = page(continued ? 1 : 0, 0, first.serial(), sequence++, lacing.subList(index, index + n), packed, at, bytes);
            out.write(page, 0, page.length);
            continued = lacing.get(index + n - 1) == 255;
            at += bytes;
            index += n;
        }
        for (int p = setupEndPage + 1; p < pages.size(); p++) {
            Page page = pages.get(p);
            byte[] copy = java.util.Arrays.copyOfRange(data, page.offset(), page.offset() + page.length());
            put32(copy, 18, sequence++);
            copy[22] = copy[23] = copy[24] = copy[25] = 0;
            put32(copy, 22, crc(copy, 0, copy.length));
            out.write(copy, 0, copy.length);
        }
        return out.toByteArray();
    }

    static void lacing(List<Integer> out, int length) {
        while (length >= 255) {
            out.add(255);
            length -= 255;
        }
        out.add(length);
    }

    /** Builds one page (CRC filled in). */
    static byte[] page(int flags, long granule, int serial, int sequence, List<Integer> segments, byte[] body, int bodyOffset, int bodyLength) {
        byte[] page = new byte[27 + segments.size() + bodyLength];
        page[0] = 'O';
        page[1] = 'g';
        page[2] = 'g';
        page[3] = 'S';
        page[5] = (byte) flags;
        for (int i = 0; i < 8; i++) page[6 + i] = (byte) (granule >>> (8 * i));
        put32(page, 14, serial);
        put32(page, 18, sequence);
        page[26] = (byte) segments.size();
        for (int i = 0; i < segments.size(); i++) page[27 + i] = (byte) (int) segments.get(i);
        System.arraycopy(body, bodyOffset, page, 27 + segments.size(), bodyLength);
        put32(page, 22, crc(page, 0, page.length));
        return page;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    static int le32(byte[] d, int at) {
        return (d[at] & 0xFF) | (d[at + 1] & 0xFF) << 8 | (d[at + 2] & 0xFF) << 16 | (d[at + 3] & 0xFF) << 24;
    }

    static long le64(byte[] d, int at) {
        return (le32(d, at) & 0xFFFFFFFFL) | ((long) le32(d, at + 4)) << 32;
    }

    static void put32(byte[] d, int at, int v) {
        d[at] = (byte) v;
        d[at + 1] = (byte) (v >>> 8);
        d[at + 2] = (byte) (v >>> 16);
        d[at + 3] = (byte) (v >>> 24);
    }
}
