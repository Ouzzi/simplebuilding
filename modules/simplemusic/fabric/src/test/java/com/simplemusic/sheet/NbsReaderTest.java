package com.simplemusic.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Note Block Studio import with small files built here (own content, no third-party song). <b>What breaks this test:</b>
 * a header field read in the wrong order (versioned vs. classic format), NBS key 33 no longer being note 0, chords or
 * instruments lost, or an import whose code the note code parser refuses.
 */
class NbsReaderTest {
    /** Little-endian NBS writer for the tests. */
    static final class Nbs {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Nbs s(int v) {
            out.write(v & 0xFF);
            out.write((v >> 8) & 0xFF);
            return this;
        }

        Nbs b(int v) {
            out.write(v & 0xFF);
            return this;
        }

        Nbs i(int v) {
            return s(v).s(v >> 16);
        }

        Nbs str(String text) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            i(bytes.length);
            out.write(bytes, 0, bytes.length);
            return this;
        }

        /** Header after the format specific start: layers, texts, tempo and statistics. */
        Nbs common(String name, int tempo100) {
            s(3).str(name).str("Tester").str("").str("").s(tempo100).b(0).b(0).b(4);
            i(0).i(0).i(0).i(0).i(0).str("");
            return this;
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }

    /** Version 5: tick 0 harp c4+e4+g4 (keys 39, 43, 46), tick 2 bass key 33, tick 4 custom instrument 20. */
    static byte[] version5() {
        Nbs n = new Nbs().s(0).b(5).b(16).s(5);
        n.common("Little Tune", 1000);
        n.b(0).b(0).s(0); // loop, max loops, loop start
        n.s(1); // tick 0
        n.s(1).b(0).b(39).b(100).b(100).s(0);
        n.s(1).b(0).b(43).b(100).b(100).s(0);
        n.s(1).b(0).b(46).b(100).b(100).s(0);
        n.s(0);
        n.s(2); // tick 2
        n.s(1).b(1).b(33).b(100).b(100).s(0);
        n.s(0);
        n.s(2); // tick 4
        n.s(1).b(20).b(45).b(100).b(100).s(0);
        n.s(0);
        n.s(0); // end of notes
        return n.bytes();
    }

    @Test
    void versionedFormat() {
        NbsReader.Import result = NbsReader.read(version5());
        assertEquals("Little Tune", result.title());
        assertEquals(5, result.notes());
        assertEquals(1, result.dropped(), "custom instrument");
        NoteCode.Result parsed = NoteCode.parse(result.code());
        assertTrue(parsed.ok(), () -> result.code() + parsed.problems());
        NoteCode.Song song = parsed.song();
        assertEquals(2, song.tempo(), "10 ticks per second = 2 game ticks per step");
        assertEquals(3, song.steps());
        List<NoteCode.Tone> chord = song.tonesAt(0);
        assertEquals(List.of(6, 10, 13), chord.stream().map(NoteCode.Tone::note).toList());
        assertEquals(1, song.tonesAt(2).size());
        assertEquals(0, song.tonesAt(2).get(0).note());
        assertTrue(result.code().contains("$bass = bass"), result.code());
        assertTrue(result.code().contains("[c4 e4 g4]"), result.code());
    }

    @Test
    void classicFormat() {
        Nbs n = new Nbs().s(8); // song length (classic: first short != 0)
        n.common("Old", 500);
        n.s(1).s(1).b(6).b(50).s(0); // tick 0 flute key 50 = note 17
        n.s(0);
        NbsReader.Import result = NbsReader.read(n.bytes());
        NoteCode.Song song = NoteCode.parse(result.code()).song();
        assertEquals(4, song.tempo(), "5 ticks per second");
        assertEquals(17, song.tonesAt(0).get(0).note());
    }

    @Test
    void foldsKeysAndLimits() {
        Nbs n = new Nbs().s(0).b(4).b(16).s(1);
        n.common("Wide", 2000);
        n.b(0).b(0).s(0);
        n.s(1);
        n.s(1).b(0).b(10).b(100).b(100).s(0); // far below: folded up by octaves
        n.s(1).b(0).b(80).b(100).b(100).s(0); // far above: folded down
        n.s(0);
        n.s(0);
        NoteCode.Song song = NoteCode.parse(NbsReader.read(n.bytes()).code()).song();
        assertEquals(List.of(1, 23), song.tonesAt(0).stream().map(NoteCode.Tone::note).toList());
        assertEquals(1, NbsReader.tempo(2000));
        assertEquals(20, NbsReader.tempo(50));
    }

    @Test
    void brokenFiles() {
        byte[] full = version5();
        assertThrows(IllegalArgumentException.class, () -> NbsReader.read(java.util.Arrays.copyOf(full, 30)));
        assertThrows(IllegalArgumentException.class, () -> NbsReader.read(new Nbs().s(0).b(9).b(16).bytes()));
    }
}
