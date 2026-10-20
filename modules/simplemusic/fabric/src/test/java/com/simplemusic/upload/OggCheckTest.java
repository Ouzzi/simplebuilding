package com.simplemusic.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Server-side Ogg check (concept §4/§8) with synthetic files from {@link OggFiles}. <b>What breaks this test:</b> a CRC
 * or page check that no longer fires (corrupt, cut off, second stream, trailing bytes), stereo accepted (owner
 * decision U4), size/length/rate limits ignored (U3), or tags surviving {@link OggCheck#strip}.
 */
class OggCheckTest {
    private static final OggCheck.Limits LIMITS = OggCheck.Limits.DEFAULT;

    private static String key(byte[] file) {
        OggCheck.Result r = OggCheck.check(file, LIMITS);
        return r.ok() ? "ok" : r.key();
    }

    @Test
    void validMono() {
        byte[] file = OggFiles.mono().build();
        OggCheck.Result r = OggCheck.check(file, LIMITS);
        assertTrue(r.ok(), r::key);
        assertEquals(1, r.info().channels());
        assertEquals(44100, r.info().rate());
        assertEquals(10, r.info().seconds());
        assertEquals(file.length, r.info().bytes());
    }

    @Test
    void longSetupHeaderOverSeveralPages() {
        OggFiles f = OggFiles.mono();
        f.setupBytes = 70000;
        assertEquals("ok", key(f.build()));
    }

    @Test
    void stereoRefused() {
        OggFiles f = OggFiles.mono();
        f.channels = 2;
        OggCheck.Result r = OggCheck.check(f.build(), LIMITS);
        assertEquals("stereo", r.key());
        assertEquals(List.of("2"), r.args());
    }

    @Test
    void badCrc() {
        byte[] file = OggFiles.mono().build();
        file[file.length - 5] ^= 0x40;
        assertEquals("bad_crc", key(file));
    }

    @Test
    void truncated() {
        byte[] file = OggFiles.mono().build();
        assertEquals("truncated", key(Arrays.copyOf(file, file.length - 10)));
        assertEquals("truncated", key(Arrays.copyOf(file, 20)));
    }

    @Test
    void secondStream() {
        OggFiles other = OggFiles.mono();
        other.serial = 77;
        byte[] a = OggFiles.mono().build();
        byte[] b = other.build();
        byte[] both = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, both, a.length, b.length);
        // the first stream already ended: a second stream after it counts as two streams, not as a clean end
        assertTrue(List.of("two_streams", "trailing").contains(key(both)), key(both));
    }

    @Test
    void trailingBytes() {
        byte[] file = OggFiles.mono().build();
        byte[] longer = Arrays.copyOf(file, file.length + 40);
        assertEquals("trailing", key(longer));
    }

    @Test
    void tooLong() {
        OggFiles f = OggFiles.mono();
        f.samples = 44100L * 301;
        OggCheck.Result r = OggCheck.check(f.build(), LIMITS);
        assertEquals("too_long", r.key());
        assertEquals(List.of("5", "0"), r.args());
        f.samples = 44100L * 300;
        assertEquals("ok", key(f.build()));
    }

    @Test
    void tooBig() {
        OggFiles f = OggFiles.mono();
        f.audioPages = 3;
        f.audioBytesPerPage = 60000;
        byte[] file = f.build();
        assertEquals("too_big", OggCheck.check(file, new OggCheck.Limits(100_000, 300, 22050, 48000)).key());
        assertEquals("ok", key(file));
    }

    @Test
    void wrongRate() {
        OggFiles f = OggFiles.mono();
        f.rate = 8000;
        assertEquals("bad_rate", key(f.build()));
        f.rate = 96000;
        assertEquals("bad_rate", key(f.build()));
    }

    @Test
    void notVorbis() {
        assertEquals("not_ogg", key("RIFF....WAVEfmt this is not an ogg file at all".getBytes(StandardCharsets.US_ASCII)));
        assertEquals("empty", key(new byte[0]));
        byte[] file = OggFiles.mono().build();
        // damage the "vorbis" magic of the identification header and fix the page CRC again
        file[27 + 1 + 1] = 'X';
        file[22] = file[23] = file[24] = file[25] = 0;
        OggCheck.put32(file, 22, OggCheck.crc(file, 0, 27 + 1 + 30));
        assertEquals("no_vorbis", key(file));
    }

    @Test
    void stripRemovesTags() {
        OggFiles f = OggFiles.mono();
        byte[] file = f.build();
        assertTrue(new String(file, StandardCharsets.ISO_8859_1).contains("ARTIST=Somebody"));
        byte[] clean = OggCheck.strip(file);
        assertFalse(new String(clean, StandardCharsets.ISO_8859_1).contains("ARTIST"));
        assertFalse(new String(clean, StandardCharsets.ISO_8859_1).contains("test vendor"));
        OggCheck.Result r = OggCheck.check(clean, LIMITS);
        assertTrue(r.ok(), r::key);
        assertEquals(10, r.info().seconds());
        assertArrayEquals(OggCheck.strip(clean), clean, "stripping is stable");
        f.setupBytes = 70000;
        assertTrue(OggCheck.check(OggCheck.strip(f.build()), LIMITS).ok());
    }
}
